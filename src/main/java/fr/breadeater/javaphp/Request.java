package fr.breadeater.javaphp;

import com.sun.net.httpserver.Headers;

import java.net.InetSocketAddress;

public class Request {
    private InetSocketAddress address;
    private Headers headers;
    private String httpVersion;
    private String method;
    private String path;
    private String body;

    private boolean https;

    public Request setRequestHttpVersion(String httpVersion){ this.httpVersion = httpVersion; return this; }
    public Request setRequestBody(String requestBody){ this.body = requestBody; return this; }
    public Request setRequestHeaders(Headers headers){ this.headers = headers; return this; }
    public Request setRequestAddress(InetSocketAddress address){ this.address = address; return this; }
    public Request setRequestPath(String path){ this.path = path; return this; }
    public Request setRequestMethod(String method){ this.method = method; return this; }
    public Request setIsHTTPS(boolean isHTTPS){ this.https = isHTTPS; return this; }

    public String getRequestHttpVersion(){ return this.httpVersion; }
    public String getRequestBody(){ return this.body; }
    public Headers getRequestHeaders(){ return this.headers; }
    public InetSocketAddress getRequestAddress(){ return this.address; }
    public String getRequestMethod(){ return this.method; }
    public String getRequestPath(){ return this.path; }

    public boolean isHttps(){ return this.https; }
}
