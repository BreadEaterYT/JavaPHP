package fr.breadeater;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

public class JavaPHP {
    // Private immutable variables
    private final String PHP_BIN_PATH;
    private final boolean NO_PHP_WARN;

    // Private variables
    private Consumer<String> ERR_CALLBACK;
    private Map<String, String> env_vars;
    private String inlineResponse = null;
    private String response = null;

    /**
     * Creates a JavaPHP instance
     * @param php_bin_path PHP binary file path (e.g /usr/bin/php)
     * @param ignorePHPWarnings Specifies if PHP Warning and PHP Startup errors should be ignored
     */
    public JavaPHP(String php_bin_path, boolean ignorePHPWarnings){
        this.PHP_BIN_PATH = php_bin_path;
        this.NO_PHP_WARN = ignorePHPWarnings;
    }

    /**
     * Creates a JavaPHP instance
     * @param ignorePHPWarnings Specifies if PHP Warning and PHP Startup errors should be ignored
     */
    public JavaPHP(boolean ignorePHPWarnings){
        this.PHP_BIN_PATH = "php";
        this.NO_PHP_WARN = ignorePHPWarnings;
    }

    /**
     * Creates a JavaPHP instance
     */
    public JavaPHP(){
        this.PHP_BIN_PATH = "php";
        this.NO_PHP_WARN = false;
    }

    /**
     * Sets PHP global variables like REQUEST_METHOD, REQUEST_ADDR, etc...<br>
     * See <a href="https://www.php.net/manual/en/reserved.variables.php">PHP global variables</a> for other examples.<br><br>
     * WARNING: THIS IS REQUIRED IF USING $_SERVER VARIABLES OR OTHERS !
     *
     * @param variables The PHP global variables
     */
    public void setPHPVars(Map<String, String> variables){
        this.env_vars = variables;
    }

    /**
     * Runs the PHP file specified in the {@link JavaPHP} constructor.<br><br>
     * The result can be retrieved with {@link #getResult()} function.
     *
     * @param php_filepath The file path of the PHP file to be executed.
     */
    public void run(String php_filepath) throws Throwable {
        ProcessBuilder processBuilder = new ProcessBuilder(this.PHP_BIN_PATH, "-f", php_filepath);
        Process process = null;

        if (this.env_vars != null){
            Map<String, String> environment = processBuilder.environment();
            environment.putAll(this.env_vars);
        }

        try {
            process = processBuilder.start();
        } catch (Throwable error){
            String err = error.getMessage();

            if (!this.NO_PHP_WARN && (err.contains("PHP Warning:") || err.contains("PHP Startup:"))) onError(err);
        }

        if (process == null){
            onError(new Throwable("Error while executing PHP file at " + php_filepath));
            return;
        }

        this.response = processOutputHandler(process);
    }

    /**
     * Runs PHP code using PHP CLI '-r' argument
     * The result can be retrieved with {@link #getInlineResult} function.
     *
     * @param php_code The PHP Code
     */
    public void runWithCli(String php_code) throws Throwable {
        ProcessBuilder processBuilder = new ProcessBuilder(this.PHP_BIN_PATH, "-r", php_code);
        Process process = null;

        if (this.env_vars != null){
            Map<String, String> environment = processBuilder.environment();
            environment.putAll(this.env_vars);
        }

        try {
            process = processBuilder.start();
        } catch (Throwable error){
            String err = error.getMessage();

            if (!this.NO_PHP_WARN && (err.contains("PHP Warning:") || err.contains("PHP Startup:"))) onError(err);
        }

        if (process == null){
            onError(new Throwable("Error while executing PHP code"));
            return;
        }

        this.inlineResponse = processOutputHandler(process);
    }

    /**
     * Sets the function that will be called to handle the error instead of printing it into the console
     * @param cb The function to be executed on error
     */
    public void setErrorCallback(Consumer<String> cb){
        this.ERR_CALLBACK = cb;
    }

    /**
     * Gets the result of the executed PHP file
     * @return The result, returns null if no results
     */
    public String getResult(){
        return this.response;
    }

    /**
     * Gets the result of the inline PHP code
     * @return The result, returns null if no results
     */
    public String getInlineResult(){
        return this.inlineResponse;
    }

    /**
     * Error event listener, outputs error with 'System.err' by default, to set custom error handler, use {@link #setErrorCallback}
     * @param error The error
     */
    private void onError(String error){
        if (this.ERR_CALLBACK == null) {
            System.err.println(error);
            return;
        }

        this.ERR_CALLBACK.accept(error);
    }

    /**
     * Error event listener, outputs error with 'System.err' by default, to set custom error handler, use {@link #setErrorCallback}
     * @param error The error
     */
    private void onError(Throwable error){
        String err = Arrays.toString(error.getStackTrace());

        if (this.ERR_CALLBACK == null){
            System.err.println(err);
            return;
        }

        this.ERR_CALLBACK.accept(err);
    }

    /**
     * Handles the Output of a process and manage errors
     *
     * @param process The process
     * @return The Output of the process
     */
    private String processOutputHandler(Process process) throws Throwable {
        String res;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            StringBuilder resBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) resBuilder.append(line).append("\n");

            res = resBuilder.toString();
        }

        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))){
            StringBuilder errorBuilder = new StringBuilder();
            String line;

            while ((line = errorReader.readLine()) != null) errorBuilder.append(line).append("\n");

            if (!errorBuilder.toString().isEmpty()){
                String err = new Throwable(errorBuilder.toString()).getMessage();

                if (!this.NO_PHP_WARN && (err.contains("PHP Warning:") || err.contains("PHP Startup:"))) onError(err);
            }
        }

        return res;
    }
}