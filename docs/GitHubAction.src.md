### ${en:'Use as a GitHub Action', ru:'Использование в качестве GitHub Action'}

NRG ships as a composite GitHub Action so any repository can regenerate<!--en-->
multi-language README files in CI without installing Maven or Java<!--en-->
locally. The action sets up Java (optional), downloads the requested NRG<!--en-->
release zip, extracts `nrg.jar`, and invokes it against the templates<!--en-->
you list — turning README maintenance into a one-step workflow.<!--en-->
NRG поставляется в виде composite GitHub Action, поэтому любой репозиторий<!--ru-->
может перегенерировать мультиязычные README в CI без локальной установки<!--ru-->
Maven или Java. Action настраивает Java (опционально), скачивает<!--ru-->
запрошенный релизный zip, извлекает `nrg.jar` и запускает его на указанных<!--ru-->
шаблонах — превращая поддержку README в одношаговый workflow.<!--ru-->

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
| `file` | ${en:'Path to the .src.md template (relative to working-directory). Use `files` for multiple.', ru:'Путь к шаблону .src.md (относительно working-directory). Для нескольких файлов используйте `files`.'} | — |
| `files` | ${en:'Multi-line list of templates (one per line). Mutually exclusive with `file`.', ru:'Многострочный список шаблонов (по одному в строке). Несовместим с `file`.'} | — |
| `charset` | ${en:'Source file encoding.', ru:'Кодировка исходных файлов.'} | `UTF-8` |
| `mode` | ${en:'Operation mode: generate, check, or validate.', ru:'Режим работы: generate, check или validate.'} | `generate` |
| `nrg-version` | ${en:'NRG release tag (e.g. v1.0) or `latest`.', ru:'Тег релиза NRG (например, v1.0) или `latest`.'} | `latest` |
| `java-version` | ${en:'JDK version for actions/setup-java. Ignored when setup-java=false.', ru:'Версия JDK для actions/setup-java. Игнорируется при setup-java=false.'} | `17` |
| `java-distribution` | ${en:'JDK distribution for actions/setup-java.', ru:'Дистрибутив JDK для actions/setup-java.'} | `temurin` |
| `setup-java` | ${en:'Whether the action installs Java itself. Set to "false" if Java is set up earlier.', ru:'Устанавливать ли Java самим action. Передайте "false", если Java уже установлена.'} | `true` |
| `log-level` | ${en:'NRG log verbosity: trace, debug, info, warn, or error.', ru:'Уровень логирования NRG: trace, debug, info, warn или error.'} | `info` |
| `working-directory` | ${en:'Directory in which NRG runs.', ru:'Каталог, в котором запускается NRG.'} | `.` |

${en:'`mode` semantics: `generate` writes files to disk; `check` is a read-only verification that exits non-zero with a unified diff if disk content drifts from what NRG would generate; `validate` scans templates for authoring mistakes (unknown widgets, undeclared language markers, missing imports, unbalanced ignore-blocks) without writing files.', ru:'Семантика `mode`: `generate` записывает файлы на диск; `check` — это проверка только на чтение, завершающаяся с ненулевым кодом и unified diff при расхождении файлов на диске с тем, что сгенерировал бы NRG; `validate` сканирует шаблоны на типичные ошибки авторов (неизвестные виджеты, неучтённые языковые маркеры, отсутствующие импорты, несбалансированные ignore-блоки), не создавая файлов.'}

#### ${en:'Outputs', ru:'Выходные значения'}

| ${en:'Name', ru:'Имя'} | ${en:'Description', ru:'Описание'} |
|---|---|
| `version` | ${en:'Resolved NRG version (e.g. v1.0). Useful when nrg-version=latest.', ru:'Разрешённая версия NRG (например, v1.0). Полезно при nrg-version=latest.'} |
| `changed-files` | ${en:'Newline-separated list of files written or modified by NRG.', ru:'Список файлов, созданных или изменённых NRG (по одному на строку).'} |

#### ${en:'Examples', ru:'Примеры'}

##### ${en:'Basic generate', ru:'Базовая генерация'}

${en:'Regenerate the README on every push to `main`:', ru:'Перегенерация README при каждом push в `main`:'}

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

##### ${en:'Drift check on PR', ru:'Проверка расхождений на PR'}

${en:'Fail the build when a contributor edits `README.md` directly instead of regenerating from `README.src.md`:', ru:'Завершить сборку с ошибкой, если автор изменил `README.md` напрямую, не перегенерировав его из `README.src.md`:'}

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

${en:'Validate-only and auto-commit (open a pull request via `peter-evans/create-pull-request`) recipes are available in the action repository — see [examples](https://github.com/nanolaba/nrg-action/tree/main/examples). Note: that link is forward-looking until the standalone repo is published.', ru:'Рецепты «только валидация» и «авто-commit» (открытие PR через `peter-evans/create-pull-request`) доступны в репозитории action — см. [examples](https://github.com/nanolaba/nrg-action/tree/main/examples). Примечание: ссылка действительна с момента публикации отдельного репозитория.'}

#### ${en:'Multi-file projects', ru:'Проекты с несколькими шаблонами'}

${en:'Pass a heredoc list to the `files:` input (one path per line). All files are processed in a single action invocation; the jar is downloaded once. `file` and `files` are mutually exclusive — set exactly one.', ru:'Передайте heredoc-список в параметр `files:` (по одному пути в строке). Все файлы обрабатываются за один вызов action; jar скачивается один раз. `file` и `files` несовместимы — задавайте только один из них.'}

```yaml
- uses: nanolaba/nrg-action@v1
  with:
    files: |
      README.src.md
      docs/CONTRIBUTING.src.md
```

#### ${en:'Skipping the built-in setup-java', ru:'Отключение встроенного setup-java'}

${en:'If the surrounding workflow already installs Java (for example, for a Maven build), you can opt out of the built-in `actions/setup-java@v4` step. Composite-action inputs are strings, so use the quoted `"false"`, not the YAML boolean:', ru:'Если в workflow уже установлена Java (например, для сборки Maven), можно отключить встроенный шаг `actions/setup-java@v4`. Параметры composite action — строки, поэтому используйте `"false"` в кавычках, а не YAML-булево:'}

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

${en:'Use `@v1` for auto-updates within the v1 major (recommended), `@v1.0` to lock to a single minor, or `@<full-sha>` to pin a specific commit (most secure; required by some supply-chain policies).', ru:'Используйте `@v1` для автообновления в пределах major-версии v1 (рекомендуется), `@v1.0` для жёсткой фиксации minor-версии или `@<full-sha>` для пина конкретного коммита (наиболее безопасно; требуется некоторыми политиками supply chain).'}

#### ${en:'Troubleshooting', ru:'Решение проблем'}

${en:'Most CI failures fall into three buckets: download issues (verify the `nrg-version` exists at the [Releases](https://github.com/nanolaba/readme-generator/releases) page), zip-layout regressions (file an issue if `nrg.jar not found inside …` appears), and platform quirks. The most common quirk is line-ending drift on `windows-latest` — `mode: check` reports diffs that do not exist locally.', ru:'Большинство ошибок в CI делятся на три группы: проблемы скачивания (убедитесь, что `nrg-version` есть на странице [Releases](https://github.com/nanolaba/readme-generator/releases)), регрессии раскладки zip (откройте issue, если появилось `nrg.jar not found inside …`), и платформенные особенности. Самая частая особенность — расхождение перевода строк на `windows-latest`: `mode: check` показывает diff, которого нет локально.'}

- ${en:'**Line-ending drift on Windows runners** — add a `.gitattributes` with `* text=auto eol=lf` and re-commit the regenerated files.', ru:'**Расхождение перевода строк на Windows-runners** — добавьте `.gitattributes` со строкой `* text=auto eol=lf` и перекоммитьте сгенерированные файлы.'}
- ${en:'**Windows: `unzip: command not found`** — git-bash on `windows-latest` normally ships `unzip`; in rare images install it via `choco install unzip` in a preceding step.', ru:'**Windows: `unzip: command not found`** — git-bash на `windows-latest` обычно содержит `unzip`; в редких образах установите его через `choco install unzip` в предыдущем шаге.'}

${en:'See the standalone repo for the full Marketplace listing.', ru:'Полный листинг для Marketplace — в отдельном репозитории.'} ${en:'Source: [`nanolaba/nrg-action`](https://github.com/nanolaba/nrg-action) (forward-looking).', ru:'Источник: [`nanolaba/nrg-action`](https://github.com/nanolaba/nrg-action) (планируемый репозиторий).'}
