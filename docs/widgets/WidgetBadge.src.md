#### ${en:'Widget', ru:'Виджет'} 'badge'

This component generates Markdown image links for shields.io badges<!--en-->
of common project-status flavors (Maven Central version, license,<!--en-->
GitHub release / stars / workflow) and a free-form `custom` variant.<!--en-->
Этот компонент формирует markdown-ссылки с картинкой для<!--ru-->
shields.io-бейджей типовых видов (Maven Central, лицензия,<!--ru-->
GitHub релиз / звёзды / workflow) и произвольного бейджа `custom`.<!--ru-->

${en:'Usage example', ru:'Пример использования'}:

```markdown
\${widget:badge(type = 'maven-central', coordinates = 'com.nanolaba:readme-generator')}
```

${en:'Result', ru:'Результат'}:

${widget:badge(type = 'maven-central', coordinates = 'com.nanolaba:readme-generator')}

${en:'Supported types and their parameters', ru:'Поддерживаемые типы и их параметры'}:

<!-- nrg:begin:reference --><!--nrg.ignore-->
|       type        | ${en:'Required parameters', ru:'Обязательные параметры'}                                                                                                                                                                                           | ${en:'Optional parameters', ru:'Необязательные параметры'}                                                                                                                                                                                                        |
|:-----------------:|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|  `maven-central`  | `coordinates` — ${en:'Maven coordinates `groupId:artifactId`.', ru:'координаты Maven `groupId:artifactId`.'}                                                                                                                                       | `alt` — ${en:'override the default alt-text `Maven Central`.', ru:'переопределить alt-текст по умолчанию `Maven Central`.'}                                                                                                                                      |
|     `license`     | `value` — ${en:'license identifier (e.g. `Apache-2.0`).', ru:'идентификатор лицензии (например, `Apache-2.0`).'}                                                                                                                                   | `url` — ${en:'link target; without it the badge is not clickable. ', ru:'целевая ссылка; без неё бейдж не кликабелен. '} `alt` — ${en:'override the default alt-text `License: <value>`.', ru:'переопределить alt-текст по умолчанию `License: <value>`.'}      |
| `github-release`  | `repo` — ${en:'repository `owner/name`.', ru:'репозиторий `owner/name`.'}                                                                                                                                                                          | `alt` — ${en:'override the default alt-text `GitHub release`.', ru:'переопределить alt-текст по умолчанию `GitHub release`.'}                                                                                                                                    |
|  `github-stars`   | `repo` — ${en:'repository `owner/name`.', ru:'репозиторий `owner/name`.'}                                                                                                                                                                          | `alt` — ${en:'override the default alt-text `GitHub stars`.', ru:'переопределить alt-текст по умолчанию `GitHub stars`.'}                                                                                                                                        |
| `github-workflow` | `repo` — ${en:'repository `owner/name`; ', ru:'репозиторий `owner/name`; '} `workflow` — ${en:'workflow filename (e.g. `ci.yml`).', ru:'имя файла workflow (например, `ci.yml`).'}                                                                 | `name` — ${en:'alt text; defaults to the workflow filename without extension. ', ru:'alt-текст; по умолчанию — имя файла без расширения. '} `branch` — ${en:'filter by branch, appended as `?branch=...`. ', ru:'фильтр по ветке, добавляется как `?branch=...`. '} `alt` — ${en:'override the alt-text (wins over `name`).', ru:'переопределить alt-текст (приоритетнее `name`).'} |
|     `custom`      | `label` — ${en:'left side of the badge; ', ru:'левая часть бейджа; '} `message` — ${en:'right side of the badge; ', ru:'правая часть бейджа; '} `color` — ${en:'shields.io color keyword or hex.', ru:'цвет (ключевое слово shields.io или hex).'} | `url` — ${en:'link target; without it the badge is not clickable. ', ru:'целевая ссылка; без неё бейдж не кликабелен. '} `alt` — ${en:'override the default alt-text (defaults to `label`).', ru:'переопределить alt-текст (по умолчанию равен `label`).'}      |
<!-- nrg:end:reference --><!--nrg.ignore-->

The optional `alt` parameter sets the Markdown image alt-text without<!--en-->
changing the visible badge label rendered by shields.io. Search engines<!--en-->
and screen readers see phrases like<!--en-->
`NRG continuous integration build status` instead of bare labels like<!--en-->
`CI`. Empty `alt=''` falls back to the type's default.<!--en-->
Необязательный параметр `alt` задаёт alt-текст markdown-изображения,<!--ru-->
не затрагивая видимую надпись на бейдже от shields.io. Поисковики<!--ru-->
и скринридеры увидят фразу вида<!--ru-->
`NRG continuous integration build status` вместо короткой метки `CI`.<!--ru-->
С пустым `alt=''` используется значение по умолчанию.<!--ru-->

Unknown `type` values and missing required parameters log an error<!--en-->
and produce no output.<!--en-->
Неизвестные значения `type` и отсутствие обязательных параметров<!--ru-->
приводят к ошибке в логе и пустому выводу.<!--ru-->

---

