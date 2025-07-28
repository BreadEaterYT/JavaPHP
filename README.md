## JavaPHP

⚠️⚠️<strong>Warning:</strong> The project is considered done because i finished what i wanted to do, integrate PHP to Java, no updates will be done unless i find another feature to implement or there's a vulnerability⚠️⚠️

JavaPHP is a lightweight Java library that permits to execute PHP code into Java, it does not rely on any dependencies and is fully standalone.

It can be used with Webservers (NanoHTTPD, Java HttpServer, etc...) or as a part of a normal Java project.

- Execute PHP files from Java
- Supports all HTTP methods and headers
- Custom error handling via `Consumer<Exception>`
- Compatible with PHP-FPM or `php-cgi` command

### How to use
- Download the source code and build it and import it <strong>OR</strong> import it directly from my maven repository like this:

```xml
<repositories>
    <repository>
        <id>breadeatercdn</id>
        <url>https://cdn.breadeater.fr/maven</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>fr.breadeater</groupId>
        <artifactId>javaphp</artifactId>
        <version>2.1.1</version>
    </dependency>
</dependencies>
```

- If not using PHP FPM, start a PHP FastCGI server using:

```text
php-cgi -b 127.0.0.1:<port>
```

<strong>Warning:</strong> binding PHP-CGI or PHP-FPM to localhost is recommended for security reasons !

- Run your PHP file in Java using this following example code:

```java
public class JavaPHPTest {
    public static void main(String[] args){
        JavaPHP javaphp = new JavaPHP(new InetSocketAddress("127.0.0.1", 7000)); // Create a JavaPHP instance with your PHP-CGI / PHP-FPM server address as parameter
        Request request = new Request(); // Create a request instance to specify method, body, etc...
        Headers headers = new Headers(); // Create a new Headers instance (if not already created), will be used to specify HTTP headers

        headers.add("Content-Type", "text/plain"); // Sets Content-Type to text/plain

        request.setRequestMethod("POST"); // Sets request method to POST
        request.setRequestPath("/");
        request.setRequestBody("Hello World !"); // Sets 'Hello World !' as body
        request.setRequestHttpVersion("HTTP/1.1");
        request.setRequestAddress(new InetSocketAddress("127.0.0.1", 47829)); // The remote address (basically the user address)
        request.setRequestHeaders(headers); // Gives the headers to the request instance
        request.setIsHTTPS(false); // Sets HTTPS to false for example

        // Specifies the options that will be used when running a PHP file
        // Note: The location of the PHP file is the same location as the PHP-CGI / PHP-FPM working directory, same for Document Root
        JavaPHP.Options options = new JavaPHP.Options()
                .setPHPDocumentRoot(new File("./").getAbsolutePath())
                .setPHPFilepath(new File("./index.php").getAbsolutePath())
                .setPHPServerSoftwareName("Java") // Sets the Server Software name (e.g Apache, Nginx, etc...)
                .setPHPServerAddress("127.0.0.1") // The address where the HTTP server listen to
                .setPHPServerPort(80); // The port where the HTTP server listen to

        // Handles error if a error occurs while running the PHP file
        // In this case we just simply throw the error
        javaphp.onError((err) -> {
            try {
                throw err;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Runs the PHP file and get the Response containing headers, body and status code given by PHP FastCGI
        Response response = javaphp.run(options, request);

        // Prints the results in the terminal
        response.getResultHeaders().forEach((name, value) -> System.out.println(name + ": " + value.getFirst()));

        System.out.println(response.getResultStatusCode());
        System.out.println(response.getResultBody());
    }
}
```

### Contribution
You can contribute as much as you want, contributors will be listed in the README.

NOTE: Only modifications in the Java code counts as contribution.

### Contributors
(No contributors yet)

### License
This project is licensed under the MIT license, see [LICENSE](./LICENSE) for more infos.
