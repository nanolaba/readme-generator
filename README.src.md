<!--@nrg.languages=en,ru-->
<!--@nrg.defaultLanguage=en-->

<!--@nrg.pom.path=pom.xml-->

<!--@name=**Nanolaba Readme Generator (NRG)**-->
<!--@stableVersion=${pom.properties.stableVersion}-->
<!--@devVersion=${pom.version}-->

${widget:languages}

# Nanolaba Readme Generator (NRG)${en:' - Automated Markdown Documentation Tool', ru:''}

${widget:badge(type='github-workflow', repo='nanolaba/readme-generator', workflow='ci.yml', name='CI')}
${widget:badge(type='maven-central', coordinates='com.nanolaba:readme-generator')}
${widget:badge(type='license', value='Apache 2.0', url='https://www.apache.org/licenses/LICENSE-2.0')}
${widget:badge(type='custom', label='Java', message='8+', color='orange')}

${name} — это инструмент с открытым исходным кодом на Java для автоматизации создания Markdown-документации <!--ru-->
с поддержкой нескольких языков, динамическими переменными и пользовательскими виджетами.<!--ru-->
${name} — is an open-source Java tool for automating Markdown documentation with multi-language <!--en-->
support, dynamic variables, and custom widgets.<!--en-->

${widget:import(path='docs/Overview.src.md')}

${widget:tableOfContents(title = "${en:'Table of contents', ru:'Содержание'}", ordered = "true")}

${widget:import(path='docs/QuickStart.src.md')}
${widget:import(path='docs/Launching.src.md')}
${widget:import(path='docs/TemplateSyntax.src.md')}
${widget:import(path='docs/Widgets.src.md')}
${widget:import(path='docs/Advanced.src.md')}
${widget:import(path='docs/RelatedProjects.src.md')}
${widget:import(path='docs/Changelog.src.md')}
${widget:import(path='docs/Feedback.src.md')}

---
*${en:'Last updated:', ru:'Дата последнего обновления:'} ${widget:date(pattern= 'dd.MM.yyyy')}*