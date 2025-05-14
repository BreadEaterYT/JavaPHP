package fr.breadeater.javaphp;

enum Status {
    FCGI_VERSION(1),
    FCGI_BEGIN_REQUEST(1),
    FCGI_PARAMS(4),
    FCGI_STDIN(5),
    FCGI_RESPONDER(1),
    FCGI_STDOUT(6),
    FCGI_END_REQUEST(3);

    public final int code;

    Status(int code){
        this.code = code;
    }

    public int getRequestStatusCode(){
        return this.code;
    }
}
