## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

## EYC Digital Play publishing

This project is connected to shared EYC Digital Google Play infrastructure.
- Full flow: `RELEASING.md` · non-secret config: `.eyc/play.json`
- Local credentials come from the machine (`GOOGLE_APPLICATION_CREDENTIALS`,
  see `%USERPROFILE%\.eyc\docs\AGENT_GUIDE.md`). GitHub releases use the
  `play-release.yml` workflow (OIDC, no stored Google key).
- Release: `powershell scripts/release-play.ps1 -Action internal|closed|verify|status`
  (production needs `-ConfirmProduction` AND an explicit human request).
- Never commit credentials. Never create replacement Play credentials.
  Never rotate signing keys without explicit authorization.
