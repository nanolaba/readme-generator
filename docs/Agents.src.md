## ${en:'Using NRG with AI coding agents', ru:'NRG и ИИ-агенты'}

More and more documentation is written by coding agents — Claude Code, Codex, Cursor, Copilot. They are quick at it, and they keep making the same few mistakes: they update `README.md` and leave `README.ru.md` as it was, quote a version number from memory, paste a code sample that stops compiling a week later, or "fix" a generated file directly. ${name} makes the README a build artifact, so each of these mistakes either cannot happen or is caught by a command the agent runs itself.<!--en-->
Документацию всё чаще пишут агенты — Claude Code, Codex, Cursor, Copilot. Пишут быстро и ошибаются в одном и том же: правят `README.md` и забывают про `README.ru.md`, называют номер версии по памяти, вставляют пример кода, который через неделю перестаёт компилироваться, или «чинят» сгенерированный файл напрямую. С ${name} README становится артефактом сборки: каждая из этих ошибок либо невозможна, либо ловится командой, которую агент запускает сам.<!--ru-->

| ${en:'What goes wrong', ru:'Что идёт не так'} | ${en:'What NRG does about it', ru:'Что с этим делает NRG'} |
|---|---|
| One language is updated, the others are forgotten | All languages live in one `.src.md` with translations on adjacent lines: the agent reads and edits them together, and a single diff shows both |<!--en-->
| Один язык обновили, остальные забыли | Все языки лежат в одном `.src.md`, переводы — на соседних строках: агент читает и правит их вместе, а в одном diff видны оба |<!--ru-->
| Versions and coordinates quoted from memory | `\${pom.version}`, `\${npm.version}`, `\${gradle.version}` are read from the build descriptor on every generation |<!--en-->
| Версии и координаты названы по памяти | `\${pom.version}`, `\${npm.version}`, `\${gradle.version}` читаются из дескриптора сборки при каждой генерации |<!--ru-->
| Code samples that no longer match the code | `\${widget:import(path='…', region='…')}` embeds a region of a real source file — one that is compiled and tested |<!--en-->
| Примеры кода разошлись с кодом | `\${widget:import(path='…', region='…')}` вставляет регион настоящего исходного файла — того, что компилируется и тестируется |<!--ru-->
| A generated file is edited by hand | `--check` exits with `1` and prints a unified diff, so CI fails on the edit |<!--en-->
| Сгенерированный файл поправили руками | `--check` завершается с кодом `1` и печатает unified diff — CI на такой правке падает |<!--ru-->
| A mistake in template syntax | `--validate` reports `ERROR: file.src.md:LINE: message` without generating anything |<!--en-->
| Ошибка в синтаксисе шаблона | `--validate` выводит `ERROR: file.src.md:LINE: message`, ничего не генерируя |<!--ru-->
| The whole README is loaded to change one section | The template is split into small fragments joined by `import`; the agent opens only the fragment it needs |<!--en-->
| Ради одной секции в контекст загружается весь README | Шаблон разбит на небольшие фрагменты, соединённые через `import`; агент открывает только нужный |<!--ru-->

### ${en:'The agent loop', ru:'Цикл работы агента'}

Three commands let an agent check its own work — each ends with an exit code and a message that says where to look:<!--en-->
Три команды позволяют агенту проверить собственную работу — каждая завершается кодом возврата и сообщением, где искать проблему:<!--ru-->

```bash
nrg --validate -f README.src.md   # ${en:'template mistakes', ru:'ошибки шаблона'}: ERROR: README.src.md:42: unknown widget 'tocc'
nrg -f README.src.md              # ${en:'regenerate every language', ru:'перегенерировать все языки'}
nrg --check -f README.src.md      # ${en:'exit 1 and a unified diff when files on disk are stale', ru:'код 1 и unified diff, если файлы на диске устарели'}
```

`--validate`, `--check` and `--stdout` never write files. The exit code tells the agent what to do next: `1` — regenerate, `2` — fix the template, `64` — fix the command ([Exit codes](#exit-codes)). Add `--format json` to get files and diagnostics (with a stable `code` and a `hint` naming the fix) as one JSON document instead of log lines — see [Machine-readable output](#machine-readable-output). Details: [Validating source templates](#validating-source-templates), [Verifying generated files](#verifying-generated-files-ci-mode).<!--en-->
`--validate`, `--check` и `--stdout` никогда не пишут файлы. Код возврата подсказывает агенту следующий шаг: `1` — перегенерировать, `2` — исправить шаблон, `64` — исправить команду ([«Коды возврата»](#коды-возврата)). С `--format json` файлы и диагностики (со стабильным `code` и подсказкой `hint`) приходят одним JSON-документом вместо строк лога — см. [«Машиночитаемый вывод»](#машиночитаемый-вывод). Подробнее: [«Валидация исходных шаблонов»](#валидация-исходных-шаблонов), [«Проверка сгенерированных файлов»](#проверка-сгенерированных-файлов-режим-ci).<!--ru-->

### ${en:'Instructions for the agent', ru:'Инструкция для агента'}

An agent does not know that `README.md` is generated until something tells it. Put this into `AGENTS.md`, `CLAUDE.md`, or whichever instruction file your agent reads:<!--en-->
Агент не знает, что `README.md` генерируется, пока ему об этом не сказали. Добавьте в `AGENTS.md`, `CLAUDE.md` или другой файл инструкций, который читает ваш агент:<!--ru-->

```markdown
## Documentation

- `README.md` and `README.*.md` are generated by NRG from `README.src.md`. Never edit them by hand.
- Change the `.src.md` template. A line tagged `<\!--en-->` has a counterpart for every other
  language right next to it — update all of them.
- After a change run `./nrgw --validate -f README.src.md && ./nrgw -f README.src.md`
  (`nrgw.cmd` on Windows).
- Commit the template together with the regenerated files.
```

Rather than adapting this snippet by hand, let NRG write it from the real configuration — which outputs exist, which templates and fragments to edit, which source files are embedded, the languages and the exact commands:<!--en-->
Вместо того чтобы подгонять этот фрагмент вручную, поручите NRG написать его по реальной конфигурации — какие файлы генерируются, какие шаблоны и фрагменты править, какие исходники встроены в документацию, языки и точные команды:<!--ru-->

```bash
nrg --agent-rules --write AGENTS.md            # insert or update the block, keep the rest of the file
nrg --agent-rules --write AGENTS.md --check    # exit 1 and a diff when the block is out of date
```

The block sits between `<\!-- nrg:agent-rules:begin -->` and `<\!-- nrg:agent-rules:end -->`; `--agent-rules` alone prints it. Run the `--check` form in CI and the instructions can no longer drift from the templates.<!--en-->
Блок стоит между `<\!-- nrg:agent-rules:begin -->` и `<\!-- nrg:agent-rules:end -->`; без `--write` команда просто печатает его. Запускайте форму с `--check` в CI — и инструкции больше не разойдутся с шаблонами.<!--ru-->

`./nrgw` is the [pinned wrapper](#pinned-wrapper-nrgw): commit it once (`nrg --init-wrapper`) and the instruction works in any sandbox that has Java, with no NRG installed. This repository's own agent instructions follow the same pattern — see its [AGENTS.md](AGENTS.md).<!--en-->
`./nrgw` — это [обёртка с закреплённой версией](#обёртка-nrgw-с-закреплённой-версией): закоммитьте её один раз (`nrg --init-wrapper`), и инструкция заработает в любой песочнице с Java, без установленного NRG. Собственные инструкции для агентов в этом репозитории устроены так же — см. его [AGENTS.md](AGENTS.md).<!--ru-->

An agent that opens `README.md` first is sent to the right file by the header of the generated file itself — it names the template and says to regenerate instead of editing:<!--en-->
Агента, открывшего сначала `README.md`, к нужному файлу направляет заголовок самого сгенерированного файла — он называет шаблон и просит перегенерировать файл, а не править его:<!--ru-->

```markdown
<\!-- This file was automatically generated by Nanolaba Readme Generator (NRG) from README.src.md -->
<\!-- Do not edit it by hand: change README.src.md and regenerate. See https://github.com/nanolaba/readme-generator -->
```

To put the exact command there, use `nrg.headerText` with the `<source>` placeholder — see [Header customisation](#header-customisation).<!--en-->
Чтобы указать там точную команду, задайте `nrg.headerText` с плейсхолдером `<source>` — см. [«Настройка заголовка»](#настройка-заголовка).<!--ru-->

### ${en:'Claude Code plugin and the NRG skill', ru:'Плагин для Claude Code и навык NRG'}

This repository is also a Claude Code plugin marketplace. The plugin brings the NRG [Agent Skill](skills/nrg/SKILL.md) — when to use NRG, the rules, the commands, and the full [syntax card](skills/nrg/SYNTAX.md) — and two hooks: before an `Edit` / `Write` of a generated file it refuses the edit and names the template to change; after a template or an imported file changes it runs `--validate` and regenerates the outputs, feeding errors back to the agent.<!--en-->
Этот репозиторий — ещё и маркетплейс плагинов Claude Code. Плагин приносит [навык](skills/nrg/SKILL.md) NRG — когда применять NRG, правила, команды и полную [шпаргалку по синтаксису](skills/nrg/SYNTAX.md) — и два хука: перед `Edit` / `Write` сгенерированного файла он отклоняет правку и называет шаблон, который нужно править; после изменения шаблона или импортируемого файла запускает `--validate` и перегенерирует выходные файлы, возвращая ошибки агенту.<!--ru-->

```text
/plugin marketplace add nanolaba/readme-generator
/plugin install nrg@nanolaba
```

The hooks need NRG ${devVersion} or newer: `nrg` from `PATH`, or the command line in the `NRG_HOOK_COMMAND` environment variable (for example `java -jar /opt/nrg/nrg.jar`). They run in every project you open, so they never execute anything the project supplies — not even its `./nrgw`: a repository you clone must not be able to run code through an installed plugin. In a project without `*.src.md` files they do nothing and start no JVM. The same syntax card is printed offline, for the installed version, by `nrg --help syntax`; other tools find the docs through [llms.txt](llms.txt).<!--en-->
Хукам нужен NRG ${devVersion} или новее: `nrg` из `PATH` или командная строка из переменной окружения `NRG_HOOK_COMMAND` (например, `java -jar /opt/nrg/nrg.jar`). Они срабатывают в каждом открытом проекте, поэтому никогда не запускают ничего, что поставляет сам проект, — даже его `./nrgw`: склонированный репозиторий не должен получить возможность выполнить код через установленный плагин. В проекте без файлов `*.src.md` они ничего не делают и не запускают JVM. Ту же шпаргалку для установленной версии без сети печатает `nrg --help syntax`; другие инструменты находят документацию через [llms.txt](llms.txt).<!--ru-->

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

Instructions can be overlooked; a hook cannot. Without the plugin, the same two hooks go into the project's `.claude/settings.json` — `--claude-hook` reads the hook event from stdin, so nothing else (no `jq`) is needed:<!--en-->
Инструкцию можно пропустить, хук — нет. Без плагина те же два хука прописываются в `.claude/settings.json` проекта — `--claude-hook` сам читает событие хука из stdin, так что больше ничего (никакого `jq`) не нужно:<!--ru-->

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

`pre-edit` exits with `2` for a generated file, which blocks the edit and hands the agent the name of the template. `post-edit` validates the templates that read the edited file and regenerates them; validation errors come back to the agent the same way. For other harnesses, `--is-generated` and `nrg $(nrg --affected <file>)` are the building blocks — see [What a template writes and reads](#what-a-template-writes-and-reads).<!--en-->
`pre-edit` завершается с кодом `2` для сгенерированного файла — правка блокируется, а агент получает имя шаблона. `post-edit` проверяет шаблоны, читающие изменённый файл, и перегенерирует их; ошибки проверки возвращаются агенту тем же путём. Для других сред строительные блоки — `--is-generated` и `nrg $(nrg --affected <file>)`, см. [«Что шаблон пишет и читает»](#что-шаблон-пишет-и-читает).<!--ru-->

### ${en:'One template for every agent', ru:'Один шаблон для всех агентов'}

Agents read different files — `AGENTS.md`, `CLAUDE.md`, `GEMINI.md` — whose content is mostly the same. The "languages" of a template do not have to be human languages:<!--en-->
Агенты читают разные файлы — `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, — а содержимое у них почти одинаковое. «Языки» шаблона не обязаны быть человеческими:<!--ru-->

```markdown
<\!--@nrg.languages=agents,claude,gemini-->
<\!--@nrg.fileNamePattern=<LANG>.md-->

# Project rules

\${widget:import(path='docs/build.md')}

A note for Claude Code only.<\!--claude-->
A note for Gemini CLI only.<\!--gemini-->
```

`nrg -f AGENTS.src.md` writes `AGENTS.md`, `CLAUDE.md` and `GEMINI.md`: untagged lines go to all three, tagged lines to one. With `import`, the instruction files and the README can share a fragment — the build commands, for instance — and can no longer contradict each other.<!--en-->
`nrg -f AGENTS.src.md` запишет `AGENTS.md`, `CLAUDE.md` и `GEMINI.md`: строки без маркера попадут во все три файла, строки с маркером — в один. Через `import` файлы инструкций и README могут использовать общий фрагмент — например, команды сборки — и больше не будут противоречить друг другу.<!--ru-->

### ${en:'Running unattended', ru:'Запуск без присмотра'}

An agent runs commands on templates it did not write — a pull request, a freshly cloned repository. ${name} treats a template as untrusted data: every path it names must stay inside the [project root](#project-root), and no command is executed unless `--allow-exec` is given on the command line.<!--en-->
Агент запускает команды на шаблонах, которые писал не он, — из pull request, из только что склонированного репозитория. ${name} считает шаблон недоверенными данными: любой путь, который он называет, должен оставаться внутри [корня проекта](#корень-проекта), а команды не выполняются, пока в командной строке нет `--allow-exec`.<!--ru-->

Two things remain yours to control: `\${env.NAME}` reads any environment variable of the process, and a template can switch remote imports on for itself (`nrg.allowRemoteImports`). For a template you do not trust, keep secrets out of the environment and run without network access.<!--en-->
Две вещи остаются на вашей стороне: `\${env.NAME}` читает любую переменную окружения процесса, а удалённый импорт шаблон может включить себе сам (`nrg.allowRemoteImports`). Для недоверенного шаблона уберите секреты из окружения и запускайте без доступа к сети.<!--ru-->
