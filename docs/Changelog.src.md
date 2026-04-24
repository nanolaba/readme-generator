## ${en:'Changelog', ru:'История изменений'}

This section summarises the main user-visible changes in each release. For full details, see the git history.<!--en-->
В разделе перечислены основные пользовательские изменения в каждой версии. Подробности — в истории коммитов.<!--ru-->

### ${en:'Unreleased', ru:'В разработке'} (${devVersion})<!--toc.ignore-->

- **`import` widget**: added the `lines`, `region`, `wrap`, `lang`, and `dedent` parameters for fine-grained inclusion of source files.<!--en-->
- Added `<\!--nrg.ignore-->` and paired `<\!--nrg.ignore.begin-->` / `<\!--nrg.ignore.end-->` markers for removing author notes from generated output (also inside imported files).<!--en-->
- **`tableOfContents` widget**: added the `min-depth` and `max-depth` parameters to limit which heading levels appear in the table of contents.<!--en-->
- **`tableOfContents` widget**: added the `min-items` parameter — the widget now skips rendering entirely (title included) when fewer than this many headings survive the filters.<!--en-->
- **`tableOfContents` widget**: added the `anchor-style` parameter (`github` | `gitlab` | `bitbucket`) to match the slugification rules of the target hosting platform.<!--en-->
- **Log levels**: added the `--log-level` CLI flag (`trace|debug|info|warn|error`, default `info`), the `NRG_LOG_LEVEL` environment variable fallback, and a matching `<logLevel>` Maven plugin parameter.<!--en-->
- **`--stdout` flag**: new CLI flag that streams generated output to standard output instead of writing files; pair with `--language <code>` to select a single variant.<!--en-->
- **Custom widgets from CLI and templates**: the `nrg.widgets` template property and the `--widgets` / `--classpath` CLI flags let users register custom `NRGWidget` implementations without a custom launcher.<!--en-->
- **Custom widgets in the Maven plugin**: the `nrg-maven-plugin` gains a `<widgets>` parameter; invalid entries fail the build with a descriptive `MojoExecutionException`. POM widgets override template-declared ones on name collision.<!--en-->
- Widget resolution now prefers the last registration on name collision, so user widgets shadow built-ins with the same name.<!--en-->
- **`--check` flag**: CI-friendly verification mode that compares generated output against files on disk, prints a diff to stderr on mismatch, and exits with status `1`. The `nrg-maven-plugin` exposes it via `<check>` and fails the build with a `MojoExecutionException`.<!--en-->
- **`alert` widget**: renders GitHub-flavored alert blocks (`> [!NOTE]`, `> [!WARNING]`, `> [!TIP]`, `> [!IMPORTANT]`, `> [!CAUTION]`) from a single `\${widget:alert(type='...', text='...')}` call, with `\n` escapes for multi-line body text.<!--en-->
- **`badge` widget**: renders shields.io badges for `maven-central`, `license`, `github-release`, `github-stars`, and a free-form `custom` variant — no more hand-crafted URLs.<!--en-->
- **`math` widget**: renders LaTeX formulas via GitHub's native `$…$` / `$$…$$` delimiters or as `![alt](…)` images through a LaTeX-to-SVG service (default: `latex.codecogs.com`).<!--en-->
- **`exec` widget (opt-in)**: runs an external command and embeds its stdout. Disabled by default; enable with `--allow-exec` (CLI) or `<allowExec>true</allowExec>` (Maven plugin). Supports `cwd`, `timeout`, `trim`, and `codeblock` parameters.<!--en-->
- Widget parameters may now contain `{` and `}` (LaTeX-friendly); the tag regex now delimits parameters by `(` / `)` instead of `}`.<!--en-->
- Fixed: the `languages` widget now produces correct link targets when rendered inside an imported fragment.<!--en-->
- **Виджет `import`**: добавлены параметры `lines`, `region`, `wrap`, `lang` и `dedent` для точного включения фрагментов файлов.<!--ru-->
- Добавлены маркеры `<\!--nrg.ignore-->` и парные `<\!--nrg.ignore.begin-->` / `<\!--nrg.ignore.end-->` для исключения авторских заметок из результирующих файлов (в т.ч. внутри импортированных файлов).<!--ru-->
- **Виджет `tableOfContents`**: добавлены параметры `min-depth` и `max-depth` для ограничения уровней заголовков, попадающих в оглавление.<!--ru-->
- **Виджет `tableOfContents`**: добавлен параметр `min-items` — если после всех фильтров остаётся меньше заголовков, виджет не рендерится совсем (включая заглавие).<!--ru-->
- **Виджет `tableOfContents`**: добавлен параметр `anchor-style` (`github` | `gitlab` | `bitbucket`) для соответствия правилам формирования якорей на целевой платформе хостинга.<!--ru-->
- **Уровни логирования**: добавлен флаг CLI `--log-level` (`trace|debug|info|warn|error`, по умолчанию `info`), резервная переменная окружения `NRG_LOG_LEVEL` и соответствующий параметр `<logLevel>` в Maven-плагине.<!--ru-->
- **Флаг `--stdout`**: новый CLI-флаг, выводящий сгенерированный результат в stdout вместо записи файлов; в паре с `--language <код>` печатает только один языковой вариант.<!--ru-->
- **Пользовательские виджеты в CLI и шаблонах**: свойство шаблона `nrg.widgets` и флаги CLI `--widgets` / `--classpath` позволяют регистрировать собственные реализации `NRGWidget` без необходимости писать свой лаунчер.<!--ru-->
- **Пользовательские виджеты в Maven-плагине**: в `nrg-maven-plugin` добавлен параметр `<widgets>`; некорректные записи прерывают сборку с понятным `MojoExecutionException`. Виджеты из POM имеют приоритет над объявленными через свойство шаблона при совпадении имён.<!--ru-->
- При совпадении имён поиск виджета теперь возвращает последний зарегистрированный, что позволяет пользовательским виджетам перекрывать встроенные.<!--ru-->
- **Флаг `--check`**: режим проверки для CI, сравнивает сгенерированный вывод с файлами на диске, выводит diff в stderr при расхождении и завершается с кодом `1`. `nrg-maven-plugin` предоставляет соответствующий параметр `<check>` и падает с `MojoExecutionException`.<!--ru-->
- **Виджет `alert`**: формирует alert-блоки в стиле GitHub (`> [!NOTE]`, `> [!WARNING]`, `> [!TIP]`, `> [!IMPORTANT]`, `> [!CAUTION]`) одним вызовом `\${widget:alert(type='...', text='...')}`, поддерживает `\n` для многострочного текста.<!--ru-->
- **Виджет `badge`**: формирует shields.io-бейджи для `maven-central`, `license`, `github-release`, `github-stars` и свободного `custom`-варианта — URL-адреса больше не нужно собирать вручную.<!--ru-->
- **Виджет `math`**: рендерит формулы LaTeX через встроенные разделители GitHub `$…$` / `$$…$$` либо как `![alt](…)`-картинку через LaTeX-to-SVG сервис (по умолчанию `latex.codecogs.com`).<!--ru-->
- **Виджет `exec` (opt-in)**: выполняет внешнюю команду и вставляет её stdout. По умолчанию выключен; включается через `--allow-exec` (CLI) или `<allowExec>true</allowExec>` (Maven-плагин). Поддерживает параметры `cwd`, `timeout`, `trim` и `codeblock`.<!--ru-->
- В параметрах виджетов теперь разрешены `{` и `}` (удобно для LaTeX); регулярка тега разделяет параметры скобками `(` / `)` вместо `}`.<!--ru-->
- Исправлено: виджет `languages` теперь правильно формирует ссылки при использовании внутри импортированного фрагмента.<!--ru-->

### 0.3<!--toc.ignore-->

- Published under an open-source license.<!--en-->
- **Table of contents**: overhauled heading-to-anchor generation to match GitHub's rules, with Unicode-aware slugification.<!--en-->
- Fixed: TOC links were malformed for headings containing colons or commas.<!--en-->
- Removed stray console output from the `tableOfContents` widget.<!--en-->
- Опубликовано под лицензией с открытым исходным кодом.<!--ru-->
- **Оглавление**: переработан механизм генерации якорей — теперь он соответствует правилам GitHub и корректно работает с Unicode.<!--ru-->
- Исправлено: ломались ссылки в оглавлении для заголовков с двоеточиями и запятыми.<!--ru-->
- Убран лишний вывод в консоль из виджета `tableOfContents`.<!--ru-->

### 0.2<!--toc.ignore-->

- Added the **`import`** widget for including external `*.src.md` files into a template.<!--en-->
- Added `ExampleWidget` to the test sources as a reference implementation for custom widgets.<!--en-->
- Добавлен виджет **`import`** для подключения внешних `*.src.md`-файлов в шаблон.<!--ru-->
- Добавлен `ExampleWidget` в тестовые исходники — как пример реализации пользовательского виджета.<!--ru-->

### 0.1<!--toc.ignore-->

${en:'First public release.', ru:'Первый публичный выпуск.'}

- Core template engine with property declarations (`<!--\@key=value-->`) and `\${var}` substitution.<!--en-->
- Multi-language output via `\${en:'…', ru:'…'}` constructs and language-tagged lines (`<\!--en-->`, `<\!--ru-->`).<!--en-->
- Built-in widgets: `languages`, `tableOfContents`, `date`, `todo`.<!--en-->
- `<\!--toc.ignore-->` marker for excluding headings from the table of contents.<!--en-->
- Escape-character support in variables and widget parameters, including doubled-quote escaping.<!--en-->
- CLI with `-f`, `--charset`, `--version`, and `-h` flags; launcher scripts `nrg.sh` / `nrg.bat`; jar-with-dependencies assembly.<!--en-->
- Maven plugin (`nrg-maven-plugin`) with the `create-files` goal.<!--en-->
- Java 8 compatibility and publication to Maven Central.<!--en-->
- Базовый движок шаблонов с поддержкой свойств (`<!--\@key=value-->`) и подстановок `\${var}`.<!--ru-->
- Многоязычный вывод через конструкции `\${en:'…', ru:'…'}` и строки с языковыми тегами (`<\!--en-->`, `<\!--ru-->`).<!--ru-->
- Встроенные виджеты: `languages`, `tableOfContents`, `date`, `todo`.<!--ru-->
- Маркер `<\!--toc.ignore-->` для исключения заголовков из оглавления.<!--ru-->
- Поддержка экранирования символов в переменных и параметрах виджетов, включая удвоение кавычек.<!--ru-->
- CLI с флагами `-f`, `--charset`, `--version`, `-h`; скрипты запуска `nrg.sh` / `nrg.bat`; сборка jar-with-dependencies.<!--ru-->
- Maven-плагин (`nrg-maven-plugin`) с целью `create-files`.<!--ru-->
- Совместимость с Java 8 и публикация в Maven Central.<!--ru-->
