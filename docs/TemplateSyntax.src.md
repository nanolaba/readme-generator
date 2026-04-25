## ${en:'Template syntax', ru:'Синтаксис шаблона'}

### ${en:'Variables', ru:'Переменные'}

The template syntax supports the use of variables. Variables are defined using the following construct:<!--en-->
Синтаксис шаблона поддерживает использование переменных.<!--ru-->
Определение переменных происходит при помощи конструкции:<!--ru-->

```markdown
<!--\@variable_name=variable value-->
```

The output of variable values is done using the following construct:<!--en-->
Вывод значения переменных происходит при помощи конструкции вида:<!--ru-->

```markdown
\${variable_name}
```

To display a construct like *\${...}* without replacing it with <!--en-->
the variable's value, precede it with the '\\' character:<!--en-->
Чтобы вывести в файл конструкцию вида *\${...}*, не заменяя ее значением<!--ru-->
переменной, предварите ее символом '\\':<!--ru-->

```markdown
\\${variable_name}
```

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th><th>${en:'Result', ru:'Результат'}</th></tr>
<tr><td>

```markdown
<!--\@app_name=My Application-->
<!--\@app_version=**1.0.1**-->
<!--\@app_descr=This is *\${app_name}* version \${app_version}-->
\${app_name} version \${app_version}
\${app_descr}
\\${app_descr}
```

</td><td>

```markdown
<!--@app_name=My Application-->
<!--@app_version=**1.0.1**-->
<!--@app_descr=This is *${app_name}* version ${app_version}-->
${app_name} version ${app_version}
${app_descr}
\${app_descr}
```

</td></tr>
</table>

### ${en:'Properties', ru:'Свойства'}

Using the syntax for setting variable values in the template,<!--en-->
you can specify application properties, for example:<!--en-->
При помощи синтаксиса установки значений переменных в шаблоне можно указывать свойства приложения, например:<!--ru-->

```markdown
<!--\@nrg.languages=en,ru-->
<!--\@nrg.defaultLanguage=en-->
```

**${en:'Available properties', ru:'Свойства приложения'}:**

***nrg.languages***

For each language, except the default language, a file will be generated with the name in the<!--en-->
format *source.language.md*, where *source* is the name of the original file and *language* is the<!--en-->
name of the language. The default value of this property is "en"<!--en-->
Перечень языков, для которых будут сгенерированы файлы.<!--ru-->
Для каждого языка, за исключением языка по-умолчанию, будет сгенерирован файл с<!--ru-->
наименованием вида *source.language.md*, где source - наименование исходного файла, language - наименование<!--ru-->
языка. Значение этого свойства по умолчанию - "en".<!--ru-->

***nrg.defaultLanguage***

The language in which the main documentation file will be generated. The language name should be<!--en-->
included in the list defined by the property *nrg.languages*. The default value of this property is<!--en-->
the first element in the *nrg.languages* list.<!--en-->
Язык, на котором будет сгенерирован главный файл документации.<!--ru-->
Название языка должно содержаться в перечне, определенным в свойстве *nrg.languages*.<!--ru-->
Значение этого свойства по умолчанию - первый элемент списка из свойства *nrg.languages*.<!--ru-->

### ${en:'Environment variables', ru:'Переменные окружения'}

Inside any `\${…}` reference, the reserved `env.` namespace pulls a value<!--en-->
from the process environment. Resolution happens before language and<!--en-->
property substitution, so `\${env.NAME}` works in raw body text, inside<!--en-->
`<\!--@key=value-->` declaration values, and inside widget parameters.<!--en-->
Внутри любой ссылки `\${…}` зарезервированное пространство имён `env.` подставляет<!--ru-->
значение из переменных окружения процесса. Разрешение происходит до языковой<!--ru-->
и обычной property-подстановки, поэтому `\${env.NAME}` работает и в обычном тексте,<!--ru-->
и в значениях `<\!--@key=value-->`, и в параметрах виджетов.<!--ru-->

```markdown
\${env.BUILD_NUMBER}
\${env.RELEASE_URL:https://github.com/nanolaba/readme-generator/releases}
<!--\@buildNumber=\${env.BUILD}-->
\${widget:badge(type='custom', label='build', message='\${env.BUILD_NUMBER:unknown}', color='blue')}
```

Behaviour:<!--en-->

- `\${env.NAME}` — substitutes the value of `NAME` from the environment. If unset, logs one warning per distinct name per generation and renders an empty string.<!--en-->
- `\${env.NAME:default}` — substitutes the env value when set (even if empty), otherwise the literal default after the first `:`.<!--en-->
- Names must match the POSIX identifier pattern `[A-Za-z_][A-Za-z0-9_]*`. Dotted names like `\${app.version}` fall through to the regular property resolver.<!--en-->
- Backslash escapes work as for any other `\${…}` reference: `\\\${env.NAME}` renders as the literal text.<!--en-->

Поведение:<!--ru-->

- `\${env.NAME}` — подставляет значение переменной `NAME`. Если переменная не задана, выводит одно предупреждение на каждое уникальное имя за прогон и подставляет пустую строку.<!--ru-->
- `\${env.NAME:default}` — подставляет значение из окружения, если переменная задана (даже пустой строкой), иначе — литерал после первого `:`.<!--ru-->
- Имена должны соответствовать POSIX-идентификатору `[A-Za-z_][A-Za-z0-9_]*`. Имена с точкой, например `\${app.version}`, обрабатываются обычным property-резолвером.<!--ru-->
- Эскейп обратным слэшем работает как для любой другой `\${…}`-конструкции: `\\\${env.NAME}` выводится как литерал.<!--ru-->

> [!WARNING]<!--en-->
> The substitution reads whatever `System.getenv()` exposes. On shared CI<!--en-->
> machines, treat the generated README as exposing every environment<!--en-->
> variable it references — do not template `\${env.AWS_SECRET_…}` into a<!--en-->
> public document.<!--en-->

> [!WARNING]<!--ru-->
> Подстановка читает то, что отдаёт `System.getenv()`. На общих CI-машинах<!--ru-->
> считайте, что сгенерированный README раскрывает любую переменную, на<!--ru-->
> которую он ссылается — не вставляйте `\${env.AWS_SECRET_…}` в публичные<!--ru-->
> документы.<!--ru-->

### Multilanguage support

To write text in different languages, there are two methods available.<!--en-->
The first one involves using comments at the end of the line, for example:<!--en-->
Для написания текста на различных языках предусмотрено два способа.<!--ru-->
Первый заключается в использовании комментариев в конце строки, например:<!--ru-->

```markdown
Some text<\!--en-->
Некоторый текст<\!--ru-->
```

The second method involves using a special construct:<!--en-->
Второй способ заключается в использовании особой конструкции:<!--ru-->

```markdown
\${en:"Some text", ru:"Некоторый текст"}
\${en:'Some text', ru:'Некоторый текст'} 
```

Для экранирования кавычек используйте задвоение символа, например:<!--ru-->
To escape quotes, use character doubling, for example:<!--en-->

- `\${en:'It''s working'}` → `${en:'It''s working'}`
- `\${en:"Text with ""quotes"""}` → `${en:"Text with ""quotes"""}`

### ${en:'Ignoring content', ru:'Игнорирование фрагментов'}

To mark a fragment as an author note that must not appear in any generated file,<!--en-->
use the `nrg.ignore` markers. They work in the root template and inside imported files.<!--en-->
Чтобы пометить фрагмент как авторскую заметку, которая не должна попасть ни в один<!--ru-->
сгенерированный файл, используйте маркеры `nrg.ignore`. Они работают как в основном<!--ru-->
шаблоне, так и внутри импортированных файлов.<!--ru-->

- `<\!--nrg.ignore-->` — ${en:'drops the entire line containing the marker.', ru:'удаляет всю строку, в которой встретился маркер.'}
- `<\!--nrg.ignore.begin-->` ... `<\!--nrg.ignore.end-->` — ${en:'drops all lines of the block, including the markers themselves.', ru:'удаляет все строки блока, включая сами маркеры.'}

If an `<\!--nrg.ignore.begin-->` has no matching `<\!--nrg.ignore.end-->`, an error is<!--en-->
logged and everything from the opening marker to the end of the file is dropped. A lone<!--en-->
`<\!--nrg.ignore.end-->` without a preceding begin is also logged as an error and<!--en-->
removed from the output.<!--en-->
Если у `<\!--nrg.ignore.begin-->` нет парного `<\!--nrg.ignore.end-->`, выводится ошибка<!--ru-->
в лог, а все строки от открывающего маркера до конца файла отбрасываются. Одиночный<!--ru-->
`<\!--nrg.ignore.end-->` без открывающего тоже логируется как ошибка и удаляется<!--ru-->
из вывода.<!--ru-->

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th><th>${en:'Result', ru:'Результат'}</th></tr>
<tr><td>

```markdown
Visible line.
This is a TODO<\!--nrg.ignore-->
<\!--nrg.ignore.begin-->
Author notes that should not leak
into the generated README.
<\!--nrg.ignore.end-->
Another visible line.
```

</td><td>

```markdown
Visible line.
Another visible line.
```

</td></tr>
</table>