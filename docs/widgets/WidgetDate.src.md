#### ${en:'Widget', ru:'Виджет'} 'date'

This component allows you to insert the current date into a document.<!--en-->
Этот компонент позволяет вставить в документ текущую дату.<!--ru-->

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th><th>${en:'Result', ru:'Результат'}</th></tr>
<tr><td>

```markdown
Last updated: \${widget:date}
```

</td><td>

```markdown
Last updated: ${widget:date}
```

</td></tr>
<tr><td>

```markdown
\${widget:date(pattern = 'dd.MM.yyyy')}
```

</td><td>

```markdown
${widget:date(pattern= 'dd.MM.yyyy')}
```

</td></tr>
</table>

${en:'Widget parameters', ru:'Свойства виджета'}:

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                              | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|---------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------:|
|             pattern             | ${en:'Pattern according to which the date will be formatted', ru:'Шаблон, в соответствии с котором будет отформатирована дата'} |               `dd.MM.yyyy HH:mm:ss`               |

You can read more about date pattern syntax in the<!--en-->
[Java documentation](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html).<!--en-->
Подробнее о синтаксисе шаблона даты можно прочитать в <!--ru-->
[документации языка Java](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html).<!--ru-->

---

