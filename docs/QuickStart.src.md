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

\${widget:import(path='path/to/your/file/another-info.src.md')}
```

**${en:'Step 2: Build the files', ru:'Шаг 2: Сгенерируйте файлы'}**

*Вариант 1:* [Скачайте](#запуск-из-командной-строки) программу и запустите в консоли команду:<!--ru-->
*Option 1:* [Download](#using-the-command-line-interface) the program and run the command in the console:<!--en-->

```bash
nrg -f /path/to/README.src.md
```

*Вариант 2:* [Подключите](#использование-как-плагина-для-maven) к проекту плагин для maven.<!--ru-->
*Option 2:* [Add](#use-as-maven-plugin) the Maven plugin to your project.<!--en-->

**${en:'Result', ru:'Результат'}**

<table>
<tr><th><b>README.md</b></th><th><b>README.ru.md</b></th></tr>
<tr><td>

```markdown
[ **en** | [ru](README.ru.md) ]

# Hello, World!<!--toc.ignore-->

## Table of contents<!--toc.ignore-->

1. [Part 1](#part-1)
    1. [Chapter 1](#chapter-1)

## Part 1<!--toc.ignore-->

### Chapter 1<!--toc.ignore-->

English text
```

</td><td>

```markdown
[ [en](README.md) | **ru** ]

# Привет, Мир!<!--toc.ignore-->

## Содержание<!--toc.ignore-->

1. [Part 1](#part-1)
    1. [Chapter 1](#chapter-1)

## Part 1<!--toc.ignore-->

### Chapter 1<!--toc.ignore-->

Русский текст
```

</td></tr></table>