## JavaPHP

JavaPHP is a lightweight Java library that permits to execute PHP code into Java so instead of using Java servlets,
you can just use PHP and execute it as a part of your existing Java code, does not rely on any dependencies, its fully standalone,
and you can use it in a compiled .jar file or in a simple .java file without having to compile it

### Features
- PHP file execution in java
- Customizable error handling

### How to use
Simply import it to your file and do like this:
```java
import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
Simply download the .jar / .java file from the repository Releases and import it in your project

To import the .java file, simply drag it into your project folder.<br>
To import the .jar file, simply use Maven / Gradle and add new dependency to your configuration that points to the .jar file

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