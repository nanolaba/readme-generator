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

***nrg.npm.path***

Override the `package.json` location used by `\${npm.NAME}` substitution. Relative<!--en-->
paths are resolved against the source-file directory; absolute paths are used<!--en-->
as-is. Defaults to `package.json` next to the source file. Only consulted when<!--en-->
the template uses `\${npm.…}` references.<!--en-->
Переопределяет путь к `package.json`, используемый подстановкой `\${npm.NAME}`.<!--ru-->
Относительные пути разрешаются относительно каталога исходного файла; абсолютные<!--ru-->
используются как есть. По умолчанию — `package.json` рядом с исходным файлом.<!--ru-->
Учитывается только когда в шаблоне есть ссылки `\${npm.…}`.<!--ru-->

***nrg.gradle.path***

Override the Gradle location used by `\${gradle.NAME}` substitution. May point at<!--en-->
either a directory (containing `gradle.properties` and/or `build.gradle{,.kts}`)<!--en-->
or an explicit build script file. Relative paths are resolved against the<!--en-->
source-file directory; absolute paths are used as-is. Defaults to the source-file<!--en-->
directory. Only consulted when the template uses `\${gradle.…}` references.<!--en-->
Переопределяет расположение Gradle, используемое подстановкой `\${gradle.NAME}`.<!--ru-->
Может указывать либо на каталог (с `gradle.properties` и/или `build.gradle{,.kts}`),<!--ru-->
либо на конкретный build-скрипт. Относительные пути разрешаются относительно<!--ru-->
каталога исходного файла; абсолютные используются как есть. По умолчанию —<!--ru-->
каталог исходного файла. Учитывается только когда в шаблоне есть ссылки `\${gradle.…}`.<!--ru-->

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

### ${en:'npm package values', ru:'Значения из package.json'}

The reserved `npm.` namespace inside `\${…}` reads values directly from<!--en-->
the project's `package.json`. Resolution happens after pom substitution but<!--en-->
before language and property substitution, so the same `\${npm.…}` reference<!--en-->
works in body text, in `<\!--@key=value-->` declaration values, and inside<!--en-->
widget parameters.<!--en-->
Зарезервированное пространство имён `npm.` внутри `\${…}` читает значения<!--ru-->
непосредственно из `package.json` проекта. Разрешение происходит после<!--ru-->
pom-подстановки, но до языковой и обычной property-подстановки, поэтому<!--ru-->
одна и та же `\${npm.…}`-ссылка работает и в основном тексте, и в значениях<!--ru-->
`<\!--@key=value-->`, и в параметрах виджетов.<!--ru-->

```markdown
\${npm.version}
\${npm.name}
\${npm.dependencies.lodash}
\${npm.version:0.0.0-SNAPSHOT}
<!--\@coords=\${npm.name}@\${npm.version}-->
```

Behaviour:<!--en-->

- The path is interpreted as a walk from the JSON root: `npm.X` reads the top-level field, `npm.X.Y` walks into the nested object, and so on.<!--en-->
- String, number, and boolean leaves are stringified. Object, array, and `null` leaves render empty (with a warning).<!--en-->
- `\${npm.path:default}` substitutes the literal default after the first `:` when the path is missing. Without a default, the substitution renders empty and one warning per distinct path is logged.<!--en-->
- Backslash escapes work as for any other `\${…}` reference: `\\\${npm.version}` renders as the literal text.<!--en-->
- The `package.json` location defaults to the source-file directory; override with `<\!--\@nrg.npm.path=relative/or/absolute/package.json-->`.<!--en-->

Поведение:<!--ru-->

- Путь интерпретируется как обход JSON-дерева: `npm.X` читает поле верхнего уровня, `npm.X.Y` спускается во вложенный объект и так далее.<!--ru-->
- Строковые, числовые и булевы значения приводятся к строке. Объекты, массивы и `null` подставляются как пустая строка (с предупреждением).<!--ru-->
- `\${npm.path:default}` подставляет литерал после первого `:`, если путь отсутствует. Без default подставляется пустая строка и логируется по одному предупреждению на каждый уникальный путь.<!--ru-->
- Эскейп обратным слэшем работает как для любой другой `\${…}`-конструкции: `\\\${npm.version}` выводится как литерал.<!--ru-->
- Расположение `package.json` по умолчанию — каталог исходного файла; переопределяется через `<\!--\@nrg.npm.path=relative/or/absolute/package.json-->`.<!--ru-->

### ${en:'Gradle values', ru:'Значения из Gradle'}

The reserved `gradle.` namespace inside `\${…}` reads values from the project's<!--en-->
`gradle.properties` (flat key=value lookup) and falls back to regex extraction<!--en-->
of `version` / `group` from `build.gradle` or `build.gradle.kts`. Resolution<!--en-->
happens after npm substitution but before language and property substitution.<!--en-->
Зарезервированное пространство имён `gradle.` внутри `\${…}` читает значения<!--ru-->
из `gradle.properties` (плоский lookup по ключу) с fallback на regex-извлечение<!--ru-->
`version` / `group` из `build.gradle` или `build.gradle.kts`. Разрешение<!--ru-->
происходит после npm-подстановки, но до языковой и обычной property-подстановки.<!--ru-->

```markdown
\${gradle.version}
\${gradle.group}
\${gradle.kotlin.version}
\${gradle.version:0.0.0-SNAPSHOT}
```

Behaviour:<!--en-->

- `gradle.X` first looks up the verbatim key `X` in `gradle.properties`. If not found and `X` is `version` or `group`, NRG regex-extracts `X = '...'` from the build script (works for both Groovy and Kotlin DSLs).<!--en-->
- `gradle.properties` always wins over the build script when the same key is defined in both places.<!--en-->
- Other Gradle DSL constructs are intentionally not parsed — define values in `gradle.properties` if you need them in the README.<!--en-->
- `\${gradle.path:default}`, backslash escapes, and warn-once-per-missing-path semantics match `\${pom.…}` and `\${npm.…}`.<!--en-->
- Override the Gradle location with `<\!--\@nrg.gradle.path=core/-->` (a directory) or `<\!--\@nrg.gradle.path=core/build.gradle.kts-->` (an explicit file).<!--en-->

Поведение:<!--ru-->

- `gradle.X` сначала ищет ключ `X` в `gradle.properties`. Если ключ не найден и `X` — это `version` или `group`, NRG извлекает `X = '...'` из build-скрипта регулярным выражением (работает и для Groovy DSL, и для Kotlin DSL).<!--ru-->
- `gradle.properties` всегда побеждает build-скрипт, если ключ определён в обоих местах.<!--ru-->
- Прочие конструкции Gradle DSL намеренно не парсятся — определяйте значения в `gradle.properties`, если они нужны в README.<!--ru-->
- `\${gradle.path:default}`, эскейп обратным слэшем и семантика «warn-once-per-missing-path» совпадают с `\${pom.…}` и `\${npm.…}`.<!--ru-->
- Переопределяйте расположение Gradle через `<\!--\@nrg.gradle.path=core/-->` (каталог) или `<\!--\@nrg.gradle.path=core/build.gradle.kts-->` (конкретный файл).<!--ru-->

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

- `\${en:'It''s working'}` → `${en:'It''s working', ru:'It''s working'}`
- `\${en:"Text with ""quotes"""}` → `${en:"Text with ""quotes""", ru:"Text with ""quotes"""}`

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