## ${en:'Changelog', ru:'История изменений'}

This section summarises the main user-visible changes in each release. For full details, see the git history.<!--en-->
В разделе перечислены основные пользовательские изменения в каждой версии. Подробности — в истории коммитов.<!--ru-->

### ${en:'Unreleased', ru:'В разработке'} (${devVersion})<!--toc.ignore-->

- **`import` widget**: added the `lines`, `region`, `wrap`, `lang`, and `dedent` parameters for fine-grained inclusion of source files.<!--en-->
- Added `<\!--nrg.ignore-->` and paired `<\!--nrg.ignore.begin-->` / `<\!--nrg.ignore.end-->` markers for removing author notes from generated output (also inside imported files).<!--en-->
- **`tableOfContents` widget**: added the `min-depth` and `max-depth` parameters to limit which heading levels appear in the table of contents.<!--en-->
- **`tableOfContents` widget**: added the `min-items` parameter — the widget now skips rendering entirely (title included) when fewer than this many headings survive the filters.<!--en-->
- **`tableOfContents` widget**: added the `anchor-style` parameter (`github` | `gitlab` | `bitbucket`) to match the slugification rules of the target hosting platform.<!--en-->
- Fixed: the `languages` widget now produces correct link targets when rendered inside an imported fragment.<!--en-->
- **Виджет `import`**: добавлены параметры `lines`, `region`, `wrap`, `lang` и `dedent` для точного включения фрагментов файлов.<!--ru-->
- Добавлены маркеры `<\!--nrg.ignore-->` и парные `<\!--nrg.ignore.begin-->` / `<\!--nrg.ignore.end-->` для исключения авторских заметок из результирующих файлов (в т.ч. внутри импортированных файлов).<!--ru-->
- **Виджет `tableOfContents`**: добавлены параметры `min-depth` и `max-depth` для ограничения уровней заголовков, попадающих в оглавление.<!--ru-->
- **Виджет `tableOfContents`**: добавлен параметр `min-items` — если после всех фильтров остаётся меньше заголовков, виджет не рендерится совсем (включая заглавие).<!--ru-->
- **Виджет `tableOfContents`**: добавлен параметр `anchor-style` (`github` | `gitlab` | `bitbucket`) для соответствия правилам формирования якорей на целевой платформе хостинга.<!--ru-->
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
