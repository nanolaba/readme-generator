<!--@nrg.languages=en,ru-->
<!--@nrg.defaultLanguage=en-->

<!--@name=**Nanolaba Readme Generator (NRG)**-->
<!--@stableVersion=0.1-->
<!--@devVersion=0.1-SNAPSHOT-->

${widget:languages}

# Nanolaba Readme Generator (NRG)

${name} — это программа для автоматической генерации файлов в формате<!--ru-->
[Markdown]( https://en.wikipedia.org/wiki/Markdown) на основе файла-прототипа.<!--ru-->
${name} — is a program for automatically<!--en-->
generating [Markdown files]( https://en.wikipedia.org/wiki/Markdown) based on a prototype file.<!--en-->

## ${en:'Overview', ru:'Краткое описание'}

Using the ${name} program, you can generate separate files for different languages<!--en-->
in [Markdown format]( https://en.wikipedia.org/wiki/Markdown) based on a single template file.<!--en-->
The template allows the use of variables and special components (widgets).<!--en-->
This document is an example of the result of this program.<!--en-->
The template from which this document was generated is available at the<!--en-->
following link - [README.src.md](https://github.com/nanolaba/readme-generator/blob/main/README.src.md?plain=1).<!--en-->
С помощью программы ${name} можно генерировать отдельные файлы для разных языков в формате <!--ru-->
[Markdown]( https://en.wikipedia.org/wiki/Markdown) на основе единого файла-шаблона.<!--ru-->
В шаблоне можно использовать переменные, а также специальные компоненты (виджеты). <!--ru-->
Данный документ является примером результата работы этой программы.<!--ru-->
Шаблон, из которого сгенерирован этот документ, доступен по ссылке - <!--ru-->
[README.src.md](https://github.com/nanolaba/readme-generator/blob/main/README.src.md?plain=1).<!--ru-->

The latest stable version of the program is **${stableVersion}**.<!--en-->
Последняя стабильная версия - **${stableVersion}**.<!--ru-->

The latest development version is **${devVersion}**.<!--en-->
Последняя версия разработки - **${devVersion}**.<!--ru-->

${name} is written in Java and requires **Java 8** or higher to run.<!--en-->
${name} написан на Java и требует для запуска версии **Java 8** и выше.<!--ru-->

The program can be run as:<!--en-->
Программа может быть запущена как:<!--ru-->

* A standalone console application,<!--en-->
* Консольное приложение,<!--ru-->
* A Maven plugin,<!--en-->
* Плагин для maven,<!--ru-->
* Or integrated into a project as a third-party library.<!--en-->
* Добавлена к проекту в качестве сторонней библиотеки.<!--ru-->

${widget:tableOfContents(title = "${en:'Table of contents', ru:'Содержание'}", ordered = "true")}

## ${en:'Quick start', ru:'Быстрый старт'}

**${en:'Step 1: Create a template', ru:'Шаг 1: Создайте шаблон'} (README.src.md)**

```markdown
<!--\@nrg.languages=en,ru-->
<!--\@nrg.defaultLanguage=en-->

<!--\@title=**\${en:'Hello, World!', ru:'Привет, Мир!'}**-->

\${widget:languages}

# \${title}<!--toc.ignore-->

\${widget:tableOfContents(title = "\${en:'Table of contents', ru:'Содержание'}", ordered = "true")}

## Part 1<!--toc.ignore-->

### Chapter 1<!--toc.ignore-->

English text<\!--en-->
Русский текст<\!--ru-->

```

**${en:'Step 2: Build the files', ru:'Шаг 2: Сгенерируйте файлы'}**

*Вариант 1:* [Скачайте](#запуск-из-командной-строки) программу и запустите в консоли команду:<!--ru-->
*Option 1:* [Download](#using-the-command-line-interface) the program and run the command in the console:<!--en-->

```bash
nrg -f /path/to/README.src.md
```

*Вариант 2:* [Подключите](#использование-как-плагина-для-maven) к проекту плагин для maven.<!--ru-->
*Option 2:* [Add](#use-as-maven-plugin) the Maven plugin to your project.<!--en-->

**${en:'Step 3: Result (README.md)', ru:'Шаг 3: Результат (README.md)'}**

```markdown
${widget:languages}

# Hello, World!<!--toc.ignore-->

## Table of contents<!--toc.ignore-->

1. [Part 1](#part-1)
    1. [Chapter 1](#chapter-1)

## Part 1<!--toc.ignore-->

### Chapter 1<!--toc.ignore-->

English text<!--en-->
Русский текст<!--ru-->

```

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

Разархивируйте скачанный архив, если вы используете Unix-like системы, то назначьте файлу `nrg.sh` права <!--ru-->
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
Add it to your project's classpath<!--en-->
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
behavior — is to use the `Generator` class, for example:<!--en-->
Альтернативным вариантом, а также более гибким в плане настройки поведения <!--ru-->
программы, является использование класса `Generator`, например:<!--ru-->

```java
package com.nanolaba.nrg.examples;

import com.nanolaba.nrg.core.*;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class GeneratorExample {

    public static void main(String[] args) throws IOException {

        Generator generator = new Generator(new File("template.md"), StandardCharsets.UTF_8);

        for (String language : generator.getConfig().getLanguages()) {

            GenerationResult generationResult = generator.getResult(language);

            FileUtils.write(
                    new File("result." + language + ".md"),
                    generationResult.getContent(),
                    StandardCharsets.UTF_8);
        }
    }
}

```

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

### ${en:'Widgets', ru:'Виджеты'}

Widgets allow you to insert programmatically generated text into a document.<!--en-->
If you are using ${name} as a Java library, you can write your own widget.<!--en-->
How to do this is explained in the [Advanced Features](#advanced-features) section.<!--en-->
Виджеты позволяют вставить в документ программно сгенерированный текст.<!--ru-->
Если вы используете ${name} как java-библиотеку, то вы можете написать свой собственный виджет.<!--ru-->
Как это сделать рассказано в разделе [Расширенные возможности](#расширенные-возможности).<!--ru-->

#### ${en:'Widget', ru:'Виджет'} 'languages'

This component allows you to generate links to other versions of a document (written in other languages).<!--en-->
Этот компонент позволяет генерировать ссылки на другие версии документа (написанные на других языках).<!--ru-->

<table>
<tr>
<th>${en:'Usage example', ru:'Пример использования'}</th>
<th>${en:'Result', ru:'Результат'}</th>
<th>${en:'Displayed result', ru:'Отображаемый результат'}</th>
</tr>
<tr><td>

```markdown
\${widget:languages} 
```

</td><td>

```markdown
${widget:languages}
```

</td>
<td>

${widget:languages}

</td>
</tr>
</table>

---

#### ${en:'Widget', ru:'Виджет'} 'tableOfContents'

This component allows you to generate a table of contents for a document.<!--en-->
The table of contents is created from headers formed using the hashtag symbol (`#`).<!--en-->
Headers located above the widget in the text are ignored.<!--en-->
Этот компонент позволяет сформировать оглавление для документа.<!--ru-->
Оглавление формируется из заголовков, сформированных при помощи знака решётки (`#`).<!--ru-->
Заголовки, которые расположены по тексту выше виджета, игнорируются.<!--ru-->

If you need to exclude a header from the table of contents, you should<!--en-->
mark it with a comment `<\!--toc.ignore-->`.<!--en-->
Если вам необходимо исключить какой-либо заголовок из оглавления, то для этого<!--ru-->
его необходимо пометить комментарием `<\!--toc.ignore-->`.<!--ru-->

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th></tr>
<tr><td>

```markdown
# Title of the document<!--toc.ignore-->

## Abstract<!--toc.ignore-->

\${widget:tableOfContents(title = "\${en:'Table of contents', ru:'Содержание'}", ordered = "true")}

## Part 1<!--toc.ignore-->

### Chapter 1<!--toc.ignore-->

### Chapter 2<!--toc.ignore-->

### Chapter 3<!--toc.ignore-->

### Ignored Chapter<\!--toc.ignore--><!--toc.ignore-->

## Part 2<!--toc.ignore-->

## Part 3<!--toc.ignore-->
```

</td></tr>
<tr><th>${en:'Result', ru:'Результат'}</th></tr>
<tr><td>

```markdown 
# Title of the document<!--toc.ignore-->

## Abstract<!--toc.ignore-->

## Table of contents<!--toc.ignore-->

1. [Part 1](#part-1)
    1. [Chapter 1](#chapter-1)
    2. [Chapter 2](#chapter-2)
    3. [Chapter 3](#chapter-3)
2. [Part 2](#part-2)
3. [Part 3](#part-3)

## Part 1<!--toc.ignore-->

### Chapter 1<!--toc.ignore-->

### Chapter 2<!--toc.ignore-->

### Chapter 3<!--toc.ignore-->

### Ignored Chapter<\!--toc.ignore--><!--toc.ignore-->

## Part 2<!--toc.ignore-->

## Part 3<!--toc.ignore-->
```

</td></tr>
</table>



${en:'Widget parameters', ru:'Свойства виджета'}:

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                 | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|--------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------:|
|              title              | ${en:'Title of the table of contents', ru:'Заглавие оглавления'}                                                   |                                                   |
|             ordered             | ${en:'Should the items in the table of contents be numbered', ru:'Должны ли быть пронумерованы пункты оглавления'} |                      `false`                      |

---

#### ${en:'Widget', ru:'Виджет'} 'date'

This component allows you to insert the current date into a document.<!--en-->
Этот компонент позволяет вставить в документ текущую дату.<!--ru-->

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th><th>${en:'Result', ru:'Результат'}</th></tr>
<tr><td>

```markdown
Last updated: \${widget:date}
```

</td><td>

```markdown
Last updated: ${widget:date}
```

</td></tr>
<tr><td>

```markdown
\${widget:date(pattern = 'dd.MM.yyyy')}
```

</td><td>

```markdown
${widget:date(pattern= 'dd.MM.yyyy')}
```

</td></tr>
</table>

${en:'Widget parameters', ru:'Свойства виджета'}:

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                              | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|---------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------:|
|             pattern             | ${en:'Pattern according to which the date will be formatted', ru:'Шаблон, в соответствии с котором будет отформатирована дата'} |               `dd.MM.yyyy HH:mm:ss`               |

You can read more about date pattern syntax in the<!--en-->
[Java documentation](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html).<!--en-->
Подробнее о синтаксисе шаблона даты можно прочитать в <!--ru-->
[документации языка Java](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html).<!--ru-->

---

#### ${en:'Widget', ru:'Виджет'} 'todo'

This component allows you to insert prominently highlighted text into the document,<!--en-->
indicating that work on this fragment has not yet been done.<!--en-->
Этот компонент позволяет вставить в документ ярко выделенный текст, с пометкой<!--ru-->
о том, что работа над данным фрагментом еще не проведена.<!--ru-->


<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th></tr>
<tr><td>

```markdown
\${widget:todo(text="\${en:'Example message', ru:'Пример сообщения'}")}
```

</td></tr>
<tr><th>${en:'Result', ru:'Результат'}</th></tr>
<tr><td>

```markdown
${widget:todo(text="${en:'Example message', ru:'Пример сообщения'}")}
```

</td></tr>
<tr><th>${en:'Displayed result', ru:'Отображаемый результат'}</th></tr>
<tr><td>
${widget:todo(text="${en:'Example message', ru:'Пример сообщения'}")}
</td></tr>
</table>


${en:'Widget parameters', ru:'Свойства виджета'}:

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}              | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|-------------------------------------------------|:-------------------------------------------------:|
|              text               | ${en:'Displayed text', ru:'Отображаемый текст'} |                 `Not done yet...`                 |

## ${en:'Advanced features', ru:'Расширенные возможности'}

### ${en:'Creating a widget', ru:'Создание виджета'}

${widget:todo}

## ${en:'Feedback', ru:'Обратная связь'}

${widget:todo}

---
*${en:'Last updated:', ru:'Дата последнего обновления:'} ${widget:date(pattern= 'dd.MM.yyyy')}*