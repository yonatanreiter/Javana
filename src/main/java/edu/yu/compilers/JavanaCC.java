package edu.yu.compilers;

import antlr4.JavanaLexer;
import antlr4.JavanaParser;
import edu.yu.compilers.backend.compiler.Compiler;
import edu.yu.compilers.backend.converter.Converter;
import edu.yu.compilers.backend.interpreter.Executor;
import edu.yu.compilers.frontend.Semantics;
import edu.yu.compilers.frontend.SyntaxErrorHandler;
import edu.yu.compilers.intermediate.symtable.SymTableEntry;
import edu.yu.compilers.intermediate.util.ParseTreePrinter;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class JavanaCC {

    public static void main(String[] args) throws Exception {
        String usageMessageString = 
        """
            USAGE: JavanaCC <option> sourceFileName
                option: -tokens
                        -parse
                        -symbols
                        -ast
                        -execute
                        -convert
                        -compile
        """;

        if (args.length != 2) {
            System.out.println(usageMessageString);
            System.exit(-1);
        }

        String operation = args[0];
        String sourceFileName = args[1];

        if (invalidOperation(operation)) {
            System.out.println(usageMessageString);
            System.exit(-1);
        }

        SyntaxErrorHandler syntaxErrorHandler = new SyntaxErrorHandler();

        var lexer = createLexer(sourceFileName, syntaxErrorHandler);
        if (lexer == null)
            System.exit(-1);

        if (operation.equals("-tokens")) {
            var tokenStream = new CommonTokenStream(lexer);
            tokenStream.fill();
            int errorCount = syntaxErrorHandler.getCount();
            if (errorCount > 0)
                System.err.printf("\nThere were %d lexical errors.\n", errorCount);
            printTokens(tokenStream.getTokens(), lexer.getVocabulary());
            System.exit(errorCount);
        }

        // Pass 1: Parse the Pascal source file.

        var parser = createParser(lexer, syntaxErrorHandler);
        ParseTree tree = parser.program();
        int errorCount = syntaxErrorHandler.getCount();

        if (operation.equals("-parse")) {
            if (errorCount > 0)
                System.err.printf("There were %d syntax errors.\n", errorCount);
            var printer = new ParseTreePrinter(parser);
            printer.printParseTreeToJson(tree);
            System.exit(errorCount);
        }

        // Pass 2: Semantic operations.

        Semantics pass2 = new Semantics();
        pass2.visit(tree);
        errorCount = pass2.getErrorCount();

        if (operation.equals("-symbols")) {
            if (errorCount > 0)
                System.err.printf("There were %d semantic errors.\n", errorCount);
            pass2.printSymbolTableStack();
            System.exit(errorCount);
        }

        // Pass 3: Abstract Syntax Tree (AST) construction.

        if (operation.equals("-ast")) {
            System.out.println("AST not implemented yet.");
            System.exit(0);
        }

        // Pass 4: Translation.

        switch (operation) {
            case "-execute" -> {
                // Pass 3: Execute the Pascal program.
                SymTableEntry programId = pass2.getProgramId();
                Executor pass3 = new Executor(programId);
                pass3.visit(tree);
            }
            case "-convert" -> {
                // Convert from Pascal to Java.
                Converter pass3 = new Converter();
                String objectCode = (String) pass3.visit(tree);
                System.out.println(objectCode);
            }
            case "-compile" -> {
                // Pass 3: Compile the Pascal program.
                SymTableEntry programId = pass2.getProgramId();
                Compiler pass3 = new Compiler(programId.getName());
                pass3.visit(tree);
                System.out.println(pass3.getObjectFileName());
            }
        }
    }

    private static boolean invalidOperation(String operation) {
        var validOperations = Set.of("-tokens", "-parse", "-symbols", "-ast", "-execute", "-convert", "-compile");
        return !validOperations.contains(operation);
    }

    private static JavanaLexer createLexer(String sourceFileName, SyntaxErrorHandler syntaxErrorHandler) {
        try {
            var lexer = new JavanaLexer(CharStreams.fromFileName(sourceFileName));
            lexer.addErrorListener(syntaxErrorHandler);
            return lexer;
        } catch (IOException e) {
            System.out.println("Source file error: " + sourceFileName);
            return null;
        }
    }

    private static JavanaParser createParser(JavanaLexer lexer, SyntaxErrorHandler syntaxErrorHandler) {
        var parser = new JavanaParser(new CommonTokenStream(lexer));
        parser.addErrorListener(syntaxErrorHandler);
        return parser;
    }

    protected static void printTokens(List<Token> tokens, Vocabulary vocabulary) {
        System.out.println("Tokens:");
        System.out.println();

        for (var token : tokens) {
            var symbolicName = vocabulary.getSymbolicName(token.getType());
            var tokenText = token.getText();

            if (symbolicName == null) {
                System.out.printf("%14s : '%s'\n", "", tokenText);
            } else if (symbolicName.equals("STRING")) {
                // Strip the single quotes from the string.
                var string = tokenText.substring(1, tokenText.length() - 1);

                // Unescape embedded single quotes
                string = string.replace("''", "'");

                // print out the token with double quotes around the strings
                System.out.printf("%14s : \"%s\"\n", symbolicName, string);
            } else if (symbolicName.equals("CHARACTER")) {
                // Length will be 3, unless it is an escaped single quote
                if (tokenText.length() == 3) {
                    System.out.printf("%14s : '%s'\n", symbolicName, tokenText.charAt(1));
                } else {
                    System.out.printf("%14s : '\\%s'\n", symbolicName, tokenText.charAt(2));
                }
            } else {
                if (symbolicName.equals("ERROR"))
                    System.out.printf("TOKEN ERROR at line %d: Invalid token at '%s'\n", token.getLine(), tokenText);

                System.out.printf("%14s : %s\n", symbolicName, tokenText);
            }
        }
    }
}
