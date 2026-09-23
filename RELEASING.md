# Releasing to Google Play (EYC infrastructure)

This project publishes through shared **EYC Digital** infrastructure.
Agent guide: `%USERPROFILE%\.eyc\docs\AGENT_GUIDE.md`

## Release

Local (uses machine ADC credentials, never committed):

```powershell
powershell scripts/release-play.ps1 -Action internal     # internal testing
powershell scripts/release-play.ps1 -Action closed       # closed track (alpha)
powershell scripts/release-play.ps1 -Action verify       # build + check only
powershell scripts/release-play.ps1 -Action status       # show state
powershell scripts/release-play.ps1 -Action production -ConfirmProduction  # explicit only
```

Via GitHub Actions (OIDC, no Google key stored):

```sh
gh workflow run play-release.yml -f track=internal
gh workflow run play-release.yml -f track=alpha
gh workflow run play-release.yml -f track=production -f production_confirm=RELEASE
```

## Rules

- Production requires an explicit human request (+ `-ConfirmProduction` / confirm input).
- Never commit credentials: no service-account JSON, keystores, passwords, tokens.
- Never create replacement Play credentials or rotate signing keys without explicit authorization.
- First-ever AAB of a new app must be uploaded manually in Play Console (API limitation).
- Play Console permission grants are human-only; if the API reports permission errors, stop and report the exact console page.
- Config (non-secret): `.eyc/play.json`. Release notes: `<module>/src/main/play/release-notes/en-US/<track>.txt`.