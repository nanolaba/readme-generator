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
<tr><th>${en:'Usage example', ru:'Пример использования'} (README.src.md)</th></tr>
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
<tr><th>${en:'Result', ru:'Результат'} (README.md)</th></tr>
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

### Ignored Chapter<!--toc.ignore-->

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

