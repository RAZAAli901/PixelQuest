// Run with: node --test "supabase/functions/**/*.test.ts"
import { test } from "node:test";
import assert from "node:assert/strict";
import { errorText, isFailure, line, logger, scrub } from "./log.ts";

test("log: ids, emails, tokens and long numbers are taken out", () => {
  const text = scrub(
    'duplicate key (user_id)=(3f2b8c1e-1a2b-4c3d-9e8f-0a1b2c3d4e5f) for aria.k@example.com with eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.abc and 923001234567',
  );
  assert.equal(text, "duplicate key (user_id)=(<id>) for <email> with <token> and <n>");
});

test("log: long text is cut short", () => {
  assert.equal(scrub("x ".repeat(300)).length, 201);
});

test("log: an error is its kind and its scrubbed message", () => {
  assert.equal(errorText(new TypeError("no row for 3f2b8c1e-1a2b-4c3d-9e8f-0a1b2c3d4e5f")), "TypeError: no row for <id>");
  assert.equal(errorText("plain"), "plain");
});

test("log: each line names the function and scrubs its strings, keeping numbers", () => {
  assert.equal(
    line("chess-move", { event: "chess_failed", error: "row 3f2b8c1e-1a2b-4c3d-9e8f-0a1b2c3d4e5f", ply: 12 }),
    '{"fn":"chess-move","event":"chess_failed","error":"row <id>","ply":12}',
  );
});

test("log: failures go to the error level, everything else to the log", () => {
  const seen: string[] = [];
  const log = logger("send-push", { log: (s) => seen.push(`log ${s}`), error: (s) => seen.push(`error ${s}`) });
  log({ event: "push_failed", status: 500 });
  log({ event: "push_sent", count: 2 });
  assert.deepEqual(seen, ['error {"fn":"send-push","event":"push_failed","status":500}', 'log {"fn":"send-push","event":"push_sent","count":2}']);
  assert.ok(isFailure("upstream_error") && isFailure("auth_unavailable") && isFailure("upstream_unreachable"));
  assert.ok(!isFailure("limit") && !isFailure("upload_rejected"));
});
