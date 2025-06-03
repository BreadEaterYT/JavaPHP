package fr.breadeater.javaphp;

import com.sun.net.httpserver.Headers;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class JavaPHPUtils {
    /**
     * Builds record and returns it
     *
     * @param type    The record type
     * @param content The content
     * @return The built record
     */
    protected static byte[] buildRecord(int type, byte[] content){
        int length = content.length;
        int padding = (8 - (length % 8)) % 8;

        ByteBuffer buffer = ByteBuffer.allocate(8 + length + padding);

        buffer.put((byte) 1);
        buffer.put((byte) type);

        buffer.putShort((short) 1);
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
    protected static byte[] encodeLength(int length){
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
     *
     * @param empty  Should it sends Empty Params
     * @param params
     * @return
     * @throws Exception
     */
    protected static ByteBuffer buildParams(boolean empty, Map<String, String> params) throws Exception {
        if (empty) return ByteBuffer.wrap(buildRecord(4, new byte[0]));

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

        return ByteBuffer.wrap(buildRecord(4, out.toByteArray()));
    }


    /**
     * Builds request and returns it
     *
     * @return The request
     */
    protected static ByteBuffer buildRequest(){
        byte[] body = new byte[8];

        body[0] = 0;
        body[1] = (byte) 1;
        body[2] = 0;

        return ByteBuffer.wrap(buildRecord(1, body));
    }


    /**
     * Builds stdin and returns it
     *
     * @param content The content
     */
    protected static ByteBuffer buildStdin(byte[] content){
        return ByteBuffer.wrap(buildRecord(5, content));
    }


    /**
     * Parses the FastCGI response and return its body
     * @param client The client socket
     * @return The body of the response
     */
    protected static BufferedReader parseFastCGIRequest(SocketChannel client) throws Exception {
        String result = "";

        InputStream in = Channels.newInputStream(client);

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

            if (type == 6 && contentLength > 0){
                result = new String(content, StandardCharsets.UTF_8);

                client.close();
                break;
            }

            if (type == 3){
                client.close();
                break;
            }
        }

        return new BufferedReader(new CharArrayReader(result.toCharArray()));
    }


    /**
     * Sets all FastCGI params and returns a Map containing all the params
     * @param runOptions The JavaPHP options to run a PHP file
     * @param request An Http Request instance containing request method, request headers, etc...
     * @return The Map containing all the params
     */
    protected static Map<String, String> setFastCGIParams(JavaPHP.Options runOptions, Request request) {
        Map<String, String> fastCGIheaders = new HashMap<>();
        Headers reqHeaders = request.headers;
        String httpsEnabled = "off";

        String[] splittedURI = request.path.split("\\?", 2);

        if (splittedURI.length == 2) fastCGIheaders.put("QUERY_STRING", splittedURI[1]);
        if (request.https) httpsEnabled = "on";

        fastCGIheaders.put("SCRIPT_FILENAME", runOptions.PHP_FILEPATH);
        fastCGIheaders.put("GATEWAY_INTERFACE", "CGI/1.1");
        fastCGIheaders.put("SERVER_PROTOCOL", request.httpVersion);
        fastCGIheaders.put("REQUEST_METHOD", request.method);
        fastCGIheaders.put("SCRIPT_NAME", splittedURI[0]);
        fastCGIheaders.put("REQUEST_URI", request.path);
        fastCGIheaders.put("DOCUMENT_ROOT", runOptions.PHP_DOC_ROOT);
        fastCGIheaders.put("SERVER_SOFTWARE", runOptions.PHP_SERVERSOFTWARE);
        fastCGIheaders.put("REMOTE_ADDR", request.address.getHostName());
        fastCGIheaders.put("REMOTE_PORT", Integer.toString(request.address.getPort()));
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

            if (request.body != null){
                int reqBodyLength = request.body.getBytes().length;

                fastCGIheaders.put("CONTENT_LENGTH", Integer.toString(reqBodyLength));
                continue;
            }

            fastCGIheaders.put("HTTP_" + key.replaceAll("-", "_").toUpperCase(), value);
        }

        return fastCGIheaders;
    }

    protected static Response parseResponse(BufferedReader reader) throws IOException {
        StringBuilder body = new StringBuilder();
        Headers headers = new Headers();
        AtomicInteger status = new AtomicInteger(200);

        String line;

        while ((line = reader.readLine()) != null){
            if (line.isEmpty()){
                String bodyLine;

                while ((bodyLine = reader.readLine()) != null) body.append(bodyLine).append("\r\n");
                break;
            }

            String[] splitheader = line.split(": ", 2);

            headers.add(splitheader[0], splitheader[1]);
        }

        headers.forEach((name, value) -> {
            if (name.equals("Status")){
                String[] splitStatus = value.get(0).split(" ");

                status.set(Integer.parseInt(splitStatus[0]));
            }
        });

        headers.remove("Status");

        return new Response(headers, body.toString(), status.get());
    }
}
