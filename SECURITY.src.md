<!--@nrg.languages=en,ru-->
<!--@nrg.defaultLanguage=en-->

<!--@nrg.pom.path=pom.xml-->

<!--@projectName=**Nanolaba Readme Generator (NRG)**-->
<!--@securityEmail=nrg@nanolaba.com-->
<!--@stableVersion=${pom.properties.stableVersion}-->

${widget:languages}

# ${en:'Security Policy', ru:'Политика безопасности'}<!--toc.ignore-->

We take the security of ${projectName} seriously. This document describes how to report vulnerabilities and what you can expect from us in return.<!--en-->
Мы серьёзно относимся к безопасности ${projectName}. Этот документ описывает, как сообщать об уязвимостях и чего ждать в ответ.<!--ru-->

## ${en:'Supported versions', ru:'Поддерживаемые версии'}

Security fixes are issued for the latest stable release. Older releases do not receive backports.<!--en-->
Исправления безопасности выпускаются для последней стабильной версии. Для более старых релизов бэкпорты не выпускаются.<!--ru-->

| ${en:'Version', ru:'Версия'} | ${en:'Supported', ru:'Поддерживается'} |
| --- | --- |
| `${stableVersion}.x` | ${en:'Yes', ru:'Да'} |
| < `${stableVersion}` | ${en:'No', ru:'Нет'} |

${en:'If you are running an older version, please upgrade before reporting — the issue may already be fixed on the current release.', ru:'Если вы используете более старую версию, обновитесь перед тем, как сообщать: возможно, проблема уже исправлена в актуальном релизе.'}

## ${en:'Reporting a vulnerability', ru:'Как сообщить об уязвимости'}

**${en:'Please do NOT open a public GitHub issue, pull request, or discussion for security reports.', ru:'Пожалуйста, НЕ создавайте публичный GitHub issue, pull request или discussion для отчётов о безопасности.'}** ${en:'Public disclosure before a fix is available puts every user at risk.', ru:'Публичное раскрытие до выхода фикса подвергает риску всех пользователей.'}

${en:'Send the report by email to', ru:'Отправьте отчёт на email'} <${securityEmail}> ${en:'with the subject prefixed', ru:'с темой, начинающейся на'} `[SECURITY]`. ${en:'If you would like the report encrypted, ask in the first message and we will exchange a PGP key out of band.', ru:'Если хотите шифровать переписку — напишите об этом в первом сообщении, и мы согласуем PGP-ключ.'}

### ${en:'What to include', ru:'Что приложить к отчёту'}

${en:'A useful report contains, at minimum:', ru:'Полезный отчёт содержит как минимум:'}

- ${en:'NRG version, distribution (CLI / Maven plugin / GitHub Action / library), Java version, and operating system.', ru:'версию NRG, способ запуска (CLI / Maven-плагин / GitHub Action / библиотека), версию Java и операционную систему;'}
- ${en:'A clear description of the vulnerability and its impact (what an attacker can do).', ru:'чёткое описание уязвимости и её последствий (что может сделать атакующий);'}
- ${en:'Step-by-step reproduction: a minimal `.src.md` template, command line, or code snippet that demonstrates the issue.', ru:'пошаговое воспроизведение: минимальный `.src.md`-шаблон, командную строку или фрагмент кода, демонстрирующий проблему;'}
- ${en:'Any proof-of-concept, logs, stack traces, or screenshots that help us reproduce.', ru:'любой proof-of-concept, логи, стек-трейсы или скриншоты, помогающие воспроизвести;'}
- ${en:'Whether the vulnerability is already known to third parties, and any deadline you have in mind for public disclosure.', ru:'известна ли уязвимость третьим лицам и какой дедлайн раскрытия вы ожидаете.'}

${en:'A working proof-of-concept is not required, but it dramatically speeds up triage.', ru:'Рабочий proof-of-concept не обязателен, но сильно ускоряет разбор.'}

## ${en:'What to expect', ru:'Чего ожидать в ответ'}

${en:'After we receive your report:', ru:'После получения отчёта мы:'}

1. ${en:'**Acknowledgement** — within 3 working days we confirm receipt and assign an internal tracker.', ru:'**Подтверждение получения** — в течение 3 рабочих дней подтверждаем получение и заводим внутренний тикет.'}
2. ${en:'**Triage** — within 10 working days we evaluate severity and either confirm or refute the vulnerability with our reasoning.', ru:'**Триаж** — в течение 10 рабочих дней оцениваем серьёзность и подтверждаем либо опровергаем уязвимость, объясняя своё решение.'}
3. ${en:'**Fix** — for confirmed vulnerabilities we develop and test a patch privately. Timeline depends on severity (typically days for critical, weeks for low-severity).', ru:'**Фикс** — для подтверждённых уязвимостей разрабатываем и тестируем патч приватно. Сроки зависят от серьёзности (обычно дни для критичных, недели — для низкоприоритетных).'}
4. ${en:'**Coordinated disclosure** — we publish a release containing the fix together with a security advisory crediting the reporter (unless you ask for anonymity). We agree on the disclosure date with the reporter beforehand.', ru:'**Согласованное раскрытие** — выпускаем релиз с фиксом вместе с security advisory, в котором благодарим репортёра (если только вы не попросите анонимности). Дату раскрытия согласуем с репортёром заранее.'}

${en:'We will keep you informed throughout the process. If progress stalls, expect at least a status update every two weeks.', ru:'Мы держим вас в курсе на каждом шаге. Если работа застопорилась, статус-апдейт приходит как минимум раз в две недели.'}

## ${en:'Disclosure policy', ru:'Политика раскрытия'}

${en:'We follow **coordinated disclosure**: we ask reporters to give us a reasonable window to ship a fix before going public. The window is usually **90 days** from initial report, shorter for low-severity issues, longer for complex ones — always negotiated, never unilaterally extended.', ru:'Мы придерживаемся **согласованного раскрытия**: просим репортёров дать нам разумное время на выпуск фикса до публикации. Окно обычно — **90 дней** с момента первого отчёта, короче для низкоприоритетных, длиннее — для сложных, и всегда согласуется, а не продлевается в одностороннем порядке.'}

${en:'After the fix ships we publish a [GitHub Security Advisory](https://github.com/nanolaba/readme-generator/security/advisories) and request a CVE if applicable.', ru:'После выхода фикса публикуем [GitHub Security Advisory](https://github.com/nanolaba/readme-generator/security/advisories) и при необходимости запрашиваем CVE.'}

## ${en:'Scope', ru:'Что входит в scope'}

**${en:'In scope:', ru:'Входит:'}** ${en:'vulnerabilities in NRG itself — the core library, the CLI, the Maven plugin, and the GitHub Action distribution. In particular, we are interested in:', ru:'уязвимости в самом NRG — ядре, CLI, Maven-плагине и GitHub Action. Особенно интересуют:'}

- Bypasses of the `--allow-exec` / `<allowExec>` opt-in for the `\${widget:exec}` widget (running external commands without the user opting in).<!--en-->
- Обходы opt-in `--allow-exec` / `<allowExec>` для виджета `\${widget:exec}` (запуск внешних команд без явного согласия пользователя).<!--ru-->
- Bypasses of `nrg.allowRemoteImports` for `\${widget:import(url=...)}` (fetching remote content when the user has not enabled remote imports).<!--en-->
- Обходы `nrg.allowRemoteImports` для `\${widget:import(url=...)}` (загрузка удалённого контента без явного включения).<!--ru-->
- Bypasses of `sha256` integrity pinning for remote imports.<!--en-->
- Обходы проверки целостности `sha256` для remote-импортов.<!--ru-->
- Path-traversal or arbitrary-file-read vulnerabilities via the `path` parameter of `\${widget:import}` or `\${widget:fileTree}`.<!--en-->
- Path-traversal и произвольное чтение файлов через параметр `path` у `\${widget:import}` или `\${widget:fileTree}`.<!--ru-->
- ${en:'Code injection / arbitrary code execution via crafted source templates loaded from untrusted input.', ru:'Инъекция кода / произвольное исполнение через специально подобранные шаблоны из недоверенного источника.'}
- ${en:'Regex denial-of-service (ReDoS) in the rendering pipeline triggered by reasonably-sized input.', ru:'Regex denial-of-service (ReDoS) в конвейере рендеринга, срабатывающий на разумно больших входах.'}
- ${en:'Vulnerabilities in NRG-published artifacts on Maven Central or in the published GitHub Action.', ru:'Уязвимости в опубликованных артефактах NRG на Maven Central и в опубликованном GitHub Action.'}

**${en:'Out of scope:', ru:'Не входит:'}**

- ${en:'Issues in third-party dependencies — please report those upstream. We will pick up dependency fixes in regular releases.', ru:'Проблемы в зависимостях — сообщайте напрямую разработчикам соответствующих библиотек. Обновления по зависимостям мы подхватываем в обычных релизах.'}
- Behaviour that follows directly from a user choosing to enable an opt-in feature — e.g. running `\${widget:exec}` after passing `--allow-exec`, or fetching a remote URL after setting `nrg.allowRemoteImports=true`. The opt-ins exist precisely so this does not happen by default.<!--en-->
- Поведение, прямо следующее из включения opt-in-фич — например, выполнение `\${widget:exec}` после `--allow-exec` или загрузка remote URL после `nrg.allowRemoteImports=true`. Эти opt-in специально для того и сделаны, чтобы такое не случалось по умолчанию.<!--ru-->
- ${en:'Vulnerabilities exploitable only by an attacker who already controls the source template author''s machine.', ru:'Уязвимости, эксплуатируемые только при условии, что атакующий уже контролирует машину автора шаблона.'}
- ${en:'The content of generated `.md` files when the source template was authored by the attacker — generated output is, by design, whatever the template said it should be.', ru:'Содержимое сгенерированных `.md`, если шаблон написан атакующим — вывод по определению является тем, что задал автор шаблона.'}
- ${en:'Issues in projects that merely *use* NRG — those belong in the respective project''s tracker, not here.', ru:'Проблемы в проектах, которые лишь *используют* NRG — такие отчёты направляйте в трекеры этих проектов, не сюда.'}

## ${en:'Safe-harbor', ru:'Гарантии для исследователей'}

${en:'We will not pursue legal action against security researchers who:', ru:'Мы не будем преследовать в судебном порядке исследователей безопасности, которые:'}

- ${en:'Make a good-faith effort to avoid privacy violations, data destruction, and service disruption.', ru:'Добросовестно стараются избегать нарушения приватности, разрушения данных и нарушения работы сервиса.'}
- ${en:'Do not exploit a vulnerability beyond the minimum necessary to demonstrate it.', ru:'Не эксплуатируют уязвимость дальше минимума, необходимого для её демонстрации.'}
- ${en:'Give us a reasonable time to respond before any public disclosure.', ru:'Дают нам разумное время на ответ до любого публичного раскрытия.'}
- ${en:'Do not attack third parties or NRG users.', ru:'Не атакуют третьих лиц и пользователей NRG.'}

## ${en:'Acknowledgements', ru:'Благодарности'}

${en:'We credit reporters who help us improve NRG''s security in the corresponding release notes and GitHub Security Advisories — unless you prefer to remain anonymous, just let us know.', ru:'Мы благодарим репортёров, помогающих сделать NRG безопаснее, в release notes соответствующего релиза и в GitHub Security Advisory — если предпочитаете остаться анонимом, просто сообщите об этом.'}

---

${en:'For non-security questions, please use the channels listed in', ru:'Для вопросов, не связанных с безопасностью, используйте каналы, перечисленные в'} [CONTRIBUTING.md](${en:'CONTRIBUTING.md', ru:'CONTRIBUTING.ru.md'}).
