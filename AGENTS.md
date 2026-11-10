# AGENTS.md

This file provides guidance to AI coding agents (Claude Code, Codex, etc.) when working with code in this repository. `CLAUDE.md` only imports this file — keep all instructions here.

## Project

**Nanolaba Readme Generator (NRG)** — a Java 8 tool that processes `*.src.md` templates into one `*.md` file per configured language. Distributed as a CLI, a Maven plugin, a GitHub Action, and a library.

This repo dogfoods itself: `README.md` and `README.ru.md` are generated from `README.src.md` (which imports `docs/*.src.md`, which in turn import `docs/widgets/Widget*.src.md`). `CONTRIBUTING`, `SECURITY` and `CODE_OF_CONDUCT` are generated the same way from their own `.src.md` files. Never hand-edit the generated files — change the `.src.md` template and regenerate.

Repository layout:

- `nrg/` — core library and CLI (artifact `com.nanolaba:readme-generator`).
- `nrg-maven-plugin/` — Maven plugin wrapping the CLI.
- `nrg-action/` — composite GitHub Action (`action.yml` + `run.sh`); not a Maven module.
- `docs/` — README source fragments; `docs/Changelog.src.md` is the changelog.
- `.github/workflows/` — `ci.yml` (build & test on JDK 8 and 17), `action-test.yml` (action self-test matrix + dogfood validation of `README.src.md`) and `release-guard.yml` (runs `.github/scripts/release-preflight.sh` on every `v*` tag / published release: POM versions must match the tag and must not be `-SNAPSHOT`, and the `readme-generator-X.Y.zip` asset must be downloadable).

Versions: the development version is the POM `<version>` (currently `1.3-SNAPSHOT`); the last released version is the `stableVersion` property in the root `pom.xml`. `README.src.md` reads both via `${pom.version}` / `${pom.properties.stableVersion}`.

## Build & test commands

Multi-module Maven build (run from repo root):

```bash
mvn clean install -Dgpg.skip=true   # build everything, run tests, install to local repo
mvn -pl nrg test                    # tests for the core module only
mvn -pl nrg-maven-plugin install -Dgpg.skip=true  # install the Maven plugin (needs nrg installed first)
```

Run a single test class / method:

```bash
mvn -pl nrg test -Dtest=TableOfContentsWidgetTest
mvn -pl nrg test -Dtest=TableOfContentsWidgetTest#methodName
```

Mutation testing is wired up via PIT but skipped by default. To run it:

```bash
mvn -pl nrg test -Dpit.skip=false
```

Regenerate the dogfooded docs after touching any `.src.md` (or an example class imported by the docs):

```bash
mvn -pl nrg package -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true
java -jar nrg/target/nrg.jar -f README.src.md
java -jar nrg/target/nrg.jar -f README.src.md --validate   # what the CI dogfood job runs
```

The README footer renders `${widget:date}`, so a regeneration always changes the "Last updated" line — that is expected, and it is also why CI gates the README with `--validate` rather than `--check`.

The `nrg` module's `package` phase produces two artifacts in `nrg/target/`: `nrg.jar` (executable jar-with-dependencies, `Main-Class` = `com.nanolaba.nrg.NRG`) and a release zip bundling the jar with `nrg.sh` / `nrg.bat`.

The parent build configures `maven-source-plugin`, `maven-javadoc-plugin`, `maven-gpg-plugin`, and `central-publishing-maven-plugin` for releases — these run on every build, so local builds without GPG keys will fail at the `verify` phase. Use `-Dgpg.skip=true` (or stay at the `package` phase) when iterating locally. CI runs `mvn verify -Dgpg.skip=true -Dmaven.javadoc.skip=true` on JDK 8 and JDK 17; changes must pass on both.

## Architecture

The code lives in `nrg/src/main/java/com/nanolaba/nrg/`, split into three packages: the root (`NRG`), `core` (pipeline, config, readers, validation, `core.freeze`, `core.json`) and `widgets`.

1. **`NRG`** — CLI entry point. `main` calls `run(args)`, which returns the exit code without `System.exit` (the Maven plugin and tests use `run`; `NRG.exitOnFailure` toggles the exit in `main`). Also exposes `NRG.addWidget(...)` for library users to register custom widgets globally.
   - Sources: `-f <file>` (one literal file, no glob expansion) **or** positional arguments (literal paths / `glob:` patterns resolved by `SourceFileResolver`) — the two are mutually exclusive. Results are aggregated across files; `--fail-fast` stops at the first failure.
   - Modes: default (write files), `--stdout` (optionally `--language <code>`), `--check` (diff generated output against files on disk, optionally narrowed by repeatable `--check-paths <glob>`), `--validate` (static template checks via `Validator`, no output). `--stdout`, `--check` and `--validate` are mutually exclusive.
   - Other flags: `--charset`, `--log-level` (or `NRG_LOG_LEVEL`), `--widgets <FQCN,...>` + `--classpath`, `--allow-exec`, `--file-name-pattern`, `--default-language-file-name-pattern`, `--line-ending auto|lf|crlf`, `--no-header`, `--header-text`, `--root <dir>`, `--version`, `-h`.
   - **Project root** (`core.ProjectRoot`, `GeneratorConfig.getProjectRoot()`): templates are treated as untrusted data and are confined to a root directory — the nearest ancestor of the root source file containing `.git`, else its own directory; `--root` / `<root>` / `setProjectRoot` override it, a template never can. Any new feature that lets a template name a path must pass it through `GeneratorConfig.requireInsideProjectRoot(...)`.
   - Output filenames come from `OutputFileNameResolver` (default: default language → `<base>.md`, others → `<base>.<lang>.md`; overridable through the filename-pattern flags / `nrg.fileNamePattern` properties, validated by `OutputFileNameValidator`). `LineEndings` preserves the on-disk file's CRLF/LF convention in `auto` mode.

2. **`Generator`** (`core`) — orchestrator. Holds a `GeneratorConfig` and lazily produces a `GenerationResult` per language (`results` map populated on first access via `generateContentIfNecessary`). For each language it: prepends the head comment (suppressed by `nrg.noHeader`, replaced by `nrg.headerText`); runs `BlockDispatcher.process` — the pre-pass of every enabled `BlockWidget`; renders the remaining lines through `TemplateLine`; and, for the root generator only, resolves freeze blocks (`FreezeBlockProcessor`) and unescapes `\$`, `<\!--`, `<!--\@`.

3. **`GeneratorConfig`** (`core`) — parsed configuration of one source template:
   - The source body is first passed through `IgnoreBlockStripper` (`<!--nrg.ignore-->` lines and `<!--nrg.ignore.begin-->` … `<!--nrg.ignore.end-->` blocks are dropped before anything else sees them).
   - First pass reads `nrg.languages`, `nrg.defaultLanguage` and `nrg.widgets` (so they're available before anything else).
   - Second pass collects all other `<!--@key=value-->` properties.
   - Registers default widgets in `initDefaultWidgets` (`ImportWidget`, `LanguagesWidget`, `TableOfContentsWidget`, `DateWidget`, `TodoWidget`, `AlertWidget`, `BadgeWidget`, `MathWidget`, `ExecWidget`, `FileTreeWidget`, `IfWidget`, `DetailsWidget`), then widgets from `nrg.widgets`, then caller-supplied ones. `getWidget(name)` matches the primary name or an alias with **last-registered-wins** semantics (precedence: API > CLI > template property > built-ins); a name claimed by two different classes only logs a WARN.
   - Lazily creates the external descriptor readers: `PomReader` (`DomPomReader`), `NpmReader` (`JsonNpmReader`, backed by `core.json.MinimalJsonParser`), `GradleReader` (`RegexGradleReader`); their locations are overridable via `nrg.pom.path` / `nrg.npm.path` / `nrg.gradle.path`. Also carries the env provider, the `execAllowed` flag, remote-import settings and the `DiskFreezeIndex` cache.
   - The `rootGenerator` flag distinguishes the top-level generator from sub-generators (see below); only the root strips NRG metadata, resolves freeze blocks and unescapes characters in final output.

4. **`TemplateLine`** (`core`) — per-line rendering. For each line and target language, in this order:
   - `isLineVisible(language)` — drops lines tagged `<!--otherLang-->` and pure property-definition lines.
   - `renderEnvProperties` / `renderPomProperties` / `renderNpmProperties` / `renderGradleProperties` — substitute `${env.NAME}`, `${pom.path}`, `${npm.path}`, `${gradle.path}`, each with an optional `:fallback`; a missing value renders empty with a one-time WARN.
   - `renderLanguageProperties` — substitutes `${en:'...', ru:'...'}` constructs (supports both `'` and `"`, doubled-quote escaping).
   - `renderProperties` — substitutes `${var}` references against `config.getProperties()`; a `var.<lang>` key overrides the bare `var` for that language (`NRGUtil.getLanguageScopedProperty`).
   - `renderWidgets` — finds `${widget:name(params)}` via `WIDGET_TAG_PATTERN` and delegates to the matching `NRGWidget`. Widgets also get `beforeRenderLine` / `afterRenderLine` hooks. `RuntimeException`s thrown by a widget propagate to the caller.
   - Backslash escapes (`\${...}`) are honored throughout via `isNotEscaped`.
   - Final cleanup (`removeNrgDataFromText`) only runs when `config.isRootGenerator()` is true.

5. **Widgets** (`com.nanolaba.nrg.widgets`) — implement `NRGWidget` or extend `DefaultWidget`. Each receives the `WidgetTag` (with raw parameters), the `GeneratorConfig`, and the current language; `getAliases()` declares alternative names (e.g. `toc` for `tableOfContents`). Custom widgets need a public no-arg constructor to be loadable via `nrg.widgets` / `--widgets` / the plugin's `<widgets>`. Notable patterns:
   - **`BlockWidget`** — abstract base for paired `${widget:NAME(...)}` … `${widget:endNAME}` markers, each on its own line (`IfWidget`, `DetailsWidget`). The pairs are rewritten in the `BlockDispatcher` pre-pass, so inner lines still flow through the normal per-line pipeline; subclasses implement `onBlockOpen` / `onBlockClose` / `isInlineForm` (and optionally `renderInline` for a single-tag form) and never override `processBlocks`. `if` conditions are evaluated by `core.IfCondition`.
   - **`ImportWidget`** loads another file (or, with `url=`, a remote one through `RemoteFetcher` + `CacheStore`, gated by `nrg.allowRemoteImports` and optional `sha256` pinning), applies region / line selection, dedent and heading shift (`ImportRegionExtractor`, `ImportLinesSpec`, `ImportDedenter`, `ImportHeadingShifter`), and — when `run-generator='true'`, the default for `.src.md` — constructs a child `Generator` with `rootGenerator = false` so escapes/metadata are preserved for the parent's final pass.
   - **`TableOfContentsWidget`** spins up a *secondary* `Generator` over the same source body with the TOC widget disabled, then scans the rendered output for `#`-prefixed headers starting at the TOC's own line. Heading-to-anchor conversion is GitHub-style by default and Unicode-aware (`Header.getAnchor` in `TableOfContentsWidget.java`, switchable via `AnchorStyle`) — preserve the existing regex pipeline rather than reinventing it. Headers tagged `<!--toc.ignore-->` are excluded.
   - **`ExecWidget`** runs external commands and is disabled unless `--allow-exec` / `<allowExec>` is set (`config.isExecAllowed()`).

6. **Freeze blocks** (`core.freeze`) — `<!--nrg.freeze id="..."-->` … `<!--/nrg.freeze-->` regions keep whatever is currently in the on-disk output file (optionally taken from another language's output via `source-lang`). `FreezeValidator` checks the template before generation; `FreezeBlockProcessor` is a post-pass over the root generator's rendered output, backed by `DiskFreezeIndex`.

7. **`Validator`** (`core`) — backs `--validate`: walks the root template and every reachable import and reports unknown widget names, language markers not in `nrg.languages`, missing import paths and unbalanced ignore markers.

8. **`nrg-maven-plugin`** is a thin shim: `NRGMojo#execute` (package `com.nanolaba.nrg.maven`) translates its parameters into CLI flags and makes a single `NRG.run(...)` call with all `<file>` entries as positional arguments. Goal name is `create-files`, default phase `compile`, `inheritByDefault = false` (child modules don't re-run it). The only logic living in the Mojo is up-front validation of `<widgets>` and the `<checkPaths>`-requires-`<check>` rule. A new CLI flag normally needs a matching Mojo parameter (and, if it makes sense there, an `nrg-action` input).

9. **`nrg-action`** — composite action; `run.sh` resolves the requested NRG release (`latest` → GitHub Releases API), downloads the release zip, runs NRG once per file in `generate` / `check` / `validate` mode and reports `version` / `changed-files` outputs. It runs a *released* jar, so `action-test.yml` pins `nrg-version` against a stable fixture in `nrg-action/test-fixtures/`, while the dogfood job builds NRG from source.

## Conventions

- **Java 8 source/target.** No `var`, no records, no switch expressions, no text blocks. Streams and lambdas are used heavily.
- **Logging** uses `com.nanolaba.logging.LOG` (the in-house `nanolog` library), not SLF4J. Pattern is `LOG.debug("msg {}", arg)`. `LOG.warn` goes to stdout, so user-facing CLI warnings that must reach stderr are printed with `System.err.println("WARN: ...")`.
- **Exception handling** uses `com.nanolaba.sugar.Code.run(...)` (in-house `sugar` library) to wrap checked exceptions in lambdas. Both `nanolog` and `sugar` are versioned via `nanolaba-nanolog.version` / `nanolaba-sugar.version` properties.
- **Tests** are JUnit 5 (`org.junit.jupiter`) and mirror the main package layout (`core`, `core/freeze`, `core/json`, `widgets`; CLI-level tests such as `CheckCliTest` / `ValidateCliTest` sit in the root test package). Examples in `nrg/src/test/java/com/nanolaba/nrg/examples/` (e.g. `ExampleWidget`, `GeneratorExample`) double as documentation samples imported by `docs/Advanced.src.md` and `docs/Launching.src.md` — if you rename or move them, regenerate the README.
- **String constants** for property and marker names live in `core.NRGConstants` (`PROPERTY_LANGUAGES`, `PROPERTY_DEFAULT_LANGUAGE`, `PROPERTY_WIDGETS`, `PROPERTY_FILE_NAME_PATTERN`, `FREEZE_OPEN_MARKER_NAME`, `DEFAULT_SOURCE_EXTENSION = "src.md"`, …). Add new `nrg.*` keys there instead of inlining literals.
- When changing widget behavior or anchor generation, run the focused test classes (`TableOfContentsWidgetTest`, `TableOfContentsWidgetSlugifyTest`, `BlockWidgetTest`, etc.) before the full suite — they exercise the most fragile logic in this codebase.
- **Adding a built-in widget** touches several places: register it in `GeneratorConfig.initDefaultWidgets`, add `docs/widgets/Widget<Name>.src.md` and import it from `docs/Widgets.src.md`, and extend both the "Built-in widgets" list and the all-widgets `<details>` example in `docs/QuickStart.src.md` — that example is meant to stay exhaustive.
- **Class-level Javadoc on every new class.** Match the style of `nrg/src/main/java/com/nanolaba/nrg/core/json/MinimalJsonParser.java`: a short paragraph stating what the class is and why it exists, followed by a `<p>` paragraph for scope, invariants, or non-obvious behavior. Add Javadoc on non-trivial methods too (subtle algorithms, multi-style branches, contract corner cases) — skip simple getters/setters and self-explanatory delegators. Don't write `WHAT` comments that restate the code; document the `WHY` and the edge cases.
- **Inspect surrounding code when adding a new class.** Before committing a new file, scan it for low-effort cleanups in the same pass: replace `new java.xxx.Yyy()` style fully-qualified names with proper imports, drop redundant intermediate variables, and remove dead branches. Verify with `mvn -pl nrg compile` (and `test-compile` if tests are touched) before reporting the work as done.
- **Every user-visible significant change must land in the changelog.** Whenever a commit (or merged PR / closed issue) introduces behaviour visible to users — a new CLI flag, widget, parameter, marker, output-format change, bug fix users would notice, etc. — add a paired EN/RU bullet to the `### Unreleased` section of `docs/Changelog.src.md` in the same change, link the closing issue (`Closes [#NN](...)`), and regenerate `README.md` / `README.ru.md`. Skip only purely internal work (refactors, build/CI/javadoc fixes, docs-only edits, repo-meta files like CONTRIBUTING/SECURITY/CODE_OF_CONDUCT, PR/issue templates) that no end user would observe. When in doubt, log it.
- **Commit messages** follow Conventional Commits in English (`feat(widget): …`, `fix(ci): …`, `docs: …`, `refactor: …`, `chore: …`). Commit a `.src.md` change together with the regenerated `.md` files.

## Local tooling

- **GitHub CLI** is installed but not on PATH — invoke as `"/c/Program Files/GitHub CLI/gh.exe"` from bash, or `"C:\Program Files\GitHub CLI\gh.exe"` from PowerShell. Use it for inspecting workflow runs, fetching authenticated logs (`gh run view <id> --log-failed`), and PR/issue management.
