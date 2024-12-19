package fr.breadeater;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * The Main class is used to test the library
 */

public class Main {
    public static void main(String[] args) throws Throwable {
        JavaPHP javaphp = new JavaPHP(false);
        Map<String, String> env = new HashMap<>();

        env.put("REQUEST_METHOD", "GET");

        javaphp.setErrorCallback((error) -> System.out.println("ERROR: " + error));

        javaphp.setPHPVars(env);
        javaphp.run(new File("./test.php").getAbsolutePath());

        System.out.println(javaphp.getResult());
    }
}