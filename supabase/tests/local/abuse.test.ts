// Abuse controls (J172, docs/ABUSE_REVIEW.md): the limits in force are the ones the review lists,
// the challenge rate limit holds, and a chess challenge can be reported and its sender blocked.
// Run with the local stack: SUPABASE_LOCAL=1 node --test supabase/tests/local/abuse.test.ts
import assert from "node:assert/strict";
import { call, localTest, sql } from "./stack.ts";
import { chessCast, endGames, rpc, studyParty, type Player } from "./chess_rls.test.ts";

localTest("abuse: the limits in force are the review's", () => {
  const chat = JSON.parse(sql(`SELECT row_to_json(c) FROM public.study_chat_config c`));
  assert.equal(chat.messages_per_minute, 30);
  assert.equal(chat.uploads_per_day, 20);
  assert.equal(chat.file_max_bytes, 20 * 1024 * 1024);
  assert.equal(chat.party_max_bytes, 500 * 1024 * 1024);
  const chess = JSON.parse(sql(`SELECT row_to_json(c) FROM public.chess_config c`));
  assert.equal(chess.challenges_per_hour, 10);
  assert.equal(chess.challenge_minutes, 10);
  assert.equal(chess.moves_per_second, 5);
  assert.equal(chess.spectator_joins_per_hour, 20);
  assert.equal(chess.pair_rated_per_day, 10);
});

localTest("abuse: 10 challenges an hour each; the 11th is refused", async () => {
  const { white, black } = await chessCast();
  endGames(white, black);
  const party = await studyParty(white, [black]);
  sql(`DELETE FROM public.chess_challenges WHERE from_user = '${white.userId}'`);
  sql(`INSERT INTO public.chess_challenges (party_id, from_user, time_control_sec, expires_at, status)
       SELECT '${party}', '${white.userId}', 300, now() - interval '1 minute', 'expired' FROM generate_series(1, 10)`);
  assert.throws(() => sql(`SELECT public.chess_create_challenge('${white.userId}', '${party}', NULL, 300, 'random', true)`), /rate_limited/);
  // An hour on, it's allowed again.
  sql(`UPDATE public.chess_challenges SET created_at = now() - interval '61 minutes' WHERE from_user = '${white.userId}'`);
  assert.ok(sql(`SELECT public.chess_create_challenge('${white.userId}', '${party}', NULL, 300, 'random', true) ->> 'id'`));
  sql(`DELETE FROM public.chess_challenges WHERE from_user = '${white.userId}'`);
});

localTest("abuse: a chess challenge is reported by a party mate, and its sender blocked", async () => {
  const { white, black, third, outsider } = await chessCast();
  endGames(white, black, third);
  const party = await studyParty(white, [black, third]);
  sql(`DELETE FROM public.chess_challenges WHERE from_user IN ('${white.userId}', '${black.userId}')`);
  sql(`DELETE FROM public.member_blocks WHERE blocker_id = '${black.userId}'`);
  const challenge = (from: Player, to: Player | null) =>
    sql(`SELECT public.chess_create_challenge('${from.userId}', '${party}', ${to ? `'${to.userId}'` : "NULL"}, 300, 'white', true) ->> 'id'`);
  const sent = challenge(white, black);

  const report = (who: Player, reason = "abuse") => rpc(who, "report_chess_challenge", { challenge_id: sent, reason });
  assert.equal((await report(black)).status, 204);
  assert.equal((await report(black)).status, 204, "again is fine");
  assert.equal((await report(third, "spam")).status, 204, "an open room's mate may too");
  assert.equal(sql(`SELECT count(*) FROM public.chess_reports WHERE challenge_id = '${sent}'`), "2");
  assert.equal(sql(`SELECT DISTINCT reported_id FROM public.chess_reports WHERE challenge_id = '${sent}'`), white.userId, "it names the challenger");
  assert.equal((await report(white)).body.message, "not_allowed", "not one's own");
  assert.equal((await report(outsider)).body.message, "not_allowed", "not from outside the party");
  assert.equal((await report(black, "made_up")).body.message, "bad_reason");
  assert.equal((await rpc(undefined, "report_chess_challenge", { challenge_id: sent, reason: "spam" })).status, 401);
  assert.equal((await call("/rest/v1/chess_reports?select=id", { token: black.accessToken })).status, 403, "reports aren't read back");

  // Blocked: the sender can't challenge them again, nor they the sender.
  sql(`UPDATE public.chess_challenges SET status = 'cancelled' WHERE id = '${sent}'`);
  assert.equal((await rpc(black, "block_member", { user_id: white.userId })).status, 204);
  assert.throws(() => challenge(white, black), /blocked/);
  assert.throws(() => challenge(black, white), /blocked/);
  assert.equal((await rpc(black, "unblock_member", { user_id: white.userId })).status, 204);
  sql(`DELETE FROM public.chess_challenges WHERE from_user IN ('${white.userId}', '${black.userId}')`);
});
