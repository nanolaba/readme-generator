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

***nrg.widgets***

Comma-separated fully-qualified class names of custom `NRGWidget` implementations<!--en-->
to register alongside the built-ins. Each class must be on the runtime classpath<!--en-->
and declare a public no-argument constructor. Equivalent to the CLI<!--en-->
`--widgets <FQCN,FQCN,...>` flag and the `<widgets>` parameter of the Maven plugin.<!--en-->
Comma-separated полные имена классов реализаций `NRGWidget`, которые нужно<!--ru-->
зарегистрировать дополнительно к встроенным виджетам. Каждый класс должен быть<!--ru-->
доступен на runtime-classpath и иметь публичный конструктор без аргументов.<!--ru-->
Эквивалентно CLI-флагу `--widgets <FQCN,FQCN,...>` и параметру `<widgets>` Maven-плагина.<!--ru-->

***nrg.pom.path***

Override the `pom.xml` location used by `\${pom.NAME}` substitution. Relative<!--en-->
paths are resolved against the source-file directory; absolute paths are used<!--en-->
as-is. Defaults to `pom.xml` next to the source file. Only consulted when the<!--en-->
template uses `\${pom.…}` references.<!--en-->
Переопределяет путь к `pom.xml`, используемый подстановкой `\${pom.NAME}`.<!--ru-->
Относительные пути разрешаются относительно каталога исходного файла; абсолютные<!--ru-->
используются как есть. По умолчанию — `pom.xml` рядом с исходным файлом.<!--ru-->
Учитывается только когда в шаблоне есть ссылки `\${pom.…}`.<!--ru-->

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

### ${en:'Maven POM values', ru:'Значения из Maven POM'}

The reserved `pom.` namespace inside `\${…}` reads values directly from<!--en-->
the project's `pom.xml`. Resolution happens after env substitution but<!--en-->
before language and property substitution, so the same `\${pom.…}`<!--en-->
reference works in body text, in `<\!--@key=value-->` declaration values,<!--en-->
and inside widget parameters.<!--en-->
Зарезервированное пространство имён `pom.` внутри `\${…}` читает<!--ru-->
значения непосредственно из `pom.xml` проекта. Разрешение происходит<!--ru-->
после env-подстановки, но до языковой и обычной property-подстановки,<!--ru-->
поэтому одна и та же `\${pom.…}`-ссылка работает и в основном тексте,<!--ru-->
и в значениях `<\!--@key=value-->`, и в параметрах виджетов.<!--ru-->

```markdown
\${pom.version}
\${pom.groupId}:\${pom.artifactId}:\${pom.version}
\${pom.scm.url}
\${pom.parent.version}
\${pom.properties.java.version}
\${pom.version:0.0.0-SNAPSHOT}
<!--\@coords=\${pom.groupId}:\${pom.artifactId}-->
```

Behaviour:<!--en-->

- The path is interpreted as a walk from the implicit `<project>` root: `pom.X` reads `<X>`, `pom.X.Y` reads `<X><Y>`, and so on.<!--en-->
- `pom.properties.KEY` is a flat-map lookup: the remainder of the path is used verbatim as the `<properties>` child element name (so dotted keys like `java.version` work).<!--en-->
- `pom.parent.X` reads the local `<parent>` block as written. Cross-file parent POM traversal is out of scope.<!--en-->
- For unqualified `pom.groupId`, `pom.version`, and `pom.name`: if the element is absent under `<project>`, the value is taken from `<project><parent>` (Maven's standard inheritance rules).<!--en-->
- POM values may themselves reference `\${prop}`, `\${project.X}`, and `\${env.NAME}` / `\${env.NAME:default}` — NRG resolves a single pass against the same POM and the template-level env provider.<!--en-->
- `\${pom.path:default}` substitutes the literal default after the first `:` when the path is missing. Without a default, the substitution renders empty and one warning per distinct path is logged.<!--en-->
- Backslash escapes work as for any other `\${…}` reference: `\\\${pom.version}` renders as the literal text.<!--en-->
- The `pom.xml` location defaults to the source-file directory; override with `<\!--\@nrg.pom.path=relative/or/absolute/pom.xml-->`.<!--en-->

Поведение:<!--ru-->

- Путь интерпретируется как обход дерева от неявного корня `<project>`: `pom.X` — `<X>`, `pom.X.Y` — `<X><Y>` и так далее.<!--ru-->
- `pom.properties.KEY` — плоский lookup: остаток пути после `properties.` используется как имя дочернего элемента `<properties>` (поэтому ключи с точками вроде `java.version` работают).<!--ru-->
- `pom.parent.X` читает локальный блок `<parent>`. Чтение parent-POM из других файлов — за рамками v1.<!--ru-->
- Для безпрефиксных `pom.groupId`, `pom.version` и `pom.name`: если элемент отсутствует в `<project>`, значение берётся из `<project><parent>` (стандартное Maven-наследование).<!--ru-->
- Значения POM сами могут содержать `\${prop}`, `\${project.X}` и `\${env.NAME}` / `\${env.NAME:default}` — NRG разрешает их одним проходом по тому же POM и через тот же env-провайдер.<!--ru-->
- `\${pom.path:default}` подставляет литерал после первого `:`, если путь отсутствует. Без default подставляется пустая строка и логируется по одному предупреждению на каждый уникальный путь.<!--ru-->
- Эскейп обратным слэшем работает как для любой другой `\${…}`-конструкции: `\\\${pom.version}` выводится как литерал.<!--ru-->
- Расположение `pom.xml` по умолчанию — каталог исходного файла; переопределяется через `<\!--\@nrg.pom.path=relative/or/absolute/pom.xml-->`.<!--ru-->

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

- `\${en:'It''s working', ru:'It''s working'}` → `${en:'It''s working', ru:'It''s working'}`
- `\${en:"Text with ""quotes""", ru:"Text with ""quotes"""}` → `${en:"Text with ""quotes""", ru:"Text with ""quotes"""}`

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