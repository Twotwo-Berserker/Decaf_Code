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
        Lexer lex = new Lexer();
        Parser parser = new Parser(lex);
        
        // Phase 1: 语法分析
        System.out.println("语法分析");
        parser.programPhase1();
        
        // Reset lexer for phase 2
        lex = new Lexer();
        parser = new Parser(lex);
        
        // Phase 2: 中间代码生成
        System.out.println("\n中间代码生成");
        parser.programPhase2();
    }
}
