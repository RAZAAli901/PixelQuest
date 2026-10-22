// Run with: node --test "supabase/functions/**/*.test.ts"
import { test } from "node:test";
import assert from "node:assert/strict";
import { functionOf, judge } from "./function_errors.ts";

test("monitor: the function is read from the request path", () => {
  assert.equal(functionOf("/functions/v1/chess-move"), "chess-move");
  assert.equal(functionOf("/chess-move"), "chess-move");
});

test("monitor: a few errors in a busy hour aren't a spike; a share of 5% or 20 errors are", () => {
  const verdicts = judge([
    { path: "/functions/v1/chess-move", status: 200, n: 1000 },
    { path: "/functions/v1/chess-move", status: 500, n: 6 },
    { path: "/functions/v1/send-push", status: 200, n: 40 },
    { path: "/functions/v1/send-push", status: 502, n: 5 },
    { path: "/functions/v1/gemini-proxy", status: 200, n: 9000 },
    { path: "/functions/v1/gemini-proxy", status: 503, n: 20 },
    { path: "/functions/v1/request-upload", status: 429, n: 50 },
  ]);
  const alert = Object.fromEntries(verdicts.map((v) => [v.fn, v.alert]));
  assert.deepEqual(alert, { "chess-move": false, "send-push": true, "gemini-proxy": true, "request-upload": false });
  assert.deepEqual(verdicts.find((v) => v.fn === "send-push"), { fn: "send-push", requests: 45, errors: 5, alert: true });
});

test("monitor: four errors, even out of four, are not yet a spike", () => {
  assert.equal(judge([{ path: "/functions/v1/chess-sweep", status: 500, n: 4 }])[0].alert, false);
});
