#### ${en:'Widget', ru:'Виджет'} 'badge'

This component generates Markdown image links for shields.io badges<!--en-->
of common project-status flavors (Maven Central version, license,<!--en-->
GitHub release/stars) and a free-form `custom` variant.<!--en-->
Этот компонент формирует markdown-ссылки с картинкой для<!--ru-->
shields.io-бейджей типовых назначений (Maven Central, лицензия,<!--ru-->
GitHub релиз/звёзды) и произвольного `custom`.<!--ru-->

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
<tr><td>

```markdown
\${widget:badge(type = 'license', value = 'Apache-2.0', url = 'https://www.apache.org/licenses/LICENSE-2.0')}
```

</td><td>

```markdown
${widget:badge(type = 'license', value = 'Apache-2.0', url = 'https://www.apache.org/licenses/LICENSE-2.0')}
```

</td></tr>
<tr><td>

```markdown
\${widget:badge(type = 'github-release', repo = 'nanolaba/readme-generator')}
```

</td><td>

```markdown
${widget:badge(type = 'github-release', repo = 'nanolaba/readme-generator')}
```

</td></tr>
<tr><td>

```markdown
\${widget:badge(type = 'github-stars', repo = 'nanolaba/readme-generator')}
```

</td><td>

```markdown
${widget:badge(type = 'github-stars', repo = 'nanolaba/readme-generator')}
```

</td></tr>
<tr><td>

```markdown
\${widget:badge(type = 'custom', label = 'docs', message = 'up to date', color = 'brightgreen')}
```

</td><td>

```markdown
${widget:badge(type = 'custom', label = 'docs', message = 'up to date', color = 'brightgreen')}
```

</td></tr>
<tr><td>

```markdown
\${widget:badge(type = 'github-workflow', repo = 'nanolaba/readme-generator', workflow = 'ci.yml', name = 'CI')}
```

</td><td>

```markdown
${widget:badge(type = 'github-workflow', repo = 'nanolaba/readme-generator', workflow = 'ci.yml', name = 'CI')}
```

</td></tr>
</table>

${en:'Widget parameters', ru:'Свойства виджета'}:

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                                                                                                                   |     ${en:'Required for', ru:'Обязательно для'}      |
|:-------------------------------:|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:---------------------------------------------------:|
|              type               | ${en:'One of: `maven-central`, `license`, `github-release`, `github-stars`, `github-workflow`, `custom`.', ru:'Одно из: `maven-central`, `license`, `github-release`, `github-stars`, `github-workflow`, `custom`.'} |                ${en:'all', ru:'все'}                |
|           coordinates           | ${en:'Maven coordinates `groupId:artifactId`.', ru:'Координаты Maven `groupId:artifactId`.'}                                                                                                                         |                   `maven-central`                   |
|              value              | ${en:'License identifier (e.g. `Apache-2.0`, `MIT`).', ru:'Идентификатор лицензии (например, `Apache-2.0`, `MIT`).'}                                                                                                 |                      `license`                      |
|              repo               | ${en:'GitHub repository `owner/name`.', ru:'Репозиторий GitHub `owner/name`.'}                                                                                                                                       | `github-release`, `github-stars`, `github-workflow` |
|            workflow             | ${en:'Workflow filename (e.g. `ci.yml`).', ru:'Имя файла workflow (например, `ci.yml`).'}                                                                                                                            |                  `github-workflow`                  |
|              name               | ${en:'Optional alt text for the workflow badge; defaults to the workflow filename without its extension.', ru:'Необязательный alt-текст для workflow-бейджа; по умолчанию — имя файла без расширения.'}              |                          —                          |
|             branch              | ${en:'Optional branch filter appended as `?branch=...`.', ru:'Необязательный фильтр ветки — добавляется как `?branch=...`.'}                                                                                         |                          —                          |
|              label              | ${en:'Left-side label of the badge.', ru:'Левая подпись бейджа.'}                                                                                                                                                    |                      `custom`                       |
|             message             | ${en:'Right-side text of the badge.', ru:'Правая часть (значение) бейджа.'}                                                                                                                                          |                      `custom`                       |
|              color              | ${en:'Badge color (`brightgreen`, `blue`, hex, …).', ru:'Цвет бейджа (`brightgreen`, `blue`, hex, …).'}                                                                                                              |                      `custom`                       |
|               url               | ${en:'Optional link target. Without it the badge is non-clickable.', ru:'Необязательная ссылка. Без неё бейдж не кликабелен.'}                                                                                       |                          —                          |

Unknown `type` values and missing required parameters log an error<!--en-->
and produce no output.<!--en-->
Неизвестные значения `type` и отсутствие обязательных параметров<!--ru-->
приводят к ошибке в логе и пустому выводу.<!--ru-->

---

