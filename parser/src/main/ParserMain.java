package main;

import java.io.IOException;

import parser.Parser;
import lexer.Lexer;

public class ParserMain {

    public static void main(String[] args) throws IOException {
        // Phase 1: 语法分析
        System.out.println("语法分析");
        Lexer lex = new Lexer();
        Parser parser = new Parser(lex);
        parser.program_phase1();

        // Reset for phase 2
        System.out.println("\n中间代码生成");
        lex = new Lexer();
        parser = new Parser(lex);
        parser.program_phase2();
    }
}
