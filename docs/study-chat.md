# Study chat

Each study party ([study-party.md](study-party.md)) has a chat: a Chat tab of messages, styled like the Darood chat but with ordinary text, and a Files tab of the quiz and study material shared in it. Like everything in a study party, it never touches points, XP, levels or streaks.

## Messages

| Field | Rules |
|---|---|
| `kind` | `text`, `file` (a shared file's message) or `system` (the server's own lines, such as a file rejected) |
| `body` | Text: 1 to 2,000 characters, trimmed. A file message's body is empty; a system line's is a token the app words |
| `file_id` | A file message's file |
| `reply_to` | Optional: a message in the same party it answers |
| `sender_id`, `created_at` | Set by the server |
| `deleted_at` | Set when it's deleted: its text and file are gone for everyone, a "message deleted" line stays |

- **Who reads:** only the party's active members. Outsiders, the invited, members who left or were removed, and the signed out read nothing, from the moment they stop being members.
- **Who writes:** an active member sends as themselves. Nobody edits a message, theirs or anyone's.
- **Deleting:** the sender deletes their own; the leader deletes any in their party. A delete is soft (`deleted_at`), so replies still make sense.
- **Unread:** each member's `last_read_at` per party (`chat_reads`) gives the unread divider and counts.
- **Limits:** 30 messages a minute per member.

## Files

| Allowed | PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, TXT, CSV, PNG, JPG |
|---|---|
| Blocked | Everything else: macro documents (docm, xlsm, pptm), zip and other archives, apps (apk, exe), scripts, HTML, SVG |
| Size | Up to 20 MB a file, and 500 MB a party |
| Uploads | 20 per member per day |
| Name | Up to 120 characters, path characters stripped |

These live in one `StudyChatConfig` on the phone and one mirrored table on the server, `study_chat_config`. The phone's copy is only for clear messages before uploading; the server's decides.

### How a file is shared

1. **Ask** (`request-upload`, an Edge Function): the member says the file's name, type and size. The server checks membership, the type against the allow-list, the size, the party's storage and the member's uploads today, then makes a `pending` file row and a signed upload URL valid for 5 minutes, for one path: `<party_id>/<uuid>` in the private bucket `study-files`.
2. **Upload:** the phone sends the bytes straight to that URL. Nothing can read them yet.
3. **Check** (`finalize-upload`): the server reads the stored object, not the phone's word for it: its real size, and its first bytes (magic numbers) against the allow-list (a PDF starts `%PDF-`, a PNG `\x89PNG`, Office files are a zip holding the right parts and no macros, old Office files are OLE without macros, text is valid UTF-8 without control characters). It works out its SHA-256, marks the file `ready` and posts the file message.
4. **Rejected:** anything that fails is deleted from storage, its row marked `rejected`, and the uploader sees why. A `pending` upload never finished is deleted after an hour.
5. **Open** (`get-download-url`): a member asks for a file; the server checks membership again and returns a signed URL valid for 60 seconds, served as an attachment (`Content-Disposition: attachment`), never inline. The phone saves it in its own private storage and hands it to the system viewer through a FileProvider. It never opens a file by itself, and warns the first time a member opens one from a party.

## Threat model for uploads

Every file is hostile until checked, and still someone else's after.

| Threat | Defence |
|---|---|
| An app or script renamed `.pdf` | The type is the content's, by magic bytes, never the name or the declared type; anything not on the allow-list is rejected |
| A zip, or an Office file carrying macros | Archives are blocked; an Office zip must hold the expected parts and no `vbaProject.bin`; old OLE documents with macro streams are rejected |
| A file too big, or a party filling storage | Declared size checked before the URL is given; real size checked after; the party's total is capped at 500 MB by a trigger |
| Upload floods | 20 uploads a member a day, counted on the server; a signed URL is for one path and 5 minutes |
| Path tricks in the name (`../`, `\`, control characters) | Storage paths are the server's own (`<party_id>/<uuid>`); the shown name is cleaned and cut to 120 characters |
| Reading another party's files | The bucket is private: no public URLs. Storage policies and every function check active membership in the file's party |
| A link shared on | Download URLs last 60 seconds |
| A removed member | Membership is checked on every read and every URL, so access ends at once; URLs already given end within a minute |
| A file opening by itself, or running in a web view | Served as an attachment; the app never auto-opens and never renders files in a web view, only hands them to the system viewer the member chooses |
| Leaking content on the lock screen | Chat notifications hide their text on the lock screen |
| Abuse | Messages and files can be reported (the report flow) and blocked members' messages are hidden |

## On the phone

- **Cache:** messages, files and read state are kept in Room, so the chat opens offline. A text written offline waits and is sent with its own key when there's a connection, so it's never stored twice.
- **Live:** new messages, deletes and files becoming ready arrive while the chat is open (Realtime, checked against each listener's row-level security).
- **Notifications:** new messages and files, on their own channel, Study chat, with a per-party mute; text hidden on the lock screen.
