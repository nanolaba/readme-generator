<a name="using-nrg-with-ai-coding-agents"></a><!--en-->
<a name="nrg-и-ии-агенты"></a><!--ru-->

## ${en:'Agent-driven development', ru:'Агентная разработка'}

When a coding agent (Claude Code, Codex, Cursor, Copilot and others) maintains the docs, it makes the same few mistakes: it updates `README.md` and leaves `README.ru.md` as it was, quotes a version number from memory, pastes a code sample that stops compiling a week later, or fixes a generated file directly. With ${name} the README is a build artifact, so each of these mistakes either cannot happen or is caught by a command the agent runs itself.<!--en-->
Когда документацию ведёт ИИ-агент (Claude Code, Codex, Cursor, Copilot и другие), он ошибается в одном и том же: правит `README.md` и забывает про `README.ru.md`, называет номер версии по памяти, вставляет пример кода, который через неделю перестаёт компилироваться, или «чинит» сгенерированный файл напрямую. С ${name} README становится артефактом сборки: каждая из этих ошибок либо невозможна, либо ловится командой, которую агент запускает сам.<!--ru-->

| ${en:'What goes wrong', ru:'Что идёт не так'} | ${en:'What NRG does about it', ru:'Что с этим делает NRG'} |
|---|---|
| One language is updated, the others are forgotten | All languages live in one `.src.md` with translations on adjacent lines: the agent reads and edits them together, and one diff shows both. `--stale-translations` lists what was left behind |<!--en-->
| Один язык обновили, остальные забыли | Все языки лежат в одном `.src.md`, переводы — на соседних строках: агент читает и правит их вместе, а один diff показывает оба. `--stale-translations` перечисляет то, что отстало |<!--ru-->
| Versions and coordinates quoted from memory | `\${pom.version}`, `\${npm.version}`, `\${gradle.version}` are read from the build descriptor on every generation |<!--en-->
| Версии и координаты названы по памяти | `\${pom.version}`, `\${npm.version}`, `\${gradle.version}` читаются из дескриптора сборки при каждой генерации |<!--ru-->
| Code samples that no longer match the code | `\${widget:import(path='…', region='…')}` embeds a region of a real source file, one that is compiled and tested |<!--en-->
| Примеры кода разошлись с кодом | `\${widget:import(path='…', region='…')}` вставляет регион настоящего исходного файла — того, что компилируется и тестируется |<!--ru-->
| A generated file is edited by hand | The header of the file names its template; the Claude Code hook refuses the edit; `--check` exits with `1` and prints a unified diff, so CI fails |<!--en-->
| Сгенерированный файл поправили руками | Заголовок файла называет его шаблон, хук Claude Code отклоняет правку, а `--check` завершается с кодом `1` и печатает unified diff — CI падает |<!--ru-->
| A mistake in template syntax | `--validate` prints `ERROR: file.src.md:LINE: message` and writes nothing |<!--en-->
| Ошибка в синтаксисе шаблона | `--validate` печатает `ERROR: file.src.md:LINE: message` и ничего не пишет |<!--ru-->
| The whole README is loaded to change one section | The template is split into fragments joined by `import`, and the agent opens only the one it needs. `--locate README.md:LINE` names the template line behind a line of the output |<!--en-->
| Ради одной секции в контекст загружается весь README | Шаблон разбит на фрагменты, соединённые через `import`, и агент открывает только нужный. `--locate README.md:LINE` называет строку шаблона, из которой получилась строка вывода |<!--ru-->

The repository is maintained the same way: [AGENTS.md](AGENTS.md) describes the build, the invariants and the test requirements (JUnit and a 100% mutation-coverage gate), and CI runs `--validate` over this README's templates and `--check` over the syntax card and `llms.txt`.<!--en-->
Сам репозиторий ведётся так же: [AGENTS.md](AGENTS.md) описывает сборку, инварианты и требования к тестам (JUnit и 100% покрытия мутационными тестами), а CI запускает `--validate` по шаблонам этого README и `--check` по шпаргалке синтаксиса и `llms.txt`.<!--ru-->

### ${en:'The agent loop', ru:'Цикл работы агента'}

Three commands let an agent check its own work. Each ends with an exit code and a message that says where to look:<!--en-->
Три команды позволяют агенту проверить собственную работу. Каждая завершается кодом возврата и сообщением о том, где искать проблему:<!--ru-->

```bash
nrg --validate -f README.src.md   # ${en:'template mistakes', ru:'ошибки шаблона'}: ERROR: README.src.md:3: unknown widget 'language'; did you mean 'languages'?
nrg -f README.src.md              # ${en:'regenerate every language', ru:'перегенерировать все языки'}
nrg --check -f README.src.md      # ${en:'exit 1 and a unified diff when files on disk are stale', ru:'код 1 и unified diff, если файлы на диске устарели'}
```

`--validate`, `--check` and `--stdout` never write files. The exit code tells the agent what to do next: `1` regenerate, `2` fix the template, `64` fix the command ([Exit codes](#exit-codes)). With `--format json` the files and the diagnostics, each with a stable `code` and a `hint` naming the fix, come as one JSON document instead of log lines, see [Machine-readable output](#machine-readable-output). Details: [Validating source templates](#validating-source-templates), [Verifying generated files](#verifying-generated-files-ci-mode).<!--en-->
`--validate`, `--check` и `--stdout` никогда не пишут файлы. Код возврата подсказывает агенту следующий шаг: `1` — перегенерировать, `2` — исправить шаблон, `64` — исправить команду ([«Коды возврата»](#коды-возврата)). С `--format json` файлы и диагностики, у каждой из которых есть стабильный `code` и подсказка `hint`, приходят одним JSON-документом вместо строк лога, см. [«Машиночитаемый вывод»](#машиночитаемый-вывод). Подробнее: [«Валидация исходных шаблонов»](#валидация-исходных-шаблонов), [«Проверка сгенерированных файлов»](#проверка-сгенерированных-файлов-режим-ci).<!--ru-->

*${devVersion}:* NRG logs through [nanolog](https://github.com/nanolaba/nanolog). When an agent runs it, nanolog's agent mode adds a `[NANOLOG]` line and a run summary on stderr and keeps the full log in `target/nanolog/` (`.nanolog/` outside a Maven or Gradle project); `NANOLOG_AGENT=off` turns this off.<!--en-->
*${devVersion}:* NRG пишет лог через [nanolog](https://github.com/nanolaba/nanolog). Когда NRG запускает агент, режим агента nanolog добавляет в stderr строку `[NANOLOG]` и сводку запуска и сохраняет полный лог в `target/nanolog/` (вне проекта Maven или Gradle — в `.nanolog/`); `NANOLOG_AGENT=off` это отключает.<!--ru-->

### ${en:'Instructions for the agent', ru:'Инструкция для агента'}

An agent does not know that `README.md` is generated until something tells it. Put this into `AGENTS.md`, `CLAUDE.md` or whichever instruction file your agent reads:<!--en-->
Агент не знает, что `README.md` генерируется, пока ему об этом не скажут. Добавьте в `AGENTS.md`, `CLAUDE.md` или другой файл инструкций, который читает ваш агент:<!--ru-->

```markdown
## Documentation

- `README.md` and `README.*.md` are generated by NRG from `README.src.md`. Never edit them by hand.
- Change the `.src.md` template. A line tagged `<\!--en-->` has a counterpart for every other
  language right next to it — update all of them.
- After a change run `./nrgw --validate -f README.src.md && ./nrgw -f README.src.md`
  (`nrgw.cmd` on Windows).
- Commit the template together with the regenerated files.
```

Or let NRG write the block from the real configuration: which outputs exist, which templates and fragments to edit, which source files are embedded, the languages and the exact commands:<!--en-->
Или поручите NRG написать этот блок по реальной конфигурации: какие файлы генерируются, какие шаблоны и фрагменты править, какие исходники встроены в документацию, языки и точные команды:<!--ru-->

```bash
nrg --agent-rules --write AGENTS.md            # insert or update the block, keep the rest of the file
nrg --agent-rules --write AGENTS.md --check    # exit 1 and a diff when the block is out of date
```

For the template from [Try it](#try-it) the block reads:<!--en-->
Для шаблона из раздела [«Попробовать»](#попробовать) блок выглядит так:<!--ru-->

```markdown
<\!-- nrg:agent-rules:begin -->
## Generated documentation (NRG)

_This block is generated by `nrg --agent-rules`; do not edit it by hand._

- Generated by NRG, never edit by hand: `README.md`, `README.ru.md` (from `README.src.md`).
- Edit the templates instead: `README.src.md`.
- Languages: `en`, `ru`. A line tagged `<\!--en-->` has a counterpart for `<\!--ru-->` next to it; update all of them, and give every `\${en:'...'}` construct an entry for each language.
- After a change: `nrg --validate README.src.md && nrg README.src.md`. Commit the templates together with the regenerated files.
<\!-- nrg:agent-rules:end -->
```

The block sits between `<\!-- nrg:agent-rules:begin -->` and `<\!-- nrg:agent-rules:end -->`; `--agent-rules` alone prints it. Run the `--check` form in CI, and the instructions can no longer drift from the templates.<!--en-->
Блок стоит между `<\!-- nrg:agent-rules:begin -->` и `<\!-- nrg:agent-rules:end -->`; без `--write` команда просто печатает его. Запускайте форму с `--check` в CI, и инструкции больше не разойдутся с шаблонами.<!--ru-->

`./nrgw` is the [pinned wrapper](#pinned-wrapper-nrgw): commit it once (`nrg --init-wrapper`), and the instruction works in any sandbox that has Java, with no NRG installed.<!--en-->
`./nrgw` — это [обёртка с закреплённой версией](#обёртка-nrgw-с-закреплённой-версией): закоммитьте её один раз (`nrg --init-wrapper`), и инструкция заработает в любой песочнице с Java, без установленного NRG.<!--ru-->

An agent that opens `README.md` first is sent to the right file by the header of the generated file. It names the template and says to regenerate instead of editing:<!--en-->
Агента, открывшего сначала `README.md`, к нужному файлу направляет заголовок самого сгенерированного файла. Он называет шаблон и просит перегенерировать файл, а не править его:<!--ru-->

```markdown
<\!-- This file was automatically generated by Nanolaba Readme Generator (NRG) from README.src.md -->
<\!-- Do not edit it by hand: change README.src.md and regenerate. See https://github.com/nanolaba/readme-generator -->
```

To put the exact command there, use `nrg.headerText` with the `<source>` placeholder, see [Header customisation](#header-customisation).<!--en-->
Чтобы указать там точную команду, задайте `nrg.headerText` с плейсхолдером `<source>`, см. [«Настройка заголовка»](#настройка-заголовка).<!--ru-->

### ${en:'Claude Code plugin and the NRG skill', ru:'Плагин для Claude Code и навык NRG'}

This repository is also a Claude Code plugin marketplace. The plugin brings the NRG [Agent Skill](skills/nrg/SKILL.md) (when to use NRG, the rules, the commands and the full [syntax card](skills/nrg/SYNTAX.md)) and two hooks. Before an `Edit` / `Write` of a generated file, one refuses the edit and names the template to change; after a template or an imported file changes, the other runs `--validate`, regenerates the outputs and feeds errors back to the agent.<!--en-->
Этот репозиторий — ещё и маркетплейс плагинов Claude Code. Плагин приносит [навык](skills/nrg/SKILL.md) NRG (когда применять NRG, правила, команды и полную [шпаргалку по синтаксису](skills/nrg/SYNTAX.md)) и два хука. Перед `Edit` / `Write` сгенерированного файла первый отклоняет правку и называет шаблон, который нужно править; после изменения шаблона или импортируемого файла второй запускает `--validate`, перегенерирует выходные файлы и возвращает ошибки агенту.<!--ru-->

```text
/plugin marketplace add nanolaba/readme-generator
/plugin install nrg@nanolaba
```

The hooks need NRG ${devVersion} or newer: `nrg` from `PATH`, or the command line in the `NRG_HOOK_COMMAND` environment variable (for example `java -jar /opt/nrg/nrg.jar`). They run in every project you open, so they never execute anything the project supplies, not even its `./nrgw`: a repository you clone must not be able to run code through an installed plugin. In a project without `*.src.md` files they do nothing and start no JVM. `nrg --help syntax` prints the same syntax card offline, for the installed version; other tools find the docs through [llms.txt](llms.txt).<!--en-->
Хукам нужен NRG ${devVersion} или новее: `nrg` из `PATH` или командная строка из переменной окружения `NRG_HOOK_COMMAND` (например, `java -jar /opt/nrg/nrg.jar`). Они срабатывают в каждом открытом проекте, поэтому никогда не запускают ничего, что поставляет сам проект, даже его `./nrgw`: склонированный репозиторий не должен получить возможность выполнить код через установленный плагин. В проекте без файлов `*.src.md` они ничего не делают и не запускают JVM. Ту же шпаргалку для установленной версии без сети печатает `nrg --help syntax`; другие инструменты находят документацию через [llms.txt](llms.txt).<!--ru-->

### ${en:'Other agents', ru:'Другие агенты'}

Each agent reads its instructions from its own file; `--agent-rules --write` keeps the NRG block in any of them:<!--en-->
Каждый агент читает инструкции из своего файла; `--agent-rules --write` поддерживает блок NRG в любом из них:<!--ru-->

| ${en:'Agent', ru:'Агент'} | ${en:'Instructions file', ru:'Файл инструкций'} | ${en:'Command', ru:'Команда'} |
|---|---|---|
| Codex, Jules, Aider, Zed | `AGENTS.md` | `nrg --agent-rules --write AGENTS.md` |
| Claude Code | `CLAUDE.md` | `nrg --agent-rules --write CLAUDE.md` |
| GitHub Copilot | `.github/copilot-instructions.md` | `nrg --agent-rules --write .github/copilot-instructions.md` |
| Cursor | `.cursor/rules/nrg.mdc` | `nrg --agent-rules --write .cursor/rules/nrg.mdc` |
| Gemini CLI | `GEMINI.md` | `nrg --agent-rules --write GEMINI.md` |

For Cursor, start the `.mdc` file with its front matter (`description`, `globs: "**/*.src.md, **/*.md"`) before the first `--write`; the block is appended after it and updated in place afterwards.<!--en-->
Для Cursor перед первым `--write` начните файл `.mdc` с front matter (`description`, `globs: "**/*.src.md, **/*.md"`); блок допишется после него и дальше будет обновляться на месте.<!--ru-->

### ${en:'Guarding generated files with a hook', ru:'Защита сгенерированных файлов хуком'}

An agent may miss an instruction; a hook runs on every edit. Without the plugin, the same two hooks go into the project's `.claude/settings.json`. `--claude-hook` reads the hook event from stdin, so nothing else, `jq` included, is needed:<!--en-->
Агент может пропустить инструкцию, а хук срабатывает на каждой правке. Без плагина те же два хука прописываются в `.claude/settings.json` проекта. `--claude-hook` сам читает событие хука из stdin, так что больше ничего, даже `jq`, не нужно:<!--ru-->

```json
{
  "hooks": {
    "PreToolUse": [{
      "matcher": "Edit|Write",
      "hooks": [{"type": "command", "command": "\"$CLAUDE_PROJECT_DIR/nrgw\" --claude-hook pre-edit"}]
    }],
    "PostToolUse": [{
      "matcher": "Edit|Write",
      "hooks": [{"type": "command", "command": "\"$CLAUDE_PROJECT_DIR/nrgw\" --claude-hook post-edit"}]
    }]
  }
}
```

`pre-edit` exits with `2` for a generated file, which blocks the edit and hands the agent the name of the template. `post-edit` validates the templates that read the edited file and regenerates them; validation errors reach the agent the same way:<!--en-->
`pre-edit` завершается с кодом `2` для сгенерированного файла: правка блокируется, а агент получает имя шаблона. `post-edit` проверяет шаблоны, читающие изменённый файл, и перегенерирует их; ошибки проверки доходят до агента тем же путём:<!--ru-->

```text
NRG: the edit leaves a template invalid; fix it before going on:
ERROR: README.src.md:3: unknown widget 'language'; did you mean 'languages'?
```

For other harnesses, `--is-generated` and `nrg $(nrg --affected <file>)` are the building blocks, see [What a template writes and reads](#what-a-template-writes-and-reads).<!--en-->
Для других сред строительные блоки — `--is-generated` и `nrg $(nrg --affected <file>)`, см. [«Что шаблон пишет и читает»](#что-шаблон-пишет-и-читает).<!--ru-->

### ${en:'One template for every agent', ru:'Один шаблон для всех агентов'}

Agents read different files, `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `.github/copilot-instructions.md`, and their content is mostly the same. Declare them as [output variants](#output-variants) of one template:<!--en-->
Агенты читают разные файлы — `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `.github/copilot-instructions.md`, — а содержимое у них почти одинаковое. Объявите их [вариантами вывода](#варианты-вывода) одного шаблона:<!--ru-->

```markdown
<\!--@nrg.variants=agents,claude,gemini,copilot-->
<\!--@nrg.fileNamePattern=<VARIANT>.md-->
<\!--@nrg.variant.copilot.file=.github/copilot-instructions.md-->

# Project rules

\${widget:import(path='docs/build.md')}

A note for Claude Code and Gemini CLI.<\!--variant:claude,gemini-->
Everyone except Copilot.<\!--variant:!copilot-->
```

`nrg -f AGENTS.src.md` writes all four files: untagged lines go to every variant, tagged lines to the variants they name. With `import`, the instruction files and the README can share a fragment, the build commands for instance, and stop contradicting each other. Variants combine with languages, so a translated `AGENTS.ru.md` is one more language in `nrg.languages`.<!--en-->
`nrg -f AGENTS.src.md` запишет все четыре файла: строки без маркера попадут в каждый вариант, строки с маркером — в названные в нём. Через `import` файлы инструкций и README могут использовать общий фрагмент, например команды сборки, и перестанут противоречить друг другу. Варианты сочетаются с языками, так что переведённый `AGENTS.ru.md` — это просто ещё один язык в `nrg.languages`.<!--ru-->

### ${en:'Reviewing translations', ru:'Проверка переводов при ревью'}

An agent that rewrites a paragraph tends to rewrite it in one language. `nrg --stale-translations origin/main` makes "keep the translations in sync" a failing check instead of an instruction the agent may forget. It lists every group and construct where one language changed and another did not, with the old and the new text, which is the context needed to update the translation. See [Translations that fell behind](#translations-that-fell-behind).<!--en-->
Агент, переписывающий абзац, обычно переписывает его на одном языке. `nrg --stale-translations origin/main` превращает «поддерживай переводы в синхроне» из инструкции, которую агент может забыть, в падающую проверку. Она перечисляет каждую группу и конструкцию, где один язык изменился, а другой нет, со старым и новым текстом — это и нужно, чтобы обновить перевод. См. [«Отставшие переводы»](#отставшие-переводы).<!--ru-->

### ${en:'Running unattended', ru:'Запуск без присмотра'}

An agent runs commands on templates it did not write: a pull request, a freshly cloned repository. ${name} treats a template as untrusted data. Every path it names must stay inside the [project root](#project-root), and no command is executed unless `--allow-exec` is given on the command line.<!--en-->
Агент запускает команды на шаблонах, которые писал не он: из pull request, из только что склонированного репозитория. ${name} считает шаблон недоверенными данными. Любой путь, который он называет, должен оставаться внутри [корня проекта](#корень-проекта), а команды не выполняются, пока в командной строке нет `--allow-exec`.<!--ru-->

`--sandbox` closes two more channels: `\${env.NAME}` can read any environment variable of the process, and a template can switch remote imports on for itself (`nrg.allowRemoteImports`). With `--sandbox` no variable is visible and no connection is opened, see [Untrusted templates](#untrusted-templates). An agent harness can pass it on every run that touches a pull request.<!--en-->
Ещё два канала закрывает `--sandbox`: `\${env.NAME}` может прочитать любую переменную окружения процесса, а удалённый импорт шаблон может включить себе сам (`nrg.allowRemoteImports`). С `--sandbox` переменные не видны и соединения не открываются, см. [«Недоверенные шаблоны»](#недоверенные-шаблоны). Среда агента может передавать этот флаг при каждом запуске, связанном с pull request.<!--ru-->
