## JavaPHP

JavaPHP is a lightweight Java library that permits to execute PHP code into Java so instead of using Java servlets,
you can just use PHP and execute it as a part of your existing Java code, does not rely on any dependencies, its fully standalone,
and you can use it in a compiled .jar file or in a simple .java file without having to compile it

Fun fact: this library was made in like 1 day because i had no other ideas and i was bored so yeah, now this library exists.

### Features
- PHP file execution in java
- Customizable error handling

There are not alot of features because i keep it very simple, why would i do a lot of features since its focused only on the purpose of executing PHP in Java.

### How to use
Simply import it to your file and do like this:

```java
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import fr.breadeater.JavaPHP;

public class Main {
    public static void main(String[] args) throws Throwable {
        JavaPHP javaphp = new JavaPHP(false);
        Map<String, String> env = new HashMap<>();

        env.put("REQUEST_METHOD", "GET");

        javaphp.setErrorCallback((error) -> System.out.println("ERROR: " + error));

        javaphp.setPHPVars(env);
        javaphp.run(new File("./test.php").getAbsolutePath());

        System.out.println(javaphp.getResult());
    }
}
```

### Installation
Simply download the .jar / .java file from the repository Releases and import it in your project.<br>
For Maven or Gradle, add it to your local repository.

### Contribution
You can contribute as much as you want, the conditions: 
- know Java.
- use Java JDK 21 or later.
- keep it standalone (means no external dependencies).
- provide least a minimum amount of documentation to the modifications you're doing.
- Make your modifications clean for readability.

### Contact
You can contact me via [mail](mailto:contact@breadeater.fr) or send message in the discussions of the repository

### License
This project is licensed under the MIT license, see [LICENSE](./LICENSE) for more infos.