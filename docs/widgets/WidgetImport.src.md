#### ${en:'Widget', ru:'Виджет'} 'import'

This component imports text from another document, code file, or template.<!--en-->
It can select a fragment by line range or named region and wrap the result in a fenced code block.<!--en-->
Этот компонент импортирует текст из другого документа, файла с кодом или шаблона.<!--ru-->
Он может выбрать фрагмент по диапазону строк или именованному региону и обернуть результат в кодовый блок.<!--ru-->

<table>
<tr><th>${en:'Basic usage example', ru:'Базовый пример использования'}</th></tr>
<tr><td>

``` 
\${widget:import(path='path/to/your/file/document.txt')} 
\${widget:import(path='path/to/your/file/document.txt', charset='windows-1251')} 
\${widget:import(path='path/to/your/file/template.src.md')} 
\${widget:import(path='path/to/your/file/template.src.md', run-generator='false')}
```

</td></tr>
</table>

<table>
<tr><th>${en:'Code import example', ru:'Пример импорта кода'}</th></tr>
<tr><td>

``` 
\${widget:import(path='Foo.java', region='example', wrap='true')} 
\${widget:import(path='Foo.java', lines='10-20', wrap='true')} 
\${widget:import(path='Foo.java', lines='10-20,30-35', wrap='true')} 
\${widget:import(path='Foo.java', lang='go', wrap='true')} 
\${widget:import(path='Foo.java', region='example', wrap='true', dedent='false')}
```

</td></tr>
</table>

${en:'Widget parameters', ru:'Свойства виджета'}:

<!-- nrg:begin:reference --><!--nrg.ignore-->
| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                                                                            | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------:|
|              path               | ${en:'Path to the imported file, relative to the importing file. Must lie inside the [project root](#project-root)', ru:'Путь к импортируемому файлу относительно импортирующего файла. Должен лежать внутри [корня проекта](#корень-проекта)'} |                                                   |
|             charset             | ${en:'File encoding', ru:'Кодировка, в которой написан файл'}                                                                                                                 |                      `UTF-8`                      |
|          run-generator          | ${en:'Whether to process the imported text as a template', ru:'Обрабатывать ли импортированный текст как шаблон'}                          |                      `true`                       |
|              lines              | ${en:'Line range(s) to extract: e.g. `10-20`, `10-`, `-20`, `15`, `10-20,30-35`', ru:'Диапазон(ы) строк для извлечения: например `10-20`, `10-`, `-20`, `15`, `10-20,30-35`'} |                                                   |
|             region              | ${en:'Name of a region marked in the source file', ru:'Имя региона, помеченного маркерами в исходном файле'}                                                                  |                                                   |
|              wrap               | ${en:'Wrap output in a code fence: `true`, `false`', ru:'Оборачивать ли вывод в кодовый блок: `true`, `false`'}                                                               |                      `false`                      |
|              lang               | ${en:'Language tag for the fence; `auto` detects from file extension', ru:'Язык кодового блока; `auto` определяет по расширению файла'}                                       |                      `auto`                       |
|             dedent              | ${en:'Strip common leading whitespace: `auto`, `true`, `false`', ru:'Удалять общий отступ: `auto`, `true`, `false`'}                                                          |                      `auto`                       |
|         heading-offset          | ${en:'Shift ATX heading levels by an integer (clamped to 1..6); cannot combine with wrap=''true''', ru:'Сдвинуть уровни ATX-заголовков на указанное число (с ограничением до 1..6); нельзя сочетать с wrap=''true'''} |                       `0`                         |
|               url               | ${en:'HTTP(S) URL to fetch (mutually exclusive with `path`); requires `nrg.allowRemoteImports=true`', ru:'HTTP(S) URL для загрузки (нельзя сочетать с `path`); требуется `nrg.allowRemoteImports=true`'} |                                                   |
|              cache              | ${en:'Cache TTL: `<int>{s,m,h,d}` or `none` (e.g. `1h`, `7d`)', ru:'TTL кэша: `<int>{s,m,h,d}` или `none` (например `1h`, `7d`)'}                                             |                      `none`                       |
|             timeout             | ${en:'HTTP timeout: `<int>{s,m,h,d}` (cannot be `none`)', ru:'HTTP-тайм-аут: `<int>{s,m,h,d}` (не может быть `none`)'}                                                        |                      `60s`                        |
|             sha256              | ${en:'64 hex chars; verifies fetched bytes; recommended for reproducibility', ru:'64 шестнадцатеричных символа; проверяет загруженные данные; рекомендуется для воспроизводимости'} |                                                   |
<!-- nrg:end:reference --><!--nrg.ignore-->

An imported template is rendered with the variables declared in the parent file,<!--en-->
so variables from the root file are available in every imported template.<!--en-->
Импортированный шаблон обрабатывается с переменными, объявленными в родительском файле,<!--ru-->
поэтому переменные из корневого файла доступны во всех импортированных шаблонах.<!--ru-->

${en:'**Wrapping in a code fence**', ru:'**Оборачивание в кодовый блок**'}

By default (`wrap='false'`), the widget emits the selected content as-is.<!--en-->
To wrap a code fragment in a fenced block, set `wrap='true'`. The fence language is taken from `lang`, or detected from the file extension when `lang='auto'` (the default).<!--en-->
По умолчанию (`wrap='false'`) виджет выводит выбранный фрагмент без изменений.<!--ru-->
Чтобы обернуть фрагмент кода в кодовый блок, задайте `wrap='true'`. Язык блока берётся из `lang`, либо определяется по расширению файла при `lang='auto'` (по умолчанию).<!--ru-->

The widget can sit on an indented line, for example inside a list item: every emitted line — both fences and the code between them — gets the indentation of the widget line, so the block stays inside the item.<!--en-->
Виджет можно ставить в строку с отступом, например внутри элемента списка: каждая выводимая строка — обе ограды и код между ними — получает отступ строки виджета, и блок остаётся внутри элемента.<!--ru-->

${en:'**Auto-dedent**', ru:'**Автоматическое удаление отступа**'}

When `dedent='auto'` (the default), common leading whitespace is stripped if `lines` or `region` is set, and left untouched otherwise.<!--en-->
Use `dedent='true'` or `dedent='false'` to force the behavior.<!--en-->
Когда `dedent='auto'` (по умолчанию), общий отступ удаляется, если задан `lines` или `region`, и сохраняется в остальных случаях.<!--ru-->
Используйте `dedent='true'` или `dedent='false'`, чтобы задать поведение явно.<!--ru-->

${en:'**Region markers**', ru:'**Маркеры регионов**'}

Mark a region in the source file using `nrg:begin:NAME` and `nrg:end:NAME` tokens inside any comment.<!--en-->
Any comment syntax works:<!--en-->
Пометьте регион в исходном файле с помощью токенов `nrg:begin:NAME` и `nrg:end:NAME` внутри любого комментария.<!--ru-->
Синтаксис комментария может быть любым:<!--ru-->

<table>
<tr><th>${en:'Comment style examples', ru:'Примеры стилей комментариев'}</th></tr>
<tr><td>

```
// nrg:begin:example          (Java, JavaScript, Kotlin, Go, Rust, C, C++, C#)
<!-- nrg:begin:example -->    (HTML, XML, Markdown)
/* nrg:begin:example */       (CSS, C-style block comments)
-- nrg:begin:example          (SQL, Lua, Haskell)
# nrg:begin:example           (Python, Ruby, Bash, YAML, TOML)
```

</td></tr>
</table>

Region names match the pattern `[A-Za-z0-9_-]+`. Region markers are stripped from the output.<!--en-->
Nested regions are supported — when extracting an outer region, inner region markers are also stripped from the output.<!--en-->
Имена регионов соответствуют шаблону `[A-Za-z0-9_-]+`. Строки-маркеры регионов исключаются из вывода.<!--ru-->
Вложенные регионы поддерживаются — при извлечении внешнего региона маркеры внутренних регионов также удаляются из вывода.<!--ru-->

${en:'**Heading offset**', ru:'**Сдвиг уровней заголовков**'}

`heading-offset='N'` shifts every ATX heading (`#`, `##`, …, `######`) in the imported content by `N` levels — useful when an imported `.src.md` has its own heading hierarchy but is included under a parent section.<!--en-->
Levels are clamped to `[1, 6]`; an import that clamps any heading logs one warning.<!--en-->
Lines inside fenced code blocks (```` ``` ```` or `~~~`) are not shifted, so a `# bash comment` inside a fence stays a `# bash comment`. A fence line may carry a language marker (`` ```<\!--en--> ``); the marker is ignored when fences are matched.<!--en-->
Setext-style headings (`====` / `----`) and indented (4-space) code blocks are not detected — prefer ATX headings and fenced code blocks in imports that use this parameter.<!--en-->
Combining `heading-offset` with a non-zero value and `wrap='true'` fails the build.<!--en-->
`heading-offset='N'` сдвигает каждый ATX-заголовок (`#`, `##`, …, `######`) в импортируемом содержимом на `N` уровней — это полезно, когда у импортируемого `.src.md` собственная иерархия заголовков, но он включается в раздел родителя.<!--ru-->
Уровни ограничиваются диапазоном `[1, 6]`; если импорт ограничил хотя бы один заголовок, в журнал пишется одно предупреждение.<!--ru-->
Строки внутри блоков кода (```` ``` ```` или `~~~`) не сдвигаются, поэтому `# bash-комментарий` внутри блока кода остаётся `# bash-комментарием`. Строка-ограда может нести языковой маркер (`` ```<\!--en--> ``); при сопоставлении оград он не учитывается.<!--ru-->
Setext-заголовки (`====` / `----`) и блоки кода с отступом 4 пробела не распознаются — в импортах с этим параметром используйте ATX-заголовки и ограждённые кодовые блоки.<!--ru-->
Сочетание `heading-offset` с ненулевым значением и `wrap='true'` приводит к ошибке сборки.<!--ru-->

${en:'**Remote imports**', ru:'**Удалённый импорт**'}

The `url` parameter fetches content over HTTP(S) and is mutually exclusive with `path`.<!--en-->
Remote imports are opt-in: the template must set `nrg.allowRemoteImports=true`, otherwise any `url=` call fails the build.<!--en-->
Параметр `url` загружает содержимое по HTTP(S); вместе с `path` его задавать нельзя.<!--ru-->
Удалённый импорт включается явно: в шаблоне должно быть задано свойство `nrg.allowRemoteImports=true`, иначе любой вызов с `url=` завершает сборку ошибкой.<!--ru-->

The cache directory defaults to `~/.nrg/cache`; the template property `nrg.cacheDir` sets another path.<!--en-->
The `cache` parameter sets the TTL in the form `<int>{s,m,h,d}` (or `none` to disable caching), and `timeout` accepts the same form but cannot be `none` (default `60s`).<!--en-->
Каталог кэша по умолчанию — `~/.nrg/cache`; другой путь задаёт свойство шаблона `nrg.cacheDir`.<!--ru-->
Параметр `cache` задаёт TTL в формате `<int>{s,m,h,d}` (или `none` для отключения кэша), а `timeout` принимает тот же формат, но не может быть `none` (по умолчанию `60s`).<!--ru-->

The `sha256` parameter (64 hex chars) is recommended: it pins the fetched bytes, so the build is reproducible and fails if the content on the server changes.<!--en-->
When `sha256` is omitted, NRG logs an INFO line with the actual hash so it can be copied back into the widget call.<!--en-->
For CI gates, set the system property `-Dnrg.requireSha256ForRemote=true` (default `false`) — remote imports without `sha256` will then fail the build.<!--en-->
Параметр `sha256` (64 шестнадцатеричных символа) рекомендуется: он фиксирует загруженные данные, поэтому сборка воспроизводима, а если содержимое на сервере изменится, она завершится ошибкой.<!--ru-->
Если `sha256` не указан, NRG выводит в журнал INFO-строку с фактическим хэшем, чтобы его можно было скопировать в вызов виджета.<!--ru-->
Для CI-проверок задайте системное свойство `-Dnrg.requireSha256ForRemote=true` (по умолчанию `false`) — после этого удалённые импорты без `sha256` будут завершать сборку с ошибкой.<!--ru-->

A remote **template** is a special case: when the fetched text is rendered as a template (`run-generator='true'`, which is the default for URLs ending in `.src.md`), `sha256` is mandatory. A template can import your files, read your project descriptors and environment variables, so without a pin whoever controls the URL or the connection would decide what ends up in your README. Either add `sha256`, or pass `run-generator='false'` to embed the remote text as is. The `exec` widget never runs inside a remote template.<!--en-->
**Удалённый шаблон** — особый случай: если загруженный текст рендерится как шаблон (`run-generator='true'`; это значение по умолчанию для URL, оканчивающихся на `.src.md`), параметр `sha256` обязателен. Шаблон может импортировать ваши файлы, читать дескрипторы проекта и переменные окружения, поэтому без закрепления тот, кто контролирует URL или соединение, решал бы, что попадёт в ваш README. Либо добавьте `sha256`, либо передайте `run-generator='false'`, чтобы вставить удалённый текст как есть. Виджет `exec` внутри удалённого шаблона не выполняется никогда.<!--ru-->

The download itself is bounded: `timeout` covers the whole exchange (connection, redirects and body), a response larger than 16 MiB is refused, a redirect may only lead to another `http(s)` URL and never from `https` down to `http`. Fetching over plain `http://` without `sha256` logs a warning.<!--en-->
Сама загрузка ограничена: `timeout` покрывает весь обмен (соединение, редиректы и тело), ответ больше 16 МиБ отклоняется, редирект может вести только на другой `http(s)`-адрес и никогда — с `https` на `http`. При загрузке по обычному `http://` без `sha256` в журнал пишется предупреждение.<!--ru-->

If the network is unreachable but a cached response exists, NRG uses it (even if stale relative to `cache` TTL) and logs a warning; without any cache entry the build fails.<!--en-->
Если сеть недоступна, но в кэше есть запись, NRG использует её (даже если она устарела относительно `cache` TTL) и выводит предупреждение; если записи в кэше нет, сборка завершается ошибкой.<!--ru-->

<table>
<tr><th>${en:'Secure remote import example', ru:'Пример безопасного удалённого импорта'}</th></tr>
<tr><td>

```
\${widget:import(url='https://raw.githubusercontent.com/myorg/shared-docs/main/CONTRIBUTING.md',
                 cache='1h',
                 sha256='abc123...')}
```

</td></tr>
</table>

${en:'**Error semantics**', ru:'**Семантика ошибок**'}

All import errors — both local and remote — fail the build with a non-zero exit code.<!--en-->
In earlier NRG versions a failed local import silently produced empty content.<!--en-->
Two cases only log a warning: the network is unreachable and a stale cache entry exists (NRG uses it), or a cache entry cannot be read or written (NRG skips the cache and fetches).<!--en-->
Все ошибки импорта — как локального, так и удалённого — завершают сборку с ненулевым кодом возврата.<!--ru-->
В предыдущих версиях NRG ошибка локального импорта молча давала пустое содержимое.<!--ru-->
В двух случаях выводится только предупреждение: сеть недоступна, а в кэше есть устаревшая запись (NRG использует её), или запись кэша не удаётся прочитать либо записать (NRG обходится без кэша и загружает файл).<!--ru-->

---
