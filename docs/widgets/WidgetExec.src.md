#### ${en:'Widget', ru:'Виджет'} 'exec'

This component runs an external command and embeds its stdout<!--en-->
into the generated document — useful for pasting in `--help` output,<!--en-->
version dumps, a CLI banner, or whatever else a build tool will print.<!--en-->
Этот компонент запускает внешнюю программу и вставляет её stdout<!--ru-->
в итоговый документ — удобно для `--help`, вывода версии,<!--ru-->
баннера CLI и всего, что может быть получено через консоль.<!--ru-->

> [!WARNING]<!--en-->
> **Opt-in for security.** Execution is disabled by default.<!--en-->
> The widget only runs commands when the caller explicitly asks for it<!--en-->
> via the `--allow-exec` CLI flag (or `<allowExec>true</allowExec>` in<!--en-->
> the Maven plugin). Running `nrg` over an untrusted template without<!--en-->
> that flag is safe: every `\${widget:exec(...)}` call logs an error and<!--en-->
> renders empty output.<!--en-->

> [!WARNING]<!--ru-->
> **Opt-in ради безопасности.** По умолчанию выполнение запрещено.<!--ru-->
> Виджет запускает команды только если вызывающая сторона явно<!--ru-->
> разрешила это через CLI-флаг `--allow-exec` (либо<!--ru-->
> `<allowExec>true</allowExec>` в Maven-плагине). Запуск `nrg`<!--ru-->
> над недоверенным шаблоном без этого флага безопасен: каждый<!--ru-->
> вызов `\${widget:exec(...)}` логирует ошибку и возвращает пустой вывод.<!--ru-->

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th><th>${en:'Behaviour', ru:'Поведение'}</th></tr>
<tr><td>

```markdown
\${widget:exec(cmd = 'java -jar nrg.jar --help')}
```

</td><td>

${en:'Runs `java -jar nrg.jar --help`, inlines stdout verbatim (with trailing whitespace trimmed).', ru:'Выполняет `java -jar nrg.jar --help`, вставляет stdout как есть (с обрезанными хвостовыми пробелами).'}

</td></tr>
<tr><td>

```markdown
\${widget:exec(cmd = 'git rev-parse --short HEAD', codeblock = 'text')}
```

</td><td>

${en:'Runs the command and wraps stdout in a fenced code block tagged `text`.', ru:'Выполняет команду и оборачивает stdout в fenced-блок с тегом `text`.'}

</td></tr>
<tr><td>

```markdown
\${widget:exec(cmd = './scripts/list-langs.sh', cwd = 'docs', timeout = '5')}
```

</td><td>

${en:'Runs the script from the `docs/` sub-directory of the source file, kills it if it exceeds 5 seconds.', ru:'Запускает скрипт из каталога `docs/` рядом с исходным файлом, убивает процесс при превышении 5 секунд.'}

</td></tr>
</table>

${en:'Widget parameters', ru:'Свойства виджета'}:

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                                                                                                                                                                                                                                                                                                                                                |      ${en:'Default value', ru:'Значение по умолчанию'}       |
|:-------------------------------:|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------------------------------------------------:|
|               cmd               | ${en:'Command line. Whitespace-split into argv; **no shell interpolation**, so pipes, redirects, and variable expansion do not work. Missing or blank values log an error and produce no output.', ru:'Командная строка. Разбивается по пробелам на argv; **интерполяция shell не выполняется**, поэтому конвейеры, перенаправления и подстановка переменных не работают. Отсутствующее или пустое значение приводит к ошибке и пустому выводу.'} |                                                              |
|               cwd               | ${en:'Working directory. Relative paths are resolved against the source-file directory; absolute paths are used as-is. Missing directory logs an error and produces no output.', ru:'Рабочий каталог. Относительные пути разрешаются от каталога исходного файла; абсолютные — используются как есть. Отсутствующий каталог приводит к ошибке и пустому выводу.'}                                                                                 | ${en:'source-file directory', ru:'каталог исходного файла'}  |
|             timeout             | ${en:'Maximum duration in seconds (positive integer). The subprocess is force-killed on timeout and a warning is logged; output is empty.', ru:'Максимальная длительность в секундах (положительное целое). При превышении процесс принудительно завершается с предупреждением; вывод пустой.'}                                                                                                                                                   |                             `30`                             |
|              trim               | ${en:'`true` strips trailing whitespace/newlines from stdout; `false` preserves them.', ru:'`true` удаляет хвостовые пробелы и переводы строк из stdout; `false` сохраняет их как есть.'}                                                                                                                                                                                                                                                         |                            `true`                            |
|            codeblock            | ${en:'When present, wraps stdout in a fenced code block with this language tag (`codeblock=""` wraps without a tag). When absent, stdout is inlined raw.', ru:'Если указан, оборачивает stdout в fenced-блок с данным тегом языка (`codeblock=""` — без тега). Если параметр отсутствует, stdout вставляется как есть.'}                                                                                                                          | ${en:'absent (no wrapping)', ru:'отсутствует (без обёртки)'} |

${en:'Enabling execution', ru:'Как включить выполнение'}:<!--en-->

- CLI: pass the `--allow-exec` flag to the `nrg` command.<!--en-->
- Maven: add `<allowExec>true</allowExec>` to the `nrg-maven-plugin` configuration (or set the `-DallowExec=true` property).<!--en-->
- Library: call `generator.getConfig().setExecAllowed(true)` before the first `getResult(...)` call.<!--en-->

${en:'Error handling', ru:'Обработка ошибок'}:<!--en-->

- Non-zero exit → error in the log (with exit code and stderr snippet) + empty output.<!--en-->
- Command not found / IO error → error in the log + empty output.<!--en-->
- Timeout → warning in the log + empty output.<!--en-->
- Invalid `timeout` or `trim` → error in the log + empty output (command is not run).<!--en-->

Как включить выполнение:<!--ru-->

- CLI: передайте флаг `--allow-exec` команде `nrg`.<!--ru-->
- Maven: добавьте `<allowExec>true</allowExec>` в конфигурацию плагина `nrg-maven-plugin` (или задайте свойство `-DallowExec=true`).<!--ru-->
- Библиотека: вызовите `generator.getConfig().setExecAllowed(true)` до первого вызова `getResult(...)`.<!--ru-->

Обработка ошибок:<!--ru-->

- Ненулевой exit-код → ошибка в логе (с кодом и фрагментом stderr) + пустой вывод.<!--ru-->
- Команда не найдена / ошибка ввода-вывода → ошибка в логе + пустой вывод.<!--ru-->
- Превышение `timeout` → предупреждение в логе + пустой вывод.<!--ru-->
- Некорректные `timeout` или `trim` → ошибка в логе + пустой вывод (команда не запускается).<!--ru-->

---

