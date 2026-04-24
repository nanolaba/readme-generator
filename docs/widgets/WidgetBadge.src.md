#### ${en:'Widget', ru:'Виджет'} 'badge'

This component generates Markdown image links for shields.io badges<!--en-->
of common project-status flavors (Maven Central version, license,<!--en-->
GitHub release / stars / workflow) and a free-form `custom` variant.<!--en-->
Этот компонент формирует markdown-ссылки с картинкой для<!--ru-->
shields.io-бейджей типовых назначений (Maven Central, лицензия,<!--ru-->
GitHub релиз / звёзды / workflow) и произвольного `custom`.<!--ru-->

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th><th>${en:'Result', ru:'Результат'}</th></tr>
<tr><td>

```markdown
\${widget:badge(type = 'maven-central', coordinates = 'com.nanolaba:readme-generator')}
```

</td><td>

```markdown
${widget:badge(type = 'maven-central', coordinates = 'com.nanolaba:readme-generator')}
```

</td></tr>
</table>

${en:'Supported types and their parameters', ru:'Поддерживаемые типы и их параметры'}:

|       type        | ${en:'Required parameters', ru:'Обязательные параметры'}                                                                                                                                                                                           | ${en:'Optional parameters', ru:'Необязательные параметры'}                                                                                                                                                                                                        |
|:-----------------:|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|  `maven-central`  | `coordinates` — ${en:'Maven coordinates `groupId:artifactId`.', ru:'координаты Maven `groupId:artifactId`.'}                                                                                                                                       | —                                                                                                                                                                                                                                                                 |
|     `license`     | `value` — ${en:'license identifier (e.g. `Apache-2.0`).', ru:'идентификатор лицензии (например, `Apache-2.0`).'}                                                                                                                                   | `url` — ${en:'link target; omitted → non-clickable badge.', ru:'целевая ссылка; без неё бейдж не кликабелен.'}                                                                                                                                                    |
| `github-release`  | `repo` — ${en:'repository `owner/name`.', ru:'репозиторий `owner/name`.'}                                                                                                                                                                          | —                                                                                                                                                                                                                                                                 |
|  `github-stars`   | `repo` — ${en:'repository `owner/name`.', ru:'репозиторий `owner/name`.'}                                                                                                                                                                          | —                                                                                                                                                                                                                                                                 |
| `github-workflow` | `repo` — ${en:'repository `owner/name`; ', ru:'репозиторий `owner/name`; '} `workflow` — ${en:'workflow filename (e.g. `ci.yml`).', ru:'имя файла workflow (например, `ci.yml`).'}                                                                 | `name` — ${en:'alt text; defaults to the workflow filename without extension. ', ru:'alt-текст; по умолчанию — имя файла без расширения. '} `branch` — ${en:'filter by branch, appended as `?branch=...`.', ru:'фильтр по ветке, добавляется как `?branch=...`.'} |
|     `custom`      | `label` — ${en:'left side of the badge; ', ru:'левая часть бейджа; '} `message` — ${en:'right side of the badge; ', ru:'правая часть бейджа; '} `color` — ${en:'shields.io color keyword or hex.', ru:'цвет (ключевое слово shields.io или hex).'} | `url` — ${en:'link target; omitted → non-clickable badge.', ru:'целевая ссылка; без неё бейдж не кликабелен.'}                                                                                                                                                    |

Unknown `type` values and missing required parameters log an error<!--en-->
and produce no output.<!--en-->
Неизвестные значения `type` и отсутствие обязательных параметров<!--ru-->
приводят к ошибке в логе и пустому выводу.<!--ru-->

---

