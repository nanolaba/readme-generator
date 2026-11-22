#### ${en:'Widget', ru:'Виджет'} 'if'

This widget keeps or drops content depending on a condition.<!--en-->
It comes in two forms. The **block form** is an opening `\${widget:if(cond='…')}` tag<!--en-->
and a matching `\${widget:endIf}` tag; the lines between them appear in the output only<!--en-->
when the condition is truthy. When it is false, the entire block, including inner widgets, is<!--en-->
dropped before the per-line pipeline runs, so widgets in dead branches never execute.<!--en-->
The **inline form** `\${widget:if(cond='…', text='…')}` emits `text` on one line when<!--en-->
the condition is truthy, or an empty string otherwise.<!--en-->
Этот виджет оставляет или убирает содержимое в зависимости от условия. Форм две:<!--ru-->
**блочная** — открывающий тег `\${widget:if(cond='…')}` и парный<!--ru-->
`\${widget:endIf}`; строки между ними попадают в вывод только при истинном условии. При<!--ru-->
ложном весь блок, включая вложенные виджеты, отбрасывается до построчной обработки,<!--ru-->
поэтому виджеты в мёртвой ветке не выполняются никогда. **Однотеговая** форма<!--ru-->
`\${widget:if(cond='…', text='…')}` выдаёт `text` одной строкой при истинном условии<!--ru-->
или пустую строку при ложном.<!--ru-->

The block is kept when `\${devVersion}` ends with `-SNAPSHOT`; otherwise the entire block (the markers and the body) is removed:<!--en-->
Блок остаётся, если `\${devVersion}` оканчивается на `-SNAPSHOT`; иначе блок полностью удаляется вместе с маркерами:<!--ru-->

```markdown
\${widget:if(cond='endsWith(\${devVersion}, -SNAPSHOT)')}
> Snapshot build — expect breaking changes.
\${widget:endIf}
```

${en:'Combines short-circuit `&&` with `!=` and `!` — the right side is not even resolved when the left is false:', ru:'Сочетает `&&` с `!=` и `!` — если левая часть `&&` ложна, правая даже не вычисляется:'}

```markdown
\${widget:if(cond='\${env.CI}!=true && !\${dryRun}')}
This message only appears outside CI and outside dry runs.
\${widget:endIf}
```

${en:'`startsWith` / `endsWith` are case-sensitive; an empty needle is always true:', ru:'`startsWith` / `endsWith` учитывают регистр; с пустой искомой строкой результат всегда истинен:'}

```markdown
\${widget:if(cond='startsWith(\${repoUrl}, https://github.com/) || startsWith(\${repoUrl}, git@github.com:)')}
Hosted on GitHub.
\${widget:endIf}
```

${en:'Inline form — short conditional snippet on one line:', ru:'Однотеговая форма — короткая условная вставка в одну строку:'}

```markdown
\${widget:if(cond='endsWith(\${devVersion}, -SNAPSHOT)', text='Snapshot build — expect breaking changes.')}
```

`\n` and `\\` inside `text=` are interpreted (`\n` → real newline, `\\` → single backslash). Property and language substitution against the outer template happen before the widget runs, so `\${var}` and language constructs inside `text=` work as usual.<!--en-->
`\n` и `\\` внутри `text=` интерпретируются (`\n` → реальный перевод строки, `\\` → одиночный обратный слэш). Подстановка свойств и языковых конструкций внешнего шаблона выполняется до запуска виджета, поэтому `\${var}` и языковые конструкции внутри `text=` работают как обычно.<!--ru-->

${en:'Condition grammar (precedence low → high)', ru:'Грамматика условия (приоритет от низкого к высокому)'}:

<!-- nrg:begin:reference --><!--nrg.ignore-->
| ${en:'Form', ru:'Форма'} | ${en:'Meaning', ru:'Смысл'}                                                                                                                    |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `X`                      | ${en:'truthy — true iff `X.trim()` is non-empty', ru:'truthy — истина, если `X.trim()` не пуст'}                                               |
| `!X`                     | ${en:'falsy — true iff `X.trim()` is empty', ru:'falsy — истина, если `X.trim()` пуст'}                                                        |
| `X == Y`                 | ${en:'equality (trim each side; quoted strings preserve whitespace)', ru:'равенство (пробелы по краям каждой стороны отбрасываются; строки в кавычках их сохраняют)'} |
| `X != Y`                 | ${en:'inequality', ru:'неравенство'}                                                                                                           |
| `A && B`                 | ${en:'and (short-circuit)', ru:'и (по короткой схеме)'}                                                                                            |
| `A \|\| B`               | ${en:'or (short-circuit)', ru:'или (по короткой схеме)'}                                                                                           |
| `(expr)`                 | ${en:'grouping', ru:'группировка'}                                                                                                             |
| `startsWith(h, n)`       | ${en:'true iff `h.startsWith(n)`; case-sensitive', ru:'истина, если `h.startsWith(n)`; с учётом регистра'}                                        |
| `endsWith(h, n)`         | ${en:'true iff `h.endsWith(n)`; case-sensitive', ru:'истина, если `h.endsWith(n)`; с учётом регистра'}                                            |
<!-- nrg:end:reference --><!--nrg.ignore-->

Operands are placeholders (`\${…}`), quoted strings (`'…'` or `"…"` with doubled-quote escape), or bare strings. Quoted strings preserve whitespace and protect operator characters; placeholders inside quoted strings are still resolved.<!--en-->
Операнды — это плейсхолдеры (`\${…}`), строки в кавычках (`'…'` или `"…"`, кавычка внутри удваивается) или строки без кавычек. Строки в кавычках сохраняют пробелы и защищают символы операторов; плейсхолдеры внутри них всё равно подставляются.<!--ru-->

Type rules:<!--en-->

- Every value is a string; there are no numbers, booleans, or null.<!--en-->
- A `\${msg}` resolving to `a && b` is treated as opaque text — operators inside placeholder values are *not* reinterpreted as boolean operators.<!--en-->
- No implicit case folding or numeric coercion: `\${env.CI}==True` does not match the env value `true`. Normalise upstream.<!--en-->

Правила типов:<!--ru-->

- Каждое значение — строка; ни чисел, ни булевых, ни null.<!--ru-->
- `\${msg}` со значением `a && b` остаётся просто текстом — операторы внутри значений *не* трактуются как булевы.<!--ru-->
- Регистр и числа неявно не приводятся: `\${env.CI}==True` не совпадёт со значением переменной окружения `true`. Нормализуйте на стороне источника.<!--ru-->

Errors:<!--en-->

- An unclosed `\${widget:if}` block is reported via `LOG.error` and everything from the outermost open marker to EOF is dropped from the output.<!--en-->
- A stray `\${widget:endIf}` (no matching open) is reported via `LOG.error` and the marker line is dropped.<!--en-->
- A malformed condition (unbalanced parens, trailing operators, unknown function names) is reported via `LOG.error` and the block is treated as if the condition were false.<!--en-->

Ошибки:<!--ru-->

- О незакрытом блоке `\${widget:if}` сообщается через `LOG.error`, а всё от внешнего открывающего маркера до EOF удаляется из вывода.<!--ru-->
- Об одиночном `\${widget:endIf}` (без пары) сообщается через `LOG.error`, строка-маркер удаляется.<!--ru-->
- О некорректном условии (несбалансированные скобки, висящие операторы, неизвестные функции) сообщается через `LOG.error`, а блок считается ложным.<!--ru-->

Not supported: numeric comparisons (`>`, `<`, …), regex matching, `else`/`elif`, scripting engines, `\${…}` resolution inside the *default* of `\${env.X:default}` references on the right-hand side of an `==`.<!--en-->
Не поддерживаются: числовые сравнения (`>`, `<`, …), регулярные выражения, `else`/`elif`, скриптовые движки, подстановка `\${…}` внутри *default*-части `\${env.X:default}` в правой части `==`.<!--ru-->

---

