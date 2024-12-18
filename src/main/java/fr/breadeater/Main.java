package fr.breadeater;

import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * The Main class is used to test the library
 */

@TestOnly
public class Main extends PHPJava {
    public static void main(String[] args) throws Throwable {
        PHPJava phpjava = new PHPJava(true);
        Map<String, String> env = new HashMap<>();

        env.put("REQUEST_METHOD", "GET");

        phpjava.setPHPVars(env);
        phpjava.run(new File("./test.php").getAbsolutePath());

        System.out.println(phpjava.getResult());
    }
}