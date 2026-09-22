# Workflow — Dockie

1. Reconstruct from Git + `graphify query` before broad exploration.
2. Never change `applicationId` or replace the production signing identity.
3. Never commit keystores, `play-service-account.json`, or passwords.
4. After coherent code changes: verify with Gradle, run `graphify update .`, update `.ai/HANDOFF.md` / `.ai/ACTIVE_WORK.md`.
5. Play publishing: Gradle Play Publisher (`./gradlew publishReleaseBundle`), default track `alpha` (closed). Override with `-PplayTrack=internal`.
6. First Play upload + App Signing enrollment require Console UI — see `SIGNING_MIGRATION.md`.
7. Prefer closed testing over internal for the 14-day personal-account gate.
