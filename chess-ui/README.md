# PixelQuest Chess: design document

Open **PixelQuest Chess.dc.html** in a browser (Chrome works best) to see the full spec and every screen.

For Claude Code: put this folder next to claude-ui-assets/ in the PixelQuest project, then say:

"Read chess-design/PixelQuest Chess.dc.html (the specs are in its logic class and section text; the screens are in PQChess.dc.html, which has one block per screen and comic/pixel tokens). Implement the Chess feature in Compose using the existing PqTheme tokens and components from claude-ui-assets, following the same patterns as the Parties and Study party screens."

Files:
- PixelQuest Chess.dc.html: the design document (time controls, Int rating, head-to-head, spectating, flow, edge cases, data model)
- PQChess.dc.html: the phone screens (screen + skin props): hub, challenge, incoming, game, over, spectate, h2h, party, rating, notifs
- PQIcon / PQNav / PQNavPixel: shared pieces used by the screens
- support.js, _ds/: runtime and document styles
