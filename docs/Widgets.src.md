### ${en:'Widgets', ru:'Виджеты'}

Widgets allow you to insert programmatically generated text into a document.<!--en-->
If you are using ${name} as a Java library, you can write your own widget.<!--en-->
How to do this is explained in the [Advanced Features](#advanced-features) section.<!--en-->
Виджеты позволяют вставить в документ программно сгенерированный текст.<!--ru-->
Если вы используете ${name} как java-библиотеку, то вы можете написать свой собственный виджет.<!--ru-->
Как это сделать рассказано в разделе [Расширенные возможности](#расширенные-возможности).<!--ru-->

Widget parameters are written as `name='value'` pairs separated by commas; a value is quoted with either `'` or `"`. Everything between the quotes belongs to the value, including parentheses and the other quote character: `cond='endsWith(\${version}, -SNAPSHOT)'`, `text="it's done (really)"`.<!--en-->
Параметры виджета записываются парами `name='value'` через запятую; значение берётся в кавычки `'` или `"`. Всё, что стоит между кавычками, относится к значению — в том числе круглые скобки и кавычка другого вида: `cond='endsWith(\${version}, -SNAPSHOT)'`, `text="it's done (really)"`.<!--ru-->

${widget:import(path='widgets/WidgetLanguages.src.md')}
${widget:import(path='widgets/WidgetImport.src.md')}
${widget:import(path='widgets/WidgetTOC.src.md')}
${widget:import(path='widgets/WidgetDate.src.md')}
${widget:import(path='widgets/WidgetTODO.src.md')}
${widget:import(path='widgets/WidgetAlert.src.md')}
${widget:import(path='widgets/WidgetBadge.src.md')}
${widget:import(path='widgets/WidgetMath.src.md')}
${widget:import(path='widgets/WidgetExec.src.md')}
${widget:import(path='widgets/WidgetIf.src.md')}
${widget:import(path='widgets/WidgetFileTree.src.md')}
${widget:import(path='widgets/WidgetDetails.src.md')}