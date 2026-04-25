#### ${en:'Widget', ru:'Виджет'} 'if'

This is a **block widget**: it spans an opening `\${widget:if(cond='…')}` tag<!--en-->
and a matching `\${widget:endIf}` tag, and decides whether the lines<!--en-->
between them appear in the generated output. When the condition is false<!--en-->
the entire block — including any inner widgets — is dropped before the<!--en-->
per-line pipeline runs, so widgets in dead branches never execute.<!--en-->
Это **блочный виджет**: он охватывает открывающий тег `\${widget:if(cond='…')}` и<!--ru-->
парный `\${widget:endIf}`, и решает, попадут ли строки между ними в<!--ru-->
итоговый файл. При ложном условии целый блок (включая вложенные виджеты)<!--ru-->
отбрасывается до запуска per-line-конвейера, поэтому виджеты в мёртвой<!--ru-->
ветке не выполняются никогда.<!--ru-->

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th><th>${en:'Behaviour', ru:'Поведение'}</th></tr>
<tr><td>

```markdown
\${widget:if(cond='endsWith(\${devVersion}, -SNAPSHOT)')}
> Snapshot build — expect breaking changes.
\${widget:endIf}
```

</td><td>

${en:'The block is kept when `\${devVersion}` ends with `-SNAPSHOT`; otherwise the entire block (the markers and the body) is removed.', ru:'Блок остаётся, если `\${devVersion}` оканчивается на `-SNAPSHOT`; иначе блок полностью удаляется вместе с маркерами.'}

</td></tr>
<tr><td>

```markdown
\${widget:if(cond='\${env.CI}!=true && !\${dryRun}')}
This message only appears outside CI and outside dry runs.
\${widget:endIf}
```

</td><td>

${en:'Combines short-circuit `&&` with `!=` and `!` — the right side is not even resolved when the left is false.', ru:'Комбинирует short-circuit `&&` с `!=` и `!` — правая часть даже не резолвится, если левая — false.'}

</td></tr>
<tr><td>

```markdown
\${widget:if(cond='startsWith(\${repoUrl}, https://github.com/) || startsWith(\${repoUrl}, git@github.com:)')}
Hosted on GitHub.
\${widget:endIf}
```

</td><td>

${en:'`startsWith` / `endsWith` are case-sensitive; an empty needle is always true.', ru:'`startsWith` / `endsWith` — case-sensitive; пустой needle всегда истинен.'}

</td></tr>
</table>

${en:'Condition grammar (precedence low → high)', ru:'Грамматика условия (приоритет от низкого к высокому)'}:

| ${en:'Form', ru:'Форма'} | ${en:'Meaning', ru:'Смысл'}                                                                                                                    |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `X`                      | ${en:'truthy — true iff `X.trim()` is non-empty', ru:'truthy — истина, если `X.trim()` не пуст'}                                               |
| `!X`                     | ${en:'falsy — true iff `X.trim()` is empty', ru:'falsy — истина, если `X.trim()` пуст'}                                                        |
| `X == Y`                 | ${en:'equality (trim each side; quoted strings preserve whitespace)', ru:'равенство (тримит каждую сторону; quoted-строки сохраняют пробелы)'} |
| `X != Y`                 | ${en:'inequality', ru:'неравенство'}                                                                                                           |
| `A && B`                 | ${en:'and (short-circuit)', ru:'и (short-circuit)'}                                                                                            |
| `A                       |                                                                                                                                                | B`                 | ${en:'or (short-circuit)', ru:'или (short-circuit)'} |
| `(expr)`                 | ${en:'grouping', ru:'группировка'}                                                                                                             |
| `startsWith(h, n)`       | ${en:'true iff `h.startsWith(n)`; case-sensitive', ru:'истина, если `h.startsWith(n)`; case-sensitive'}                                        |
| `endsWith(h, n)`         | ${en:'true iff `h.endsWith(n)`; case-sensitive', ru:'истина, если `h.endsWith(n)`; case-sensitive'}                                            |

${en:'Operands are placeholders (`\${…}`), quoted strings (`\'…\'` or `"…"` with doubled-quote escape), or bare strings. Quoted strings preserve whitespace and protect operator characters; placeholders inside quoted strings are still resolved.', ru:'Операнды — это плейсхолдеры (`\${…}`), quoted-строки (`\'…\'` или `"…"` с удвоением кавычки) или bare-строки. Quoted-строки сохраняют пробелы и защищают operator-символы; плейсхолдеры внутри quoted-строк всё равно разрешаются.'}

${en:'Type rules', ru:'Типы'}:<!--en-->

- ${en:'Every value is a string; there are no numbers, booleans, or null.', ru:'Каждое значение — строка; ни чисел, ни булевых, ни null.'}<!--en-->
- ${en:'A `${msg}` resolving to `a && b` is treated as opaque text — operators inside placeholder values are *not* reinterpreted as boolean operators.', ru:'`${msg}`, резолвящийся в `a && b`, остаётся opaque-текстом — операторы внутри значений *не* трактуются как булевы.'}<!--en-->
- ${en:'No implicit case folding or numeric coercion: `\${env.CI}==True` does not match the env value `true`. Normalise upstream.', ru:'Никакого implicit case folding или числового приведения: `\${env.CI}==True` не совпадёт с env-значением `true`. Нормализуйте на стороне источника.'}<!--en-->

${en:'Правила типов', ru:''}:<!--ru-->

- ${en:'', ru:'Каждое значение — строка; ни чисел, ни булевых, ни null.'}<!--ru-->
- ${en:'', ru:'`${msg}`, резолвящийся в `a && b`, остаётся opaque-текстом — операторы внутри значений не трактуются как булевы.'}<!--ru-->
- ${en:'', ru:'Никакого implicit case folding или числового приведения: `\${env.CI}==True` не совпадёт с env-значением `true`. Нормализуйте на стороне источника.'}<!--ru-->

${en:'Errors', ru:'Ошибки'}:<!--en-->

- ${en:'An unclosed `\${widget:if}` block is reported via `LOG.error` and everything from the outermost open marker to EOF is dropped from the output.', ru:'Незакрытый блок `\${widget:if}` сообщается через `LOG.error`, а всё от внешнего открывающего маркера до EOF выбрасывается из вывода.'}<!--en-->
- ${en:'A stray `\${widget:endIf}` (no matching open) is reported via `LOG.error` and the marker line is dropped.', ru:'Одиночный `\${widget:endIf}` (без пары) сообщается через `LOG.error`, строка-маркер выбрасывается.'}<!--en-->
- ${en:'A malformed condition (unbalanced parens, trailing operators, unknown function names) is reported via `LOG.error` and the block is treated as if the condition were false.', ru:'Некорректное условие (несбалансированные скобки, висящие операторы, неизвестные функции) сообщается через `LOG.error`, блок трактуется как ложный.'}<!--en-->

${en:'', ru:'- Незакрытый блок `\${widget:if}` сообщается через `LOG.error`, а всё от внешнего открывающего маркера до EOF выбрасывается из вывода.'}<!--ru-->
${en:'', ru:'- Одиночный `\${widget:endIf}` (без пары) сообщается через `LOG.error`, строка-маркер выбрасывается.'}<!--ru-->
${en:'', ru:'- Некорректное условие (несбалансированные скобки, висящие операторы, неизвестные функции) сообщается через `LOG.error`, блок трактуется как ложный.'}<!--ru-->

${en:'Out of scope (v1)', ru:'За рамками v1'}: ${en:'numeric comparisons (`>`, `<`, …), regex matching, `else`/`elif`, scripting engine integration, `\${…}` resolution inside the *default* of `\${env.X:default}` references on the right-hand side of an `==`.', ru:'численные сравнения (`>`, `<`, …), regex-сопоставление, `else`/`elif`, scripting-engine, разрешение `\${…}` внутри *default*-части `\${env.X:default}` на правой стороне `==`.'}

---

