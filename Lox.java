package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

    // We’ll use this to ensure we don’t try to execute code that has a known error.
    private static boolean hadError = false;

    public static void main(String[] args) throws IOException {

        if (args.length > 1) {
            System.out.println("Usage: jlox <script file>");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    // Run Direct Script File.
    private static void runFile(String path) throws IOException {

        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code.
        // it lets us exit with a non-zero exit code like a good command line citizen should.
        if (hadError) {
            System.exit(65);
        }
    }

    // REPL - Read, Evaluate, Print and Loop.
    private static void runPrompt() throws IOException {

        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while (true) {

            System.out.print("> ");
            String line = reader.readLine();

            if (line == null) {
                break;
            }

            run(line);

            //  We need to reset this flag in the interactive loop.
            //  If the user makes a mistake, it shouldn’t kill their entire session.
            hadError = false;
        }
    }

    // Run the actual source code
    private static void run(String source) {

        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        for (Token token : tokens) {
            System.out.println(tokens);
        }
    }

    // It’s good engineering practice to separate the code that generates the (errors) from the code that (reports) them.
    // Handling Error
    public static void error(int line, String message) {

        report(line, "", message);
    }

    // Reporting Error
    private static void report(int line, String where, String message) {

        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }


}