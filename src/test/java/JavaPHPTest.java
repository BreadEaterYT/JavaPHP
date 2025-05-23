import com.sun.net.httpserver.Headers;

import fr.breadeater.javaphp.JavaPHP;
import fr.breadeater.javaphp.Request;
import fr.breadeater.javaphp.Response;

import java.io.File;
import java.net.InetSocketAddress;

/**
 * The Main class is used to test the library
 */

public class JavaPHPTest {
    public static void main(String[] args){
        JavaPHP javaphp = new JavaPHP(new InetSocketAddress("127.0.0.1", 7000));
        Headers headers = new Headers();

        javaphp.onError((err) -> {
            try {
                throw err;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // javaphp.useUnixSocket(true, new File("/run/php/php8.4-fpm.sock"));

        headers.add("Content-Type", "text/plain");

        Request request = new Request()
                .setRequestMethod("POST")
                .setRequestPath("/")
                .setRequestBody("Hello World !")
                .setRequestHttpVersion("HTTP/1.1")
                .setRequestAddress(new InetSocketAddress("127.0.0.1", 47829))
                .setRequestHeaders(headers)
                .setHTTPS(false);

        JavaPHP.Options options = new JavaPHP.Options()
                .setPHPDocumentRoot(new File("./").getAbsolutePath())
                .setPHPFilepath(new File("./index.php").getAbsolutePath())
                .setPHPServerSoftwareName("Java")
                .setPHPServerAddress("127.0.0.1")
                .setPHPServerPort(80);

        Response response = javaphp.run(options, request);

        response.getHeaders().forEach((name, value) -> System.out.println(name + ": " + value.get(0)));

        System.out.println(response.getStatusCode());
        System.out.println(response.getBody());
    }
}