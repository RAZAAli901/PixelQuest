// Phase F's load test (J171): one study party of 30 members, 5 live games (10 players) and 20
// members watching (4 to a game) on the games' private realtime channels. Every watcher must hear
// every move of their game, within the realtime limits (supabase/config.toml [realtime]).
// Slow (about a minute): run with LOAD=1 SUPABASE_LOCAL=1 node --test supabase/tests/local/load.test.ts
import assert from "node:assert/strict";
import { test } from "node:test";
import { call, newPlayer, sql, stack, type Session } from "./stack.ts";

const RUN = process.env.SUPABASE_LOCAL === "1" && process.env.LOAD === "1";
const fn = (name: string, who: Session, body: unknown) => call(`/functions/v1/${name}`, { token: who.accessToken, body });
const rpc = (who: Session, name: string, body: unknown) => call(`/rest/v1/rpc/${name}`, { token: who.accessToken, body });
const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

/** A member listening on a game's private channel: counts the "game" broadcasts it hears. */
function listen(who: Session, game: string): Promise<{ heard: () => number; close: () => void }> {
  const s = stack();
  const url = `${s.apiUrl.replace(/^http/, "ws")}/realtime/v1/websocket?apikey=${s.publishableKey}&vsn=1.0.0`;
  const ws = new WebSocket(url);
  let heard = 0;
  let ref = 1;
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error("join timed out")), 15_000);
    ws.onopen = () => {
      ws.send(JSON.stringify({
        topic: `realtime:chess:game:${game}`, event: "phx_join", ref: String(ref++),
        payload: { config: { broadcast: { self: false }, presence: { key: who.userId }, private: true }, access_token: who.accessToken },
      }));
    };
    ws.onmessage = (event) => {
      const msg = JSON.parse(String(event.data));
      if (msg.event === "phx_reply" && msg.payload?.status === "ok" && msg.ref === "1") {
        clearTimeout(timer);
        // Heartbeats keep the socket alive through the test.
        const beat = setInterval(() => ws.send(JSON.stringify({ topic: "phoenix", event: "heartbeat", payload: {}, ref: String(ref++) })), 20_000);
        resolve({ heard: () => heard, close: () => { clearInterval(beat); ws.close(); } });
      } else if (msg.event === "phx_reply" && msg.payload?.status === "error") {
        clearTimeout(timer);
        reject(new Error(`join refused: ${JSON.stringify(msg.payload)}`));
      } else if (msg.event === "broadcast" && msg.payload?.event === "game") {
        heard++;
      }
    };
    ws.onerror = (e) => reject(e as unknown as Error);
  });
}

(RUN ? test : test.skip)("load: 30 members, 5 live games, 20 watching, every move heard", { timeout: 300_000 }, async () => {
  const members: (Session & { username: string })[] = [];
  for (let i = 0; i < 30; i++) {
    const session = await newPlayer(`load_${i}`);
    const username = `ld_${i}_${Math.random().toString(36).slice(2, 7)}`;
    assert.equal((await rpc(session, "set_username", { name: username })).body?.status, "ok");
    members.push({ ...session, username });
  }
  const leader = members[0];
  const made = await rpc(leader, "create_party", { name: `Load ${Date.now()}`, max_members: 30, party_type: "study" });
  assert.equal(made.status, 200, JSON.stringify(made.body));
  const party = made.body as string;
  for (const m of members.slice(1)) {
    sql(`DELETE FROM private.invite_log WHERE inviter = '${leader.userId}'`);
    await rpc(leader, "invite_member", { party_id: party, username: m.username });
    await rpc(m, "respond_invite", { party_id: party, accept: true });
  }
  assert.equal(sql(`SELECT count(*) FROM public.party_members WHERE party_id = '${party}' AND status = 'active'`), "30");

  // 5 games: members 0-9 in pairs.
  const games: { id: string; white: Session; black: Session }[] = [];
  for (let g = 0; g < 5; g++) {
    const [white, black] = [members[g * 2], members[g * 2 + 1]];
    const c = await fn("chess-create-challenge", white, { party_id: party, to_user: black.userId, time_control: 600, colour: "white" });
    assert.equal(c.status, 200, JSON.stringify(c.body));
    const a = await fn("chess-respond-challenge", black, { challenge_id: c.body.challenge.id, accept: true });
    assert.equal(a.status, 200, JSON.stringify(a.body));
    games.push({ id: a.body.game.id, white, black });
  }

  // 20 watchers, 4 to a game: each logs the watch, then joins the game's private channel.
  const watchers = [] as { game: number; ear: Awaited<ReturnType<typeof listen>> }[];
  for (let w = 0; w < 20; w++) {
    const who = members[10 + w];
    const g = w % 5;
    assert.equal((await rpc(who, "chess_watch", { game_id: games[g].id })).body.status, "watching");
    watchers.push({ game: g, ear: await listen(who, games[g].id) });
  }

  // The same opening in every game, played in turn (5 moves a second at most per game).
  const line = ["e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6", "b5a4", "g8f6"];
  for (const [i, uci] of line.entries()) {
    await Promise.all(games.map(async (g) => {
      const res = await fn("chess-move", i % 2 === 0 ? g.white : g.black, { game_id: g.id, ply: i + 1, uci });
      assert.equal(res.status, 200, `${uci}: ${JSON.stringify(res.body)}`);
    }));
    await sleep(250);
  }
  await sleep(3_000);

  const counts = watchers.map((w) => w.ear.heard());
  watchers.forEach((w) => w.ear.close());
  // Every watcher heard every move of their game (one broadcast a move, at least).
  for (const [i, heard] of counts.entries()) assert.ok(heard >= line.length, `watcher ${i} heard ${heard} of ${line.length}`);
  for (const g of games) sql(`SELECT private.chess_finish('${g.id}', 'draw', 'draw_agreed')`);
});
