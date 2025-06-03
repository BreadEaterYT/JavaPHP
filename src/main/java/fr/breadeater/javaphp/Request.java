package fr.breadeater.javaphp;

import com.sun.net.httpserver.*;

import java.net.InetSocketAddress;

public class Request {
    protected InetSocketAddress address;
    protected Headers headers;
    protected String httpVersion;
    protected String method;
    protected String path;
    protected String body;

    protected boolean https;

    public Request setRequestHttpVersion(String httpVersion){ this.httpVersion = httpVersion; return this; }
    public Request setRequestBody(String requestBody){ this.body = requestBody; return this; }
    public Request setRequestHeaders(Headers headers){ this.headers = headers; return this; }
    public Request setRequestAddress(InetSocketAddress address){ this.address = address; return this; }
    public Request setRequestPath(String path){ this.path = path; return this; }
    public Request setRequestMethod(String method){ this.method = method; return this; }
    public Request setHTTPS(boolean isHTTPS){ this.https = isHTTPS; return this; }

    /**
     * Creates a {@link Request} instance based on {@link HttpExchange}
     * @param exchange The {@link HttpExchange} of an Http Request of Java {@link HttpServer}
     * @return A new a {@link Request} instance containing datas of {@link HttpExchange}
     */
    public static Request parseExchange(HttpExchange exchange){
        Request request = new Request();

        try {
            request.address = exchange.getRemoteAddress();
            request.headers = exchange.getRequestHeaders();
            request.httpVersion = exchange.getProtocol();
            request.method = exchange.getRequestMethod();
            request.path = exchange.getRequestURI().getPath();
            request.body = new String(exchange.getRequestBody().readAllBytes());

            request.https = false;
        } catch (Exception err){
            throw new RuntimeException(err);
        }

        return request;
    }

    /**
     * Creates a {@link Request} instance based on {@link HttpsExchange}
     * @param exchange The {@link HttpsExchange} of an Http Request of Java {@link HttpsServer}
     * @return A new a {@link Request} instance containing datas of {@link HttpsExchange}
     */
    public static Request parseExchange(HttpsExchange exchange){
        Request request = new Request();

        try {
            request.address = exchange.getRemoteAddress();
            request.headers = exchange.getRequestHeaders();
            request.httpVersion = exchange.getProtocol();
            request.method = exchange.getRequestMethod();
            request.path = exchange.getRequestURI().getPath();
            request.body = new String(exchange.getRequestBody().readAllBytes());

            request.https = true;
        } catch (Exception err){
            throw new RuntimeException(err);
        }

        return request;
    }
}
