package main;

import java.io.IOException;

import parser.Parser;
import lexer.Lexer;

public class ParserMain {

    public static void main(String[] args) throws IOException {
        Lexer lex = new Lexer();
        Parser parser = new Parser(lex);
        
        // Phase 1: 语法分析
        System.out.println("语法分析");
        parser.program_phase1();
        
        // Phase 2: 中间代码生成
        System.out.println("\n中间代码生成");
        parser.program_phase2();
    }
}
