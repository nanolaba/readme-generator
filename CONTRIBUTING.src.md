<!--@nrg.languages=en,ru-->
<!--@nrg.defaultLanguage=en-->

<!--@projectName=**Nanolaba Readme Generator (NRG)**-->
<!--@issuesUrl=https://github.com/nanolaba/readme-generator/issues-->
<!--@discussionsUrl=https://github.com/nanolaba/readme-generator/discussions-->
<!--@securityEmail=nrg@nanolaba.com-->

${widget:languages}

# ${en:'Contributing to Nanolaba Readme Generator', ru:'Как внести вклад в Nanolaba Readme Generator'}<!--toc.ignore-->

Thanks for your interest in improving **NRG**! This document explains how to report issues, propose changes, and get a pull request merged. By participating, you agree to be respectful and constructive — please assume good faith on both sides.<!--en-->
Спасибо за интерес к развитию **NRG**! Этот документ описывает, как сообщать о проблемах, предлагать изменения и довести pull request до мерджа. Участвуя в проекте, вы соглашаетесь быть уважительными и конструктивными — пожалуйста, исходите из доброго умысла собеседника.<!--ru-->

${widget:tableOfContents(title = "${en:'Table of contents', ru:'Содержание'}", ordered = "true")}

## ${en:'Ways to contribute', ru:'Как помочь проекту'}

- **${en:'Report bugs', ru:'Сообщайте о багах'}** ${en:'or unexpected behavior via', ru:'и неожиданном поведении через'} [GitHub Issues](${issuesUrl}).
- **${en:'Propose features', ru:'Предлагайте новые фичи'}** ${en:'via', ru:'через'} [GitHub Discussions](${discussionsUrl}) ${en:"or as an issue with the `enhancement` label.", ru:'или issue с меткой `enhancement`.'}
- **${en:'Improve documentation', ru:'Улучшайте документацию'}** — ${en:'README, Javadoc, or examples in', ru:'README, Javadoc или примеры в'} `nrg/src/test/java/com/nanolaba/nrg/examples/` ${en:'(these double as documentation samples imported by the README).', ru:'(они одновременно используются как примеры в README).'}
- **${en:'Submit code', ru:'Присылайте код'}** — ${en:'bug fixes, new widgets, or other improvements via pull request.', ru:'багфиксы, новые виджеты или другие улучшения через pull request.'}
- **${en:'Help others', ru:'Помогайте другим'}** ${en:'by answering questions in', ru:', отвечая на вопросы в'} [${en:'Discussions', ru:'Discussions'}](${discussionsUrl}).

## ${en:'Reporting bugs', ru:'Как сообщить о баге'}

Before opening a new issue, please:<!--en-->
Перед созданием нового issue, пожалуйста:<!--ru-->

1. ${en:'Search', ru:'Поищите среди'} [${en:'existing issues', ru:'существующих issues'}](${issuesUrl}?q=is%3Aissue) ${en:'to avoid duplicates.', ru:', чтобы избежать дублей.'}
2. ${en:'Confirm the bug reproduces against the latest release (or `main`).', ru:'Убедитесь, что баг воспроизводится на последнем релизе (или на `main`).'}
3. ${en:'Open a new issue with a clear, descriptive title and include:', ru:'Откройте новый issue с понятным, описательным заголовком и приложите:'}
   - ${en:'NRG version, Java version, and operating system.', ru:'версию NRG, версию Java и операционную систему;'}
   - ${en:'The minimal `.src.md` template (or code snippet) that reproduces the problem.', ru:'минимальный `.src.md`-шаблон (или фрагмент кода), воспроизводящий проблему;'}
   - ${en:'The exact command or invocation used.', ru:'точную команду или способ запуска;'}
   - ${en:"Expected vs. actual output (paste both, don't paraphrase).", ru:'ожидаемый и фактический вывод (приложите оба, не пересказывайте);'}
   - ${en:'Stack traces or log output, if any (run with `--log-level debug` or `NRG_LOG_LEVEL=debug` for more detail).', ru:'стек-трейсы и логи, если есть (для подробного вывода используйте `--log-level debug` или `NRG_LOG_LEVEL=debug`).'}

## ${en:'Proposing features', ru:'Предложение новых фич'}

Open an issue first — even a short one — before writing a feature PR. This avoids duplicated work and lets us discuss scope and design up front. Useful things to include:<!--en-->
Сначала откройте issue — пусть даже короткий — прежде чем браться за PR с новой фичей. Это исключает дублирование работы и даёт возможность обсудить scope и дизайн заранее. Полезно указать:<!--ru-->

- ${en:'The problem the feature solves and a concrete use case.', ru:'какую проблему решает фича и конкретный сценарий применения;'}
- ${en:'A short proposal of the template syntax / CLI flag / API surface.', ru:'короткое предложение по синтаксису шаблона / CLI-флагу / API;'}
- ${en:'Alternatives you considered.', ru:'альтернативы, которые вы рассматривали.'}

For larger changes, a discussion thread is often a better starting point than an issue.<!--en-->
Для крупных изменений тред в Discussions часто удобнее, чем issue.<!--ru-->

## ${en:'Security issues', ru:'Уязвимости и безопасность'}

**${en:'Do not open a public issue for security reports.', ru:'Не публикуйте отчёты о безопасности в открытых issues.'}** ${en:'Email', ru:'Напишите на'} <${securityEmail}> ${en:'with the subject prefixed', ru:'с темой, начинающейся на'} `[SECURITY]`. ${en:'We will acknowledge receipt and coordinate a fix and disclosure timeline.', ru:'Мы подтвердим получение и согласуем сроки фикса и раскрытия.'}

## ${en:'Development setup', ru:'Подготовка окружения'}

**${en:'Prerequisites:', ru:'Что нужно:'}**

- ${en:'Java **8 or higher** (Temurin 8 or 17 are what CI uses).', ru:'Java **8 или выше** (CI использует Temurin 8 и 17).'}
- ${en:'Maven 3.6+ (the IntelliJ IDEA bundled distribution works; any standard install does too).', ru:'Maven 3.6+ (подойдёт версия, поставляемая с IntelliJ IDEA, или любая стандартная установка).'}
- Git.

**${en:'Clone and build:', ru:'Склонировать и собрать:'}**

```bash
git clone https://github.com/nanolaba/readme-generator.git
cd readme-generator
mvn install -Dgpg.skip=true
```

The parent build wires up GPG signing for releases, so local builds without GPG keys will fail at `verify`. Pass `-Dgpg.skip=true` (or stop at the `package` phase) when iterating locally.<!--en-->
Корневой `pom.xml` подключает GPG-подпись для релизов, поэтому локальная сборка без ключей упадёт на фазе `verify`. При локальной разработке используйте `-Dgpg.skip=true` (или останавливайтесь на фазе `package`).<!--ru-->

## ${en:'Project layout', ru:'Структура проекта'}

${en:'NRG is a multi-module Maven project:', ru:'NRG — многомодульный Maven-проект:'}

- `nrg/` — ${en:'core library and CLI (`Main-Class` = `com.nanolaba.nrg.NRG`). Produces a fat jar `nrg.jar` plus a release zip with `nrg.sh` / `nrg.bat` wrappers.', ru:'основная библиотека и CLI (`Main-Class` = `com.nanolaba.nrg.NRG`). Собирает fat-jar `nrg.jar` и релизный zip с обёртками `nrg.sh` / `nrg.bat`.'}
- `nrg-maven-plugin/` — ${en:'thin Maven plugin (`create-files` goal) that wraps the CLI.', ru:'тонкий Maven-плагин (цель `create-files`), оборачивающий CLI.'}
- `nrg-action/` — ${en:'GitHub Action distribution.', ru:'дистрибутив для GitHub Action.'}
- `docs/*.src.md` — ${en:"source fragments imported by the top-level `README.src.md` (the project dogfoods itself: never hand-edit `README.md` or `README.ru.md` — change the source template and regenerate).", ru:'фрагменты, импортируемые в корневой `README.src.md` (проект «ест свой dog food»: никогда не редактируйте `README.md` или `README.ru.md` руками — меняйте шаблон и перегенерируйте).'}

The generation pipeline is documented at the top of `nrg/src/main/java/com/nanolaba/nrg/core/Generator.java` and `TemplateLine.java`; widget contracts live in `widgets/NRGWidget.java`.<!--en-->
Конвейер генерации описан в шапке файлов `nrg/src/main/java/com/nanolaba/nrg/core/Generator.java` и `TemplateLine.java`; контракты виджетов — в `widgets/NRGWidget.java`.<!--ru-->

## ${en:'Building and testing', ru:'Сборка и тесты'}

${en:'Common commands (run from the repository root):', ru:'Часто используемые команды (запускать из корня репозитория):'}

```bash
# ${en:'Full build, run tests, install to local Maven repo', ru:'Полная сборка, прогон тестов, установка в локальный Maven-репозиторий'}
mvn install -Dgpg.skip=true

# ${en:'Compile core only', ru:'Скомпилировать только ядро'}
mvn -pl nrg compile

# ${en:'Run all tests in the core module', ru:'Прогнать все тесты модуля ядра'}
mvn -pl nrg test

# ${en:'Run a single test class or method', ru:'Запустить один тест-класс или метод'}
mvn -pl nrg test -Dtest=TableOfContentsWidgetTest
mvn -pl nrg test -Dtest=TableOfContentsWidgetTest#methodName

# ${en:'Mutation testing (PIT) — opt-in', ru:'Mutation testing (PIT) — по запросу'}
mvn -pl nrg test -Dpit.skip=false
```

When changing widget behavior or anchor generation, please also run the focused tests (`TableOfContentsWidgetTest`, `TableOfContentsWidgetSlugifyTest`, `ImportWidgetTest`, etc.) — they exercise the most fragile logic in the codebase.<!--en-->
При изменении поведения виджетов или генерации анкоров запускайте также точечные тесты (`TableOfContentsWidgetTest`, `TableOfContentsWidgetSlugifyTest`, `ImportWidgetTest` и т.д.) — они покрывают самую хрупкую логику.<!--ru-->

CI runs `mvn verify` against JDK 8 and JDK 17 on every push and pull request to `main`. Your changes need to pass on both.<!--en-->
CI прогоняет `mvn verify` на JDK 8 и JDK 17 при каждом push и pull request в `main`. Ваши изменения должны проходить и там, и там.<!--ru-->

## ${en:'Coding conventions', ru:'Соглашения по коду'}

**${en:'Language level — Java 8.', ru:'Уровень языка — Java 8.'}** ${en:'No `var`, no records, no switch expressions, no text blocks. Streams and lambdas are fine and encouraged where they read well.', ru:'Никаких `var`, records, switch-выражений и текстовых блоков. Stream-API и лямбды — пожалуйста, там, где это читаемо.'}

**${en:'Logging.', ru:'Логирование.'}** ${en:'Use the in-house `com.nanolaba.logging.LOG` (NOT SLF4J). Pattern is `LOG.debug("msg {}", arg)`.', ru:'Используется внутренний `com.nanolaba.logging.LOG` (НЕ SLF4J). Шаблон: `LOG.debug("msg {}", arg)`.'}

**${en:'Checked exceptions in lambdas.', ru:'Checked-исключения в лямбдах.'}** ${en:'Wrap with `com.nanolaba.sugar.Code.run(...)` rather than try/catch boilerplate.', ru:'Оборачивайте через `com.nanolaba.sugar.Code.run(...)` вместо громоздкого try/catch.'}

**${en:'Constants for property names', ru:'Константы имён свойств'}** ${en:'live in `NRGConstants` (`PROPERTY_LANGUAGES`, `PROPERTY_DEFAULT_LANGUAGE`, etc.). Don''t hard-code `"nrg.xxx"` strings in widgets.', ru:'лежат в `NRGConstants` (`PROPERTY_LANGUAGES`, `PROPERTY_DEFAULT_LANGUAGE` и т.д.). Не хардкодьте строки `"nrg.xxx"` в виджетах.'}

**${en:'Class-level Javadoc on every new class.', ru:'Javadoc на каждом новом классе.'}** ${en:'Match the style of `nrg/src/main/java/com/nanolaba/nrg/core/json/MinimalJsonParser.java`: a short paragraph stating what the class is and why it exists, followed by a `<p>` paragraph for scope, invariants, or non-obvious behavior. Add Javadoc on non-trivial methods too (subtle algorithms, multi-style branches, contract corner cases) — skip simple getters/setters and self-explanatory delegators. Document the **why** and edge cases, not the **what**; well-named identifiers already say what.', ru:'Стиль — как у `nrg/src/main/java/com/nanolaba/nrg/core/json/MinimalJsonParser.java`: короткий абзац о том, что это за класс и зачем он нужен, затем абзац `<p>` про область применения, инварианты или неочевидное поведение. Документируйте также нетривиальные методы (тонкие алгоритмы, мульти-стилевые ветки, угловые случаи контракта) — простые геттеры/сеттеры и очевидные делегаторы пропускайте. Пишите про **почему** и про крайние случаи, а не про **что** — хорошо названные идентификаторы и так говорят, что код делает.'}

**${en:'Inspect surrounding code when adding a new class.', ru:'Осматривайте окружающий код при добавлении нового класса.'}** ${en:'Before opening a PR, scan it for low-effort cleanups in the same pass: prefer proper imports over `new java.xxx.Yyy()` style fully-qualified names, drop redundant intermediate variables, remove dead branches.', ru:'Перед PR пройдитесь по тому же файлу с небольшими улучшениями: используйте обычные импорты вместо `new java.xxx.Yyy()`, убирайте лишние промежуточные переменные и мёртвые ветки.'}

**${en:'Tests.', ru:'Тесты.'}** ${en:'JUnit 5 (`org.junit.jupiter`). Place new tests under `nrg/src/test/java/`. If you add a test under the `examples/` package, remember it doubles as a documentation sample (it may be imported by `docs/Advanced.src.md` and rendered into the README) — regenerate the README if you rename or restructure such files.', ru:'JUnit 5 (`org.junit.jupiter`). Новые тесты — в `nrg/src/test/java/`. Если добавляете тест в пакет `examples/`, помните, что он используется как пример в документации (может импортироваться в `docs/Advanced.src.md` и попадать в README) — при переименовании или реорганизации перегенерируйте README.'}

**${en:'Defensive code.', ru:'Защитное программирование.'}** ${en:"Don't add validation, error handling, or fallbacks for scenarios that can't happen. Trust internal callers; only validate at system boundaries (CLI input, external files, network). Don't introduce backwards-compatibility shims when changing internal APIs.", ru:'Не добавляйте проверки, обработку ошибок и fallback''и для невозможных ситуаций. Доверяйте внутренним вызовам; валидируйте только на границах системы (CLI-ввод, внешние файлы, сеть). Не вводите backwards-compatibility прокладки при изменении внутренних API.'}

## ${en:'Documentation', ru:'Документация'}

The top-level `README.md` and `README.ru.md` are **generated** from `README.src.md` (which `\${widget:import}`s fragments under `docs/`). Workflow when documentation needs to change:<!--en-->
Корневые `README.md` и `README.ru.md` **генерируются** из `README.src.md` (он импортирует фрагменты из `docs/` через `\${widget:import}`). Рабочий процесс при изменении документации:<!--ru-->

1. ${en:'Edit the relevant `.src.md` source — **never** the generated `.md`.', ru:'Редактируйте соответствующий `.src.md` — **никогда** сгенерированный `.md`.'}
2. ${en:'Regenerate locally:', ru:'Локально перегенерируйте файлы:'}
   ```bash
   nrg -f README.src.md
   # ${en:'or, if not installed:', ru:'или, если CLI не установлен:'}
   java -jar nrg/target/nrg.jar -f README.src.md
   ```
3. ${en:'Commit both the `.src.md` change and the regenerated `.md` files in the same commit.', ru:'Коммитьте изменения в `.src.md` и перегенерированные `.md` в одном коммите.'}

CI runs the generator in `--check` mode and fails the build if the committed `.md` files don't match what the source would regenerate.<!--en-->
CI запускает генератор в режиме `--check` и валит сборку, если закоммиченные `.md` не совпадают с тем, что произвёл бы генератор.<!--ru-->

## ${en:'Commit messages', ru:'Сообщения коммитов'}

Follow the convention used in the existing history (a [Conventional Commits](https://www.conventionalcommits.org/) flavour):<!--en-->
Следуйте конвенции из истории репозитория (вариация [Conventional Commits](https://www.conventionalcommits.org/)):<!--ru-->

```
<type>: <short summary>

<optional body explaining why, not what>
```

Common types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `revert`. Keep the subject line under ~70 characters and write in the imperative mood ("add X", not "added X"). Reference issues in the subject when applicable: `feat: ${en:'\\${widget:asset}', ru:'\\${widget:asset}'} per-language widget (#42)`.<!--en-->
Распространённые типы: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `revert`. Заголовок — до ~70 символов, в повелительном наклонении («add X», а не «added X»). Ссылайтесь на issue в заголовке, когда уместно: `feat: \\${widget:asset} per-language widget (#42)`.<!--ru-->

## ${en:'Pull request checklist', ru:'Чеклист pull request'}

${en:'Before submitting:', ru:'Перед отправкой PR:'}

- [ ] ${en:'Branch is up to date with `main`.', ru:'Ветка обновлена относительно `main`.'}
- [ ] ${en:'Code compiles cleanly:', ru:'Код собирается без ошибок:'} `mvn -pl nrg compile`.
- [ ] ${en:'All tests pass:', ru:'Все тесты проходят:'} `mvn install -Dgpg.skip=true`.
- [ ] ${en:'New behavior has unit tests (and, where it touches widget output, an integration sample).', ru:'Новое поведение покрыто unit-тестами (а если затрагивает вывод виджета — то и integration-примером).'}
- [ ] ${en:'New or changed public classes/methods have Javadoc.', ru:'У новых и изменённых публичных классов/методов есть Javadoc.'}
- [ ] ${en:'If you touched template-driven docs, the generated `README.md` / `README.ru.md` have been regenerated and committed alongside the source.', ru:'Если правили документацию через шаблон, сгенерированные `README.md` / `README.ru.md` перегенерированы и закоммичены рядом с исходником.'}
- [ ] ${en:'Commit messages follow the convention above.', ru:'Сообщения коммитов соответствуют конвенции выше.'}
- [ ] ${en:'No unrelated changes are bundled in (no formatting churn outside the modified files, no IDE settings, no `.idea/`, `.claude/`, or local plan files).', ru:'PR не содержит посторонних изменений (никаких форматных правок вне затронутых файлов, никаких IDE-настроек — `.idea/`, `.claude/` и локальных планов).'}

${en:'When opening the PR:', ru:'При открытии PR:'}

- ${en:'Describe **what** changed and **why**, not just the diff.', ru:'Опишите **что** изменилось и **почему**, а не просто diff.'}
- ${en:'Link the issue it resolves (`Closes #123`).', ru:'Ссылайтесь на закрываемый issue (`Closes #123`).'}
- ${en:'Include before/after examples if the change affects rendered output.', ru:'Прикладывайте примеры «до/после», если изменение влияет на сгенерированный вывод.'}

We aim to give first feedback within a few days. If you don't hear back in a week, feel free to ping the PR.<!--en-->
Первый ответ мы стараемся дать в течение нескольких дней. Если за неделю никто не отписался — не стесняйтесь пинговать PR.<!--ru-->

## ${en:'Release process', ru:'Процесс релизов'}

Releases are cut from `main` by maintainers. Versioning follows [Semantic Versioning](https://semver.org/). Contributors don't need to touch `pom.xml` versions in feature PRs — that happens at release time.<!--en-->
Релизы режутся с `main` мейнтейнерами. Версионирование — [Semantic Versioning](https://semver.org/). В фича-PR не нужно править версии в `pom.xml` — это делается на релизе.<!--ru-->

## ${en:'License', ru:'Лицензия'}

By contributing, you agree that your contributions will be licensed under the project's [Apache License 2.0](LICENSE).<!--en-->
Отправляя вклад, вы соглашаетесь, что он распространяется под [Apache License 2.0](LICENSE).<!--ru-->

If you're contributing on behalf of an employer, please ensure you have permission to do so under your employment agreement.<!--en-->
Если вы вносите вклад от имени работодателя, убедитесь, что это разрешено вашим трудовым договором.<!--ru-->

---

Thanks again for helping make ${projectName} better! For anything not covered here, [open a discussion](${discussionsUrl}) or reach out at <${securityEmail}>.<!--en-->
Спасибо, что помогаете делать ${projectName} лучше! Если что-то осталось за кадром — [откройте discussion](${discussionsUrl}) или напишите на <${securityEmail}>.<!--ru-->
