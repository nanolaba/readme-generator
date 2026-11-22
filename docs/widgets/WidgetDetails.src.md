#### ${en:'Widget', ru:'Виджет'} 'details'

This component renders a collapsible block<!--en-->
(`<details><summary>…</summary>…</details>`) so verbose tables, long examples,<!--en-->
and troubleshooting sections can hide behind a click without hand-writing HTML.<!--en-->
Two forms are supported: a **block form** with paired markers around inner<!--en-->
markdown (which keeps rendering through the normal pipeline — nested widgets,<!--en-->
variables, and language tags work inside), and a **single-tag form** for<!--en-->
one-liners where the body fits in a `content=` parameter.<!--en-->
Этот компонент формирует сворачиваемый блок<!--ru-->
(`<details><summary>…</summary>…</details>`): громоздкие<!--ru-->
таблицы, длинные примеры и разделы о решении проблем открываются по клику, а писать<!--ru-->
HTML вручную не нужно. Поддерживаются две формы: **блочная** с парными маркерами вокруг<!--ru-->
внутреннего markdown (он проходит обычную обработку — вложенные<!--ru-->
виджеты, переменные и языковые теги внутри работают), и **однотеговая** для<!--ru-->
однострочных вставок, где тело помещается в параметр `content=`.<!--ru-->

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th><th>${en:'Result', ru:'Результат'}</th></tr>
<tr><td>

```markdown
\${widget:details(summary='Advanced')}
inner *markdown* and \${widget:date}
\${widget:endDetails}
```

</td><td>

```markdown
${widget:details(summary='Advanced')}
inner *markdown* and ${widget:date}
${widget:endDetails}
```

</td></tr>
<tr><td>

```markdown
\${widget:details(summary='Click', content='Hidden', open='true')}
```

</td><td>

```markdown
${widget:details(summary='Click', content='Hidden', open='true')}
```

</td></tr>
</table>

${en:'Widget parameters', ru:'Свойства виджета'}:

<!-- nrg:begin:reference --><!--nrg.ignore-->
| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------:|
|             summary             | ${en:'Always-visible title text. `\${var}` substitution is applied in both forms.', ru:'Всегда видимый заголовок. Подстановка `\${var}` работает в обеих формах.'}                                                                                                                                                                                                                                                                                                                                                                                                                              |                                                   |
|              open               | ${en:'Initial state. `''true''` emits `<details open>`; any other value (including `''false''`, empty, missing) emits plain `<details>`. Parsed via `Boolean.parseBoolean`.', ru:'Начальное состояние. `''true''` даёт `<details open>`; любое другое значение (включая `''false''`, пустое, отсутствие) даёт обычный `<details>`. Парсится через `Boolean.parseBoolean`.'}                                                                                                                                                                                                                   |                      `false`                      |
|             content             | ${en:'Single-tag form only. Body of the disclosure block. Setting this parameter selects the single-tag form, so the block form (with `\${widget:endDetails}`) must not set it. `\n` and `\\` are interpreted (other backslashes are preserved verbatim).', ru:'Только однотеговая форма. Тело блока. Если параметр задан, виджет работает в однотеговой форме, поэтому в блочной форме (с `\${widget:endDetails}`) его задавать нельзя. `\n` и `\\` интерпретируются (другие обратные слэши сохраняются).'} |                                                   |
<!-- nrg:end:reference --><!--nrg.ignore-->

---
