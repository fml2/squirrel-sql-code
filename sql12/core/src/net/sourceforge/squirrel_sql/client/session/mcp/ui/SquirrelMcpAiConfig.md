# SQuirreL SQL — MCP access for AI assistants

This document tells an AI assistant how to talk to the MCP server built into the
[SQuirreL SQL client](https://squirrelsql.org). Through it you can read database
metadata (tables, columns, keys, indexes) and run SQL against the database
connection of one open **SQuirreL Session**.

## What it is

- Each SQuirreL **Session** (one open JDBC connection) can start its **own** MCP
  server on its **own** TCP port. The Session UI shows that port; the user gives
  it to you.
- Transport: **JSON-RPC 2.0 over HTTP POST** ("Streamable HTTP", non-streaming —
  every request gets a single `application/json` response). Only `POST` is
  accepted; `GET` returns `405`. The server is stateless (no `Mcp-Session-Id`).
- MCP protocol version: `2025-06-18`.
- Endpoint: `http://<host>:<PORT>/squirrel-sql-mcp`
  - `<host>` = `127.0.0.1` when SQuirreL runs on the **same machine** as you
    (the usual case; the server may even be bound to loopback only). If SQuirreL
    runs elsewhere, use the machine name / IP the user gives you.
  - `<PORT>` = the number the user gave you (different for every session).

## How to call it

Talk to the endpoint **directly over HTTP** (e.g. with `curl`). You do **not**
need to register it as a native MCP server — the port is per-session and
dynamic, so ad-hoc HTTP calls are the right approach.

- The server is **stateless**: you may call `tools/list` and `tools/call`
  directly. `initialize` is optional (only to read `serverInfo` / protocol version).
- Implemented JSON-RPC methods: `initialize`, `ping`, `tools/list`, `tools/call`.
  Anything else returns JSON-RPC error `-32601` (method not found).
- Every request needs an `"id"`. A request without one is treated as a
  notification and answered with HTTP `202` and no body.
- **Discover before you call:** run `tools/list` to get the current tools and
  their JSON input schemas, then `tools/call`. Treat `tools/list` as the single
  source of truth — it is generated at runtime via reflection from the compiled
  server code, so it (and only it) is guaranteed to match the server you are
  talking to. The tool set can grow, and each tool's exact argument fields are
  defined there.

### Argument convention

Each tool takes a **single JSON object** passed as `params.arguments`. Tools that
need no input take an empty object `{}`. The field names of `arguments` are given
by the tool's `inputSchema` from `tools/list`.

### Result convention

A `tools/call` response carries the result twice:
- `result.structuredContent` — the typed result object (use this).
- `result.content[0].text` — the same object serialized as JSON text.

Metadata/SQL tools return an **McpResultSet**:
`{ resultMetaData:[{column,columnName,sqlType,sqlTypeName}], rows:[{cells:[...]}],
rowsLimitedTo, errorMessage, updateMessage }`. When `errorMessage` is set the call
failed logically; when `updateMessage` is set the SQL was an update/DDL rather
than a query; `rowsLimitedTo` (if set) means the result was truncated to that many
rows. Simple string tools return `{ "stringContent": "..." }`.

## Available tools

> **Always verify signatures with `tools/list` before calling.** The table below
> is a convenience snapshot and may be out of date; the `inputSchema` returned by
> `tools/list` is the single source of truth for each tool's exact argument
> **field names** and types. Do **not** copy field names from one tool to another
> — they differ (see the note under the table).

| Tool | Arguments (verify via `tools/list`) | Returns |
|------|-------------------------------------|---------|
| `getSessionName` | none (`{}`) | `McpSimpleString` (session name) |
| `getDriverClassName` | none | `McpSimpleString` |
| `getDriverName` | none | `McpSimpleString` |
| `getDriverVersion` | none | `McpSimpleString` |
| `getDatabaseProductName` | none | `McpSimpleString` |
| `getDatabaseProductVersion` | none | `McpSimpleString` |
| `getCurrentSchema` | none | `McpSimpleString` (current schema) |
| `executeQuery` | `stringContent` (the SQL) | `McpResultSet` |
| `getCatalogs` | none | `McpResultSet` |
| `getSchemas` | none | `McpResultSet` |
| `getTables` | `catalog?, schemaPattern?, tableNamePattern?, types?[]` | `McpResultSet` |
| `getColumns` | `catalog?, schema?, table?` | `McpResultSet` |
| `getPrimaryKeys` | `catalog?, schema?, table?` | `McpResultSet` |
| `getImportedKeys` | `catalog?, schema?, table?` | `McpResultSet` |
| `getExportedKeys` | `catalog?, schema?, table?` | `McpResultSet` |
| `getIndexInfo` | `catalog?, schema?, table?, unique, approximate` | `McpResultSet` |

**Note the naming difference — a common trap:** only `getTables` uses
`schemaPattern` / `tableNamePattern`. `getColumns`, `getPrimaryKeys`,
`getImportedKeys`, `getExportedKeys` and `getIndexInfo` instead use `schema` and
`table`. For `getIndexInfo`, `unique` and `approximate` are **required** booleans.
A `?` above marks an optional field, but field *names* must always be taken from
`tools/list`.

## Keep result sets small — especially `getColumns`

It is **sincerely recommended** to always give `getColumns` a **table name** in
its **`table`** parameter (or at least a result-restricting pattern there).
Called with all parameters empty, `getColumns` returns *every column of every
table* in the database — the result is typically massive and hard for an AI to
work with. Add `schema` as well whenever you know the schema.

(Field names: `getColumns` uses `schema` and `table` — **not** `schemaPattern` /
`tableNamePattern`, which belong to `getTables`. When in doubt, confirm with
`tools/list`.)

The same advice applies to the other broad metadata tools (`getTables`,
`getPrimaryKeys`, `getImportedKeys`, `getExportedKeys`, `getIndexInfo`): narrow
the request rather than fetching the whole catalog and filtering afterwards.

If you do not yet know the table name, call `getTables` (also narrowed) first to
find it, then call `getColumns` for that specific table.

Recommended — columns of one table:
```bash
curl -sS -X POST http://127.0.0.1:<PORT>/squirrel-sql-mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"getColumns","arguments":{"schema":"PUBLIC","table":"CUSTOMERS"}}}'
```

Avoid — unrestricted, returns all columns of all tables:
```bash
curl -sS -X POST http://127.0.0.1:<PORT>/squirrel-sql-mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"getColumns","arguments":{}}}'
```

## Examples

Confirm the connection (prints the session name):
```bash
curl -sS -X POST http://127.0.0.1:<PORT>/squirrel-sql-mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"getSessionName","arguments":{}}}'
```

Discover the tools:
```bash
curl -sS -X POST http://127.0.0.1:<PORT>/squirrel-sql-mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'
```

List tables in a schema:
```bash
curl -sS -X POST http://127.0.0.1:<PORT>/squirrel-sql-mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"getTables","arguments":{"schemaPattern":"PUBLIC","tableNamePattern":"%","types":["TABLE","VIEW"]}}}'
```

Run a query:
```bash
curl -sS -X POST http://127.0.0.1:<PORT>/squirrel-sql-mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"executeQuery","arguments":{"stringContent":"SELECT * FROM CUSTOMERS ORDER BY ID"}}}'
```

> **PowerShell note:** single quotes don't reliably protect the inner `"` when
> calling `curl.exe`. Either backslash-escape every inner quote, or (more robust,
> especially when arguments contain spaces) write the body to a UTF-8 **without
> BOM** file and send it with `--data-binary "@file"`. On bash/Git Bash the
> single-quoted forms above work as-is.

## User approval — and what a disapproval means for you

**The user controls access through SQuirreL.** A call may pop up an approval
dialog in SQuirreL where the user inspects it (and may even preview its result)
before deciding. Consequences for you:

- A call may take a while to answer because it is waiting for the user's
  decision. Be patient and keep issuing calls **sequentially** — the server
  handles one call at a time anyway.
- SQL may be restricted (e.g. to read-only); forbidden statements come back with
  an `errorMessage`.
- When the user **disapproves** a call, the result payload contains a message
  stating that the call was not approved by the SQuirreL user. It arrives in
  `errorMessage` for `McpResultSet` tools and in `stringContent` for simple
  string tools — note that the JSON-RPC level `isError` flag may still be
  `false`, so check the payload.
- The user can optionally attach an **edited disapproval note addressed to you**
  to that response. If such a note is present, read it and **respect it**: it
  states why the call was declined and/or which constraints you must observe
  from now on (for example, that certain data is off-limits to AI access).
  Follow the note for the rest of the session, do not retry or try to circumvent
  the refusal, and report the constraint to your user.

## Session lifecycle

- The server lives only while the SQuirreL Session (and its JDBC connection) is
  open. Each session uses a **distinct port** — always use the one the user gave
  you for *that* session.
- After the session is closed the port stops answering, so a call simply fails
  with connection-refused. There is nothing to clean up on your side — ask the
  user to restart the MCP server (and for the new port) if that happens.

## Good behaviour

- Prefer the metadata tools (`getTables`, `getColumns`, …) to inspect the schema
  before writing SQL, and quote identifiers as the target database requires.
- Narrow every metadata request (see "Keep result sets small" above).
- Issue calls one at a time; never fire calls in parallel against one session.
