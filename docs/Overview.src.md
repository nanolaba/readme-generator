## ${en:'Overview', ru:'Краткое описание'}

Using ${name}, you can: <!--en-->
С помощью ${name} вы можете: <!--ru-->

- Generate professional README files in multiple languages <!--en-->
- Генерировать README-файлы на нескольких языках <!--ru-->
- Automate documentation with dynamic templates <!--en-->
- Автоматизировать создание документации с помощью динамических шаблонов <!--ru-->
- Create maintainable Markdown with variables and widgets <!--en-->
- Создавать удобную в поддержке Markdown-документацию с переменными и виджетами <!--ru-->
- Streamline GitHub project documentation <!--en-->
- Упрощать создание документации GitHub-проектов <!--ru-->

> 💡 **Example**: This document was generated from [this template](README.src.md). <!--en-->
> Try our **[Quick Start Guide](#quick-start)** to begin!<!--en-->
> 💡 **Пример**: Этот документ был сгенерирован из [этого шаблона](README.src.md). <!--ru-->
> Попробуйте наше **[Руководство по быстрому старту](#quick-start)**, чтобы начать! <!--ru-->

## Key Features <!--en-->

- **Multi-language READMEs** - Support for EN/ZN/RU and any other languages<!--en-->
- **CI drift detection** - the `--check` flag (CLI) and `mode: check` (GitHub Action) fail the build with a unified diff if generated `.md` files drift from the template — so a contributor's hand-edit can never silently land in `main`<!--en-->
- **Smart Variables** - Reusable content blocks<!--en-->
- **Prebuilt Widgets** - Table of contents, file import, TODOs, alerts, badges, and more<!--en-->
- **LaTeX math** - Reliable formula rendering via `$…$` / `$$…$$` or an SVG fallback for places where GitHub's native MathJax breaks<!--en-->
- **Flexible Integration** - CLI, Maven plugin, or Java library <!--en-->
- **Extensibility** - Supports the ability to create custom widgets for content generation <!--en-->

## Ключевые возможности <!--ru-->

- **README на нескольких языках** - Поддержка EN/ZN/RU и любых других языков<!--ru-->
- **Drift-проверка в CI** - флаг `--check` (CLI) и `mode: check` (GitHub Action) валят сборку с unified diff, если сгенерированные `.md` разошлись с шаблоном — никакая ручная правка контрибьютора не залетит молча в `main`<!--ru-->
- **Переменные** - Повторно используемые блоки контента<!--ru-->
- **Готовые виджеты** - Оглавление, импорт файлов, TODO-списки, alert-блоки, бейджи и другие<!--ru-->
- **LaTeX-формулы** - Надёжный рендеринг через `$…$` / `$$…$$` или SVG-фолбэк для случаев, где встроенный GitHub MathJax не справляется<!--ru-->
- **Гибкая интеграция** - CLI, Maven-плагин или Java-библиотека <!--ru-->
- **Расширяемость** - Возможность писать собственные виджеты для генерации контента <!--ru-->

> 💡 ${name} is written in Java and requires **Java 8** or higher to run.<!--en-->
> 💡 ${name} написан на Java и требует для запуска версии **Java 8** и выше.<!--ru-->

The latest stable version of the program is **${stableVersion}**.<!--en-->
The current development version is **${devVersion}**.<!--en-->
Последняя стабильная версия — **${stableVersion}**.<!--ru-->
Текущая версия разработки — **${devVersion}**.<!--ru-->

### ${en:'Used by', ru:'Используется в'}<!--toc.ignore-->

This very README is generated with NRG — see [`README.src.md`](README.src.md). The same template is also used to keep `README.ru.md` in sync.<!--en-->
Этот README сгенерирован самим NRG — см. [`README.src.md`](README.src.md). Тот же шаблон поддерживает в синхроне `README.ru.md`.<!--ru-->

