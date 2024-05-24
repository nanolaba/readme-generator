<!--@nrg.languages=en,ru-->
<!--@nrg.defaultLanguage=en-->

<!--@nrg.name=**Nanolaba Readme Generator (NRG)**-->
<!--@nrg.stableVersion=1.0-->

${widget:languages}

# Nanolaba Readme Generator (NRG)

${nrg.name} — это программа для автоматической генерации файлов в формате<!--ru-->
[Markdown]( https://en.wikipedia.org/wiki/Markdown) на основе файла-прототипа.<!--ru-->
${nrg.name} — is a program for automatically<!--en-->
generating [Markdown files]( https://en.wikipedia.org/wiki/Markdown) based on a prototype file.<!--en-->

## ${en:'Overview', ru:'Краткое описание'}

Using the ${nrg.name} program, you can generate separate files for different languages<!--en-->
in [Markdown format]( https://en.wikipedia.org/wiki/Markdown) based on a single template file.<!--en-->
The template allows the use of variables and special components - widgets.<!--en-->
This document is an example of the result of this program.<!--en-->
<!--en-->
The latest stable version of the program is **${nrg.stableVersion}**.<!--en-->
С помощью программы ${nrg.name} можно генерировать отдельные файлы для разных языков в формате <!--ru-->
[Markdown]( https://en.wikipedia.org/wiki/Markdown) на основе единого файла-шаблона.<!--ru-->
В шаблоне можно использовать переменные, а также специальные компоненты - виджеты. <!--ru-->
Данный документ является примером результата работы этой программы.<!--ru-->
<!--ru-->
Последняя стабильная версия программы - **${nrg.stableVersion}**.<!--ru-->

${widget:tableOfContents(title = "${en:'Table of contents', ru:'Содержание'}", ordered = "true")}

## Usage

### Using the Command Line Interface

### Use as a java-library

## Template syntax

### Properties

### Multilanguage support

### Language properties

### Widgets

#### languages

#### tableOfContents

#### date

### Feedback

*${en:'Last updated:', ru:'Дата последнего обновления:'} ${widget:date(pattern= 'dd.MM.yyyy')}*