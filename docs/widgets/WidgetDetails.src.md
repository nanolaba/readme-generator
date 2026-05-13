#### ${en:'Widget', ru:'Виджет'} 'details'

This component renders a GitHub-flavored collapsible disclosure block<!--en-->
(`<details><summary>…</summary>…</details>`) so verbose tables, long examples,<!--en-->
and troubleshooting sections can hide behind a click without hand-writing HTML.<!--en-->
Two forms are supported: a **block form** with paired markers around inner<!--en-->
markdown (which keeps rendering through the normal pipeline — nested widgets,<!--en-->
variables, and language tags work inside), and a **single-tag form** for short<!--en-->
one-liners where the body fits in a `content=` parameter.<!--en-->
Этот компонент формирует «свёрнутый» блок-disclosure в стиле GitHub<!--ru-->
(`<details><summary>…</summary>…</details>`), позволяя спрятать громоздкие<!--ru-->
таблицы, длинные примеры и разделы troubleshooting за один клик, не выписывая<!--ru-->
HTML вручную. Поддерживаются две формы: **блочная** с парными маркерами вокруг<!--ru-->
внутреннего markdown (он по-прежнему проходит через основной пайплайн — вложенные<!--ru-->
виджеты, переменные и языковые теги внутри работают), и **однотеговая** для<!--ru-->
коротких однострочных вставок, где тело помещается в параметр `content=`.<!--ru-->

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

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------:|
|             summary             | ${en:'Always-visible title text. `${var}` substitution is applied in both forms.', ru:'Всегда видимый заголовок. Подстановка `${var}` работает в обеих формах.'}                                                                                                                                                                                                                                                                                                                                                                                                                              |                                                   |
|              open               | ${en:'Initial state. `''true''` emits `<details open>`; any other value (including `''false''`, empty, missing) emits plain `<details>`. Parsed via `Boolean.parseBoolean`.', ru:'Начальное состояние. `''true''` даёт `<details open>`; любое другое значение (включая `''false''`, пустое, отсутствие) даёт обычный `<details>`. Парсится через `Boolean.parseBoolean`.'}                                                                                                                                                                                                                   |                      `false`                      |
|             content             | ${en:'Single-tag form only. Body of the disclosure block. Presence of this parameter switches to the compact-inline single-tag form; the block form (with `\${widget:endDetails}`) MUST NOT set it. `\n` and `\\` are interpreted (other backslashes are preserved verbatim).', ru:'Только однотеговая форма. Тело блока. Наличие этого параметра переключает в компактный inline-режим; в блочной форме (с `\${widget:endDetails}`) он не должен задаваться. `\n` и `\\` интерпретируются (другие обратные слэши сохраняются).'} |                                                   |

---
