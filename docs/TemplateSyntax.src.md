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

### ${en:'Backslash escapes', ru:'Обратные слэши и экранирование'}

After every substitution and widget has run, the root generator does one final pass over the<!--en-->
output and strips a backslash in **only** these three patterns; every other `\\X` reaches the<!--en-->
output verbatim.<!--en-->
После того как все подстановки и виджеты отработали, корневой генератор делает один финальный<!--ru-->
проход и убирает обратный слэш только в этих трёх шаблонах; любая другая последовательность<!--ru-->
`\\X` остаётся в выводе как есть.<!--ru-->

| ${en:'In your template', ru:'В шаблоне'} | ${en:'In the output', ru:'В выводе'} | ${en:'Use it to', ru:'Зачем нужно'} |
|---|---|---|
| <code>&#92;$</code>        | <code>$</code>        | ${en:'suppress any `\\${…}` reference (property / language / env / pom / npm / gradle / widget). Applies to **any** `\\$`, not only `\\${…}`.', ru:'подавить любую ссылку `\\${…}` (свойство / язык / env / pom / npm / gradle / widget). Срабатывает на **любое** `\\$`, не только на `\\${…}`.'} |
| <code>&lt;&#92;!--</code>  | <code>&lt;!--</code>  | ${en:'suppress any HTML-comment marker — language tag, `nrg.ignore`, `nrg.freeze`, property declaration. Use it to put a literal `<\!--…-->` into the output.', ru:'подавить любой HTML-маркер — языковой тег, `nrg.ignore`, `nrg.freeze`, объявление свойства. Используется, чтобы вывести литерал `<\!--…-->`.'} |
| <code>&lt;!--&#92;@</code> | <code>&lt;!--@</code> | ${en:'render `<\!--@key=value-->` cleanly inside an example — **cosmetic only**, does **not** stop NRG from parsing the line as a real property declaration. To actually suppress parsing, escape the comment opener with rule 2: <code>&lt;&#92;!--@key=value--&gt;</code>.', ru:'красиво вывести `<\!--@key=value-->` в примере — **только косметика**, **не** мешает NRG распарсить строку как настоящее объявление свойства. Чтобы реально подавить разбор, экранируйте открытие комментария по правилу 2: <code>&lt;&#92;!--@key=value--&gt;</code>.'} |

Markdown's own escape sequences (`\(`, `\)`, `\_`, `\*`, `\\`, `` \` ``, etc.) are not on this<!--en-->
list — they pass through NRG unchanged and reach the markdown renderer untouched.<!--en-->
Эскейпы самого Markdown (`\(`, `\)`, `\_`, `\*`, `\\`, `` \` `` и т. п.) в этот список<!--ru-->
не входят — они проходят NRG без изменений и попадают на рендерер Markdown как есть.<!--ru-->

> [!TIP]<!--en-->
> Rule 1 strips the backslash from **any** `\\$`, not only `\\${…}`. To keep a literal `\\$` in the<!--en-->
> output (e.g. to render `[\\$]` in a markdown link without triggering a property lookup), write<!--en-->
> `\\\$` in the template — the trailing `\\$` becomes `$`, the leading `\` survives.<!--en-->

> [!TIP]<!--ru-->
> Правило 1 убирает обратный слэш у **любой** последовательности `\\$`, не только у `\\${…}`.<!--ru-->
> Чтобы оставить в выводе литерал `\\$` (например, для `[\\$]` в markdown-ссылке без подстановки),<!--ru-->
> пишите в шаблоне `\\\$` — замыкающий `\\$` превратится в `$`, ведущий `\` останется на месте.<!--ru-->

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

***nrg.fileNamePattern***

Output filename pattern applied to all languages. Placeholders: `<base>` (source<!--en-->
filename without `.src.md`), `<lang>` (language code as written), `<LANG>` (language<!--en-->
code upper-cased). May include `/` separators — intermediate directories are created<!--en-->
on demand. Defaults to `<base>.md` for the default language and `<base>.<lang>.md`<!--en-->
for the others. Examples: `<base>_<LANG>.md`, `<base>-<lang>.md`, `docs/<lang>/<base>.md`.<!--en-->
Шаблон имени выходного файла для всех языков. Плейсхолдеры: `<base>` (имя исходного<!--ru-->
файла без `.src.md`), `<lang>` (код языка как есть), `<LANG>` (код языка в верхнем<!--ru-->
регистре). Может содержать `/` — недостающие каталоги создаются автоматически.<!--ru-->
По умолчанию — `<base>.md` для языка по умолчанию и `<base>.<lang>.md` для остальных.<!--ru-->
Примеры: `<base>_<LANG>.md`, `<base>-<lang>.md`, `docs/<lang>/<base>.md`.<!--ru-->

***nrg.defaultLanguageFileNamePattern***

Override `nrg.fileNamePattern` for the default language only. Useful when the<!--en-->
default language should keep the bare `README.md` while other languages get a<!--en-->
suffix: `nrg.fileNamePattern=<base>_<LANG>.md` plus `nrg.defaultLanguageFileNamePattern=<base>.md`.<!--en-->
Переопределяет `nrg.fileNamePattern` только для языка по умолчанию. Удобно когда<!--ru-->
основной язык должен лежать как `README.md`, а остальные — с суффиксом:<!--ru-->
`nrg.fileNamePattern=<base>_<LANG>.md` + `nrg.defaultLanguageFileNamePattern=<base>.md`.<!--ru-->

***nrg.fileNamePattern.&lt;lang&gt;***

Per-language override (e.g. `nrg.fileNamePattern.zh-CN=README_<LANG>.md`). Beats both<!--en-->
`nrg.fileNamePattern` and `nrg.defaultLanguageFileNamePattern` for that exact language.<!--en-->
Most-specific-first resolution: per-language → default-language → global → built-in.<!--en-->
If two configured languages would resolve to the same output path, generation aborts.<!--en-->
Переопределение для конкретного языка (например `nrg.fileNamePattern.zh-CN=README_<LANG>.md`).<!--ru-->
Имеет приоритет и над `nrg.fileNamePattern`, и над `nrg.defaultLanguageFileNamePattern`<!--ru-->
для указанного языка. Порядок разрешения: per-language → default-language → global → встроенный.<!--ru-->
Если два сконфигурированных языка попадают в один и тот же файл, генерация прерывается.<!--ru-->

### ${en:'Per-language overrides', ru:'Переопределения для разных языков'}

Any property may declare a per-language override by suffixing its name with `.<lang>`.<!--en-->
When the template is rendered for a specific language, `\${name}` first resolves to the value of `name.<lang>` if defined,<!--en-->
otherwise it falls back to the bare `name`. If neither is defined, the literal `\${name}` is left in the output.<!--en-->
Любое свойство может объявить переопределение для конкретного языка с помощью суффикса `.<lang>`.<!--ru-->
При рендере шаблона на конкретный язык `\${name}` сначала ищет значение `name.<lang>`,<!--ru-->
а при его отсутствии — голый ключ `name`. Если ни то, ни другое не задано, в выводе остаётся литерал `\${name}`.<!--ru-->

```markdown
<!--\@nrg.languages=en,ru,ja-->
<!--\@screenshot.en=./public/show-en.png-->
<!--\@screenshot.ru=./public/show-ru.png-->
<!--\@screenshot=./public/show.png-->

<img src="\${screenshot}" />
```

${en:'Result for `en`', ru:'Результат для `en`'}: `<img src="./public/show-en.png" />`<br>
${en:'Result for `ru`', ru:'Результат для `ru`'}: `<img src="./public/show-ru.png" />`<br>
${en:'Result for `ja` (no per-language override)', ru:'Результат для `ja` (нет переопределения)'}: `<img src="./public/show.png" />`

The same convention is also used by built-in NRG properties such as `nrg.fileNamePattern.<lang>`.<!--en-->
Эта же конвенция используется встроенными свойствами NRG, например `nrg.fileNamePattern.<lang>`.<!--ru-->

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

### ${en:'Frozen regions', ru:'Замороженные области'}

Frozen regions let NRG-generated files coexist with third-party tools that<!--en-->
mutate the **generated** file directly — for example<!--en-->
[`akhilmhdh/contributors-readme-action`](https://github.com/akhilmhdh/contributors-readme-action),<!--en-->
GitHub Sponsors widgets, or RSS embedders. On regeneration, NRG copies the<!--en-->
**current on-disk** content between the freeze markers into the freshly-generated<!--en-->
output instead of materialising whatever the template contains in the same span.<!--en-->
Замороженные области позволяют файлам, сгенерированным NRG, сосуществовать со<!--ru-->
сторонними инструментами, которые правят **сгенерированный** файл напрямую — например,<!--ru-->
[`akhilmhdh/contributors-readme-action`](https://github.com/akhilmhdh/contributors-readme-action),<!--ru-->
виджетами GitHub Sponsors или RSS-эмбеддерами. При перегенерации NRG копирует<!--ru-->
**текущее содержимое с диска** между маркерами заморозки в свежесгенерированный<!--ru-->
вывод вместо того, чтобы материализовать туда то, что находится в шаблоне.<!--ru-->

```markdown
## Contributors

<\!--nrg.freeze id="contributors"-->
<\!-- readme: contributors -start -->
<\!-- contents managed by akhilmhdh/contributors-readme-action -->
<\!-- readme: contributors -end -->
<\!--/nrg.freeze-->
```

The block content in the template is a **bootstrap placeholder**: it lands in the<!--en-->
output only the first time the file is generated (when no on-disk version exists).<!--en-->
On every subsequent run, NRG reads the existing output file, finds the matching<!--en-->
`id`, and splices its current content into the new output — so any external<!--en-->
edits inside the block survive regeneration. Edits *outside* the block are still<!--en-->
overwritten as usual.<!--en-->
Содержимое блока в шаблоне — это **bootstrap-плейсхолдер**: оно попадает в вывод<!--ru-->
только при первой генерации (когда выходного файла ещё нет на диске). При каждом<!--ru-->
следующем запуске NRG читает существующий выходной файл, находит совпадающий<!--ru-->
`id` и подставляет его текущее содержимое в новый вывод — поэтому любые<!--ru-->
правки внешнего инструмента внутри блока переживают перегенерацию. Правки<!--ru-->
*вне* блока всё так же перетираются.<!--ru-->

**${en:'Attributes', ru:'Атрибуты'}:**

***id*** — ${en:'required, non-empty, must be unique within a template.', ru:'обязательный, непустой, должен быть уникален в пределах шаблона.'}

***source-lang*** — ${en:'optional. Names a single language declared in `nrg.languages`. When present, the block content for *every* language is sourced from that one language\\'s output file.', ru:'опциональный. Указывает один язык из `nrg.languages`. Когда указан, содержимое блока для *каждого* языка берётся из выходного файла этого одного языка.'}

The `source-lang` mode covers the common case where an external tool only<!--en-->
writes to one language's output (e.g. contributors-action only knows about<!--en-->
`README.md`), but the resulting content (an HTML table of avatars, etc.) is<!--en-->
language-agnostic and should appear in every language file:<!--en-->
Режим `source-lang` покрывает типовой кейс, когда внешний инструмент пишет<!--ru-->
только в один языковой файл (например, contributors-action знает только про<!--ru-->
`README.md`), но получившееся содержимое (HTML-таблица аватарок и т.п.)<!--ru-->
language-agnostic и должно появиться во всех языковых файлах:<!--ru-->

```markdown
<\!--nrg.freeze id="contributors" source-lang="en"-->
placeholder
<\!--/nrg.freeze-->
```

**${en:'Modes', ru:'Режимы'}:**

| ${en:'Attributes', ru:'Атрибуты'}              | ${en:'Behaviour', ru:'Поведение'} |
|------------------------------------------------|----------------------------------|
| `id="X"`                                       | ${en:'Each language file is independent: when generating `README.md` NRG reads the freeze content from `README.md`, when generating `README.ru.md` from `README.ru.md`.', ru:'Каждый языковой файл независим: при сборке `README.md` NRG читает содержимое заморозки из `README.md`, при сборке `README.ru.md` — из `README.ru.md`.'} |
| `id="X" source-lang="en"`                      | ${en:'The block appears in **every** language file, but the content for **all** of them is sourced from the `en` output (`README.md`).', ru:'Блок появляется в **каждом** языковом файле, но содержимое **для всех** берётся из вывода языка `en` (`README.md`).'} |

**${en:'Restricting a freeze to one language', ru:'Ограничение заморозки одним языком'}:**

`<\!--nrg.freeze-->` itself has no `lang` attribute. To make a freeze block<!--en-->
appear only in one language, wrap it in a `\${widget:if}` block driven by a<!--en-->
per-language property:<!--en-->
У `<\!--nrg.freeze-->` нет атрибута `lang`. Чтобы заморозка появлялась только<!--ru-->
в одном языке, оберните её в `\${widget:if}`-блок, управляемый per-language<!--ru-->
свойством:<!--ru-->

```markdown
<\!--\@onlyEn.en=1-->

\${widget:if(cond='\${onlyEn}')}
<\!--nrg.freeze id="ru-only-block"-->
placeholder
<\!--/nrg.freeze-->
\${widget:endIf}
```

When generating `README.md` (`en`), `\${onlyEn}` resolves to `1` (truthy) and<!--en-->
the block stays. When generating `README.ru.md`, `\${onlyEn}` resolves to the<!--en-->
empty string (falsy) and the entire block is dropped before NRG even sees the<!--en-->
freeze markers.<!--en-->
При сборке `README.md` (`en`) `\${onlyEn}` резолвится в `1` (truthy), блок<!--ru-->
остаётся. При сборке `README.ru.md` `\${onlyEn}` резолвится в пустую строку<!--ru-->
(falsy), и весь блок выбрасывается ещё до того, как NRG видит маркеры<!--ru-->
заморозки.<!--ru-->

**${en:'Important properties', ru:'Важные свойства'}:**

- ${en:'Open and close markers must each be on their own line.', ru:'Открывающий и закрывающий маркеры должны быть каждый на своей строке.'}
- ${en:'Markers must not nest. Nesting is a validation error.', ru:'Маркеры нельзя вкладывать друг в друга. Вложенность — ошибка валидации.'}
- ${en:'Disk content is opaque to NRG: `\${...}` references and `<\!--\@key=value-->` declarations *inside* the freeze block content from disk are **not** interpreted. Only the bootstrap placeholder in the template goes through the rendering pipeline (once, at first generation).', ru:'Содержимое с диска для NRG непрозрачно: ссылки `\${...}` и декларации `<\!--\@key=value-->` *внутри* содержимого блока, прочитанного с диска, **не** интерпретируются. Через рендеринг проходит только bootstrap-плейсхолдер в шаблоне (один раз, при первой генерации).'}
- ${en:'Markers themselves are written to the output verbatim, including original whitespace — they have to be there for the next regeneration to find the block.', ru:'Сами маркеры выводятся в файл verbatim, с оригинальными пробелами — они должны там быть, чтобы следующая перегенерация нашла блок.'}
- ${en:'Freeze blocks work transparently across `\${widget:import(...)}` — markers in imported files bubble up to the root output and are resolved against the root output file.', ru:'Заморозки работают прозрачно через `\${widget:import(...)}` — маркеры из импортированных файлов всплывают в корневой вывод и резолвятся по корневому выходному файлу.'}

**${en:'Validation', ru:'Валидация'}:**

The following are **template authoring errors** — they fail generation with<!--en-->
exit 1 and are reported by `--validate`:<!--en-->
Это **ошибки авторинга шаблона** — они валят генерацию с exit 1 и сообщаются<!--ru-->
через `--validate`:<!--ru-->

- ${en:'missing or empty `id`;', ru:'отсутствующий или пустой `id`;'}
- ${en:'duplicate `id` within the same template;', ru:'дубль `id` в одном шаблоне;'}
- ${en:'unbalanced markers (open without close, or stray close);', ru:'несбалансированные маркеры (открытие без закрытия или одиночное закрытие);'}
- ${en:'nested freeze blocks;', ru:'вложенные блоки заморозки;'}
- ${en:'`source-lang` referencing a language not declared in `nrg.languages`;', ru:'`source-lang` ссылается на язык, не объявленный в `nrg.languages`;'}
- ${en:'unknown attributes (only `id` and `source-lang` are allowed).', ru:'неизвестные атрибуты (разрешены только `id` и `source-lang`).'}

The following are **on-disk anomalies** caused by the external tool or manual<!--en-->
edits — they emit `LOG.warn` once and fall back to the bootstrap placeholder,<!--en-->
without aborting generation:<!--en-->
Это **on-disk-аномалии**, вызванные внешним инструментом или ручными правками —<!--ru-->
они логируются `LOG.warn` один раз и откатываются на bootstrap-плейсхолдер<!--ru-->
без прерывания генерации:<!--ru-->

- ${en:'`id` declared in the template not found in the on-disk file;', ru:'`id`, объявленный в шаблоне, не найден в файле на диске;'}
- ${en:'malformed disk block (e.g. missing close);', ru:'битый блок на диске (например, отсутствует закрытие);'}
- ${en:'duplicate `id` on disk — the first occurrence wins;', ru:'дубль `id` на диске — побеждает первое вхождение;'}
- ${en:'`source-lang` file does not exist on disk yet (treated as bootstrap).', ru:'файл, на который указывает `source-lang`, ещё не существует на диске (трактуется как bootstrap).'}