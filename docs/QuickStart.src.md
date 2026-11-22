## ${en:'Installation', ru:'Установка'}

NRG needs Java 8 or newer ([download](https://adoptium.net/)). Pick the way that fits where it runs:<!--en-->
NRG нужна Java 8 или новее ([скачать](https://adoptium.net/)). Выберите способ по тому, где NRG будет запускаться:<!--ru-->

| ${en:'Where', ru:'Где'} | ${en:'How', ru:'Как'} | ${en:'Details', ru:'Подробнее'} |
|---|---|---|
| Command line | unzip `readme-generator-${stableVersion}.zip` from [the release page](https://github.com/nanolaba/readme-generator/releases/tag/v${stableVersion}) and run `nrg.sh` / `nrg.bat` | [CLI](#using-the-command-line-interface) |<!--en-->
| Командная строка | распакуйте `readme-generator-${stableVersion}.zip` со [страницы релиза](https://github.com/nanolaba/readme-generator/releases/tag/v${stableVersion}) и запустите `nrg.sh` / `nrg.bat` | [CLI](#запуск-из-командной-строки) |<!--ru-->
| A repository: CI, contributors, agents | `nrg --init-wrapper`, then commit `nrgw`, `nrgw.cmd` and `.nrg/` | [nrgw](#pinned-wrapper-nrgw) |<!--en-->
| Репозиторий: CI, контрибьюторы, агенты | `nrg --init-wrapper`, затем закоммитьте `nrgw`, `nrgw.cmd` и `.nrg/` | [nrgw](#обёртка-nrgw-с-закреплённой-версией) |<!--ru-->
| A Maven build | `com.nanolaba:nrg-maven-plugin:${stableVersion}`, goal `create-files` | [Maven plugin](#use-as-maven-plugin) |<!--en-->
| Сборка Maven | `com.nanolaba:nrg-maven-plugin:${stableVersion}`, цель `create-files` | [Maven-плагин](#использование-как-плагина-для-maven) |<!--ru-->
| GitHub Actions | `uses: nanolaba/nrg-action@v1` | [GitHub Action](#use-as-a-github-action) |<!--en-->
| GitHub Actions | `uses: nanolaba/nrg-action@v1` | [GitHub Action](#использование-в-качестве-github-action) |<!--ru-->
| Java code | `com.nanolaba:readme-generator:${stableVersion}` | [Java library](#use-as-a-java-library) |<!--en-->
| Java-код | `com.nanolaba:readme-generator:${stableVersion}` | [Java-библиотека](#использование-в-качестве-java-библиотеки) |<!--ru-->
| Claude Code | `/plugin marketplace add nanolaba/readme-generator`, then `/plugin install nrg@nanolaba` | [Plugin](#claude-code-plugin-and-the-nrg-skill) |<!--en-->
| Claude Code | `/plugin marketplace add nanolaba/readme-generator`, затем `/plugin install nrg@nanolaba` | [Плагин](#плагин-для-claude-code-и-навык-nrg) |<!--ru-->

## ${en:'Quick start', ru:'Быстрый старт'}

**${en:'Step 1: write the template', ru:'Шаг 1: напишите шаблон'} (`README.src.md`)**

```markdown
<\!--@nrg.languages=en,ru-->

# Hello<\!--en-->
# Привет<\!--ru-->

English text<\!--en-->
Русский текст<\!--ru-->

The same line in both files.
```

Lines tagged `<\!--en-->` go to `README.md`, lines tagged `<\!--ru-->` go to `README.ru.md`, and untagged lines go to both.<!--en-->
Строки с `<\!--en-->` попадут в `README.md`, строки с `<\!--ru-->` — в `README.ru.md`, а строки без маркера — в оба файла.<!--ru-->

**${en:'Step 2: generate the files', ru:'Шаг 2: сгенерируйте файлы'}**

With the CLI:<!--en-->
Из командной строки:<!--ru-->

```bash
nrg -f README.src.md
```

Or in a Maven build (the full configuration is in [Maven plugin](#use-as-maven-plugin)):<!--en-->
Или в сборке Maven (полная конфигурация — в разделе [«Использование как плагина для maven»](#использование-как-плагина-для-maven)):<!--ru-->

```xml
<plugin>
    <groupId>com.nanolaba</groupId>
    <artifactId>nrg-maven-plugin</artifactId>
    <version>${stableVersion}</version>
    <configuration>
        <file><item>README.src.md</item></file>
    </configuration>
    <executions>
        <execution>
            <phase>compile</phase>
            <goals><goal>create-files</goal></goals>
        </execution>
    </executions>
</plugin>
```

Two files appear next to the template:<!--en-->
Рядом с шаблоном появятся два файла:<!--ru-->

<table>
<tr><th><b>README.md</b></th><th><b>README.ru.md</b></th></tr>
<tr><td>

```markdown
# Hello

English text

The same line in both files.
```

</td><td>

```markdown
# Привет

Русский текст

The same line in both files.
```

</td></tr></table>

**${en:'Step 3: check in CI', ru:'Шаг 3: проверка в CI'}**

Commit the template together with the generated files, and add a check to CI that fails when someone edits `README.md` by hand or forgets to regenerate:<!--en-->
Закоммитьте шаблон вместе со сгенерированными файлами и добавьте в CI проверку, которая падает, если кто-то поправил `README.md` руками или забыл перегенерировать:<!--ru-->

```yaml
- uses: nanolaba/nrg-action@v1
  with:
    file: README.src.md
    mode: check
```

Outside GitHub Actions the same check is `nrg --check -f README.src.md`, see [Verifying generated files](#verifying-generated-files-ci-mode).<!--en-->
Вне GitHub Actions та же проверка — `nrg --check -f README.src.md`, см. [«Проверка сгенерированных файлов»](#проверка-сгенерированных-файлов-режим-ci).<!--ru-->

**${en:'Step 4: tell the coding agent', ru:'Шаг 4: расскажите о шаблоне агенту'}**

```bash
nrg --agent-rules --write AGENTS.md
```

This adds a block to `AGENTS.md` that names the generated files, the templates to edit, the languages and the commands to run after a change. Use `CLAUDE.md`, `GEMINI.md` or `.github/copilot-instructions.md` for other agents, see [Agent-driven development](#agent-driven-development).<!--en-->
Команда добавит в `AGENTS.md` блок, который перечисляет сгенерированные файлы, шаблоны для правки, языки и команды, которые нужно запускать после изменения. Для других агентов укажите `CLAUDE.md`, `GEMINI.md` или `.github/copilot-instructions.md`, см. [«Агентная разработка»](#агентная-разработка).<!--ru-->

**${en:'What comes next', ru:'Что дальше'}**

- Variables, language constructs and escapes: [Template syntax](#template-syntax).<!--en-->
- Built-in widgets (table of contents, import, languages, date, todo, alert, badge, math, exec, if, fileTree, details): [Widgets](#widgets).<!--en-->
- Переменные, многоязычные конструкции и экранирование: [«Синтаксис шаблона»](#синтаксис-шаблона).<!--ru-->
- Готовые виджеты (оглавление, импорт, языки, дата, todo, alert, badge, math, exec, if, fileTree, details): [«Виджеты»](#виджеты).<!--ru-->

<details>
<summary><b>${en:'Full template example (all widgets)', ru:'Полный пример шаблона (все виджеты)'}</b></summary>

```markdown
<\!--@nrg.languages=en,ru-->
<\!--@nrg.defaultLanguage=en-->

<!--\@title=**\${en:'Hello, World!', ru:'Привет, Мир!'}**-->
<!--\@version=1.0-->

\${widget:languages}

\${widget:badge(type='maven-central', coordinates='com.example:my-project')}

# \${title}<\!--toc.ignore-->

Last updated: \${widget:date}

\${widget:tableOfContents(title = "\${en:'Table of contents', ru:'Содержание'}", ordered = "true")}

## Part 1<\!--toc.ignore-->

### Chapter 1<\!--toc.ignore-->

English text<\!--en-->
Русский текст<\!--ru-->

\${widget:alert(type='note', text='\${en:'Heads up!', ru:'Обратите внимание!'}')}

The area of a circle is \${widget:math(expr='\\pi r^2')}.

\${widget:todo(text="\${en:'Document the next chapter', ru:'Описать следующую главу'}")}

\${widget:if(cond='endsWith(\${version}, -SNAPSHOT)')}
This is a development build.
\${widget:endIf}

\${widget:exec(cmd='git rev-parse --short HEAD', codeblock='text')}

\${widget:fileTree(path='src/main/java', depth='2', exclude='target,*.class')}

\${widget:import(path='path/to/your/file/another-info.src.md')}

\${widget:details(summary='Advanced configuration')}
Hidden inner markdown — \${widget:date(pattern='yyyy')} still works in here.
\${widget:endDetails}

\${widget:details(summary='Click', content='Short hidden text', open='true')}
```

</details>
