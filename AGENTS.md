# Chicken's Tech Shop

Chickenboy's mod (a tech market for loot items, tied to the contact Chicken). This is Andrzej's
fork at `github.com/andrzejsokolowski/ChickenTechShop`. These instructions apply to this
repository, including when it is opened without the parent mod workspace. The workspace
`AGENTS.md` one folder up governs Git identity and communication.

## Notion

Tasks and history: the **Chicken's Tech Shop board** in Andrzej's Notion, under
**Starsector mods** (`collection://20ec1504-df97-4a46-a9d2-820c840bcaa8`). Working rules and game
API notes: **Claude Information** (`collection://7bf9b1d6-b9e5-4b62-87f1-757bbaa51919`), starting
with "Start here: working on Starsector mods through Notion". Read the board before working, give
every task a ticket (ticket ids start with CTS) and keep it current. The Notion tools are
deferred: load them with ToolSearch first.

Update the board during the work, not only at release time. Before changing files, put the
message on its ticket: a new request gets a new ticket (In progress), and a problem Andrzej
reports on Needs Human Test work moves that ticket to Changes Requested with the notes quoted.
Before ending the turn, update the ticket's body (What was done, How, Commits, Test list) and
its Status, Version and End date. Read the board with `notion-query-data-sources` mode `view`
on `view://3fd20ca7-5d20-81db-83aa-000c4f1b3d5b` or `notion-fetch` a ticket. SQL and rows mode share a
small quota that runs out. The workspace `AGENTS.md` has the full routine.

## Essentials

- id `chickentechshop`, author Chickenboy. Most of the history is the original author's work:
  keep changes small and in the existing style.
- Build: `./gradlew jar`. The Gradle `release` task builds the ZIP.
- LunaLib is an optional dependency (the starting level setting).
