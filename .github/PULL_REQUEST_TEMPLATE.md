<!--
  Thanks for sending a pull request! Please fill in the sections below.
  If anything in this checklist doesn't apply to your change, leave a "n/a" note instead of removing it — that way reviewers can tell you considered it.
-->

## Summary

<!-- 1–3 sentences. What changes, and why? Lead with the user-visible behaviour, not the implementation. -->

## Related issues

<!-- Link the issue this PR resolves. Use `Closes #N` so it auto-closes on merge. For multiple, repeat the keyword. -->

Closes #

## Type of change

<!-- Check ALL that apply. -->

- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] New widget
- [ ] Refactor / internal change (no user-visible behaviour change)
- [ ] Documentation only
- [ ] Build, CI, or release plumbing
- [ ] **Breaking change** (template syntax, CLI flags, public API, or generated-output format)

## What changed

<!-- A slightly longer "what" + "why" than the summary. Mention any non-obvious design decisions, tradeoffs, or alternatives you considered. Reviewers should be able to understand the PR without re-reading the diff. -->

## Test plan

<!--
  Describe how you verified the change. For widget output, include the input template and the rendered output (before vs. after when relevant).
  For library / CLI changes, list the test classes / commands you ran.
-->

```
mvn -pl nrg compile
mvn install -Dgpg.skip=true
mvn -pl nrg test -Dtest=...
```

## Screenshots / sample output

<!-- Optional. Useful for changes that affect rendered Markdown, the CLI's stdout, or the GitHub Action's logs. -->

## Checklist

- [ ] Branch is up to date with `main`.
- [ ] Code compiles cleanly (`mvn -pl nrg compile`) on Java 8.
- [ ] All tests pass (`mvn install -Dgpg.skip=true`).
- [ ] New behaviour has unit tests; widget output changes have an integration sample where relevant.
- [ ] New or changed public classes/methods have Javadoc following the [`MinimalJsonParser` style](../blob/main/nrg/src/main/java/com/nanolaba/nrg/core/json/MinimalJsonParser.java).
- [ ] If template-driven docs were touched, the generated `README.md` / `README.ru.md` (and any other generated `*.md`) have been regenerated and committed alongside the source `.src.md`.
- [ ] Commit messages follow the [Conventional Commits](https://www.conventionalcommits.org/) flavour used in this repo (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`, `build:`, `revert:`).
- [ ] No unrelated changes are bundled in (no formatting churn outside the modified files, no IDE settings, no `.idea/`, `.claude/`, or local plan files).
- [ ] If this is a breaking change, the rationale is documented above and the changelog has been updated.
