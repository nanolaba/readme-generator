#### ${en:'Widget', ru:'Виджет'} 'fileTree'

This component renders a `tree -L`-style directory listing with Unicode<!--en-->
box-drawing characters. Use it to embed an always-current view of a<!--en-->
folder's structure into the README without hand-maintaining ASCII art.<!--en-->
Этот компонент рендерит листинг каталога в стиле `tree -L` с<!--ru-->
Unicode-символами рамок. Удобно встроить в README актуальную<!--ru-->
структуру папок, не поддерживая ASCII-арт вручную.<!--ru-->

${en:'Lists the directory contents one level deep, wrapped in a fenced code block:', ru:'Выводит содержимое каталога на один уровень, обёрнутое в fenced-блок:'}

```markdown
\${widget:fileTree(path = 'nrg/src/main/java/com/nanolaba/nrg/widgets', depth = '1')}
```

${en:'Two-level listing with build artefacts and IDE folders excluded via comma-separated globs:', ru:'Двухуровневый листинг с исключением build-артефактов и IDE-каталогов через comma-separated глоб-шаблоны:'}

```markdown
\${widget:fileTree(path = '.', depth = '2', exclude = 'target,.idea,.git,*.class')}
```

${en:'Three-level directory-only outline, emitted raw without a code fence:', ru:'Трёхуровневая структура только из каталогов, без code-fence:'}

```markdown
\${widget:fileTree(path = 'nrg/src', depth = '3', dirsOnly = 'true', codeblock = 'false')}
```

${en:"Live example", ru:"Живой пример"} — `\${widget:fileTree(path='../../nrg/src/', dirsOnly = 'true', depth='3')}`:

${widget:fileTree(path='../../nrg/src/', dirsOnly = 'true', depth='3')}

${en:'Widget parameters', ru:'Свойства виджета'}:

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                                                                                                                                                                                                                                                                                          | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------:|
|              path               | ${en:'Directory to list. Relative paths are resolved against the source-file directory; absolute paths are used as-is. Missing or non-directory paths log an error and produce no output.', ru:'Каталог для листинга. Относительные пути разрешаются относительно каталога исходного файла; абсолютные — используются как есть. Отсутствующий или не-каталог приводит к ошибке и пустому выводу.'} |                                                   |
|              depth              | ${en:'Recursion limit (positive integer). `1` lists only direct children; `2` descends one level deeper, and so on.', ru:'Лимит рекурсии (положительное целое). `1` выводит только прямых потомков; `2` — на уровень глубже, и т. д.'}                                                                                                                                                  |                       `2`                         |
|             exclude             | ${en:'Comma-separated glob patterns. Each pattern is matched against both the entry name and the entry''s path relative to `path` — so `target` skips a folder named `target` anywhere, and `sub/drop.txt` targets one specific file.', ru:'Comma-separated глоб-шаблоны. Каждый шаблон сопоставляется и с именем элемента, и с его путём относительно `path` — поэтому `target` пропускает папку `target` на любой глубине, а `sub/drop.txt` — конкретный файл.'} |                  ${en:'(none)', ru:'(нет)'}      |
|            dirsOnly             | ${en:'`true` lists directories only; files are hidden.', ru:'`true` показывает только каталоги; файлы скрываются.'}                                                                                                                                                                                                                                                                          |                     `false`                       |
|            codeblock            | ${en:'`true` wraps the output in a fenced code block; `false` emits raw text.', ru:'`true` оборачивает вывод в fenced-блок; `false` — без обёртки.'}                                                                                                                                                                                                                                          |                      `true`                       |

Behaviour:<!--en-->

- Entries are sorted directories-first, then alphabetically within each group, for stable byte-exact output that survives `--check`.<!--en-->
- Glob syntax follows `java.nio.file.PathMatcher` (`*`, `?`, `**`, `{a,b}`, `[abc]`).<!--en-->
- Symbolic links are followed as regular directories or files; cycles are not detected — keep `depth` finite.<!--en-->

Поведение:<!--ru-->

- Записи сортируются: сначала каталоги, потом файлы; внутри групп — по алфавиту. Это даёт стабильный byte-exact вывод, переживающий `--check`.<!--ru-->
- Синтаксис glob — `java.nio.file.PathMatcher` (`*`, `?`, `**`, `{a,b}`, `[abc]`).<!--ru-->
- Символические ссылки трактуются как обычные каталоги или файлы; циклы не отслеживаются — задавайте конечный `depth`.<!--ru-->

---

