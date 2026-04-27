## ${en:'Related projects', ru:'Похожие проекты'}

Other tools in the same space — useful if ${name} does not fit your stack or workflow. ✅ = supported, ➖ = partial, ❌ = not supported.<!--en-->
Другие инструменты в этой области — пригодятся, если ${name} не подходит под ваш стек или рабочий процесс. ✅ — поддерживается, ➖ — частично, ❌ — нет.<!--ru-->

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

[ml-md]: https://github.com/ryul1206/multilingual-markdown "multilingual-markdown"
[doctoc]: https://github.com/thlorenz/doctoc "doctoc"
[embedme]: https://github.com/zakhenry/embedme "embedme"
[cog]: https://github.com/nedbat/cog "cog"
[gitdown]: https://github.com/gajus/gitdown "gitdown"
[md-magic]: https://github.com/DavidWells/markdown-magic "markdown-magic"
[remark]: https://github.com/remarkjs/remark "remark + remark-toc + remark-include"

