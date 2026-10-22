// The functions' logs (J175, docs/MONITORING.md): one JSON line per event, with no personal data.
// Events carry only what's needed to see that something is wrong: the function, the event, a
// reason token or a status. Anything that reaches a log as text (an error's message above all,
// which can echo a database row) is scrubbed first: ids, emails, tokens and long numbers are
// replaced, and it's cut short.

export type LogEvent = Record<string, unknown>;

const UUID = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/gi;
const EMAIL = /[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g;
// JWTs, API keys, push tokens: long runs of token characters.
const TOKEN = /[A-Za-z0-9_\-:.]{32,}/g;
const NUMBER = /\d{7,}/g;
const MAX = 200;

/** [text] with ids, emails, tokens and long numbers taken out, at most 200 characters. */
export function scrub(text: string): string {
  const clean = text.replace(UUID, "<id>").replace(EMAIL, "<email>").replace(TOKEN, "<token>").replace(NUMBER, "<n>");
  return clean.length > MAX ? `${clean.slice(0, MAX)}…` : clean;
}

/** An error, for a log: its kind and scrubbed message. */
export function errorText(e: unknown): string {
  return scrub(e instanceof Error ? `${e.name}: ${e.message}` : String(e));
}

/** Events that mean something failed: logged at error level, which the monitor counts. */
export function isFailure(event: string): boolean {
  return /(_failed|_error|_unavailable|_unreachable)$/.test(event);
}

/** The line written for [event] in [fn]: its string values scrubbed. */
export function line(fn: string, event: LogEvent): string {
  const out: LogEvent = { fn };
  for (const [key, value] of Object.entries(event)) {
    out[key] = typeof value === "string" ? scrub(value) : value;
  }
  return JSON.stringify(out);
}

/** The log function each index.ts hands its handler. */
export function logger(fn: string, write: { log: (s: string) => void; error: (s: string) => void } = console) {
  return (event: LogEvent) => {
    const text = line(fn, event);
    if (isFailure(String(event.event ?? ""))) write.error(text);
    else write.log(text);
  };
}
