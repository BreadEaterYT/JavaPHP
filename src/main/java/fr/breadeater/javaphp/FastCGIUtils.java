package fr.breadeater.javaphp;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

class FastCGIUtils {
    public static byte[] buildRecord(Status type, int requestId, byte[] content){
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

    public static byte[] encodeLength(int length){
        if (length < 128) return new byte[]{ (byte) length };

        return new byte[]{
                (byte) ((length >> 24) | 0x80),
                (byte) (length >> 16),
                (byte) (length >> 8),
                (byte) length
        };
    }

    public static byte[] buildParams(boolean empty, int requestId, Map<String, String> params) throws Exception {
        if (empty) return buildRecord(Status.FCGI_PARAMS, requestId, new byte[0]);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (Map.Entry<String, String> entry : params.entrySet()){
            byte[] key = entry.getKey().getBytes(StandardCharsets.UTF_8);
            byte[] value = entry.getValue().getBytes(StandardCharsets.UTF_8);

            out.write(encodeLength(key.length));
            out.write(encodeLength(value.length));
            out.write(key);
            out.write(value);
        }

        return buildRecord(Status.FCGI_PARAMS, requestId, out.toByteArray());
    }

    public static byte[] buildStdin(int requestId, byte[] body){
        return buildRecord(Status.FCGI_STDIN, requestId, body);
    }

    public static byte[] buildEmptyStdin(int requestId){
        return buildRecord(Status.FCGI_STDIN, requestId, new byte[0]);
    }

    public static byte[] buildRequest(int requestId){
        byte[] body = new byte[8];

        body[0] = 0;
        body[1] = (byte) Status.FCGI_RESPONDER.getRequestStatusCode();
        body[2] = 0;

        return buildRecord(Status.FCGI_BEGIN_REQUEST, requestId, body);
    }

    public static String parseFastCGIRequest(InputStream in) throws Exception {
        String result = null;

        while (true){
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

            if (type == Status.FCGI_STDOUT.getRequestStatusCode() && contentLength > 0) result = new String(content, StandardCharsets.UTF_8);
            if (type == Status.FCGI_END_REQUEST.getRequestStatusCode()) break;
        }

        return result;
    }
}
