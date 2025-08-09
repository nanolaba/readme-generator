## ${en:'Advanced features', ru:'Расширенные возможности'}

### ${en:'Creating a widget', ru:'Создание виджета'}

Для создания виджета вам нужно реализовать интерфейс `NRGWidget`, или создать<!--ru-->
наследника существующего виджета, например `DefaultWidget`:<!--ru-->
To create a widget, you need to implement the `NRGWidget` interface or <!--en-->
extend an existing widget (e.g., `DefaultWidget`):<!--en-->

```java
${widget:import(path='../nrg/src/test/java/com/nanolaba/nrg/examples/ExampleWidget.java')}
```

Теперь вы можете использовать виджет в шаблоне:<!--ru-->
Now you can use the widget in your template:<!--en-->

```markdown
\${widget:exampleWidget(name='World')}
```

Теперь необходимо запустить программу и заставить ее использовать новый шаблон. Для этого есть два варианта: <!--ru-->
Now you need to launch the program and make it use the new template. There are two ways to do this:<!--en-->

**Вариант 1:** Использование статического метода класса NRG:<!--ru-->
**Option 1:** Using the NRG class static method:<!--en-->

```javascript
NRG.addWidget(new ExampleWidget());
NRG.main("--charset", "UTF-8", "-f", "/path/to/your/file.src.md");
```

**Вариант 2:** Использование класса `Generator` и передача списка виджетов в конструктор:<!--ru-->
**Option 2:** Use the Generator class and pass the widget list to its constructor:<!--en-->

```java
Generator generator = new Generator(new File("README.src.md"),
        "${widget:exampleWidget(name='World')}",
        Collections.singletonList(new ExampleWidget()));

Collection<GenerationResult> results = generator.getResults();
```
