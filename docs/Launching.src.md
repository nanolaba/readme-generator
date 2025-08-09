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
        </configuration>
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
