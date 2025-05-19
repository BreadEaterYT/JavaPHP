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
        Request request = new Request();
        Headers headers = new Headers();

        headers.add("Content-Type", "text/plain");

        request.setRequestMethod("POST");
        request.setRequestPath("/");
        request.setRequestBody("Hello World !");
        request.setRequestHttpVersion("HTTP/1.1");
        request.setRequestAddress(new InetSocketAddress("127.0.0.1", 47829));
        request.setRequestHeaders(headers);
        request.setIsHTTPS(false);

        JavaPHP.RunOptions options = new JavaPHP.RunOptions()
                .setPHPDocumentRoot(new File("./").getAbsolutePath())
                .setPHPFilepath(new File("./index.php").getAbsolutePath())
                .setPHPServerSoftwareName("Java")
                .setPHPServerAddress("127.0.0.1")
                .setPHPServerPort(80);

        javaphp.onError((err) -> {
            try {
                throw err;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Response response = javaphp.run(options, request);

        response.getResultHeaders().forEach((name, value) -> System.out.println(name + ": " + value.getFirst()));

        System.out.println(response.getResultStatusCode());
        System.out.println(response.getResultBody());
    }
}