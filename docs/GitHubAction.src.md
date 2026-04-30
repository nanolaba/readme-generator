### ${en:'Use as a GitHub Action', ru:'Использование в качестве GitHub Action'}

NRG ships as a composite GitHub Action so any repository can regenerate<!--en-->
multi-language README files in CI without installing Maven or Java<!--en-->
locally. The action optionally sets up Java, downloads the requested NRG<!--en-->
release zip, extracts `nrg.jar`, and runs it against the templates you<!--en-->
list — README maintenance becomes a single workflow step.<!--en-->
NRG доступен как composite GitHub Action: любой репозиторий может<!--ru-->
регенерировать мультиязычные README прямо в CI, не устанавливая Maven или<!--ru-->
Java локально. Action (опционально) ставит Java, скачивает нужный релиз<!--ru-->
NRG, извлекает `nrg.jar` и запускает его для перечисленных шаблонов —<!--ru-->
поддержка README сводится к одному шагу workflow.<!--ru-->

#### ${en:'Quickstart', ru:'Быстрый старт'}

```yaml
- uses: actions/checkout@v4
- uses: nanolaba/nrg-action@v1
  with:
    file: README.src.md
```

#### ${en:'Inputs', ru:'Входные параметры'}

| ${en:'Name', ru:'Имя'} | ${en:'Description', ru:'Описание'} | ${en:'Default', ru:'По умолчанию'} |
|---|---|---|
| `file` | ${en:'Path to the `.src.md` template (relative to `working-directory`). Use `files` for multiple.', ru:'Путь к `.src.md`-шаблону (относительно `working-directory`). Для нескольких файлов используйте `files`.'} | — |
| `files` | ${en:'Multi-line list of templates (one per line). Mutually exclusive with `file`.', ru:'Многострочный список шаблонов (по одному на строку). Не совместим с `file`.'} | — |
| `charset` | ${en:'Source file encoding.', ru:'Кодировка исходных файлов.'} | `UTF-8` |
| `mode` | ${en:'Operation mode: `generate`, `check`, or `validate`.', ru:'Режим работы: `generate`, `check` или `validate`.'} | `generate` |
| `check-paths` | ${en:'Multi-line list of glob patterns (one per line) limiting which generated outputs `mode: check` compares against on-disk files. Requires `mode: check`.', ru:'Многострочный список glob-шаблонов (по одному на строку), ограничивающий, какие сгенерированные файлы `mode: check` сравнивает с файлами на диске. Требует `mode: check`.'} | — |
| `nrg-version` | ${en:'NRG release tag (e.g. `v1.0`) or `latest`.', ru:'Тег релиза NRG (например, `v1.0`) или `latest`.'} | `latest` |
| `java-version` | ${en:'JDK version for `actions/setup-java`. Ignored when `setup-java=false`.', ru:'Версия JDK для `actions/setup-java`. Игнорируется при `setup-java=false`.'} | `17` |
| `java-distribution` | ${en:'JDK distribution for `actions/setup-java`.', ru:'Дистрибутив JDK для `actions/setup-java`.'} | `temurin` |
| `setup-java` | ${en:'Whether the action should install Java itself. Set to `"false"` if Java is already set up earlier in the job.', ru:'Должен ли action сам устанавливать Java. Передайте `"false"`, если Java уже установлена в предыдущем шаге job-а.'} | `true` |
| `log-level` | ${en:'NRG log verbosity: `trace`, `debug`, `info`, `warn`, or `error`.', ru:'Уровень логирования NRG: `trace`, `debug`, `info`, `warn` или `error`.'} | `info` |
| `working-directory` | ${en:'Directory in which NRG runs.', ru:'Рабочий каталог, в котором запускается NRG.'} | `.` |

${en:'`mode` semantics:', ru:'Семантика режима (`mode`):'}

- ${en:'`generate` writes `README.md`, `README.ru.md`, … to disk (default).', ru:'`generate` записывает `README.md`, `README.ru.md`, … на диск (по умолчанию).'}
- ${en:'`check` is a read-only verification: if files on disk differ from what NRG would generate, it exits with a non-zero status and prints a unified diff. Use this on pull requests.', ru:'`check` — проверка без записи: если файлы на диске расходятся с тем, что сгенерировал бы NRG, action завершается с ненулевым кодом и печатает unified diff. Удобен для pull request-ов.'}
- ${en:'`validate` scans the template for authoring mistakes (unknown widgets, undeclared language markers, missing imports, unbalanced ignore-blocks). No files are written.', ru:'`validate` проверяет шаблон на авторские ошибки (неизвестные виджеты, необъявленные языковые маркеры, отсутствующие импорты, несбалансированные ignore-блоки). Файлы не записываются.'}

#### ${en:'Outputs', ru:'Выходные значения'}

| ${en:'Name', ru:'Имя'} | ${en:'Description', ru:'Описание'} |
|---|---|
| `version` | ${en:'Resolved NRG version (e.g. `v1.0`). Useful with `nrg-version=latest`.', ru:'Итоговая версия NRG (например, `v1.0`). Полезно при `nrg-version=latest`.'} |
| `changed-files` | ${en:'Newline-separated list of files written or modified by NRG.', ru:'Список файлов, созданных или изменённых NRG, по одному на строку.'} |

#### ${en:'Examples', ru:'Примеры'}

##### ${en:'Basic generate', ru:'Базовая генерация'}

${en:'Regenerate the README on every push to `main`:', ru:'Регенерация README при каждом push в `main`:'}

```yaml
name: Regenerate README
on:
  push:
    branches: [main]
permissions:
  contents: read
jobs:
  regenerate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: nanolaba/nrg-action@v1
        with:
          file: README.src.md
```

##### ${en:'Drift check on PR', ru:'Проверка расхождений в PR'}

${en:'Fail the build when a contributor edits `README.md` directly instead of regenerating it from `README.src.md`:', ru:'Завершать сборку с ошибкой, если автор изменил `README.md` напрямую, вместо того чтобы перегенерировать его из `README.src.md`:'}

```yaml
name: README drift check
on:
  pull_request:
    paths: ['**/*.src.md', '**/*.md']
permissions:
  contents: read
jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: nanolaba/nrg-action@v1
        with:
          file: README.src.md
          mode: check
```

##### ${en:'Drift check on a subset of outputs', ru:'Drift-проверка по подмножеству файлов'}

${en:'When only some generated files are tracked in git (e.g. only the canonical `README.md` is committed and translations are bot-managed), `check-paths` limits the comparison to those files. Patterns are cwd-relative `glob:` globs (`**/` matches zero or more directories). A pattern that matches no generated output prints a `WARN` to stderr but still exits `0`, so typos do not silently disable the check:', ru:'Когда в git лежит только часть сгенерированных файлов (например, коммитится только канонический `README.md`, а переводы регенерируются ботом), `check-paths` сужает сравнение до перечисленных. Шаблоны — cwd-относительные `glob:`-маски (`**/` соответствует нулю и более каталогов). Шаблон без совпадений печатает `WARN` в stderr, но завершается с кодом `0`, чтобы опечатка не отключала проверку молча:'}

```yaml
- uses: nanolaba/nrg-action@v1
  with:
    file: README.src.md
    mode: check
    check-paths: |
      README.md
      docs/canonical/*.md
```

${en:'Validate-only and auto-commit-via-PR recipes are in the [`nrg-action/examples`](https://github.com/nanolaba/readme-generator/tree/main/nrg-action/examples) directory of this repository.', ru:'Рецепты «только валидация» и «авто-коммит через PR» лежат в каталоге [`nrg-action/examples`](https://github.com/nanolaba/readme-generator/tree/main/nrg-action/examples) этого репозитория.'}

#### ${en:'Multi-file projects', ru:'Проекты с несколькими шаблонами'}

${en:'Pass a multi-line list to the `files:` input (one path per line). All files are processed in a single action invocation; the jar is downloaded only once. `file` and `files` are mutually exclusive — set exactly one.', ru:'Передайте многострочный список в параметр `files:` (по одному пути на строку). Все файлы обрабатываются за один вызов action-а, jar скачивается ровно один раз. `file` и `files` не совместимы — задавайте только один из них.'}

```yaml
- uses: nanolaba/nrg-action@v1
  with:
    files: |
      README.src.md
      docs/CONTRIBUTING.src.md
```

#### ${en:'Skipping the built-in setup-java', ru:'Отключение встроенного setup-java'}

${en:'If the surrounding workflow already installs Java (for example, for a Maven build), opt out of the built-in `actions/setup-java@v4` step. Composite-action inputs are strings, so use the quoted `"false"`, not the YAML boolean:', ru:'Если в окружающем workflow Java уже устанавливается (например, для Maven-сборки), встроенный шаг `actions/setup-java@v4` можно отключить. Параметры composite action-а — строки, поэтому используйте `"false"` в кавычках, а не YAML-литерал:'}

```yaml
- uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: '21'
- uses: nanolaba/nrg-action@v1
  with:
    file: README.src.md
    setup-java: 'false'
```

#### ${en:'Pinning the action version', ru:'Закрепление версии action'}

${en:'Use `@v1` for auto-updates within the v1 major (recommended), `@v1.0` to lock to a single minor, or `@<full-sha>` to pin a specific commit (most secure; required by some supply-chain policies).', ru:'`@v1` — автообновления в пределах major-версии v1 (рекомендуется); `@v1.0` — жёсткая фиксация minor-версии; `@<full-sha>` — привязка к конкретному коммиту (наиболее безопасный вариант; требуется некоторыми политиками безопасности цепочки поставок).'}

#### ${en:'Troubleshooting', ru:'Решение проблем'}

${en:'Most CI failures fall into three buckets: download issues (verify the `nrg-version` exists on the [Releases](https://github.com/nanolaba/readme-generator/releases) page), zip-layout regressions (file an issue if `nrg.jar not found inside …` appears), and platform quirks. The most common quirk is line-ending drift on `windows-latest` — `mode: check` reports diffs that do not appear locally.', ru:'Большинство сбоев в CI делятся на три группы: проблемы скачивания (проверьте, что `nrg-version` есть на странице [Releases](https://github.com/nanolaba/readme-generator/releases)), изменения структуры zip-архива (заведите issue, если появляется `nrg.jar not found inside …`), и платформенные особенности. Самая частая особенность — расхождение переводов строк на `windows-latest`: `mode: check` показывает diff, которого нет локально.'}

- ${en:'**Line-ending drift on Windows runners** — add a `.gitattributes` line `* text=auto eol=lf` and re-commit the regenerated files.', ru:'**Расхождение переводов строк на Windows-раннерах** — добавьте в `.gitattributes` строку `* text=auto eol=lf` и закоммитьте сгенерированные файлы заново.'}
- ${en:'**Windows: `unzip: command not found`** — git-bash on `windows-latest` normally ships `unzip`; on rare images install it via `choco install unzip` in a preceding step.', ru:'**Windows: `unzip: command not found`** — git-bash на `windows-latest` обычно содержит `unzip`; в редких образах добавьте предыдущим шагом `choco install unzip`.'}

${en:'The action is published as a standalone repository [`nanolaba/nrg-action`](https://github.com/nanolaba/nrg-action) (tags `v1.0` and rolling `v1`) — that is the address consumers reference via `uses:`. This `nrg-action/` subdirectory is the dev workspace where the action and its regression tests live; releases are mirrored to the standalone repo. A GitHub Marketplace listing is the next step.', ru:'Action опубликован как отдельный репозиторий [`nanolaba/nrg-action`](https://github.com/nanolaba/nrg-action) (теги `v1.0` и rolling `v1`) — именно по этому адресу пользователи подключают его через `uses:`. Подкаталог `nrg-action/` — это dev-воркспейс, где живут исходники и регрессионные тесты action; релизы зеркалируются в отдельный репозиторий. Публикация в GitHub Marketplace — следующий шаг.'}
