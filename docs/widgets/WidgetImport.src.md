#### ${en:'Widget', ru:'Виджет'} 'import'

This component enables text import from another document, code file, or template.<!--en-->
Optionally selects a fragment by line range or named region, and wraps the result in a fenced code block.<!--en-->
Этот компонент позволяет импортировать текст из другого документа, файла кода или шаблона.<!--ru-->
Опционально выбирает фрагмент по диапазону строк или именованному региону и оборачивает результат в кодовый блок.<!--ru-->

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

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                                                                            | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------:|
|              path               | ${en:'Path to the imported file', ru:'Путь к импортируемому файлу'}                                                                                                           |                                                   |
|             charset             | ${en:'File encoding', ru:'Кодировка, в которой написан файл'}                                                                                                                 |                      `UTF-8`                      |
|          run-generator          | ${en:'Should the system perform text generation when importing template files', ru:'Нужно ли при импорте файла-шаблона произвести генерацию текста'}                          |                      `true`                       |
|              lines              | ${en:'Line range(s) to extract: e.g. `10-20`, `10-`, `-20`, `15`, `10-20,30-35`', ru:'Диапазон(ы) строк для извлечения: например `10-20`, `10-`, `-20`, `15`, `10-20,30-35`'} |                                                   |
|             region              | ${en:'Name of a region marked in the source file', ru:'Имя региона, помеченного маркерами в исходном файле'}                                                                  |                                                   |
|              wrap               | ${en:'Wrap output in a code fence: `true`, `false`', ru:'Оборачивать ли вывод в кодовый блок: `true`, `false`'}                                                               |                      `false`                      |
|              lang               | ${en:'Language tag for the fence; `auto` detects from file extension', ru:'Язык кодового блока; `auto` определяет по расширению файла'}                                       |                      `auto`                       |
|             dedent              | ${en:'Strip common leading whitespace: `auto`, `true`, `false`', ru:'Удалять общий отступ: `auto`, `true`, `false`'}                                                          |                      `auto`                       |
|               url               | ${en:'HTTP(S) URL to fetch (mutually exclusive with `path`); requires `nrg.allowRemoteImports=true`', ru:'HTTP(S) URL для загрузки (взаимоисключим с `path`); требуется `nrg.allowRemoteImports=true`'} |                                                   |
|              cache              | ${en:'Cache TTL: `<int>{s,m,h,d}` or `none` (e.g. `1h`, `7d`)', ru:'TTL кэша: `<int>{s,m,h,d}` или `none` (например `1h`, `7d`)'}                                             |                      `none`                       |
|             timeout             | ${en:'HTTP timeout: `<int>{s,m,h,d}` (cannot be `none`)', ru:'HTTP-тайм-аут: `<int>{s,m,h,d}` (не может быть `none`)'}                                                        |                      `60s`                        |
|             sha256              | ${en:'64 hex chars; verifies fetched bytes; recommended for reproducibility', ru:'64 шестнадцатеричных символа; проверяет загруженные данные; рекомендуется для воспроизводимости'} |                                                   |

When importing a template file, generation is performed using variables declared in the parent file.<!--en-->
This allows defining global variables in the root file and reusing them across all imported templates.<!--en-->
При импорте файла шаблона генерация выполняется с использованием переменных, объявленных в родительском файле.<!--ru-->
Это позволяет определять глобальные переменные в корневом файле и повторно <!--ru-->
использовать их во всех импортированных шаблонах.<!--ru-->

${en:'**Wrapping in a code fence**', ru:'**Оборачивание в кодовый блок**'}

By default (`wrap='false'`), the widget emits the selected content as-is, which preserves the behavior of existing template-composition imports (`*.src.md` files).<!--en-->
To wrap a code fragment in a fenced block, set `wrap='true'` explicitly. The fence language is taken from `lang`, or detected from the file extension when `lang='auto'` (the default).<!--en-->
По умолчанию (`wrap='false'`) виджет выводит выбранный фрагмент без изменений, что сохраняет поведение существующих шаблонных импортов (файлов `*.src.md`).<!--ru-->
Чтобы обернуть фрагмент кода в кодовый блок, явно задайте `wrap='true'`. Язык блока берётся из `lang`, либо определяется по расширению файла при `lang='auto'` (по умолчанию).<!--ru-->

${en:'**Auto-dedent**', ru:'**Автоматическое удаление отступа**'}

When `dedent='auto'` (the default), common leading whitespace is stripped automatically if `lines` or `region` is set, and left untouched otherwise.<!--en-->
Use `dedent='true'` or `dedent='false'` to force the behavior explicitly.<!--en-->
Когда `dedent='auto'` (по умолчанию), общий отступ удаляется автоматически, если задан `lines` или `region`, и сохраняется в остальных случаях.<!--ru-->
Используйте `dedent='true'` или `dedent='false'`, чтобы задать поведение явно.<!--ru-->

${en:'**Region markers**', ru:'**Маркеры регионов**'}

Mark a region in the source file using `nrg:begin:NAME` and `nrg:end:NAME` tokens inside any comment.<!--en-->
The matching is language-agnostic — the widget recognizes the markers regardless of the surrounding comment syntax:<!--en-->
Пометьте регион в исходном файле с помощью токенов `nrg:begin:NAME` и `nrg:end:NAME` внутри любого комментария.<!--ru-->
Сопоставление не зависит от языка — виджет распознаёт маркеры независимо от синтаксиса окружающего комментария:<!--ru-->

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

${en:'**Remote imports**', ru:'**Удалённый импорт**'}

The `url` parameter fetches content over HTTP(S) and is mutually exclusive with `path`.<!--en-->
Remote imports are opt-in: the template must set `nrg.allowRemoteImports=true` as a standard NRG property marker, otherwise any `url=` invocation fails the build with a clear error.<!--en-->
Параметр `url` загружает содержимое по HTTP(S) и взаимоисключим с `path`.<!--ru-->
Удалённый импорт включается явно: в шаблоне должно быть задано свойство `nrg.allowRemoteImports=true` (как стандартный маркер свойства NRG), иначе любой вызов с `url=` завершит сборку с понятной ошибкой.<!--ru-->

The cache directory defaults to `~/.nrg/cache` and can be overridden by setting the `nrg.cacheDir` template property to the desired path.<!--en-->
The `cache` parameter sets the TTL using the grammar `<int>{s,m,h,d}` (or `none` to disable caching), and `timeout` accepts the same grammar but cannot be `none` (default `60s`).<!--en-->
Каталог кэша по умолчанию `~/.nrg/cache` и может быть переопределён через свойство шаблона `nrg.cacheDir` со значением нужного пути.<!--ru-->
Параметр `cache` задаёт TTL по грамматике `<int>{s,m,h,d}` (или `none` для отключения кэша), а `timeout` принимает ту же грамматику, но не может быть `none` (по умолчанию `60s`).<!--ru-->

The `sha256` parameter (64 hex chars) is strongly recommended — it pins the fetched bytes for reproducible builds and supply-chain safety.<!--en-->
When `sha256` is omitted, NRG logs an INFO line with the actual hash so it can be copied back into the widget call.<!--en-->
For CI gates, set the system property `-Dnrg.requireSha256ForRemote=true` (default `false`) — remote imports without `sha256` will then fail the build.<!--en-->
Параметр `sha256` (64 шестнадцатеричных символа) настоятельно рекомендуется — он фиксирует загруженные данные для воспроизводимых сборок и безопасности цепочки поставок.<!--ru-->
Если `sha256` не указан, NRG выводит в журнал INFO-строку с фактическим хэшем, чтобы её можно было скопировать обратно в вызов виджета.<!--ru-->
Для CI-проверок задайте системное свойство `-Dnrg.requireSha256ForRemote=true` (по умолчанию `false`) — после этого удалённые импорты без `sha256` будут завершать сборку с ошибкой.<!--ru-->

If the network is unreachable but a cached response exists, NRG uses it (even if stale relative to `cache` TTL) and logs a warning; without any cache entry the build fails.<!--en-->
Если сеть недоступна, но в кэше есть запись, NRG использует её (даже если она устарела относительно `cache` TTL) и выводит предупреждение; при отсутствии записи в кэше сборка завершится с ошибкой.<!--ru-->

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

All import errors — both local and remote — now fail the build with a non-zero exit code.<!--en-->
This is a behavior change from earlier NRG versions, where local-import errors silently produced empty content.<!--en-->
The only non-throw cases are stale-cache fallback when the network is unreachable (warn-only) and cache filesystem hiccups (cache is skipped, fetch continues).<!--en-->
Все ошибки импорта — как локального, так и удалённого — теперь приводят к завершению сборки с ненулевым кодом возврата.<!--ru-->
Это изменение поведения по сравнению с предыдущими версиями NRG, где ошибки локального импорта молча давали пустое содержимое.<!--ru-->
Исключения составляют только использование устаревшего кэша при недоступной сети (только предупреждение) и сбои файловой системы кэша (кэш пропускается, загрузка продолжается).<!--ru-->

---
