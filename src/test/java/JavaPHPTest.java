import com.sun.net.httpserver.Headers;

import fr.breadeater.javaphp.JavaPHP;
import fr.breadeater.javaphp.Request;
import fr.breadeater.javaphp.Response;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * This class is used to test the library
 */
public class JavaPHPTest {
    public static void main(String[] args) throws Exception {
        JavaPHP javaphp = new JavaPHP(new InetSocketAddress("127.0.0.1", 7000));
        Headers headers = new Headers();

        javaphp.useThreadPool(true);
        javaphp.setThreadPool(Executors.newFixedThreadPool(6));

        javaphp.onError((err) -> {
            try {
                throw err;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        headers.add("Content-Type", "text/plain");

        Request request = new Request()
                .setRequestMethod("POST")
                .setRequestPath("/")
                .setRequestBody("Hello World !")
                .setRequestHttpVersion("HTTP/1.1")
                .setRequestAddress(new InetSocketAddress("127.0.0.1", 47829))
                .setRequestHeaders(headers)
                .setHTTPS(false);

        // ... or use Request.parseExchange() instead of setting everything manually if using Java HttpServer / HttpsServer

        JavaPHP.Options options = new JavaPHP.Options()
                .setPHPDocumentRoot(new File("./").getAbsolutePath())
                .setPHPFilepath(new File("./index.php").getAbsolutePath())
                .setPHPServerSoftwareName("Java")
                .setPHPServerAddress("127.0.0.1")
                .setPHPServerPort(80);

        Response response = javaphp.run(options, request);

        response.getHeaders().forEach((name, value) -> System.out.println(name + ": " + value.get(0)));

        System.out.println(response.getStatus());
        System.out.println(response.getBody());
    }
}