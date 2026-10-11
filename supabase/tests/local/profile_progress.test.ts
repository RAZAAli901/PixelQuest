// The leaderboard's plausibility rules (migration 20261011000000_profile_progress_plausibility.sql),
// against a local Postgres. Players write their rows the way CloudProfileRepository does.
import assert from "node:assert/strict";
import { asService, call, localTest, newPlayer, sql, type Session } from "./stack.ts";

const upsertHeaders = { Prefer: "resolution=merge-duplicates,return=representation" };

interface Stats {
  current_streak: number;
  longest_streak: number;
  level: number;
  total_xp: number;
}

async function upload(player: Session, stats: Partial<Stats> & Record<string, unknown>): Promise<Stats & { updated_at: string }> {
  const res = await call("/rest/v1/profiles?on_conflict=id", {
    token: player.accessToken,
    body: { id: player.userId, display_name: `Hero_${player.userId.slice(0, 6)}`, leaderboard_opt_in: true, ...stats },
    headers: upsertHeaders,
  });
  assert.ok([200, 201].includes(res.status), `upload ${res.status} ${JSON.stringify(res.body)}`);
  return res.body[0];
}

const statsOf = (row: Stats) => ({
  current_streak: row.current_streak,
  longest_streak: row.longest_streak,
  level: row.level,
  total_xp: row.total_xp,
});

/** Pretends the player's streak (or level) last changed [days] ago. */
function daysAgo(player: Session, column: "streak_changed_at" | "level_changed_at", days: number) {
  sql(`UPDATE public.profile_progress_marks SET ${column} = now() - interval '${days} days' WHERE id = '${player.userId}'`);
}

localTest("a first upload keeps a believable record, however long the player played before signing in", async () => {
  const veteran = await newPlayer("veteran");
  const record = { current_streak: 200, longest_streak: 250, level: 20, total_xp: 12_500 };

  assert.deepEqual(statsOf(await upload(veteran, record)), record);
});

localTest("numbers the app can't produce are lowered, not refused", async () => {
  const cheat = await newPlayer("cheat");
  // 600 XP is 12 quests at most: no more than 12 perfect days, and level 5 (3 perfect days a level).
  const row = await upload(cheat, { current_streak: 50, longest_streak: 40, level: 30, total_xp: 600 });
  assert.deepEqual(statsOf(row), { current_streak: 12, longest_streak: 12, level: 5, total_xp: 600 });

  // The most the app's own backup import allows.
  const huge = await upload(cheat, { current_streak: 0, longest_streak: 0, level: 1, total_xp: 2_000_000_000 });
  assert.equal(huge.total_xp, 10_000_000);
});

localTest("the server's clock is stored, not the app's", async () => {
  const player = await newPlayer("clock");
  const row = await upload(player, { current_streak: 1, longest_streak: 1, level: 1, total_xp: 50, updated_at: "2999-01-01T00:00:00Z" });
  assert.ok(Math.abs(Date.parse(row.updated_at) - Date.now()) < 10 * 60_000, `updated_at ${row.updated_at}`);
});

localTest("a streak grows by one a day at most, with two days of slack", async () => {
  const player = await newPlayer("streak");
  await upload(player, { current_streak: 5, longest_streak: 5, level: 1, total_xp: 10_000 });

  // Same day: 25 more days of streak can't be real.
  const sameDay = await upload(player, { current_streak: 30, longest_streak: 30 });
  assert.deepEqual([sameDay.current_streak, sameDay.longest_streak], [7, 7]);

  // Ten days later: up to twelve more.
  daysAgo(player, "streak_changed_at", 10);
  const later = await upload(player, { current_streak: 30, longest_streak: 30 });
  assert.deepEqual([later.current_streak, later.longest_streak], [19, 19]);

  // The rest arrives with the following days' syncs, as an honest catch-up would.
  daysAgo(player, "streak_changed_at", 9);
  const caughtUp = await upload(player, { current_streak: 30, longest_streak: 30 });
  assert.deepEqual([caughtUp.current_streak, caughtUp.longest_streak], [30, 30]);
});

localTest("a level takes three days, one sooner with progress carried over", async () => {
  const player = await newPlayer("level");
  await upload(player, { current_streak: 0, longest_streak: 0, level: 2, total_xp: 30_000 });

  assert.equal((await upload(player, { level: 10 })).level, 3);

  daysAgo(player, "level_changed_at", 30);
  assert.equal((await upload(player, { level: 10 })).level, 10);
});

localTest("dropping back is always accepted (RESET ALL PROGRESS, an older backup)", async () => {
  const player = await newPlayer("reset");
  await upload(player, { current_streak: 9, longest_streak: 12, level: 4, total_xp: 2_000 });

  const reset = { current_streak: 0, longest_streak: 0, level: 1, total_xp: 0 };
  assert.deepEqual(statsOf(await upload(player, reset)), reset);
});

localTest("leaving the board for months doesn't hold the streak back when the player returns", async () => {
  const player = await newPlayer("returning");
  await upload(player, { current_streak: 5, longest_streak: 5, level: 1, total_xp: 10_000 });
  daysAgo(player, "streak_changed_at", 100);

  // While off the board the app only clears the flag (CloudProfileRepository.optOutFromLeaderboard).
  const off = await call(`/rest/v1/profiles?id=eq.${player.userId}`, {
    method: "PATCH",
    token: player.accessToken,
    body: { leaderboard_opt_in: false },
  });
  assert.equal(off.status, 204);

  const back = await upload(player, { current_streak: 104, longest_streak: 104, leaderboard_opt_in: true });
  assert.deepEqual([back.current_streak, back.longest_streak], [104, 104]);
});

localTest("players can't read or change when their streak last changed", async () => {
  const player = await newPlayer("marks");
  await upload(player, { current_streak: 1, longest_streak: 1, level: 1, total_xp: 50 });

  const read = await call(`/rest/v1/profile_progress_marks?id=eq.${player.userId}`, { token: player.accessToken });
  assert.ok(read.status >= 400, `read ${read.status}`);
  const write = await call(`/rest/v1/profile_progress_marks?id=eq.${player.userId}`, {
    method: "PATCH",
    token: player.accessToken,
    body: { streak_changed_at: "2000-01-01T00:00:00Z" },
  });
  assert.ok(write.status >= 400, `write ${write.status}`);
});

localTest("the service role (the project's own code and this bench) is not limited", async () => {
  const player = await newPlayer("service");
  const set = await asService("/rest/v1/profiles?on_conflict=id", {
    body: { id: player.userId, display_name: `Hero_${player.userId.slice(0, 6)}`, level: 600, total_xp: 0 },
    headers: upsertHeaders,
  });
  assert.ok([200, 201].includes(set.status), `service ${set.status}`);
  assert.equal(set.body[0].level, 600);
});
