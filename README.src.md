<!--@nrg.languages=en,ru-->
<!--@nrg.defaultLanguage=en-->

<!--@name=**Nanolaba Readme Generator (NRG)**-->
<!--@stableVersion=0.3-->
<!--@devVersion=0.4-SNAPSHOT-->

${widget:languages}

# Nanolaba Readme Generator (NRG)${en:' - Automated Markdown Documentation Tool', ru:''}

[![CI](https://github.com/nanolaba/readme-generator/actions/workflows/ci.yml/badge.svg)](https://github.com/nanolaba/readme-generator/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.nanolaba/readme-generator?label=Maven%20Central)](https://central.sonatype.com/artifact/com.nanolaba/readme-generator)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
![Java](https://img.shields.io/badge/Java-8%2B-orange)

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
${widget:import(path='docs/RelatedProjects.src.md')}
${widget:import(path='docs/Changelog.src.md')}
${widget:import(path='docs/Feedback.src.md')}

---
*${en:'Last updated:', ru:'Дата последнего обновления:'} ${widget:date(pattern= 'dd.MM.yyyy')}*