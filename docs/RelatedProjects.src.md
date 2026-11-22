## ${en:'Related projects', ru:'Похожие проекты'}

Other tools that generate or update Markdown. ✅ = supported, ➖ = partial, ❌ = not supported.<!--en-->
Другие инструменты, которые генерируют или обновляют Markdown. ✅ — поддерживается, ➖ — частично, ❌ — нет.<!--ru-->

| ${en:'Feature', ru:'Возможность'} | **NRG** | [ml-md][ml-md] | [doctoc][doctoc] | [embedme][embedme] | [cog][cog] | [gitdown][gitdown] | [md-magic][md-magic] | [remark][remark] |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| ${en:'Stack', ru:'Стек'} | Java 8 | Python | Node | Node | Python | Node | Node | Node |
| ${en:'Multi-lang output¹', ru:'Многоязычный вывод¹'} | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| ${en:'File imports', ru:'Импорт файлов'} | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ | ➖ |
| ${en:'Auto TOC', ru:'Авто-TOC'} | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ➖ |
| ${en:'Variables', ru:'Переменные'} | ✅ | ➖ | ❌ | ❌ | ✅ | ✅ | ✅ | ➖ |
| ${en:'Custom widgets', ru:'Свои виджеты'} | ✅ | ❌ | ❌ | ❌ | ✅² | ✅ | ✅ | ✅ |
| ${en:'Maven plugin', ru:'Maven-плагин'} | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| ${en:'GitHub Action', ru:'GitHub Action'} | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| ${en:'Frozen regions³', ru:'Замороженные регионы³'} | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ➖ | ❌ |

¹ ${en:'one source → many language files (`README.md`, `README.ru.md`, …).', ru:'один исходник → несколько файлов на разных языках (`README.md`, `README.ru.md`, …).'}<br>
² ${en:'cog runs arbitrary Python — extensible by definition, but no widget API.', ru:'cog исполняет произвольный Python — расширяем по определению, но без widget-API.'}<br>
³ ${en:'preserve content written by external tools (contributors-readme-action, sponsors widgets, RSS) across regenerations.', ru:'сохранять содержимое, записанное внешними инструментами (contributors-readme-action, sponsors-виджеты, RSS), между перегенерациями.'}

### ${en:'When another tool fits better', ru:'Когда лучше подойдёт другой инструмент'}

- **A README in one language that only needs a table of contents.** doctoc updates the TOC inside `README.md` itself, with no template and no second file.<!--en-->
- **README на одном языке, которому нужно только оглавление.** doctoc обновляет оглавление прямо в `README.md`, без шаблона и второго файла.<!--ru-->
- **You want to keep editing `README.md` directly.** embedme, cog and markdown-magic regenerate marked blocks inside the file they live in, so there is no `.src.md`. NRG keeps the source and the output in separate files, and `--check` and the edit hook rely on that.<!--en-->
- **Вы хотите и дальше править сам `README.md`.** embedme, cog и markdown-magic перегенерируют размеченные блоки внутри того же файла, и `.src.md` не нужен. NRG держит исходник и результат в разных файлах, и на этом построены `--check` и хук правок.<!--ru-->
- **No JVM in the toolchain.** NRG needs Java 8+. multilingual-markdown, in Python, also builds one file per language from one source.<!--en-->
- **В окружении нет JVM.** NRG нужна Java 8+. multilingual-markdown на Python тоже собирает по файлу на язык из одного исходника.<!--ru-->
- **Linting and reformatting Markdown.** remark works on the syntax tree, with plugins for lint rules and formatting; NRG only generates.<!--en-->
- **Линтинг и форматирование Markdown.** remark работает с синтаксическим деревом и подключает плагины для правил линтинга и форматирования; NRG только генерирует.<!--ru-->

[ml-md]: https://github.com/ryul1206/multilingual-markdown "multilingual-markdown"
[doctoc]: https://github.com/thlorenz/doctoc "doctoc"
[embedme]: https://github.com/zakhenry/embedme "embedme"
[cog]: https://github.com/nedbat/cog "cog"
[gitdown]: https://github.com/gajus/gitdown "gitdown"
[md-magic]: https://github.com/DavidWells/markdown-magic "markdown-magic"
[remark]: https://github.com/remarkjs/remark "remark + remark-toc + remark-include"

