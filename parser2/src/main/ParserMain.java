package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import parser.Parser;
import lexer.Lexer;

public class ParserMain {

    public static void main(String[] args) throws IOException {
        File file = new File("test.txt");
        Reader reader = new InputStreamReader(new FileInputStream(file));
        Lexer lex = new Lexer(reader);
        Parser parser = new Parser(lex);
        parser.program();
        System.out.print("\n");
    }
}
