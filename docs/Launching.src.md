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

#### ${en:'Verifying generated files (CI mode)', ru:'Проверка сгенерированных файлов (режим CI)'}

Use `--check` to verify that files on disk match what NRG would<!--en-->
generate right now. The flag is meant for CI / pre-commit hooks:<!--en-->
no files are written, a missing or out-of-date target file prints<!--en-->
a diff to stderr, and the process exits with status `1`.<!--en-->
Флаг `--check` проверяет, что файлы на диске совпадают с тем, что<!--ru-->
NRG сгенерировал бы прямо сейчас. Режим предназначен для CI / pre-commit:<!--ru-->
файлы не записываются, при отсутствии или расхождении выводится diff<!--ru-->
в stderr, процесс завершается с кодом `1`.<!--ru-->

```bash
nrg --check -f README.src.md && echo "README is up to date"
```

`--check` validates every language configured via `nrg.languages`<!--en-->
and is mutually exclusive with `--stdout`.<!--en-->
`--check` проверяет все языки, заявленные в `nrg.languages`, и<!--ru-->
несовместим с флагом `--stdout`.<!--ru-->

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
