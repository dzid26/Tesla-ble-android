# Contributing / Release Notes

## Branch + PR strategy (single maintainer)

To avoid PR conflicts, keep one linear branch per feature and one active PR.

Recommended flow:

1. `git checkout -b feature/tesla-ble`
2. Commit iteratively on that branch.
3. Open one PR from that branch.
4. If you need updates, push additional commits to the same branch (do not open stacked PRs from intermediate snapshots).
5. Before merge, squash to a single commit (or use GitHub squash merge).

## Rebase before merge

```bash
git fetch origin
git rebase origin/main
```

If conflicts occur, resolve once locally and continue rebase.

## CI expectations

- PRs run build + unit tests.
- Tags `v*` produce APK artifact and GitHub release asset.

## GitHub access from local automation

The local environment must be configured with either:

- a git remote (`origin`) and push permissions, and
- GitHub CLI (`gh`) or a token-enabled workflow.

Without those, automation can prepare commits and PR metadata but cannot push/open PRs directly on GitHub.

## Forbidden commit guard

CI blocks ancestry that includes commit `ad3fc96d779c00aded0206e3a7c11b9cbb50795a`.
If that commit appears upstream, rebase/cherry-pick onto a clean branch from `main` and reopen/update the PR.
