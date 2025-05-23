package fr.breadeater.javaphp;

import com.sun.net.httpserver.Headers;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FastCGIUtils {
    /**
     * Builds record and returns it
     * @param type The record type
     * @param requestId The request ID
     * @param content The content
     * @return The built record
     */
    static byte[] buildRecord(Status type, int requestId, byte[] content){
        int length = content.length;
        int padding = (8 - (length % 8)) % 8;

        ByteBuffer buffer = ByteBuffer.allocate(8 + length + padding);

        buffer.put((byte) Status.FCGI_VERSION.getRequestStatusCode());
        buffer.put((byte) type.getRequestStatusCode());

        buffer.putShort((short) requestId);
        buffer.putShort((short) length);

        buffer.put((byte) padding);
        buffer.put((byte) 0);
        buffer.put(content);

        if (padding > 0) buffer.put(new byte[padding]);

        return buffer.array();
    }


    /**
     * Encodes byte array length to be sent to FastCGI server
     * @param length The length to be encoded.
     * @return A byte array containing the encoded length
     */
    static byte[] encodeLength(int length){
        if (length < 128) return new byte[]{ (byte) length };

        return new byte[]{
                (byte) ((length >> 24) | 0x80),
                (byte) (length >> 16),
                (byte) (length >> 8),
                (byte) length
        };
    }


    /**
     * Build params and sends them to FastCGI server
     * @param empty Should it sends Empty Params
     * @param requestId
     * @param params
     * @return
     * @throws Exception
     */
    static byte[] buildParams(boolean empty, int requestId, Map<String, String> params) throws Exception {
        if (empty) return buildRecord(Status.FCGI_PARAMS, requestId, new byte[0]);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (Map.Entry<String, String> entry : params.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null) continue;

            out.write(encodeLength(key.getBytes(StandardCharsets.UTF_8).length));
            out.write(encodeLength(value.getBytes(StandardCharsets.UTF_8).length));
            out.write(key.getBytes(StandardCharsets.UTF_8));
            out.write(value.getBytes(StandardCharsets.UTF_8));
        }

        return buildRecord(Status.FCGI_PARAMS, requestId, out.toByteArray());
    }


    /**
     * Builds stdin containing a byte array and returns it
     * @param requestId The request ID
     * @param body
     * @return The built stdin
     */
    static byte[] buildStdin(int requestId, byte[] body){
        return buildRecord(Status.FCGI_STDIN, requestId, body);
    }


    /**
     * Builds a empty stdin and returns it
     * @param requestId The request ID
     * @return The built stdin
     */
    static byte[] buildEmptyStdin(int requestId){
        return buildRecord(Status.FCGI_STDIN, requestId, new byte[0]);
    }


    /**
     * Builds request and returns it
     * @param requestId The request ID
     * @return The request
     */
    static byte[] buildRequest(int requestId){
        byte[] body = new byte[8];

        body[0] = 0;
        body[1] = (byte) Status.FCGI_RESPONDER.getRequestStatusCode();
        body[2] = 0;

        return buildRecord(Status.FCGI_BEGIN_REQUEST, requestId, body);
    }


    /**
     * Parses the FastCGI response and return its body
     * @param client The client socket
     * @param in The input stream of the client
     * @return The body of the response
     */
    static String parseFastCGIRequest(SocketChannel client, InputStream in) throws Exception {
        String result = null;

        while (client.isOpen()){
            int version = in.read();
            int type = in.read();
            int requestIdHigh = in.read();
            int requestIdLow = in.read();
            int contentLengthHigh = in.read();
            int contentLengthLow = in.read();
            int padding = in.read();

            in.read();

            if (version == -1) break;

            int contentLength = (contentLengthHigh << 8) | contentLengthLow;
            byte[] content = in.readNBytes(contentLength);

            if (padding > 0) in.skip(padding);

            if (type == Status.FCGI_STDOUT.getRequestStatusCode() && contentLength > 0){
                result = new String(content, StandardCharsets.UTF_8);

                client.close();
                break;
            }

            if (type == Status.FCGI_END_REQUEST.getRequestStatusCode()){
                client.close();
                break;
            }
        }

        return result;
    }


    /**
     * Sets all FastCGI params and returns a Map containing all the params
     * @param runOptions The JavaPHP options to run a PHP file
     * @param request An Http Request instance containing request method, request headers, etc...
     * @return The Map containing all the params
     */
    static Map<String, String> setFastCGIParams(JavaPHP.Options runOptions, Request request) {
        Map<String, String> fastCGIheaders = new HashMap<>();
        Headers reqHeaders = request.getRequestHeaders();
        String httpsEnabled = "off";

        String[] splittedURI = request.getRequestPath().split("\\?", 2);

        if (splittedURI.length == 2) fastCGIheaders.put("QUERY_STRING", splittedURI[1]);
        if (request.isHttps()) httpsEnabled = "on";

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
            String value = entry.getValue().get(0);

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

        return fastCGIheaders;
    }
}
