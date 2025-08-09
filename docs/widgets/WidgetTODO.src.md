#### ${en:'Widget', ru:'Виджет'} 'todo'

This component allows you to insert prominently highlighted text into the document,<!--en-->
indicating that work on this fragment has not yet been done.<!--en-->
Этот компонент позволяет вставить в документ ярко выделенный текст, с пометкой<!--ru-->
о том, что работа над данным фрагментом еще не проведена.<!--ru-->


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

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}              | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|-------------------------------------------------|:-------------------------------------------------:|
|              text               | ${en:'Displayed text', ru:'Отображаемый текст'} |                 `Not done yet...`                 |

