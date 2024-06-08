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

## ${en:'Template syntax', ru:'Создание шаблона'}

### Properties

TODO

### Multilanguage support

TODO

### Language properties

TODO

### Widgets

#### LanguagesWidget (languages)

This component allows you to generate links to other versions of a document (written in other languages).<!--en-->
Этот компонент позволяет генерировать ссылки на другие версии документа (написанные на других языках).<!--ru-->

${en:'Usage example', ru:'Пример использования'}:

```markdown
&#36;{widget:languages}
```

${en:'Result', ru:'Результат'}:

```markdown
${widget:languages}
```

#### tableOfContents

TODO

#### date

TODO

### Feedback

TODO

*${en:'Last updated:', ru:'Дата последнего обновления:'} ${widget:date(pattern= 'dd.MM.yyyy')}*