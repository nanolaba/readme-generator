<!--@nrg.languages=en,ru-->
<!--@nrg.defaultLanguage=en-->

<!--@name=**Nanolaba Readme Generator (NRG)**-->
<!--@stableVersion=0.2-->
<!--@devVersion=0.3-SNAPSHOT-->

${widget:languages}

# Nanolaba Readme Generator (NRG)

${name} — инструмент автоматической генерации Markdown, работающий с шаблонами-прототипами и поддерживающий <!--ru-->
многоязычность, импорт файлов, переменные и кастомные виджеты для динамического наполнения.<!--ru-->
${name} — is an automated Markdown generation tool that processes prototype templates with <!--en-->
multilingual support, file imports, variables, and custom widgets for dynamic content.<!--en-->

${widget:import(path='docs/Overview.src.md')}

${widget:tableOfContents(title = "${en:'Table of contents', ru:'Содержание'}", ordered = "true")}

${widget:import(path='docs/QuickStart.src.md')}
${widget:import(path='docs/Launching.src.md')}
${widget:import(path='docs/TemplateSyntax.src.md')}
${widget:import(path='docs/Widgets.src.md')}
${widget:import(path='docs/Feedback.src.md')}

---
*${en:'Last updated:', ru:'Дата последнего обновления:'} ${widget:date(pattern= 'dd.MM.yyyy')}*