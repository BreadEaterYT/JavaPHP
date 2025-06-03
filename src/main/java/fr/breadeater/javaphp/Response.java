package fr.breadeater.javaphp;

import com.sun.net.httpserver.Headers;

public class Response {
    private final Headers headers;
    private final String body;
    private final int status;

    protected Response(Headers headers, String body, int status){
        this.headers = headers;
        this.body = body;
        this.status = status;
    }

    public Headers getHeaders(){ return this.headers; }
    public String getBody(){ return this.body; }
    public int getStatus(){ return this.status; }
}