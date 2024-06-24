<!--@nrg.languages=en,ru-->
<!--@nrg.defaultLanguage=en-->

<!--@name=**Nanolaba Readme Generator (NRG)**-->
<!--@stableVersion=1.0-->

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
<!--en-->
The latest stable version of the program is **${stableVersion}**.<!--en-->

С помощью программы ${name} можно генерировать отдельные файлы для разных языков в формате <!--ru-->
[Markdown]( https://en.wikipedia.org/wiki/Markdown) на основе единого файла-шаблона.<!--ru-->
В шаблоне можно использовать переменные, а также специальные компоненты (виджеты). <!--ru-->
Данный документ является примером результата работы этой программы.<!--ru-->
<!--ru-->
Последняя стабильная версия - **${stableVersion}**.<!--ru-->

${widget:tableOfContents(title = "${en:'Table of contents', ru:'Содержание'}", ordered = "true")}

## ${en:'Usage', ru:'Способы запуска программы'}

TODO

### ${en:'Using the Command Line Interface', ru:'Запуск из командной строки'}

TODO

### ${en:'Use as a java-library', ru:'Использование в качестве java-библиотеки'}

TODO

## ${en:'Template syntax', ru:'Синтаксис шаблона'}

### ${en:'Variables', ru:'Переменные'}

The template syntax supports the use of variables. Variables are defined using the following construct:<!--en-->
Синтаксис шаблона поддерживает использование переменных.<!--ru-->
Определение переменных происходит при помощи конструкции:<!--ru-->

```markdown
&lt;!--@variable_name=variable value-->
```

The output of variable values is done using the following construct:<!--en-->
Вывод значения переменных происходит при помощи конструкции вида:<!--ru-->

```markdown
&#36;{variable_name}
```

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th><th>${en:'Result', ru:'Результат'}</th></tr>
<tr><td>

```markdown
&lt;!--@app_name=My Application--&gt;
&lt;!--@app_version=**1.0.1**--&gt;
&lt;!--@app_descr=This is *&#36;{app_name}* version &#36;{app_version}--&gt;
&#36;{app_name} version &#36;{app_version}
&#36;{app_descr}
```

</td><td>

```markdown
<!--@app_name=My Application-->
<!--@app_version=**1.0.1**-->
<!--@app_descr=This is *${app_name}* version ${app_version}-->
${app_name} version ${app_version}
${app_descr}
```

</td></tr>
</table>

### ${en:'Properties', ru:'Свойства'}

TODO

### Multilanguage support

TODO

### Language properties

TODO

### ${en:'Widgets', ru:'Виджеты'}

#### ${en:'Widget', ru:'Виджет'} 'languages'

This component allows you to generate links to other versions of a document (written in other languages).<!--en-->
Этот компонент позволяет генерировать ссылки на другие версии документа (написанные на других языках).<!--ru-->

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th><th>${en:'Result', ru:'Результат'}</th></tr>
<tr><td>

```markdown
&#36;{widget:languages} 
```

</td><td>

```markdown
${widget:languages}
```

</td></tr>
</table>

#### ${en:'Widget', ru:'Виджет'} 'tableOfContents'

This component allows you to generate a table of contents for a document.<!--en-->
The table of contents is created from headers formed using the hashtag symbol (**#**).<!--en-->
Headers located above the widget in the text are ignored.<!--en-->
Этот компонент позволяет сформировать оглавление для документа.<!--ru-->
Оглавление формируется из заголовков, сформированных при помощи знака решётки (**#**).<!--ru-->
Заголовки, которые расположены по тексту выше виджета, игнорируются.<!--ru-->


<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th><th>${en:'Result', ru:'Результат'}</th></tr>
<tr><td>

```markdown
# Title of the document

## Abstract

&#36;{widget:tableOfContents(title = "&#36;{en:'Table of contents', ru:'Содержание'}", ordered = "true")}

## Part 1

### Chapter 1

### Chapter 2

### Chapter 3

## Part 2

## Part 3
```

</td><td>

```markdown 
# Title of the document

## Abstract

## Table of contents

1. [Part 1](#part-1)
    1. [Chapter 1](#chapter-1)
    2. [Chapter 2](#chapter-2)
    3. [Chapter 3](#chapter-3)
2. [Part 2](#part-2)
3. [Part 3](#part-3)

## Part 1

### Chapter 1

### Chapter 2

### Chapter 3

## Part 2

## Part 3
```

</td></tr> 
</table>

${en:'Widget parameters', ru:'Свойства виджета'}:

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                 | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|--------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------:|
|              title              | ${en:'Title of the table of contents', ru:'Заглавие оглавления'}                                                   |                                                   |
|             ordered             | ${en:'Should the items in the table of contents be numbered', ru:'Должны ли быть пронумерованы пункты оглавления'} |                       false                       |

#### ${en:'Widget', ru:'Виджет'} 'date'

This component allows you to insert the current date into a document.<!--en-->
Этот компонент позволяет вставить в документ текущую дату.<!--ru-->

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th><th>${en:'Result', ru:'Результат'}</th></tr>
<tr><td>

```markdown
Last updated: &#36;{widget:date}
```

</td><td>

```markdown
Last updated: ${widget:date}
```

</td></tr>
<tr><td>

```markdown
&#36;{widget:date(pattern = 'dd.MM.yyyy')}
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
|             pattern             | ${en:'Pattern according to which the date will be formatted', ru:'Шаблон, в соответствии с котором будет отформатирована дата'} |                dd.MM.yyyy HH:mm:ss                |

You can read more about date pattern syntax in the<!--en-->
[Java documentation](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html).<!--en-->
Подробнее о синтаксисе шаблона даты можно прочитать в <!--ru-->
[документации языка Java](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html).<!--ru-->

### Feedback

TODO

*${en:'Last updated:', ru:'Дата последнего обновления:'} ${widget:date(pattern= 'dd.MM.yyyy')}*