package fr.breadeater.javaphp;

import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class JavaPHP {
    private static ExecutorService threadPool = Executors.newCachedThreadPool();

    private SocketChannel socketChannel;
    private Consumer<Exception> onError = null;
    private InetSocketAddress address;
    private File unixSocketFile;

    private boolean useThreadPool = false;


    /**
     * Creates a new JavaPHP instance.
     *
     * <p>JavaPHP communicates with a PHP FastCGI / PHP FPM server over TCP socket.</p>
     *
     * @param address The {@link InetSocketAddress} of the PHP FastCGI / PHP-FPM server. {@link InetSocketAddress} with 127.0.0.1 as hostname is <strong>STRONGLY</strong> recommended.
     *
     * @implNote <strong>Note:</strong> Make sure you start a PHP FastCGI / PHP FPM server (e.g., using <code>php-cgi -b 127.0.0.1:PORT</code> or use PHP FPM server)
     * and bind it to <code>localhost</code> to avoid exposing it to external connections.
     */
    public JavaPHP(InetSocketAddress address) throws Exception {
        this.socketChannel = SocketChannel.open(StandardProtocolFamily.INET);
        this.address = address;
    }


    /**
     * Should this JavaPHP instance use a Thread Pool to run PHP scripts ?
     *
     * @param useThreadPool Enable / Disable the usage of a Thread Pool by this instance of JavaPHP.
     *
     * @apiNote <strong>Note: </strong> By default: the thread pool is a {@link Executors#newCachedThreadPool()}, to change it, use {@link #setThreadPool(ExecutorService threadPool)} function.
     */
    public void useThreadPool(boolean useThreadPool){
        this.useThreadPool = useThreadPool;
    }


    /**
     * Sets a new Thread Pool that all JavaPHP instances can use.
     * @param threadPool The Thread Pool to use.
     */
    public void setThreadPool(ExecutorService threadPool){
        JavaPHP.threadPool = threadPool;
    }


    /**
     * Uses Unix Socket instead of TCP/IP to communicate with PHP FastCGI (Linux only)
     * @param unixSocketFile The Unix Socket file to use.
     * @throws IllegalAccessException If the current user is not root.
     * @throws UnsupportedOperationException If the current OS is not Linux.
     * @throws IOException If an error happens when openning the {@link SocketChannel}.
     */
    public void useUnixSocket(File unixSocketFile) throws IllegalAccessException, IOException {
        String osname = System.getProperty("os.name").toLowerCase();
        String user = System.getProperty("user.name");

        if (!osname.contains("linux")) throw new UnsupportedOperationException("Unix Socket cannot be used on other Operating Systems than Linux/WSL !");
        if (!user.equals("root")) throw new IllegalAccessException("Unix Socket requires root privileges !");

        this.socketChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
        this.unixSocketFile = unixSocketFile;
    }


    /**
     * Sets a {@link Consumer} function to execute when an error is being thrown.
     * @param callback The function to be executed if an error is thrown.
     */
    public void onError(Consumer<Exception> callback){
        this.onError = callback;
    }


    /**
     * Runs a PHP file on the PHP FastCGI server and returns result.
     *
     * @param options Options to use when running an PHP file, basically specifies FastCGI params (e.g: REMOTE_ADDR, HTTPS, etc...)
     * @param request An instance of {@link Request} containing Headers, Body, etc...
     */
    public Response run(Options options, Request request) throws Exception {
        Callable<Response> runLogic = () -> {
            try {
                Map<String, String> fastCGIheaders = JavaPHPUtils.setFastCGIParams(options, request);
                String reqbody = "";

                if (request.body != null) reqbody = request.body;

                if (this.unixSocketFile != null){
                    if (!this.unixSocketFile.exists()) throw new IllegalArgumentException("Unix Socket path is invalid or does not exists (file name must be formatted like this: phpX.X-fpm.sock) !");

                    this.socketChannel.connect(UnixDomainSocketAddress.of(this.unixSocketFile.getCanonicalPath()));
                } else this.socketChannel.connect(this.address);

                this.socketChannel.write(JavaPHPUtils.buildRequest());
                this.socketChannel.write(JavaPHPUtils.buildParams(false, fastCGIheaders));
                this.socketChannel.write(JavaPHPUtils.buildParams(true, fastCGIheaders));
                this.socketChannel.write(JavaPHPUtils.buildStdin(reqbody.getBytes()));
                this.socketChannel.write(JavaPHPUtils.buildStdin(new byte[0]));

                BufferedReader responseReader = JavaPHPUtils.parseFastCGIRequest(this.socketChannel);

                return JavaPHPUtils.parseResponse(responseReader);
            } catch (Exception err){
                if (this.onError != null) this.onError.accept(err);

                return null;
            }
        };

        if (this.useThreadPool){
            return JavaPHP.threadPool.submit(runLogic).get();
        } else return runLogic.call();
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