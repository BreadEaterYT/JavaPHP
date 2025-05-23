package fr.breadeater.javaphp;

import com.sun.net.httpserver.Headers;

import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class JavaPHP {
    private Consumer<Exception> onError = null;
    private InetSocketAddress address;


    private boolean useUnixSocket = false;
    private File unixSocketFile;


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
     * Uses Unix Socket instead of TCP/IP for running PHP files with FastCGI
     *
     * @implNote <strong>Note:</strong> This only works on Linux/WSL using root, won't work under Windows !
     *
     * @param unixSocket Should the Unix Socket be used instead of TCP/IP to run PHP files.<br>
     * @param unixSocketFile The file pointing to the Unix Socket (e.g /run/php/php8.4-fpm.sock or /run/php/php8-fpm.sock).<br>
     */
    public void useUnixSocket(boolean unixSocket, File unixSocketFile){
        try {
            String osname = System.getProperty("os.name").toLowerCase();
            String user = System.getProperty("user.name");

            if (!osname.contains("linux")) throw new UnsupportedOperationException("Unix Socket cannot be used on other Operating Systems than Linux/WSL !");
            if (!user.equals("root")) throw new IllegalAccessException("Unix Socket requires root privileges !");

            this.unixSocketFile = unixSocketFile;
            this.useUnixSocket = unixSocket;
        } catch (Exception error){
            if (this.onError != null){
                this.onError.accept(error);
            } else throw new RuntimeException(error);
        }
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
    public Response run(Options runOptions, Request request){
        Response response = new Response();
        ProtocolFamily protocol = StandardProtocolFamily.INET;
        SocketAddress address = this.address;

        if (this.useUnixSocket){
            if (this.unixSocketFile.exists() && this.validatePHPUnixSocketPath(this.unixSocketFile)){

                address = UnixDomainSocketAddress.of(this.unixSocketFile.toPath());
                protocol = StandardProtocolFamily.UNIX;

            } else {
                throw new IllegalArgumentException("Unix Socket path is invalid or does not exists (file name must be formatted like this: phpX.X-fpm.sock) !");
            }
        }

        Objects.requireNonNull(address);
        Objects.requireNonNull(runOptions);
        Objects.requireNonNull(request);

        try {
            SocketChannel socket = SocketChannel.open(protocol);

            socket.connect(address);

            Map<String, String> fastCGIheaders = FastCGIUtils.setFastCGIParams(runOptions, request);
            OutputStream out = Channels.newOutputStream(socket);
            InputStream in = Channels.newInputStream(socket);

            byte[] reqbody = new byte[0];

            if (request.getRequestBody() != null) reqbody = request.getRequestBody().getBytes();

            out.write(FastCGIUtils.buildRequest(1));
            out.write(FastCGIUtils.buildParams(false, 1, fastCGIheaders));
            out.write(FastCGIUtils.buildParams(true, 1, fastCGIheaders));
            out.write(FastCGIUtils.buildStdin(1, reqbody));
            out.write(FastCGIUtils.buildEmptyStdin(1));

            String fastCGIresponse = FastCGIUtils.parseFastCGIRequest(socket, in);
            String line;

            BufferedReader responseReader = new BufferedReader(new CharArrayReader(fastCGIresponse.toCharArray()));
            StringBuilder body = new StringBuilder();
            Headers headers = new Headers();

            while ((line = responseReader.readLine()) != null){
                if (line.isEmpty()){
                    String bodyLine;

                    while ((bodyLine = responseReader.readLine()) != null) body.append(bodyLine).append("\r\n");
                    break;
                }

                String[] splitheader = line.split(": ", 2);

                headers.add(splitheader[0], splitheader[1]);
            }

            headers.forEach((name, value) -> {
                if (name.equals("Status")){
                    String[] splitStatus = value.get(0).split(" ");

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
     * Checks if the Unix Socket file exists and if the filename of the PHP Unix Socket ends is formatted correctly (phpX-fpm.sock or phpX.X-fpm.sock).
     * @param file The file to be checked.
     * @return True / False depending on if the filename is correctly formatted and if the Unix Socket file exists.
     */
    private boolean validatePHPUnixSocketPath(File file){
        final Pattern SOCKET_PATTERN = Pattern.compile("^php\\d+(\\.\\d+)?-fpm\\.sock$");

        if (file == null) return false;
        if (!file.exists()) return false;

        return SOCKET_PATTERN.matcher(file.getName()).matches();
    }


    /**
     * Options specifies the options to be used when running an PHP file, this is used to specify predefined variables (DOCUMENT_ROOT, SERVERADDR, etc...)
     */
    public static class Options {
        String PHP_FILEPATH;
        String PHP_DOC_ROOT;
        String PHP_SERVERADDR;
        String PHP_SERVERSOFTWARE;
        int PHP_SERVERPORT;

        public Options setPHPDocumentRoot(String php_docroot){ this.PHP_DOC_ROOT = php_docroot; return this; }
        public Options setPHPFilepath(String php_filepath){ this.PHP_FILEPATH = php_filepath; return this; }
        public Options setPHPServerAddress(String php_serveraddr){ this.PHP_SERVERADDR = php_serveraddr; return this; }
        public Options setPHPServerPort(int php_serverport){ this.PHP_SERVERPORT = php_serverport; return this; }
        public Options setPHPServerSoftwareName(String php_serversoftware){ this.PHP_SERVERSOFTWARE = php_serversoftware; return this; }
    }
}