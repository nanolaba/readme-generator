#### ${en:'Widget', ru:'Виджет'} 'tableOfContents'

This component allows you to generate a table of contents for a document.<!--en-->
The table of contents is created from headers formed using the hashtag symbol (`#`).<!--en-->
Headers located above the widget in the text are ignored.<!--en-->
Этот компонент позволяет сформировать оглавление для документа.<!--ru-->
Оглавление формируется из заголовков, сформированных при помощи знака решётки (`#`).<!--ru-->
Заголовки, которые расположены по тексту выше виджета, игнорируются.<!--ru-->

Also addressable as `\${widget:toc(...)}` — both names render identically.<!--en-->
Также доступен под коротким именем `\${widget:toc(...)}` — обе формы рендерятся одинаково.<!--ru-->

If you need to exclude a header from the table of contents, you should<!--en-->
mark it with a comment `<\!--toc.ignore-->`.<!--en-->
Если вам необходимо исключить какой-либо заголовок из оглавления, то для этого<!--ru-->
его необходимо пометить комментарием `<\!--toc.ignore-->`.<!--ru-->

Lines that look like headings but are part of a fenced code block (opened with three or more backticks or tildes), of an indented code block (4+ leading spaces), of an inline code span, or of a backslash-escaped heading are not treated as headings and do not appear in the table of contents.<!--en-->
Строки, выглядящие как заголовки, но входящие в огороженный блок кода (открываемый тремя или более обратными апострофами или тильдами), в блок кода с отступом (4+ ведущих пробела), во встроенный код или в экранированный обратной косой чертой заголовок, не считаются заголовками и не попадают в оглавление.<!--ru-->

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

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------:|
|              title              | ${en:'Title of the table of contents', ru:'Заглавие оглавления'}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |                                                   |
|             ordered             | ${en:'Should the items in the table of contents be numbered', ru:'Должны ли быть пронумерованы пункты оглавления'}                                                                                                                                                                                                                                                                                                                                                                                                                                                    |                      `false`                      |
|            min-depth            | ${en:'Minimum heading level to include (1–6). Headings shallower than this are skipped. 1 includes top-level `#` headings.', ru:'Минимальный уровень заголовка (1–6). Заголовки более мелкого уровня исключаются. Значение 1 включает заголовки верхнего уровня `#`.'}                                                                                                                                                                                                                                                                                                |                        `2`                        |
|            max-depth            | ${en:'Maximum heading level to include (1–6). Headings deeper than this are skipped.', ru:'Максимальный уровень заголовка (1–6). Заголовки более глубокого уровня исключаются.'}                                                                                                                                                                                                                                                                                                                                                                                      |                        `6`                        |
|            min-items            | ${en:'Minimum number of headings (after depth and `<\!--toc.ignore-->` filters) required to render the widget. Below this threshold the widget produces no output (no title, no items).', ru:'Минимальное число заголовков (после фильтров по глубине и `<\!--toc.ignore-->`), при котором виджет выводит оглавление. При меньшем числе виджет не выводит ничего (ни заголовка, ни пунктов).'}                                                                                                                                                                        |                        `1`                        |
|          anchor-style           | ${en:'Anchor-slugification style: `github` (default), `gitlab`, or `bitbucket`. GitLab preserves underscores and does not collapse consecutive hyphens; Bitbucket prefixes anchors with `markdown-header-`. An unknown value logs an error and causes the widget to render nothing.', ru:'Стиль формирования якорей: `github` (по умолчанию), `gitlab` или `bitbucket`. GitLab сохраняет подчёркивания и не схлопывает подряд идущие дефисы; Bitbucket добавляет префикс `markdown-header-`. При неизвестном значении выводится ошибка и виджет ничего не рендерит.'} |                     `github`                      |
|         numbering-style         | ${en:'Counter-prefix style when `ordered=true`: `default` (today''s `1.` markers — byte-identical to omitting the parameter), hierarchical `dotted` (`1`, `1.1`, `1.1.1`), `legal` (`1.`, `1.1.`, `1.1.1.`), `appendix` (`A`, `A.1`, `A.1.1`), or flat global counters `arabic` / `roman` / `roman-upper` / `alpha` / `alpha-upper`. Unknown values log an error and fall back to `default`. No effect when `ordered=false`.', ru:'Стиль префикса-счётчика при `ordered=true`: `default` (текущие маркеры `1.` — побайтно совпадает с пропуском параметра), иерархические `dotted` (`1`, `1.1`, `1.1.1`), `legal` (`1.`, `1.1.`, `1.1.1.`), `appendix` (`A`, `A.1`, `A.1.1`) или плоские глобальные счётчики `arabic` / `roman` / `roman-upper` / `alpha` / `alpha-upper`. При неизвестном значении в лог пишется ошибка и используется `default`. Не действует при `ordered=false`.'}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |                     `default`                     |
|              start              | ${en:'First top-level counter value. Type-matched to `numbering-style`: digits for `dotted` / `legal` / `arabic`, roman numeral for `roman` / `roman-upper`, single letter for `alpha` / `alpha-upper` / `appendix`. Invalid input logs an error and falls back to the natural first value. Ignored when `numbering-style=default`.', ru:'Первое значение счётчика верхнего уровня. Тип соответствует `numbering-style`: цифры для `dotted` / `legal` / `arabic`, римское число для `roman` / `roman-upper`, одна буква для `alpha` / `alpha-upper` / `appendix`. При недопустимом значении в лог пишется ошибка и используется естественное первое значение. Игнорируется при `numbering-style=default`.'}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |                                                   |

${en:'Numbering styles', ru:'Стили нумерации'}:

Set `numbering-style=...` (with `ordered="true"`) to pick a counter shape — hierarchical for outline-style references, flat for short summaries.<!--en-->
Задайте `numbering-style=...` (вместе с `ordered="true"`), чтобы выбрать форму счётчика — иерархическую для ссылок-«разделов» или плоскую для коротких списков.<!--ru-->

<table>
<tr><th>${en:'Usage', ru:'Использование'} — `numbering-style="dotted"` (README.src.md)</th></tr>
<tr><td>

```markdown
\${widget:tableOfContents(ordered = "true", numbering-style = "dotted", min-depth = "1")}

# Introduction<!--toc.ignore-->

## Setup<!--toc.ignore-->

## Usage<!--toc.ignore-->
```

</td></tr>
<tr><th>${en:'Result', ru:'Результат'} (README.md)</th></tr>
<tr><td>

```markdown
- 1 [Introduction](#introduction)
    - 1.1 [Setup](#setup)
    - 1.2 [Usage](#usage)
```

</td></tr>
<tr><th>${en:'Usage', ru:'Использование'} — `numbering-style="legal"` (README.src.md)</th></tr>
<tr><td>

```markdown
\${widget:tableOfContents(ordered = "true", numbering-style = "legal", min-depth = "1")}

# Introduction<!--toc.ignore-->

## Setup<!--toc.ignore-->

## Usage<!--toc.ignore-->
```

</td></tr>
<tr><th>${en:'Result', ru:'Результат'} (README.md)</th></tr>
<tr><td>

```markdown
- 1. [Introduction](#introduction)
    - 1.1. [Setup](#setup)
    - 1.2. [Usage](#usage)
```

</td></tr>
<tr><th>${en:'Usage', ru:'Использование'} — `numbering-style="appendix"` (README.src.md)</th></tr>
<tr><td>

```markdown
\${widget:tableOfContents(ordered = "true", numbering-style = "appendix", min-depth = "1")}

# Appendix One<!--toc.ignore-->

## Tables<!--toc.ignore-->

# Appendix Two<!--toc.ignore-->
```

</td></tr>
<tr><th>${en:'Result', ru:'Результат'} (README.md)</th></tr>
<tr><td>

```markdown
- A [Appendix One](#appendix-one)
    - A.1 [Tables](#tables)
- B [Appendix Two](#appendix-two)
```

</td></tr>
<tr><th>${en:'Usage', ru:'Использование'} — `numbering-style="arabic"` (README.src.md)</th></tr>
<tr><td>

```markdown
\${widget:tableOfContents(ordered = "true", numbering-style = "arabic")}

## Introduction<!--toc.ignore-->

## Setup<!--toc.ignore-->

## Usage<!--toc.ignore-->
```

</td></tr>
<tr><th>${en:'Result', ru:'Результат'} (README.md)</th></tr>
<tr><td>

```markdown
- 1 [Introduction](#introduction)
- 2 [Setup](#setup)
- 3 [Usage](#usage)
```

</td></tr>
</table>

---

