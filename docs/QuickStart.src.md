## ${en:'Quick start', ru:'Быстрый старт'}

**${en:'Prerequisites', ru:'Что нужно'}:** ${en:'Java 8 or higher', ru:'Java 8 или выше'} ([${en:'download', ru:'скачать'}](https://www.java.com/en/download/)).

**${en:'Step 1: Create a minimal template', ru:'Шаг 1: Создайте минимальный шаблон'} (`README.src.md`)**

```markdown
<!--\@nrg.languages=en,ru-->

# Hello<\!--en-->
# Привет<\!--ru-->

English text<\!--en-->
Русский текст<\!--ru-->
```

Lines tagged `<\!--en-->` go to `README.md`; lines tagged `<\!--ru-->` go to `README.ru.md`.<!--en-->
Строки, помеченные `<\!--en-->`, попадут в `README.md`; строки с `<\!--ru-->` — в `README.ru.md`.<!--ru-->

**${en:'Step 2: Generate the files', ru:'Шаг 2: Сгенерируйте файлы'}**

**Option A — CLI.** [Download](https://github.com/nanolaba/readme-generator/releases/tag/v${stableVersion}) the standalone jar, unzip it, then run:<!--en-->
**Вариант A — CLI.** [Скачайте](https://github.com/nanolaba/readme-generator/releases/tag/v${stableVersion}) автономный jar, распакуйте архив и запустите:<!--ru-->

```bash
nrg -f /path/to/README.src.md
```

**Option B — Maven plugin.** Add this to your `pom.xml` (full configuration in the [Maven plugin](#use-as-maven-plugin) section below):<!--en-->
**Вариант B — Maven-плагин.** Добавьте в `pom.xml` (полная конфигурация ниже, в разделе [«Использование как плагина для maven»](#использование-как-плагина-для-maven)):<!--ru-->

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

**${en:'Step 3: Check the result', ru:'Шаг 3: Проверьте результат'}**

Two files appear next to the template — `README.md` and `README.ru.md`:<!--en-->
Рядом с шаблоном появятся два файла — `README.md` и `README.ru.md`:<!--ru-->

<table>
<tr><th><b>README.md</b></th><th><b>README.ru.md</b></th></tr>
<tr><td>

```markdown
# Hello

English text
```

</td><td>

```markdown
# Привет

Русский текст
```

</td></tr></table>

**${en:'What comes next', ru:'Что дальше'}**

- Variables, language constructs, and escapes — see [Template syntax](#template-syntax).<!--en-->
- Built-in widgets (table of contents, import, languages, date, todo, alert, badge, math, exec, if, fileTree) — see [Widgets](#widgets).<!--en-->
- Переменные, многоязычный синтаксис и экранирование — см. [«Синтаксис шаблона»](#синтаксис-шаблона).<!--ru-->
- Готовые виджеты (оглавление, импорт, языки, дата, todo, alert, badge, math, exec, if, fileTree) — см. [«Виджеты»](#виджеты).<!--ru-->

<details>
<summary><b>${en:'Full template example (all widgets)', ru:'Полный пример шаблона (все виджеты)'}</b></summary>

```markdown
<!--\@nrg.languages=en,ru-->
<!--\@nrg.defaultLanguage=en-->

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
```

</details>
