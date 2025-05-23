package fr.breadeater.javaphp;

import com.sun.net.httpserver.Headers;

public class Response {
    protected int statuscode = 200;
    protected Headers headers;
    protected String body;

    public int getStatusCode(){ return this.statuscode; }
    public Headers getHeaders(){ return this.headers; }
    public String getBody(){ return this.body; }
}
