## JavaPHP

JavaPHP is a lightweight Java library that permits to execute PHP code into Java so instead of using Java servlets,
you can just use PHP and execute it as a part of your existing Java code, does not rely on any dependencies, its fully standalone,
and you can use it in a compiled .jar file or in a simple .java file without having to compile it.

It can be used with Webservers (NanoHTTPD, Java socket, etc...) or as a part of your Java app.

### Features
- PHP file / code execution in java
- Customizable error handling

### Planned features
- PHP file / code execution over TCP/IP using PHP-CGI

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
        javaphp.runWithCli("echo 'Hello World !';");

        System.out.println(javaphp.getResult());
        System.out.println(javaphp.getInlineResult());
    }
}
```

### Installation
Simply download the .jar / .java file from the repository Releases and import it in your project.<br>
For Maven or Gradle, add it to your local repository.

### Contribution
You can contribute as much as you want, contributors will be listed in the README.

NOTE: Only modifications in the Java code counts as contribution.

### Contributors
(No contributors yet)

### License
This project is licensed under the MIT license, see [LICENSE](./LICENSE) for more infos.