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

---
