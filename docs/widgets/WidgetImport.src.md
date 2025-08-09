#### ${en:'Widget', ru:'Виджет'} 'import'

This component enables text import from another document or template.<!--en-->
Этот компонент позволяет импортировать текст из другого документа или шаблона.<!--ru-->

<table>
<tr><th>${en:'Usage example', ru:'Пример использования'}</th></tr>
<tr><td>

```markdown
\${widget:import(path='path/to/your/file/document.txt')}
\${widget:import(path='path/to/your/file/document.txt', charset='windows-1251')}
\${widget:import(path='path/to/your/file/template.src.md')}
\${widget:import(path='path/to/your/file/template.src.md', run-generator='false')}
```

</td></tr>
<tr></tr>
</table>

${en:'Widget parameters', ru:'Свойства виджета'}:

| ${en:'Name', ru:'Наименование'} | ${en:'Description', ru:'Описание'}                                                                                                                   | ${en:'Default value', ru:'Значение по умолчанию'} |
|:-------------------------------:|------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------:|
|              path               | ${en:'Path to the imported file', ru:'Путь к импортируемому файлу'}                                                                                  |                                                   |
|             charset             | ${en:'File encoding', ru:'Кодировка, в которой написан файл'}                                                                                        |                      `UTF-8`                      |
|          run-generator          | ${en:'Should the system perform text generation when importing template files', ru:'Нужно ли при импорте файла-шаблона произвести генерацию текста'} |                      `true`                       |

При импорте файла шаблона генерация выполняется с использованием переменных, объявленных в родительском файле.<!--ru-->
Это позволяет определять глобальные переменные в корневом файле и повторно <!--ru-->
использовать их во всех импортированных шаблонах.<!--ru-->
When importing a template file, generation is performed using variables declared in the parent file.<!--en-->
This allows defining global variables in the root file and reusing them across all imported templates.<!--en-->

---

