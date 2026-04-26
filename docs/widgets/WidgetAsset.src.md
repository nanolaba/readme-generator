#### ${en:'Widget', ru:'Виджет'} 'asset'

This component resolves to a per-language asset path (image, file, URL, etc.),<!--en-->
falling back to a shared default when no language-specific value is configured.<!--en-->
Avoids the noisy `${en:'...', ru:'...'}` form when the same asset is referenced from many places.<!--en-->
Этот компонент возвращает путь к ресурсу (изображению, файлу, URL и т. п.) для конкретного языка,<!--ru-->
а при отсутствии локализованного значения подставляет общее значение по умолчанию.<!--ru-->
Позволяет избежать многословной конструкции `${en:'...', ru:'...'}`, когда один ресурс упоминается в нескольких местах.<!--ru-->

Configure asset values via standard NRG property markers:<!--en-->
`asset.<name>.<lang>` defines a value for a specific language;<!--en-->
`asset.<name>` (without a language suffix) is used as a fallback when no per-language override exists.<!--en-->
Значения задаются обычными свойствами NRG:<!--ru-->
`asset.<name>.<lang>` определяет значение для конкретного языка;<!--ru-->
`asset.<name>` (без суффикса языка) используется как значение по умолчанию, если для языка нет переопределения.<!--ru-->

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th></tr>
<tr><td>

```markdown
<!--\@asset.screenshot.en=./public/show-en.png-->
<!--\@asset.screenshot.zh=./public/show-zh.png-->
<!--\@asset.screenshot=./public/show.png-->

<img src="\${widget:asset(name='screenshot')}" alt="screenshot" />
```

</td></tr>
<tr><th>${en:'Result for en', ru:'Результат для en'}</th></tr>
<tr><td>

```markdown
<img src="./public/show-en.png" alt="screenshot" />
```

</td></tr>
<tr><th>${en:'Result for zh', ru:'Результат для zh'}</th></tr>
<tr><td>

```markdown
<img src="./public/show-zh.png" alt="screenshot" />
```

</td></tr>
<tr><th>${en:'Result for any other language', ru:'Результат для любого другого языка'}</th></tr>
<tr><td>

```markdown
<img src="./public/show.png" alt="screenshot" />
```

</td></tr>
</table>

${en:'Widget parameters', ru:'Свойства виджета'}:

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                                       | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------:|
|              name               | ${en:'Asset identifier (matches the suffix in `asset.<name>` and `asset.<name>.<lang>`)', ru:'Идентификатор ресурса (соответствует суффиксу в `asset.<name>` и `asset.<name>.<lang>`)'} |       —       |

If neither `asset.<name>.<lang>` nor `asset.<name>` is defined, the widget logs an error and emits an empty string.<!--en-->
Если не определены ни `asset.<name>.<lang>`, ни `asset.<name>`, виджет регистрирует ошибку и выдает пустую строку.<!--ru-->

---

