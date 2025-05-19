package fr.breadeater.javaphp;

import com.sun.net.httpserver.Headers;

public class Response {
    protected int statuscode = 200;
    protected Headers headers;
    protected String body;

    public int getResultStatusCode(){ return this.statuscode; }
    public Headers getResultHeaders(){ return this.headers; }
    public String getResultBody(){ return this.body; }
}
