#### ${en:'Widget', ru:'Виджет'} 'math'

This component renders LaTeX math formulas. GitHub's inline `$…$` /<!--en-->
block `$$…$$` support can be flaky around `\text`, punctuation, or<!--en-->
table cells — when that bites, switch to the SVG renderer to get a<!--en-->
pre-rendered image via a LaTeX-to-SVG endpoint.<!--en-->
Этот компонент отображает формулы LaTeX. Встроенная поддержка<!--ru-->
`$…$` / `$$…$$` в GitHub иногда капризничает с `\text`,<!--ru-->
знаками пунктуации и содержимым таблиц — в таких случаях<!--ru-->
переключайтесь на SVG-рендерер, который возвращает готовую<!--ru-->
картинку через LaTeX-to-SVG сервис.<!--ru-->

${en:'Inline LaTeX with the default native renderer:', ru:'Инлайновая формула LaTeX через стандартный native-рендерер:'}

```markdown
\${widget:math(expr = '\\pi r^2')}
```

${en:'Generated Markdown:', ru:'Сгенерированный Markdown:'}

```markdown
${widget:math(expr = '\\pi r^2')}
```

---

${en:'Block-level LaTeX (use `display = ''block''` to wrap with `$$…$$`):', ru:'Блочная формула LaTeX (`display = ''block''` оборачивает в `$$…$$`):'}

```markdown
\${widget:math(expr = '\\sum_{i=0}^{n} x_i', display = 'block')}
```

${en:'Generated Markdown:', ru:'Сгенерированный Markdown:'}

```markdown
${widget:math(expr = '\\sum_{i=0}^{n} x_i', display = 'block')}
```

---

${en:'SVG fallback (`renderer = ''svg''`) for cases where GitHub''s native MathJax mis-parses the formula. The full time-dependent Schrödinger equation, with nested fractions, partial derivatives, and Greek letters, renders as a single image that GitHub displays inline:', ru:'SVG-фолбэк (`renderer = ''svg''`) для случаев, когда встроенный GitHub MathJax неправильно разбирает формулу. Полное нестационарное уравнение Шрёдингера со вложенными дробями, частными производными и греческими буквами рендерится как одна картинка, которую GitHub показывает прямо в тексте:'}

```markdown
\${widget:math(expr = 'i\\hbar\\,\\frac{\\partial\\Psi}{\\partial t}=-\\frac{\\hbar^2}{2m}\\,\\nabla^2\\Psi+V\\Psi', renderer = 'svg', display = 'block')}
```

${en:'Generated Markdown:', ru:'Сгенерированный Markdown:'}

```markdown
${widget:math(expr = 'i\\hbar\\,\\frac{\\partial\\Psi}{\\partial t}=-\\frac{\\hbar^2}{2m}\\,\\nabla^2\\Psi+V\\Psi', renderer = 'svg', display = 'block')}
```

${en:'Rendered result:', ru:'Отображение:'}

${widget:math(expr = 'i\\hbar\\,\\frac{\\partial\\Psi}{\\partial t}=-\\frac{\\hbar^2}{2m}\\,\\nabla^2\\Psi+V\\Psi', renderer = 'svg', display = 'block')}

${en:'Widget parameters', ru:'Свойства виджета'}:

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                                                                                                                                                                                                                                                                                                                                                             | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------:|
|              expr               | ${en:'LaTeX source. Every backslash must be doubled (so `\\pi` produces a single `\pi`, `\\sum` produces `\sum`, and so on). Missing or empty values log an error and produce no output.', ru:'Исходник LaTeX. Каждый обратный слэш удваивается (`\\pi` даёт `\pi`, `\\sum` даёт `\sum` и т. д.). Отсутствующее или пустое значение приводит к ошибке и пустому выводу.'}                                                                                      |                                                   |
|             display             | ${en:'`inline` renders `$…$`; `block` renders `$$…$$` for the native renderer, or prepends `\\displaystyle` for the svg renderer. Unknown values log an error and produce no output.', ru:'`inline` формирует `$…$`; `block` — `$$…$$` для native либо добавляет `\\displaystyle` перед выражением для svg. Неизвестные значения приводят к ошибке и пустому выводу.'}                                                                                         |                     `inline`                      |
|            renderer             | ${en:'`native` emits GitHub MathJax delimiters. `svg` emits a Markdown image that links to a LaTeX-to-SVG service — use it when native rendering mis-parses the formula. Unknown values log an error and produce no output.', ru:'`native` использует разделители GitHub MathJax. `svg` формирует Markdown-картинку через LaTeX-to-SVG сервис — удобен, когда native неправильно разбирает формулу. Неизвестные значения приводят к ошибке и пустому выводу.'} |                     `native`                      |
|               alt               | ${en:'Alt text used by the svg renderer. Defaults to the raw expression.', ru:'Alt-текст для svg-рендерера. По умолчанию — само выражение.'}                                                                                                                                                                                                                                                                                                                   |                      `expr`                       |
|             service             | ${en:'URL prefix of the LaTeX-to-SVG endpoint; the URL-encoded expression is appended to it. Used only by the svg renderer.', ru:'URL-префикс LaTeX-to-SVG сервиса; закодированное выражение дописывается в конец. Используется только svg-рендерером.'}                                                                                                                                                                                                       |      `https://latex.codecogs.com/svg.image?`      |

Tips / caveats:<!--en-->

- Curly braces `{` / `}` inside `expr` work as in LaTeX (subscripts, superscripts, `\\text{…}`, `\\frac{…}{…}`).<!--en-->
- Raw `(` and `)` are not allowed inside `expr` because the widget-tag parser uses them as delimiters — wrap them with `\\left(` / `\\right)` for LaTeX grouping.<!--en-->
- The svg renderer depends on an external service, so generated images break if the endpoint disappears. Self-host or pin a known-good URL via `service` for long-lived docs.<!--en-->
- Pre-rendering, MathML output, and LaTeX linting are out of scope.<!--en-->

Подсказки и ограничения:<!--ru-->

- Фигурные скобки `{` / `}` внутри `expr` работают как в LaTeX (подстрочные/надстрочные индексы, `\\text`, `\\frac` и т. п.).<!--ru-->
- Круглые скобки `(` и `)` внутри `expr` использовать нельзя: парсер тегов использует их как разделители. Для группировки применяйте `\\left(` / `\\right)`.<!--ru-->
- svg-рендерер зависит от внешнего сервиса — если сервис исчезнет, картинки перестанут отображаться. Для долгоживущей документации имеет смысл поднять собственный сервис и указать его через `service`.<!--ru-->
- Пре-рендеринг, вывод MathML и проверка синтаксиса LaTeX не поддерживаются.<!--ru-->

---

