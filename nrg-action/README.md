# Nanolaba Readme Generator — GitHub Action

Generate multi-language README/Markdown files from a single `.src.md` template, in CI, with one workflow step.

<!-- Marketplace badge omitted until the action is published.
     Re-add after publication:
     [![Marketplace](https://img.shields.io/badge/marketplace-nrg--action-blue?logo=github)](https://github.com/marketplace/actions/nanolaba-readme-generator)
-->
[![Latest NRG release](https://img.shields.io/github/v/release/nanolaba/readme-generator)](https://github.com/nanolaba/readme-generator/releases)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](./LICENSE)

This action is a thin composite wrapper around the [NRG CLI](https://github.com/nanolaba/readme-generator). On each run it sets up Java (optional), downloads the requested NRG release zip, extracts `nrg.jar`, and invokes it via `java -cp nrg.jar com.nanolaba.nrg.NRG …` against the templates you list. No Maven, no local Java install, no language requirements on the consuming repository — if a project authors its README via NRG, this action regenerates or verifies it on every push.

## Quickstart

```yaml
- uses: actions/checkout@v4
- uses: nanolaba/nrg-action@v1
  with:
    file: README.src.md
```

Full example: [`examples/basic.yml`](./examples/basic.yml).

## Inputs

| Name | Description | Default |
|---|---|---|
| `file` | Path to the `.src.md` template (relative to `working-directory`). Use `files` for multiple. | — |
| `files` | Multi-line list of `.src.md` templates (one per line). Mutually exclusive with `file`. | — |
| `charset` | Source file encoding. | `UTF-8` |
| `mode` | Operation mode: `generate`, `check`, or `validate`. | `generate` |
| `nrg-version` | NRG release tag (e.g. `v1.1`) or `latest`. | `latest` |
| `java-version` | JDK version for `actions/setup-java`. Ignored when `setup-java=false`. | `17` |
| `java-distribution` | JDK distribution for `actions/setup-java`. | `temurin` |
| `setup-java` | Whether the action should install Java itself. Set to `'false'` if you set up Java in a previous step. | `true` |
| `log-level` | NRG log verbosity: `trace`, `debug`, `info`, `warn`, or `error`. | `info` |
| `working-directory` | Directory in which NRG runs. | `.` |

`mode` semantics:

- `generate` — write `README.md`, `README.ru.md`, … to disk. Default.
- `check` — read-only verification. Exits non-zero with a unified diff on stderr if disk content does not match what NRG would generate. Use on pull requests.
- `validate` — scan the template for authoring mistakes (unknown widgets, undeclared language markers, missing imports, unbalanced ignore-blocks). No files written.

## Outputs

| Name | Description |
|---|---|
| `version` | Resolved NRG version (e.g. `v1.1`). Useful when `nrg-version=latest`. |
| `changed-files` | Newline-separated list of files written or modified by NRG. |

## Examples

### Basic generate

Regenerate the README on every push to `main`:

```yaml
name: Regenerate README
on:
  push:
    branches: [main]
permissions:
  contents: read
jobs:
  regenerate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: nanolaba/nrg-action@v1
        with:
          file: README.src.md
```

See [`examples/basic.yml`](./examples/basic.yml).

### Drift check on PR

Fail the build when a contributor edits `README.md` directly instead of regenerating from `README.src.md`:

```yaml
name: README drift check
on:
  pull_request:
    paths: ['**/*.src.md', '**/*.md']
permissions:
  contents: read
jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: nanolaba/nrg-action@v1
        with:
          file: README.src.md
          mode: check
```

See [`examples/check-on-pr.yml`](./examples/check-on-pr.yml).

### Validate-only

Run the validator against templates on every PR. No files are written; the build fails if any diagnostic is reported:

```yaml
name: Validate templates
on:
  pull_request:
    paths: ['**/*.src.md']
permissions:
  contents: read
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: nanolaba/nrg-action@v1
        with:
          file: README.src.md
          mode: validate
```

### Auto-commit regenerated files

Open a pull request whenever a `.src.md` template changes. Direct push from a workflow needs `permissions: contents: write` and may be blocked by branch protection — prefer `peter-evans/create-pull-request` (shown below) over a raw `git push`:

```yaml
name: Regenerate README and open PR
on:
  push:
    branches: [main]
    paths: ['**/*.src.md']
permissions:
  contents: write
  pull-requests: write
jobs:
  regenerate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - id: nrg
        uses: nanolaba/nrg-action@v1
        with:
          file: README.src.md
      - uses: peter-evans/create-pull-request@v6
        with:
          commit-message: 'docs: regenerate README from src'
          title: 'docs: regenerate README'
          branch: nrg/regenerate-readme
          delete-branch: true
          add-paths: |
            README.md
            README.*.md
```

See [`examples/auto-commit.yml`](./examples/auto-commit.yml).

## Multi-file projects

Pass a heredoc list to the `files:` input (one path per line). All files are processed in a single action invocation; the jar is downloaded once:

```yaml
- uses: nanolaba/nrg-action@v1
  with:
    files: |
      README.src.md
      docs/CONTRIBUTING.src.md
```

`file` and `files` are mutually exclusive — set exactly one.

## Skipping the built-in setup-java

If the surrounding workflow already installs Java (for example, for a Maven build), you can opt out of the action's own `actions/setup-java@v4` step. Composite-action inputs are strings, so use the quoted `'false'`, not the YAML boolean:

```yaml
- uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: '21'
- uses: nanolaba/nrg-action@v1
  with:
    file: README.src.md
    setup-java: 'false'
```

## Pinning the action version

- `@v1` (recommended) — auto-updates within the v1 major; you get bug fixes without breaking changes.
- `@v1.0` — locked to a single minor; predictable across re-runs at the cost of manual bumps.
- `@<full-sha>` — most secure; pinned to one commit and unaffected by re-tagging. Use this if your supply-chain policy requires it.

## Troubleshooting

- **`Failed to download …`** — verify the `nrg-version` you passed exists at <https://github.com/nanolaba/readme-generator/releases>. The default `latest` resolves via the GitHub API; rate-limit pressure is mitigated by the workflow's `github.token`, but a private fork without that token will hit the 60-requests/hour ceiling.
- **`nrg.jar not found inside …`** — the upstream release zip layout changed. Open an issue at <https://github.com/nanolaba/readme-generator/issues> with the version you tried and your runner OS.
- **Windows runner: `unzip: command not found`** — git-bash on `windows-latest` ships `unzip`, but rare images don't. Workaround: precede the action with a step that installs it (e.g. `choco install unzip`).
- **`mode: check` works locally but fails in CI** — almost always line-ending drift on a `windows-latest` checkout. Add a `.gitattributes` with `* text=auto eol=lf` and re-commit the regenerated files.

## License

Apache 2.0 — see [`LICENSE`](./LICENSE).
