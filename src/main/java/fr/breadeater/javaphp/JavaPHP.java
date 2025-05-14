package fr.breadeater.javaphp;

import com.sun.net.httpserver.Headers;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class JavaPHP {
    private Consumer<Exception> onError = null;
    private InetSocketAddress address;

    /**
     * Creates a new JavaPHP instance.
     *
     * <p>JavaPHP communicates with a PHP FastCGI / PHP FPM server over TCP socket.</p>
     *
     * @param address The {@link InetSocketAddress} of the PHP FastCGI / PHP FPM server. {@link InetSocketAddress} with 127.0.0.1 as hostname is <strong>STRONGLY</strong> recommended.
     *
     * @implNote <strong>Note:</strong> Unix sockets are not supported. Make sure you start a PHP FastCGI / PHP FPM server (e.g., using <code>php-cgi -b 127.0.0.1:PORT</code> or use PHP FPM server)
     * and bind it to <code>localhost</code> to avoid exposing it to external connections.
     */
    public JavaPHP(InetSocketAddress address){
        this.address = address;
    }

    /**
     * Sets a {@link Consumer} function to execute when an error is being thrown.
     *
     * @param callback The function to be executed if an error is thrown.
     */
    public void onError(Consumer<Exception> callback){
        this.onError = callback;
    }

    /**
     * Runs a PHP file on the PHP FastCGI server and returns result.
     *
     * @param runOptions Options to use when running an PHP file, basically specifies FastCGI params (e.g <code>$_SERVER["REMOTE_ADDR"]</code>)
     * @param request An instance of {@link Request} containing Headers, Body, etc...
     */
    public Response run(RunOptions runOptions, Request request){
        Socket socket = new Socket();
        Response response = new Response();

        if (runOptions == null) throw new IllegalArgumentException("runOptions must not be null !");
        if (request == null) throw new IllegalArgumentException("request must not be null !");

        try {
            socket.connect(this.address);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            Map<String, String> fastCGIheaders = new HashMap<>();
            Headers reqHeaders = request.getRequestHeaders();
            String httpsEnabled = "off";

            if (request.isHttps()) httpsEnabled = "on";

            String[] splittedURI = request.getRequestPath().split("\\?", 2);

            if (splittedURI.length == 2) fastCGIheaders.put("QUERY_STRING", splittedURI[1]);

            fastCGIheaders.put("SCRIPT_FILENAME", runOptions.PHP_FILEPATH);
            fastCGIheaders.put("GATEWAY_INTERFACE", "CGI/1.1");
            fastCGIheaders.put("SERVER_PROTOCOL", request.getRequestHttpVersion());
            fastCGIheaders.put("REQUEST_METHOD", request.getRequestMethod());
            fastCGIheaders.put("SCRIPT_NAME", splittedURI[0]);
            fastCGIheaders.put("REQUEST_URI", request.getRequestPath());
            fastCGIheaders.put("DOCUMENT_ROOT", runOptions.PHP_DOC_ROOT);
            fastCGIheaders.put("SERVER_SOFTWARE", runOptions.PHP_SERVERSOFTWARE);
            fastCGIheaders.put("REMOTE_ADDR", request.getRequestAddress().getHostName());
            fastCGIheaders.put("REMOTE_PORT", Integer.toString(request.getRequestAddress().getPort()));
            fastCGIheaders.put("SERVER_ADDR", runOptions.PHP_SERVERADDR);
            fastCGIheaders.put("SERVER_PORT", Integer.toString(runOptions.PHP_SERVERPORT));
            fastCGIheaders.put("HTTPS", httpsEnabled);

            for (Map.Entry<String, List<String>> entry : reqHeaders.entrySet()){
                String key = entry.getKey();
                String value = entry.getValue().getFirst();

                if (key.equals("Content-Length")) continue;

                if (key.equals("Content-Type")){
                    fastCGIheaders.put("CONTENT_TYPE", value);
                    continue;
                }

                if (request.getRequestBody() != null){
                    int reqBodyLength = request.getRequestBody().getBytes().length;

                    fastCGIheaders.put("CONTENT_LENGTH", Integer.toString(reqBodyLength));
                    continue;
                }

                fastCGIheaders.put("HTTP_" + key.replaceAll("-", "_").toUpperCase(), value);
            }

            byte[] reqbody = new byte[0];

            if (request.getRequestBody() != null) reqbody = request.getRequestBody().getBytes();

            out.write(FastCGIUtils.buildRequest(1));
            out.write(FastCGIUtils.buildParams(false, 1, fastCGIheaders));
            out.write(FastCGIUtils.buildParams(true, 1, fastCGIheaders));
            out.write(FastCGIUtils.buildStdin(1, reqbody));

            String fastCGIresponse = FastCGIUtils.parseFastCGIRequest(in);
            String line;

            BufferedReader responseReader = new BufferedReader(new CharArrayReader(fastCGIresponse.toCharArray()));
            StringBuilder body = new StringBuilder();
            Headers headers = new Headers();

            boolean parseBody = false;

            while ((line = responseReader.readLine()) != null){
                if (line.isEmpty()) parseBody = true;

                if (!parseBody){
                    String[] splitheader = line.split(": ", 2);

                    headers.add(splitheader[0], splitheader[1]);
                    continue;
                }

                body.append(line).append("\r\n");
            }

            headers.forEach((name, value) -> {
                if (name.equals("Status")){
                    String[] splitStatus = value.getFirst().split(" ");

                    response.statuscode = Integer.parseInt(splitStatus[0]);
                }
            });

            headers.remove("Status");

            response.headers = headers;
            response.body = body.toString();
        } catch (Exception err){
            if (this.onError != null) this.onError.accept(err);

            return null;
        }

        return response;
    }

    /**
     * RunOptions specifies the options to be used when running an PHP file, this is used to specify predefined variables (DOCUMENT_ROOT, SERVERADDR, etc...)
     */
    public static class RunOptions {
        private String PHP_FILEPATH;
        private String PHP_DOC_ROOT;
        private String PHP_SERVERADDR;
        private String PHP_SERVERSOFTWARE;
        private int PHP_SERVERPORT;

        public RunOptions setPHPDocumentRoot(String php_docroot){ this.PHP_DOC_ROOT = php_docroot; return this; }
        public RunOptions setPHPFilepath(String php_filepath){ this.PHP_FILEPATH = php_filepath; return this; }
        public RunOptions setPHPServerAddress(String php_serveraddr){ this.PHP_SERVERADDR = php_serveraddr; return this; }
        public RunOptions setPHPServerPort(int php_serverport){ this.PHP_SERVERPORT = php_serverport; return this; }
        public RunOptions setPHPServerSoftwareName(String php_serversoftware){ this.PHP_SERVERSOFTWARE = php_serversoftware; return this; }
    }
}