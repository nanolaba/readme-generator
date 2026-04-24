#### ${en:'Widget', ru:'Виджет'} 'alert'

This component renders a GitHub-flavored alert block<!--en-->
(`> [!NOTE]`, `> [!WARNING]`, and so on) so you don't have to<!--en-->
hand-type blockquote syntax in your source templates.<!--en-->
Этот компонент формирует «alert-блок» в стиле GitHub<!--ru-->
(`> [!NOTE]`, `> [!WARNING]` и т. п.), чтобы не приходилось<!--ru-->
вручную писать синтаксис blockquote в исходных шаблонах.<!--ru-->

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th><th>${en:'Result', ru:'Результат'}</th></tr>
<tr><td>

```markdown
\${widget:alert(type = 'note', text = 'Hello')}
```

</td><td>

```markdown
${widget:alert(type = 'note', text = 'Hello')}
```

</td></tr>
<tr><td>

```markdown
\${widget:alert(type = 'warning', text = 'Line 1\nLine 2')}
```

</td><td>

```markdown
${widget:alert(type = 'warning', text = 'Line 1\nLine 2')}
```

</td></tr>
</table>

${en:'Widget parameters', ru:'Свойства виджета'}:

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                                                                                                                                                                                                                                        | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------:|
|              type               | ${en:'Alert kind: `note`, `tip`, `important`, `warning`, or `caution` (case-insensitive). Unknown values log an error and produce empty output.', ru:'Тип блока: `note`, `tip`, `important`, `warning` или `caution` (регистр не важен). Неизвестное значение приводит к ошибке и пустому выводу.'}                                       |                                                   |
|              text               | ${en:'Body of the alert. Use `\\n` to split into multiple quoted lines and `\\\\` for a literal backslash. Combines with `\\${en:"...", ru:"..."}` for language-specific text.', ru:'Текст блока. Используйте `\\n` для перевода строки и `\\\\` для литерального обратного слэша. Совместимо с `\\${en:"...", ru:"..."}` для перевода.'} |                       `''`                        |

---

