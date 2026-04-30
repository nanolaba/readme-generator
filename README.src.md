<!--@nrg.languages=en,ru-->
<!--@nrg.defaultLanguage=en-->

<!--@nrg.pom.path=pom.xml-->

<!--@name=**Nanolaba Readme Generator (NRG)**-->
<!--@stableVersion=${pom.properties.stableVersion}-->
<!--@devVersion=${pom.version}-->

${widget:languages}

# Nanolaba Readme Generator (NRG)${en:' - Automated Markdown Documentation Tool', ru:''}

${widget:badge(type='github-workflow', repo='nanolaba/readme-generator', workflow='ci.yml', name='CI', alt='NRG continuous integration build status')}
${widget:badge(type='maven-central', coordinates='com.nanolaba:readme-generator', alt='NRG release on Maven Central')}
${widget:badge(type='license', value='Apache 2.0', url='https://www.apache.org/licenses/LICENSE-2.0', alt='NRG is open source under the Apache 2.0 license')}
${widget:badge(type='custom', label='Java', message='8+', color='orange', alt='Built with Java 8+')}

![${en:'NRG demo: generating README.md and README.ru.md from a single .src.md template', ru:'Демо NRG: генерация README.md и README.ru.md из одного .src.md-шаблона'}](assets/demo.gif)

**NRG** is a **README generator** and **Markdown template engine** that builds **multi-language** README files from a single `.src.md` source. Open-source Java 8+, ships as a [CLI](#using-the-command-line-interface), a [Maven plugin](#use-as-maven-plugin), a [GitHub Action](#use-as-a-github-action), and a [Java library](#use-as-a-java-library).<!--en-->
**NRG** — это **генератор README** и **шаблонизатор Markdown**, который собирает **многоязычные** README-файлы из одного `.src.md`-исходника. Open-source Java 8+, поставляется как [CLI](#запуск-из-командной-строки), [Maven-плагин](#использование-как-плагина-для-maven), [GitHub Action](#использование-в-качестве-github-action) и [Java-библиотека](#использование-в-качестве-java-библиотеки).<!--ru-->

${widget:import(path='docs/Overview.src.md')}

${widget:tableOfContents(title = "${en:'Table of contents', ru:'Содержание'}", ordered = "true", numbering-style = "dotted")}

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