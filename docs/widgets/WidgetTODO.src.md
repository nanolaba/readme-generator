#### ${en:'Widget', ru:'Виджет'} 'todo'

This component inserts prominently highlighted text into the document<!--en-->
to mark a fragment that is not finished yet.<!--en-->
Этот компонент вставляет в документ ярко выделенный текст —<!--ru-->
пометку о том, что фрагмент ещё не готов.<!--ru-->


<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th></tr>
<tr><td>

```markdown
\${widget:todo(text="\${en:'Example message', ru:'Пример сообщения'}")}
```

</td></tr>
<tr><th>${en:'Result', ru:'Результат'}</th></tr>
<tr><td>

```markdown
${widget:todo(text="${en:'Example message', ru:'Пример сообщения'}")}
```

</td></tr>
<tr><th>${en:'Displayed result', ru:'Отображаемый результат'}</th></tr>
<tr><td>
${widget:todo(text="${en:'Example message', ru:'Пример сообщения'}")}
</td></tr>
</table>


${en:'Widget parameters', ru:'Свойства виджета'}:

<!-- nrg:begin:reference --><!--nrg.ignore-->
| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}              | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|-------------------------------------------------|:-------------------------------------------------:|
|              text               | ${en:'Displayed text', ru:'Отображаемый текст'} |                 `Not done yet...`                 |
<!-- nrg:end:reference --><!--nrg.ignore-->

