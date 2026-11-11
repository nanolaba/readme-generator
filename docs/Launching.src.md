## ${en:'Usage', ru:'Способы запуска программы'}

### ${en:'Using the Command Line Interface', ru:'Запуск из командной строки'}

${name} написан на Java и требует для запуска версии **Java 8** и выше.<!--ru-->
[Установите](https://www.java.com/en/download/) Java, если она отсутствует у вас в системе.<!--ru-->
${name} is written in Java and requires **Java 8** or higher to run.<!--en-->
[Install](https://www.java.com/en/download/) Java if it’s not already present on your system.<!--en-->

[Скачайте](https://github.com/nanolaba/readme-generator/releases/tag/v${stableVersion}) последнюю <!--ru-->
стабильную версию приложения.<!--ru-->
[Download](https://github.com/nanolaba/readme-generator/releases/tag/v${stableVersion}) the latest<!--en-->
stable version of the application.<!--en-->

Разархивируйте скачанный архив. Если вы используете Unix-like системы, то назначьте файлу `nrg.sh` права <!--ru-->
на исполнение:<!--ru-->
Unzip the downloaded archive. If you're using a Unix-like system, make the `nrg.sh` file executable:<!--en-->

```bash
chmod +x nrg.sh  
```

Теперь вы можете запустить программу для генерации файлов:<!--ru-->
Now you can run the program to generate the files:<!--en-->

```bash
nrg -f /path/to/README.src.md
```

Чтобы посмотреть список доступных опций консольного приложения наберите:<!--ru-->
To see the list of available options for the console application, type:<!--en-->

```bash
nrg --help
```

#### ${en:'Pinned wrapper: nrgw', ru:'Обёртка nrgw с закреплённой версией'}

`nrg --init-wrapper` writes three files into the current directory — `nrgw`, `nrgw.cmd` and<!--en-->
`.nrg/wrapper.properties` with the release version and the sha256 of its zip. Commit them, and<!--en-->
anyone who clones the repository — a contributor, a CI job, a coding agent in a fresh sandbox —<!--en-->
runs that exact NRG with one command and nothing installed but Java, the same idea as `mvnw` / `gradlew`:<!--en-->
`nrg --init-wrapper` записывает в текущий каталог три файла — `nrgw`, `nrgw.cmd` и<!--ru-->
`.nrg/wrapper.properties` с версией релиза и sha256 его архива. Закоммитьте их — и любой, кто<!--ru-->
склонирует репозиторий, — контрибьютор, задача CI, ИИ-агент в свежей песочнице, — запускает именно<!--ru-->
эту версию NRG одной командой, установив только Java. Та же идея, что у `mvnw` / `gradlew`:<!--ru-->

```bash
nrg --init-wrapper --wrapper-version ${stableVersion}
./nrgw -f README.src.md        # nrgw.cmd -f README.src.md on Windows
```

On the first run the wrapper downloads the release zip from GitHub into `~/.nrg/wrapper/<version>/`<!--en-->
(`NRG_WRAPPER_HOME` overrides the location), checks it against the pinned sha256 and runs it;<!--en-->
later runs work offline. A missing Java, a failed download or a checksum mismatch stops it with<!--en-->
exit code `69`; otherwise it exits with NRG's own code. `--wrapper-version` defaults to the running<!--en-->
NRG's version, and the checksum is taken from the `.sha256` file published with the release unless<!--en-->
`--wrapper-sha256 <hex>` names it. Run `--init-wrapper` again to move to another release.<!--en-->
При первом запуске обёртка скачивает архив релиза с GitHub в `~/.nrg/wrapper/<version>/`<!--ru-->
(место переопределяет `NRG_WRAPPER_HOME`), сверяет его с закреплённым sha256 и запускает;<!--ru-->
дальнейшие запуски работают без сети. Если нет Java, загрузка не удалась или не совпала контрольная<!--ru-->
сумма, обёртка завершается с кодом `69`; иначе — с кодом самого NRG. По умолчанию `--wrapper-version` —<!--ru-->
версия запущенного NRG, а контрольная сумма берётся из файла `.sha256`, опубликованного с релизом,<!--ru-->
если её не указать через `--wrapper-sha256 <hex>`. Чтобы перейти на другой релиз, запустите `--init-wrapper` ещё раз.<!--ru-->

#### ${en:'Verifying generated files (CI mode)', ru:'Проверка сгенерированных файлов (режим CI)'}

Use `--check` to verify that files on disk match what NRG would<!--en-->
generate right now. The flag is meant for CI / pre-commit hooks:<!--en-->
no files are written, a missing or out-of-date target file prints<!--en-->
a diff to stderr, and the process exits with status `1`.<!--en-->
Флаг `--check` проверяет, что файлы на диске совпадают с тем, что<!--ru-->
NRG сгенерировал бы прямо сейчас. Режим предназначен для CI / pre-commit:<!--ru-->
файлы не записываются, при отсутствии или расхождении выводится diff<!--ru-->
в stderr, процесс завершается с кодом `1`.<!--ru-->

The diff shows generated content, so anything a template pulls in — including values of `\${env.NAME}` — ends up in the CI log when the check fails. Keep secrets out of templates.<!--en-->
В diff попадает сгенерированное содержимое, поэтому всё, что шаблон подтягивает, — включая значения `\${env.NAME}` — при провале проверки окажется в логе CI. Не используйте секреты в шаблонах.<!--ru-->

```bash
nrg --check -f README.src.md && echo "README is up to date"
```

`--check` validates every language configured via `nrg.languages`<!--en-->
and is mutually exclusive with `--stdout`.<!--en-->
`--check` проверяет все языки, заявленные в `nrg.languages`, и<!--ru-->
несовместим с флагом `--stdout`.<!--ru-->

`--check-paths <pattern>` (repeatable) limits the check to outputs whose<!--en-->
path matches one of the supplied glob patterns. Patterns use the same<!--en-->
`glob:` syntax as multi-file source globs (`**/` matches zero or more<!--en-->
directories) and are resolved against the current working directory.<!--en-->
Outputs that don't match any pattern are skipped from both the diff and<!--en-->
the missing-file check — useful when only some generated files are tracked<!--en-->
in git and the rest are regenerated by a bot. A pattern that matches<!--en-->
nothing prints a stderr `WARN` (still exits `0`) so typos do not silently<!--en-->
disable the check. `--check-paths` requires `--check`.<!--en-->
`--check-paths <шаблон>` (повторяемый) ограничивает проверку выходными<!--ru-->
файлами, путь которых совпадает с одним из указанных glob-шаблонов.<!--ru-->
Шаблоны используют тот же синтаксис `glob:`, что и мульти-файловые<!--ru-->
исходные glob-ы (`**/` соответствует нулю и более каталогов), и<!--ru-->
разрешаются относительно текущего каталога. Выходы, не попавшие ни<!--ru-->
под один шаблон, пропускаются и в diff, и в проверке наличия — удобно,<!--ru-->
когда часть сгенерированных файлов лежит в git, а остальные регенерирует<!--ru-->
бот. Шаблон без совпадений печатает `WARN` в stderr (код выхода всё равно<!--ru-->
`0`), чтобы опечатка не отключала проверку молча. `--check-paths`<!--ru-->
требует `--check`.<!--ru-->

```bash
nrg --check --check-paths README.md -f README.src.md
```

#### ${en:'Validating source templates', ru:'Валидация исходных шаблонов'}

Use `--validate` to scan the template (and every reachable<!--en-->
`\${widget:import}`-imported template) for authoring mistakes without<!--en-->
generating any output. Errors:<!--en-->
Флаг `--validate` сканирует шаблон (и все импортируемые через<!--ru-->
`\${widget:import}` шаблоны) на типичные ошибки авторов, ничего не<!--ru-->
генерируя. Ошибки:<!--ru-->

- unregistered widget names (`\${widget:doesNotExist}`),<!--en-->
- parameters a built-in widget does not have (`\${widget:toc(maxDepth='2')}` — the parameter is `max-depth`),<!--en-->
- language markers `<\!--xx-->` whose code is not in `nrg.languages`,<!--en-->
- `\${widget:import(path='...')}` paths that do not exist on disk or leave the project root,<!--en-->
- unbalanced `<\!--nrg.ignore.begin-->` / `<\!--nrg.ignore.end-->` pairs,<!--en-->
- unclosed or stray block widgets (`\${widget:if(...)}` without `\${widget:endIf}`, a lone `\${widget:endDetails}`),<!--en-->
- a `\${name}` reference that is not declared but is one or two edits away from a property that is (`\${stableVerison}`),<!--en-->
- a missing translation: a run of language-tagged lines (blank lines do not end it) that has no line for one of the languages, or a `\${en:'…'}` construct without an entry for a language the line is shown in, or with an entry for an undeclared one.<!--en-->
- незарегистрированные имена виджетов (`\${widget:doesNotExist}`),<!--ru-->
- параметры, которых нет у встроенного виджета (`\${widget:toc(maxDepth='2')}` — параметр называется `max-depth`),<!--ru-->
- языковые маркеры `<\!--xx-->`, чей код отсутствует в `nrg.languages`,<!--ru-->
- пути в `\${widget:import(path='...')}`, которых нет на диске или которые выходят за корень проекта,<!--ru-->
- несбалансированные пары `<\!--nrg.ignore.begin-->` / `<\!--nrg.ignore.end-->`,<!--ru-->
- незакрытые или одинокие блочные виджеты (`\${widget:if(...)}` без `\${widget:endIf}`, отдельный `\${widget:endDetails}`),<!--ru-->
- ссылка `\${name}` на необъявленное свойство, отличающаяся на одну-две правки от объявленного (`\${stableVerison}`),<!--ru-->
- пропущенный перевод: серия строк с языковыми маркерами (пустые строки её не прерывают), где нет строк для одного из языков, или конструкция `\${en:'…'}` без записи для языка, в котором выводится строка, либо с записью для необъявленного языка.<!--ru-->

Warnings (reported, but the exit status stays `0`):<!--en-->
Предупреждения (выводятся, но код возврата остаётся `0`):<!--ru-->

- translations whose structure differs: inside a run of tagged lines one language has a heading, list item, table row, code block or paragraph that another one lacks (a warning, because translations legitimately wrap text differently),<!--en-->
- any other undefined `\${name}` — it stays in the output as written, which is right for a shell example like `\${HOME}` and wrong for a forgotten property,<!--en-->
- `\${pom.…}` / `\${npm.…}` / `\${gradle.…}` without a `:fallback` that the descriptor does not define, so it renders empty,<!--en-->
- <code>&lt;!--&#92;@nrg.languages=...--&gt;</code> and other `nrg.*` keys written with the cosmetic `\@` escape — such a line still declares the property (see [Backslash escapes](#backslash-escapes)); write <code>&lt;&#92;!--@...--&gt;</code> to show it in an example.<!--en-->
- переводы с разной структурой: внутри серии строк с маркерами у одного языка есть заголовок, пункт списка, строка таблицы, блок кода или абзац, которого нет у другого (предупреждение, потому что переводы законно по-разному переносят текст),<!--ru-->
- любой другой необъявленный `\${name}` — он остаётся в выводе как есть, что правильно для shell-примера вроде `\${HOME}` и неправильно для забытого свойства,<!--ru-->
- `\${pom.…}` / `\${npm.…}` / `\${gradle.…}` без `:fallback`, которых нет в дескрипторе, — они рендерятся пустыми,<!--ru-->
- <code>&lt;!--&#92;@nrg.languages=...--&gt;</code> и другие ключи `nrg.*`, записанные с косметическим экранированием `\@`, — такая строка всё равно объявляет свойство (см. [«Обратные слэши и экранирование»](#обратные-слэши-и-экранирование)); чтобы показать её в примере, пишите <code>&lt;&#92;!--@...--&gt;</code>.<!--ru-->

```bash
nrg --validate -f README.src.md && echo "Template is clean"
```

Each diagnostic is printed as `ERROR: file.src.md:LINE: message` or<!--en-->
`WARNING: file.src.md:LINE: message`; where there is an obvious fix, the<!--en-->
message ends with it (`unknown widget 'tocc'; did you mean 'toc'?`).<!--en-->
With no diagnostics, NRG exits silently with status `0`. All diagnostics<!--en-->
go to stderr; the process exits with status `2` when at least one of them<!--en-->
is an error. An import with `run-generator='false'` is only checked for<!--en-->
existence — its content is not a template. `--validate` is mutually<!--en-->
exclusive with `--check` and `--stdout`.<!--en-->
Каждое сообщение печатается в формате `ERROR: file.src.md:LINE: text` или<!--ru-->
`WARNING: file.src.md:LINE: text`; если исправление очевидно, сообщение<!--ru-->
заканчивается им (`unknown widget 'tocc'; did you mean 'toc'?`). Без<!--ru-->
диагностик NRG молча завершается с кодом `0`. Все сообщения печатаются<!--ru-->
в stderr; процесс завершается с кодом `2`, если среди них есть хотя бы<!--ru-->
одна ошибка. Импорт с `run-generator='false'` проверяется только на<!--ru-->
наличие файла — его содержимое не является шаблоном. `--validate`<!--ru-->
несовместим с `--check` и `--stdout`.<!--ru-->

A line meant for one language only is left out of the translation checks with<!--en-->
`<\!--nrg.parity.ignore-->` (the marker is removed from the output);<!--en-->
`<\!--\@nrg.validate.parity=false-->` turns them off for a template and the files it imports.<!--en-->
Строку, предназначенную только для одного языка, исключают из проверки переводов<!--ru-->
маркером `<\!--nrg.parity.ignore-->` (из вывода он удаляется);<!--ru-->
`<\!--\@nrg.validate.parity=false-->` отключает эти проверки для шаблона и импортируемых им файлов.<!--ru-->

#### ${en:'Print to stdout', ru:'Вывод в stdout'}

Use `--stdout` to stream generated output to standard output instead<!--en-->
of writing files to disk. Combine with `--language <code>` to print<!--en-->
only a single language variant; without it, every configured variant<!--en-->
is printed, prefixed with a separator line like `=== README.ru.md ===`<!--en-->
so the output can be split by downstream tools.<!--en-->
Флаг `--stdout` перенаправляет сгенерированный вывод в stdout,<!--ru-->
файлы на диск при этом не создаются. В сочетании с `--language <код>`<!--ru-->
печатается только один языковой вариант; без него выводятся все<!--ru-->
настроенные варианты, каждый предваряется строкой-разделителем вида<!--ru-->
`=== README.ru.md ===`, чтобы вывод можно было разрезать во внешних инструментах.<!--ru-->

```bash
nrg --stdout -f README.src.md
nrg --stdout --language en -f README.src.md
```

The `--language` flag is only meaningful with `--stdout` — using it<!--en-->
on its own logs a warning and the flag is ignored.<!--en-->
Флаг `--language` имеет смысл только вместе с `--stdout` — в одиночку<!--ru-->
он логируется как предупреждение и игнорируется.<!--ru-->

#### ${en:'Machine-readable output', ru:'Машиночитаемый вывод'}

`--format json` turns a run into one JSON document on stdout — for coding agents,<!--en-->
hooks and CI steps that need to know what happened without parsing log lines.<!--en-->
Logging moves to stderr (the `--check` diff stays there too). It works with<!--en-->
generation, `--check` and `--validate`, and cannot be combined with `--stdout`.<!--en-->
`--format json` превращает запуск в один JSON-документ в stdout — для ИИ-агентов,<!--ru-->
хуков и шагов CI, которым нужно знать, что произошло, не разбирая строки лога.<!--ru-->
Логирование уходит в stderr (туда же по-прежнему печатается diff `--check`).<!--ru-->
Режим работает с генерацией, `--check` и `--validate` и несовместим с `--stdout`.<!--ru-->

```bash
nrg --check --format json -f README.src.md
```

```json
{
  "version": "${devVersion}",
  "mode": "check",
  "ok": false,
  "exitCode": 1,
  "files": [
    {"source": "README.src.md", "output": "README.md", "language": "en", "status": "ok"},
    {"source": "README.src.md", "output": "README.ru.md", "language": "ru", "status": "stale"}
  ],
  "diagnostics": [
    {"severity": "error", "code": "unknown-widget", "file": "docs/Widgets.src.md", "line": 12, "message": "unknown widget 'tocc'", "hint": "did you mean 'toc'?"}
  ]
}
```

- `files[].status`: `written` / `unchanged` (generation), `ok` / `stale` / `missing` (`--check`), `ok` / `invalid` (`--validate`, one entry per source file).<!--en-->
- `diagnostics[].code` is stable — match on it, not on `message`: `unknown-widget`, `unknown-parameter`, `undeclared-language`, `import-not-found`, `import-outside-root`, `unbalanced-ignore`, `unbalanced-block`, `undefined-variable`, `unresolved-reference`, `cosmetic-escape`, `missing-translation`, `language-construct`, `structure-mismatch`, `freeze-block`, `file-name-pattern`, `generation-failed`, `source-not-found`, `check-paths-unmatched`, `config-error`, `descriptor-error`. `hint` is the suggested fix or `null`.<!--en-->
- Paths are relative to the working directory, with `/` separators.<!--en-->
- On a usage error (exit code `64`) nothing is written to stdout; the message is on stderr.<!--en-->
- `files[].status`: `written` / `unchanged` (генерация), `ok` / `stale` / `missing` (`--check`), `ok` / `invalid` (`--validate`, по записи на исходный файл).<!--ru-->
- `diagnostics[].code` стабилен — ориентируйтесь на него, а не на `message`: `unknown-widget`, `unknown-parameter`, `undeclared-language`, `import-not-found`, `import-outside-root`, `unbalanced-ignore`, `unbalanced-block`, `undefined-variable`, `unresolved-reference`, `cosmetic-escape`, `missing-translation`, `language-construct`, `structure-mismatch`, `freeze-block`, `file-name-pattern`, `generation-failed`, `source-not-found`, `check-paths-unmatched`, `config-error`, `descriptor-error`. `hint` — предлагаемое исправление или `null`.<!--ru-->
- Пути указаны относительно рабочего каталога, с разделителем `/`.<!--ru-->
- При ошибке в аргументах (код `64`) в stdout ничего не пишется; сообщение — в stderr.<!--ru-->

`--format github` prints the same findings as GitHub Actions workflow commands<!--en-->
(`::error file=…,line=…::…`, `::warning …`), so a CI step shows them next to the<!--en-->
offending lines of a pull request; a stale or missing output is reported on the output file.<!--en-->
`--format github` печатает те же находки как команды GitHub Actions<!--ru-->
(`::error file=…,line=…::…`, `::warning …`) — шаг CI показывает их прямо у нужных<!--ru-->
строк pull request; устаревший или отсутствующий выходной файл отмечается на нём самом.<!--ru-->

```bash
nrg --validate --format github "**/*.src.md"
```

#### ${en:'Exit codes', ru:'Коды возврата'}

| ${en:'Code', ru:'Код'} | ${en:'Meaning', ru:'Значение'} |
|:-:|---|
| `0` | ${en:'Success. Warnings alone do not change it.', ru:'Успех. Одни лишь предупреждения на него не влияют.'} |
| `1` | ${en:'`--check`: a generated file differs from the template or is missing — regenerate.', ru:'`--check`: сгенерированный файл расходится с шаблоном или отсутствует — перегенерируйте.'} |
| `2` | ${en:'A template is invalid (`--validate` found an error) or generation failed — fix the template.', ru:'Шаблон некорректен (`--validate` нашёл ошибку) или генерация не удалась — исправьте шаблон.'} |
| `64` | ${en:'The command line is wrong: unknown or conflicting options, a source file that does not exist, patterns that match nothing.', ru:'Неверная командная строка: неизвестные или конфликтующие опции, несуществующий исходный файл, паттерны без совпадений.'} |

With several source files the run exits with the highest code among them. A failed<!--en-->
text-mode `--check` ends with the command that fixes it (`Regenerate with: nrg -f README.src.md`).<!--en-->
При нескольких исходных файлах запуск завершается с наибольшим из их кодов. Неудачный<!--ru-->
`--check` в текстовом режиме заканчивается командой, которая всё исправит (`Regenerate with: nrg -f README.src.md`).<!--ru-->

#### ${en:'What a template writes and reads', ru:'Что шаблон пишет и читает'}

Four read-only queries answer what a tool has to know before it edits a file; none of them<!--en-->
writes anything:<!--en-->
Четыре запроса только на чтение отвечают на вопросы, которые инструмент задаёт, прежде чем<!--ru-->
править файл; ни один из них ничего не записывает:<!--ru-->

```bash
nrg --manifest                          # outputs and inputs of every template
nrg --is-generated README.ru.md         # exit 0 and print README.src.md; exit 1 for a file NRG does not write
nrg --affected docs/Overview.src.md     # templates to regenerate after this file changed
nrg --locate README.ru.md:340           # the template line that produces line 340
```

`--locate FILE:LINE` (or `FILE:FIRST-LAST`) renders the template behind the file and prints, for<!--en-->
each line, where it comes from — the line to edit and the chain of imports that led there:<!--en-->
`--locate FILE:LINE` (или `FILE:FIRST-LAST`) рендерит шаблон, из которого получается файл, и для<!--ru-->
каждой строки печатает, откуда она взялась, — строку, которую нужно править, и цепочку импортов:<!--ru-->

```text
README.ru.md:340: docs/Launching.src.md:45 <- imported at README.src.md:36
README.ru.md:64: README.src.md:32 (widget tableOfContents)
README.ru.md:1: head comment
```

A line a widget produced and that cannot be traced further names the widget's own line;<!--en-->
lines of a frozen region name its opening marker. `--format json` prints<!--en-->
`{"lines": [{"line", "text", "origin": {"file", "line", "widget", "note", "importChain"}}]}`.<!--en-->
The position refers to the file as NRG would generate it now, so regenerate first if it is stale.<!--en-->
Строка, которую создал виджет и которую дальше не проследить, указывает на строку самого виджета;<!--ru-->
строки замороженной области — на её открывающий маркер. `--format json` печатает<!--ru-->
`{"lines": [{"line", "text", "origin": {"file", "line", "widget", "note", "importChain"}}]}`.<!--ru-->
Номер строки относится к файлу в том виде, в каком NRG сгенерировал бы его сейчас, поэтому устаревший файл сначала перегенерируйте.<!--ru-->

- `--manifest` lists, for each template, the output file of every language and its inputs: the template, every file it imports (transitively), the directories `fileTree` lists and the descriptors behind `\${pom.…}` / `\${npm.…}` / `\${gradle.…}`. What cannot be listed as a file is reported as `dynamic`: `url:…` (remote import), `exec:…`, `env:NAME`, `date`. `--format json` prints `{"templates": [{"source", "outputs", "inputs", "dynamic"}]}`.<!--en-->
- `--is-generated <file>` exits `0` and prints the template when the file is an output, `1` otherwise.<!--en-->
- `--affected <file>…` prints the templates that read any of the files; a file inside a directory listed by `fileTree` counts. The output can be passed straight back to NRG: `nrg $(nrg --affected docs/Overview.src.md)`.<!--en-->
- `--manifest` для каждого шаблона перечисляет выходной файл каждого языка и входы: сам шаблон, все импортируемые им файлы (транзитивно), каталоги, которые показывает `fileTree`, и дескрипторы за `\${pom.…}` / `\${npm.…}` / `\${gradle.…}`. То, что нельзя назвать файлом, выводится как `dynamic`: `url:…` (удалённый импорт), `exec:…`, `env:NAME`, `date`. С `--format json` печатается `{"templates": [{"source", "outputs", "inputs", "dynamic"}]}`.<!--ru-->
- `--is-generated <file>` завершается с кодом `0` и печатает шаблон, если файл — результат генерации, иначе с кодом `1`.<!--ru-->
- `--affected <file>…` печатает шаблоны, которые читают любой из файлов; файл внутри каталога, который показывает `fileTree`, тоже считается. Вывод можно сразу передать NRG: `nrg $(nrg --affected docs/Overview.src.md)`.<!--ru-->

The templates are the source files given with `-f` or as arguments; without them NRG takes every<!--en-->
`*.src.md` below the current directory that no other template imports (hidden directories,<!--en-->
`node_modules`, `target` and `build` are skipped).<!--en-->
Шаблоны — это исходные файлы, переданные через `-f` или аргументами; без них NRG берёт все<!--ru-->
`*.src.md` ниже текущего каталога, которые не импортирует другой шаблон (скрытые каталоги,<!--ru-->
`node_modules`, `target` и `build` пропускаются).<!--ru-->

#### ${en:'Logging verbosity', ru:'Уровень логирования'}

Control how much NRG prints to the console with `--log-level`. Accepted<!--en-->
values are `trace`, `debug`, `info` (default), `warn`, and `error` —<!--en-->
each level suppresses messages below it. The environment variable<!--en-->
`NRG_LOG_LEVEL` is consulted when `--log-level` is not supplied, which<!--en-->
is convenient for CI and the Maven plugin. Invalid values abort the<!--en-->
run with a usage error on stderr.<!--en-->
Управляйте детализацией вывода через `--log-level`. Допустимые значения —<!--ru-->
`trace`, `debug`, `info` (по умолчанию), `warn`, `error`; каждый уровень<!--ru-->
подавляет сообщения ниже по важности. Если флаг не задан, используется<!--ru-->
переменная окружения `NRG_LOG_LEVEL` — удобно для CI и Maven-плагина.<!--ru-->
Неизвестное значение приводит к завершению с сообщением об ошибке в stderr.<!--ru-->

```bash
nrg --log-level warn -f /path/to/README.src.md
NRG_LOG_LEVEL=warn nrg -f /path/to/README.src.md
```

#### ${en:'Customising output filenames', ru:'Настройка имён выходных файлов'}

Use `--file-name-pattern <PATTERN>` to override the output filename layout.<!--en-->
Placeholders: `<base>` (source filename without `.src.md`), `<lang>` (language<!--en-->
code as written), `<LANG>` (uppercased). Patterns may contain `/` separators —<!--en-->
intermediate directories are created on demand. Equivalent to setting<!--en-->
`<\!--\@nrg.fileNamePattern=PATTERN-->` in the template; the CLI flag wins.<!--en-->
Use `--default-language-file-name-pattern <PATTERN>` to override the default<!--en-->
language only (mirrors `<\!--\@nrg.defaultLanguageFileNamePattern=PATTERN-->`).<!--en-->
Per-language overrides (`<\!--\@nrg.fileNamePattern.<lang>=...-->`) are<!--en-->
template-only and have no CLI counterpart.<!--en-->
Флаг `--file-name-pattern <PATTERN>` переопределяет схему имени выходного<!--ru-->
файла. Плейсхолдеры: `<base>` (имя исходного файла без `.src.md`), `<lang>`<!--ru-->
(код языка как есть), `<LANG>` (в верхнем регистре). Паттерн может содержать<!--ru-->
`/` — недостающие каталоги создаются автоматически. Эквивалент свойства<!--ru-->
`<\!--\@nrg.fileNamePattern=PATTERN-->`; CLI-флаг побеждает шаблон.<!--ru-->
Флаг `--default-language-file-name-pattern <PATTERN>` переопределяет только<!--ru-->
язык по умолчанию (зеркало `<\!--\@nrg.defaultLanguageFileNamePattern=PATTERN-->`).<!--ru-->
Пер-языковые переопределения (`<\!--\@nrg.fileNamePattern.<lang>=...-->`)<!--ru-->
доступны только в шаблоне и CLI-аналога не имеют.<!--ru-->

```bash
nrg --file-name-pattern '<base>_<LANG>.md' -f README.src.md
nrg --file-name-pattern 'docs/<lang>/<base>.md' --default-language-file-name-pattern '<base>.md' -f README.src.md
```

#### ${en:'Project root', ru:'Корень проекта'}

A template is confined to a **project root**: every path it names must stay inside that directory — the output files it asks for, the files it imports (`import`), the directories it lists (`fileTree`), and the descriptors behind `\${pom.*}` / `\${npm.*}` / `\${gradle.*}` (`nrg.pom.path`, `nrg.npm.path`, `nrg.gradle.path`).<!--en-->
The root is the nearest ancestor of the source file that contains a `.git` entry, or the source file's own directory when there is none. It is never taken from the template.<!--en-->
A path that resolves outside the root (for example `../../somewhere/<base>.md` as a file name pattern, or `path='../../other-project/file.md'` in an import) stops the run with an error; nothing is read from there and nothing is written. `--validate` reports such imports as errors without opening the files.<!--en-->
This is what keeps a template you did not write — a pull request, a freshly cloned repository — from copying your files into a README or overwriting files on your machine. Environment variables are a different matter: `\${env.NAME}` reads any variable of the process, so do not run NRG on untrusted templates with secrets in the environment.<!--en-->
If your layout legitimately needs a wider root, name it explicitly with `--root <dir>` (Maven plugin: `<root>`, GitHub Action: `root`).<!--en-->
Шаблон ограничен **корнем проекта**: любой путь, который он называет, должен оставаться внутри этого каталога — выходные файлы, импортируемые файлы (`import`), перечисляемые каталоги (`fileTree`) и дескрипторы, стоящие за `\${pom.*}` / `\${npm.*}` / `\${gradle.*}` (`nrg.pom.path`, `nrg.npm.path`, `nrg.gradle.path`).<!--ru-->
Корень — ближайший предок исходного файла, содержащий `.git`, а если такого нет — каталог самого исходного файла. Из шаблона корень задать нельзя.<!--ru-->
Путь, выводящий за пределы корня (например, `../../somewhere/<base>.md` в паттерне имени файла или `path='../../other-project/file.md'` в импорте), останавливает запуск с ошибкой: оттуда ничего не читается и туда ничего не записывается. `--validate` сообщает о таких импортах как об ошибках, не открывая файлы.<!--ru-->
Именно это не даёт чужому шаблону — из pull request или только что склонированного репозитория — скопировать ваши файлы в README или перезаписать файлы на вашей машине. С переменными окружения иначе: `\${env.NAME}` читает любую переменную процесса, поэтому не запускайте NRG на недоверенных шаблонах, когда в окружении лежат секреты.<!--ru-->
Если раскладке проекта действительно нужен более широкий корень, задайте его явно: `--root <dir>` (Maven-плагин: `<root>`, GitHub Action: `root`).<!--ru-->

```bash
nrg --root /path/to/workspace -f docs/README.src.md
```

#### ${en:'Line endings', ru:'Перевод строк'}

`--line-ending=auto|lf|crlf` controls the line-ending convention of the<!--en-->
generated output. Default `auto` preserves the existing on-disk file's<!--en-->
convention (CRLF stays CRLF, LF stays LF) and falls back to the platform<!--en-->
default for first-time generation. Use `lf` or `crlf` to pin the output<!--en-->
unconditionally — useful for Windows contributors regenerating Linux-LF<!--en-->
repos and vice versa.<!--en-->
`--line-ending=auto|lf|crlf` управляет переводом строк в сгенерированном<!--ru-->
выводе. По умолчанию `auto` сохраняет конвенцию уже лежащего на диске<!--ru-->
файла (CRLF останется CRLF, LF — LF) и откатывается на системный default<!--ru-->
при первой генерации. Значения `lf` и `crlf` жёстко фиксируют формат —<!--ru-->
полезно, например, для Windows-контрибьюторов, регенерирующих<!--ru-->
Linux-репозитории с LF, и наоборот.<!--ru-->

```bash
nrg --line-ending lf -f README.src.md
nrg --line-ending crlf -f README.src.md
```

In `--check` mode, `auto` ignores LE-only differences against the on-disk<!--en-->
file (regen would have preserved the existing convention anyway), so a<!--en-->
mixed-OS contributor base does not trip CI. Explicit `lf` / `crlf` does<!--en-->
flag a mismatch — that's the user-asked-for invariant.<!--en-->
В режиме `--check` со значением `auto` различия только в переводах строк<!--ru-->
против файла на диске не считаются ошибкой (повторная генерация и так<!--ru-->
сохранила бы существующую конвенцию), поэтому контрибьюторы с разных<!--ru-->
ОС не валят CI. Явные `lf` / `crlf` всё равно сигнализируют о несовпадении<!--ru-->
— это инвариант, который пользователь явно запросил.<!--ru-->

#### ${en:'Header customisation', ru:'Настройка заголовка'}

By default every generated file opens with two HTML comment lines that name<!--en-->
the template it was generated from and ask not to hand-edit the output:<!--en-->
По умолчанию каждый сгенерированный файл начинается с двух строк HTML-<!--ru-->
комментария: они называют шаблон, из которого сгенерирован файл, и просят<!--ru-->
не редактировать его руками:<!--ru-->

```markdown
<\!-- This file was automatically generated by Nanolaba Readme Generator (NRG) from README.src.md -->
<\!-- Do not edit it by hand: change README.src.md and regenerate. See https://github.com/nanolaba/readme-generator -->
```

The template path is relative to the generated file (`../../README.src.md` for<!--en-->
`docs/ru/README.md`), so a reader — or a coding agent — that opens the output first<!--en-->
goes straight to the file to edit. The NRG version is not part of the default<!--en-->
header, so upgrading NRG does not change every generated file.<!--en-->
Путь к шаблону указан относительно сгенерированного файла (`../../README.src.md`<!--ru-->
для `docs/ru/README.md`): читатель — или ИИ-агент, — открывший сначала результат,<!--ru-->
сразу попадает в файл, который нужно править. Версии NRG в заголовке по умолчанию<!--ru-->
нет, поэтому обновление NRG не меняет все сгенерированные файлы.<!--ru-->

Two flags override the header: `--no-header` strips the comment entirely;<!--en-->
`--header-text "..."` replaces it with arbitrary text (use `\n` for line breaks,<!--en-->
also `\r`, `\t`, `\\`). The text may contain the placeholders `<source>` (the<!--en-->
template path, as above), `<lang>` (language code) and `<version>` (NRG version).<!--en-->
The two flags are mutually exclusive. Equivalent template properties<!--en-->
`<\!--\@nrg.noHeader=true-->` and `<\!--\@nrg.headerText=...-->` work the<!--en-->
same way; the CLI flag wins when both are set.<!--en-->
Два флага переопределяют заголовок: `--no-header` полностью убирает<!--ru-->
комментарий, `--header-text "..."` заменяет его произвольным текстом<!--ru-->
(используйте `\n` для переноса строки, также `\r`, `\t`, `\\`). В тексте можно<!--ru-->
использовать плейсхолдеры `<source>` (путь к шаблону, как выше), `<lang>` (код<!--ru-->
языка) и `<version>` (версия NRG). Флаги взаимно исключающие. Эквивалентные<!--ru-->
свойства шаблона `<\!--\@nrg.noHeader=true-->` и `<\!--\@nrg.headerText=...-->`<!--ru-->
работают аналогично; CLI-флаг побеждает значение в шаблоне.<!--ru-->

```bash
nrg --no-header -f README.src.md
nrg --header-text '<!-- Generated from <source> by NRG <version> -->\n<!-- See /wiki for editing rules -->' -f README.src.md
```

A property marker ends at the first `-->`, so a header that is itself an HTML<!--en-->
comment is written in the template with both ends escaped — <code>&lt;&#92;!--</code> and <code>--&#92;&gt;</code>:<!--en-->
Маркер свойства заканчивается на первом `-->`, поэтому заголовок, который сам<!--ru-->
является HTML-комментарием, записывается в шаблоне с экранированием обоих концов —<!--ru-->
<code>&lt;&#92;!--</code> и <code>--&#92;&gt;</code>:<!--ru-->

<code>&lt;!--@nrg.headerText=&lt;&#92;!-- Generated from &lt;source&gt;; edit the template, not this file --&#92;&gt;--&gt;</code>

#### ${en:'Multiple files and glob patterns', ru:'Несколько файлов и glob-паттерны'}

Pass several source files in one invocation, either as explicit positional<!--en-->
arguments or as `glob:`-style patterns:<!--en-->
В одну команду можно передать несколько исходных файлов — либо явным<!--ru-->
списком, либо glob-паттернами:<!--ru-->

```bash
nrg README.src.md docs/Guide.src.md
nrg "docs/**/*.src.md"
```

Glob expansion uses `java.nio.file.PathMatcher` with `glob:` syntax, so<!--en-->
behavior is identical on Windows, Linux, and macOS regardless of the shell.<!--en-->
Quote the pattern to keep it from being expanded by the shell first.<!--en-->
The legacy `-f <file>` flag is still supported as a single-file alias and<!--en-->
is mutually exclusive with positional arguments — passing both prints<!--en-->
an error and exits with status `64`.<!--en-->
Раскрытие glob-паттерна выполняется через `java.nio.file.PathMatcher`<!--ru-->
с синтаксисом `glob:` — поведение одинаково на Windows, Linux и macOS<!--ru-->
независимо от командной оболочки. Заключайте паттерн в кавычки, чтобы<!--ru-->
сначала его не раскрыл сам шелл. Старый флаг `-f <file>` остаётся как<!--ru-->
алиас для единственного файла и несовместим с позиционными аргументами —<!--ru-->
их совместная передача приводит к ошибке и коду возврата `64`.<!--ru-->

By default each input is processed independently: an error on one file<!--en-->
is logged but the rest still run, and the overall exit code is the most<!--en-->
severe one of all files (see [Exit codes](#exit-codes)). Pass `--fail-fast` to stop on the first non-zero result.<!--en-->
По умолчанию каждый файл обрабатывается независимо: ошибка на одном<!--ru-->
логируется, но остальные продолжают выполняться, а общий код возврата —<!--ru-->
самый серьёзный из кодов всех файлов (см. [«Коды возврата»](#коды-возврата)). Флаг `--fail-fast` останавливает<!--ru-->
обработку на первом ненулевом результате.<!--ru-->

```bash
nrg --fail-fast "docs/**/*.src.md"
```

A pattern that matches no files logs a warning. If the entire input set<!--en-->
matches nothing, the run exits with status `64` and the message<!--en-->
`No source files matched any of the supplied patterns`.<!--en-->
Паттерн без совпадений логируется как предупреждение. Если ни один из<!--ru-->
входов не нашёл файлов, программа завершается с кодом `64` и сообщением<!--ru-->
`No source files matched any of the supplied patterns`.<!--ru-->

### ${en:'Use as maven plugin', ru:'Использование как плагина для maven'}

Добавьте следующий код в ваш `pom.xml`:<!--ru-->
Add the following code to your `pom.xml`:<!--en-->

```xml

<plugins>
    <plugin>
        <groupId>com.nanolaba</groupId>
        <artifactId>nrg-maven-plugin</artifactId>
        <version>${stableVersion}</version>
        <configuration>
            <file>
                <item>README.src.md</item>
                <item>another-file.src.md</item>
            </file>
            <logLevel>warn</logLevel>
            <widgets>
                <widget>com.example.MyWidget</widget>
                <widget>com.example.OtherWidget</widget>
            </widgets>
            <check>false</check>
            <failFast>false</failFast>
            <lineEnding>auto</lineEnding>
            <noHeader>false</noHeader>
        </configuration>
        <dependencies>
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>my-widgets</artifactId>
                <version>1.0.0</version>
            </dependency>
        </dependencies>
        <executions>
            <execution>
                <phase>compile</phase>
                <goals>
                    <goal>create-files</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
</plugins>
```

Each `<file>` entry can be a literal path or a glob pattern (same `glob:`<!--en-->
syntax as the CLI). Multi-doc projects can replace a long list with<!--en-->
`<file>**/*.src.md</file>` and let NRG expand it. Set<!--en-->
`<failFast>true</failFast>` (or `-DfailFast=true`) to abort on the first<!--en-->
non-zero result; default `false` preserves today's behavior of aggregating<!--en-->
diagnostics across every matched file.<!--en-->
Каждый элемент `<file>` может быть как явным путём, так и glob-паттерном<!--ru-->
(тот же синтаксис `glob:`, что и в CLI). Многодокументные проекты могут<!--ru-->
заменить длинный список одним `<file>**/*.src.md</file>`, NRG раскроет<!--ru-->
паттерн сам. Параметр `<failFast>true</failFast>` (или `-DfailFast=true`)<!--ru-->
прерывает обработку на первом ненулевом результате; по умолчанию `false` —<!--ru-->
сохраняется сегодняшнее поведение с агрегированием диагностик по всем<!--ru-->
найденным файлам.<!--ru-->

`<noHeader>true</noHeader>` (or `-DnoHeader=true`) drops the auto-generated<!--en-->
two-line head comment from every output file; `<headerText>...</headerText>`<!--en-->
(or `-DheaderText="..."`) replaces it with arbitrary text. Use `\n` for<!--en-->
line breaks. The two parameters are mutually exclusive — passing both<!--en-->
fails the build with a CLI parse error.<!--en-->
`<noHeader>true</noHeader>` (или `-DnoHeader=true`) убирает автоматический<!--ru-->
двухстрочный head-комментарий из каждого выходного файла;<!--ru-->
`<headerText>...</headerText>` (или `-DheaderText="..."`) заменяет его<!--ru-->
произвольным текстом. Используйте `\n` для перевода строк. Параметры<!--ru-->
взаимно исключающие — одновременная передача роняет сборку с ошибкой<!--ru-->
разбора аргументов CLI.<!--ru-->

> [!NOTE]<!--en-->
> **Multi-module (aggregator) projects:** the `create-files` goal is declared<!--en-->
> with `inheritByDefault = false`, so child modules of a `<packaging>pom</packaging>`<!--en-->
> aggregator do **not** re-run NRG in their own working directory by default<!--en-->
> (where `README.src.md` typically does not exist). Declare the plugin once in<!--en-->
> the parent POM and the goal runs only at the root. A child module that<!--en-->
> genuinely needs its own README generation must opt in explicitly with<!--en-->
> `<inherited>true</inherited>` in its own POM.<!--en-->

> [!NOTE]<!--ru-->
> **Многомодульные (aggregator) проекты:** цель `create-files` объявлена с<!--ru-->
> `inheritByDefault = false`, поэтому дочерние модули агрегатора с<!--ru-->
> `<packaging>pom</packaging>` **не** перезапускают NRG в собственном каталоге<!--ru-->
> по умолчанию (где `README.src.md` обычно отсутствует). Объявите плагин один<!--ru-->
> раз в родительском POM, и цель отработает только в корне. Если дочернему<!--ru-->
> модулю всё же нужна собственная генерация README, явно включите её через<!--ru-->
> `<inherited>true</inherited>` в его POM.<!--ru-->

The `<widgets>` entries must name public classes that implement `NRGWidget`<!--en-->
and declare a public no-argument constructor, and their artifact must be<!--en-->
listed under the plugin's own `<dependencies>` so Maven can resolve them.<!--en-->
On a name collision, POM-declared widgets override those declared via the<!--en-->
`nrg.widgets` template property.<!--en-->
Элементы `<widgets>` должны указывать на публичные классы, реализующие<!--ru-->
`NRGWidget` и имеющие публичный конструктор без аргументов; их артефакт<!--ru-->
необходимо подключить через `<dependencies>` самого плагина, чтобы<!--ru-->
Maven мог их найти. При совпадении имён виджеты из POM имеют приоритет<!--ru-->
над объявленными через свойство шаблона `nrg.widgets`.<!--ru-->

Set `<check>true</check>` (or pass `-Dcheck=true` on the command line)<!--en-->
to run the plugin in verification mode: no files are written, and the<!--en-->
build fails with a `MojoExecutionException` and a diff in the log when<!--en-->
the generated output diverges from the committed files. Handy for a<!--en-->
`mvn verify` step in CI to guard against stale READMEs.<!--en-->
Параметр `<check>true</check>` (или `-Dcheck=true` из командной строки)<!--ru-->
переключает плагин в режим проверки: файлы не записываются, при<!--ru-->
расхождении сборка падает с `MojoExecutionException` и выводом diff<!--ru-->
в лог. Удобно для шага `mvn verify` в CI, чтобы не пропустить устаревший<!--ru-->
README.<!--ru-->

Set `<validate>true</validate>` (or pass `-Dvalidate=true`) to scan<!--en-->
the templates for authoring mistakes (the checks listed in<!--en-->
[Validating source templates](#validating-source-templates)) without<!--en-->
generating any output. The build fails with a `MojoExecutionException`<!--en-->
when an error is reported; warnings are only logged. Mutually exclusive with `<check>`.<!--en-->
Параметр `<validate>true</validate>` (или `-Dvalidate=true`) сканирует<!--ru-->
шаблоны на типичные ошибки авторов (проверки перечислены в разделе<!--ru-->
[«Валидация исходных шаблонов»](#валидация-исходных-шаблонов)) без генерации<!--ru-->
файлов. Сборка падает с `MojoExecutionException`, если найдена ошибка;<!--ru-->
предупреждения только выводятся в лог. Несовместим с `<check>`.<!--ru-->

`<format>json</format>` (or `-Dformat=json`) passes `--format` through — see<!--en-->
[Machine-readable output](#machine-readable-output). The build fails on exit code `1`<!--en-->
(`<check>`), `2` and `64` with a message that names which of them it was.<!--en-->
`<format>json</format>` (или `-Dformat=json`) передаёт `--format` — см.<!--ru-->
[«Машиночитаемый вывод»](#машиночитаемый-вывод). Сборка падает на кодах `1`<!--ru-->
(`<check>`), `2` и `64` с сообщением, называющим, какой из них это был.<!--ru-->

`<fileNamePattern>` and `<defaultLanguageFileNamePattern>` (or<!--en-->
`-DfileNamePattern=...` / `-DdefaultLanguageFileNamePattern=...`) mirror<!--en-->
the `--file-name-pattern` / `--default-language-file-name-pattern` CLI flags<!--en-->
and override `nrg.fileNamePattern` / `nrg.defaultLanguageFileNamePattern`<!--en-->
template properties when set.<!--en-->
Параметры `<fileNamePattern>` и `<defaultLanguageFileNamePattern>` (либо<!--ru-->
`-DfileNamePattern=...` / `-DdefaultLanguageFileNamePattern=...`) дублируют<!--ru-->
CLI-флаги `--file-name-pattern` / `--default-language-file-name-pattern` и<!--ru-->
переопределяют свойства шаблона `nrg.fileNamePattern` /<!--ru-->
`nrg.defaultLanguageFileNamePattern`, если заданы.<!--ru-->

Для использования SNAPSHOT-версий также необходимо добавить в `pom.xml` следующий код:<!--ru-->
To use SNAPSHOT versions, you also need to add the following code to your `pom.xml`:<!--en-->

```xml

<pluginRepositories>
    <pluginRepository>
        <id>central.sonatype.com-snapshot</id>
        <url>https://central.sonatype.com/repository/maven-snapshots</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </pluginRepository>
</pluginRepositories>
```

${widget:import(path='GitHubAction.src.md')}

### ${en:'Use as a java-library', ru:'Использование в качестве java-библиотеки'}

**Maven (pom.xml)**

```xml

<dependency>
    <groupId>com.nanolaba</groupId>
    <artifactId>readme-generator</artifactId>
    <version>${stableVersion}</version>
</dependency>  
```

**Gradle (build.gradle)**

```groovy
implementation 'com.nanolaba:readme-generator:${stableVersion}'
```

**${en:'Manual download', ru:'Скачивание вручную'}**

<!--@mavenCentral=https://repo1.maven.org/maven2/com/nanolaba/readme-generator/${stableVersion}-->
Get the JAR from [Maven Central](${mavenCentral}).<!--en-->
Add it to your project's classpath.<!--en-->
Скачайте JAR из [Maven Central](${mavenCentral})<!--ru-->
и добавьте его в classpath проекта.<!--ru-->

After this, you can call the file generation function in your project by passing <!--en-->
the same parameters as in the console application, for example:<!--en-->
После этого вы можете в своем проекте вызывать функцию создания файлов, <!--ru-->
передав те же параметры, что и в консольном приложении, например:<!--ru-->

```java
NRG.main("-f","path-to-file","--charset","UTF-8");
```

An alternative approach — and a more flexible one for configuring program <!--en-->
behavior — is to use the `Generator` class:<!--en-->
Альтернативным вариантом, а также более гибким в плане настройки поведения <!--ru-->
программы, является использование класса `Generator`:<!--ru-->

```java
${widget:import(path='../nrg/src/test/java/com/nanolaba/nrg/examples/GeneratorExample.java')}
```
