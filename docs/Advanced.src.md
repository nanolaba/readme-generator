## ${en:'Advanced features', ru:'Расширенные возможности'}

### ${en:'Creating a widget', ru:'Создание виджета'}

Для создания виджета вам нужно реализовать интерфейс `NRGWidget`, или создать<!--ru-->
наследника существующего виджета, например `DefaultWidget`:<!--ru-->
To create a widget, you need to implement the `NRGWidget` interface or <!--en-->
extend an existing widget (e.g., `DefaultWidget`):<!--en-->

${widget:import(path='../nrg/src/test/java/com/nanolaba/nrg/examples/ExampleWidget.java', region='example', wrap='true')}

Теперь этот виджет можно использовать в шаблоне:<!--ru-->
Now you can use the widget in your template:<!--en-->

```markdown
\${widget:exampleWidget(name='World')}
```

Прежде чем запускать генерацию, виджет нужно зарегистрировать в NRG. Это можно сделать двумя способами:<!--ru-->
Before running the generator, the widget must be registered with NRG. There are two ways to do this:<!--en-->

**Вариант 1:** через статический метод `NRG.addWidget`:<!--ru-->
**Option 1:** via the `NRG.addWidget` static method:<!--en-->

```java
NRG.addWidget(new ExampleWidget());
NRG.main("--charset", "UTF-8", "-f", "/path/to/your/file.src.md");
```

**Вариант 2:** через конструктор класса `Generator`, принимающий список виджетов:<!--ru-->
**Option 2:** via the `Generator` class constructor that accepts a widget list:<!--en-->

```java
import com.nanolaba.nrg.core.GenerationResult;
import com.nanolaba.nrg.core.Generator;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

Generator generator = new Generator(
        new File("README.src.md"),
        StandardCharsets.UTF_8,
        Collections.singletonList(new ExampleWidget()));

Collection<GenerationResult> results = generator.getResults();
```

**${en:'Option 3', ru:'Вариант 3'}:** ${en:'register widgets directly from the template via the `nrg.widgets` property, or from the CLI via `--widgets` (with `--classpath` pointing to the JAR that contains the classes):', ru:'регистрация виджетов прямо из шаблона при помощи свойства `nrg.widgets` либо из CLI через `--widgets` (при необходимости добавив JAR-файлы через `--classpath`):'}

```markdown
<!--\@nrg.widgets=com.acme.widgets.Tag,com.acme.widgets.Banner-->
```

```bash
nrg --classpath my-widgets.jar --widgets com.acme.widgets.Tag,com.acme.widgets.Banner -f README.src.md
```

Each widget class must be public and expose a public no-argument<!--en-->
constructor. NRG falls back to a helpful error message on the console<!--en-->
if the class cannot be found, doesn't implement `NRGWidget`, or throws<!--en-->
during instantiation.<!--en-->
Каждый класс-виджет должен быть публичным и иметь публичный конструктор<!--ru-->
без аргументов. Если класс не найден, не реализует `NRGWidget` или выбрасывает<!--ru-->
исключение при создании, NRG выводит понятное сообщение в консоль.<!--ru-->
