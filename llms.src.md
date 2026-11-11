<!--@nrg.languages=en-->
<!--@nrg.fileNamePattern=<base>.txt-->
<!--@nrg.noHeader=true-->
<!--@nrg.pom.path=pom.xml-->
<!--@repo=https://github.com/nanolaba/readme-generator-->
<!--@raw=https://raw.githubusercontent.com/nanolaba/readme-generator/main-->
<!--nrg.ignore.begin-->
This template generates llms.txt (https://llmstxt.org): edit it here and regenerate with
`nrg -f llms.src.md`; llms.txt itself carries no header comment because the format starts with
the project name.
<!--nrg.ignore.end-->
# Nanolaba Readme Generator (NRG)

> Java 8+ tool that generates README.md, README.ru.md and any other localized Markdown file from one `*.src.md` template, and fails CI when the generated files drift from it. Ships as a CLI, a Maven plugin, a GitHub Action and a Java library. Latest release: ${pom.properties.stableVersion}.

Templates are `*.src.md` files; the `*.md` files they produce are build output and must not be edited by hand. Validate a template with `nrg --validate -f README.src.md`, regenerate with `nrg -f README.src.md`, find the template line behind a generated line with `nrg --locate README.md:LINE`. Add `--format json` for machine-readable results.

## Docs

- [Syntax card](${raw}/skills/nrg/SYNTAX.md): the whole template syntax and every widget's parameters on one page
- [Using NRG with AI coding agents](${repo}#using-nrg-with-ai-coding-agents): the validate / generate / check loop, an AGENTS.md block, edit-guard hooks, the nrgw wrapper
- [README](${raw}/README.md): full documentation
- [Template syntax](${repo}#template-syntax): languages, properties, escapes, ignore and frozen regions
- [Widgets](${repo}#widgets): import, tableOfContents, if, details, alert, badge, date, todo, math, exec, fileTree, languages
- [Command line](${repo}#using-the-command-line-interface): every CLI option, exit codes, JSON output

## Optional

- [Agent Skill](${raw}/skills/nrg/SKILL.md): the NRG skill for Claude Code and other Agent Skills hosts
- [Maven plugin](${repo}#use-as-maven-plugin)
- [GitHub Action](${repo}#use-as-a-github-action)
- [Changelog](${repo}#changelog)
