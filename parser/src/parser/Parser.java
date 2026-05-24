package parser;

import inter.Access;
import inter.And;
import inter.Arith;
import inter.Break;
import inter.Constant;
import inter.Do;
import inter.Else;
import inter.Expr;
import inter.Id;
import inter.If;
import inter.Not;
import inter.Or;
import inter.Rel;
import inter.Seq;
import inter.Set;
import inter.SetElem;
import inter.Stmt;
import inter.Unary;
import inter.While;
import inter.For;

import java.io.IOException;
import java.io.Reader;

import symbols.Array;
import symbols.Env;
import symbols.Type;
import lexer.Lexer;
import lexer.Tag;
import lexer.Token;
import lexer.Word;
import lexer.Num;

public class Parser {

	   private Lexer lex;    // lexical analyzer for this parser
	   private Token look;   // lookahead tagen
	   Env top = null;       // current or top symbol table
	   int used = 0;         // storage used for declarations

	   public Parser(Lexer l) throws IOException { lex = l; move(); }

	   void move() throws IOException { look = lex.scan(); }

	   public void reset() throws IOException { lex.reset(); move(); }

	   void error(String s) { 
	      throw new ParseException("Syntax error near line " + Lexer.line + ": " + s, Lexer.line, look);
	   }
	   
	   /** Custom exception for parse errors with detailed information */
	   public static class ParseException extends RuntimeException {
	      private final int line;
	      private final Token token;
	      
	      public ParseException(String message, int line, Token token) {
	         super(message);
	         this.line = line;
	         this.token = token;
	      }
	      
	      public int getLine() { return line; }
	      public Token getToken() { return token; }
	      
	      @Override
	      public String toString() {
	         return "ParseException: " + getMessage();
	      }
	   }

	   void match(int t) throws IOException {
	      if( look.tag == t ) move();
	      else {
	         String expected = getTokenName(t);
	         String found = getTokenName(look.tag);
	         error("expected '" + expected + "' but found '" + found + "'");
	      }
	   }
	   
	   /** Helper method to get human-readable token names */
	   private String getTokenName(int tag) {
	      switch(tag) {
	         case Tag.BASIC: return "type";
	         case Tag.ID: return "identifier";
	         case Tag.NUM: return "number";
	         case Tag.REAL: return "real number";
	         case Tag.TRUE: return "true";
	         case Tag.FALSE: return "false";
	         case Tag.IF: return "if";
	         case Tag.ELSE: return "else";
	         case Tag.WHILE: return "while";
	         case Tag.DO: return "do";
	         case Tag.FOR: return "for";
	         case Tag.BREAK: return "break";
	         case Tag.AND: return "&&";
	         case Tag.OR: return "||";
	         case Tag.EQ: return "==";
	         case Tag.NE: return "!=";
	         case Tag.LE: return "<=";
	         case Tag.GE: return ">=";
	         case '+': return "+";
	         case '-': return "-";
	         case '*': return "*";
	         case '/': return "/";
	         case '!': return "!";
	         case '=': return "=";
	         case '<': return "<";
	         case '>': return ">";
	         case ';': return ";";
	         case '{': return "{";
	         case '}': return "}";
	         case '(': return "(";
	         case ')': return ")";
	         case '[': return "[";
	         case ']': return "]";
	         default: return "token(" + tag + ")";
	      }
	   }

	   // Phase 1: Syntax Analysis - build and display the syntax tree
	   public void program_phase1() throws IOException {
	      Stmt s = block();
	      // display the syntax tree
	      // only display the stmts, without expr
	      s.display();
	   }

	   // Phase 2: Intermediate Code Generation
	   public void program_phase2() throws IOException {
		   Stmt s = block();
		   if (s == null) {
			   error("syntax tree not built, run phase 1 first");
			   return;
		   }
		   int begin = s.newlabel();
		   int after = s.newlabel();
		   s.emitlabel(begin);
		   s.gen(begin, after);
		   s.emitlabel(after);
	   }

	   Stmt block() throws IOException {  // block -> { decls stmts }
	      match('{');  Env savedEnv = top;  top = new Env(top);
	      decls(); Stmt s = stmts();
	      match('}');  top = savedEnv;
	      return s;
	   }

	   void decls() throws IOException {

	      while( look.tag == Tag.BASIC ) {   // D -> type ID ;
	         Type p = type(); Token tok = look; match(Tag.ID); match(';');
	         Id id = new Id((Word)tok, p, used);
	         top.put( tok, id );
	         used = used + p.width;
	      }
	   }

	   Type type() throws IOException {

	      Type p = (Type)look;            // expect look.tag == Tag.BASIC 
	      match(Tag.BASIC);
	      if( look.tag != '[' ) return p; // T -> basic
	      else return dims(p);            // return array type
	   }

	   Type dims(Type p) throws IOException {
	      match('[');  Token tok = look;  match(Tag.NUM);  match(']');
	      if( look.tag == '[' )
	      	p = dims(p);
	      return new Array(((Num)tok).value, p);
	   }

	   Stmt stmts() throws IOException {
	      if ( look.tag == '}' ) return Stmt.Null;
	      else return new Seq(stmt(), stmts());
	   }

	   Stmt stmt() throws IOException {
	      Expr x;  Stmt s, s1, s2;
	      Stmt savedStmt;         // save enclosing loop for breaks

	      switch( look.tag ) {

	      case ';':
	         move();
	         return Stmt.Null;

	      case Tag.IF:
	         match(Tag.IF); match('('); x = bool(); match(')');
	         s1 = stmt();
	         if( look.tag != Tag.ELSE ) return new If(x, s1);
	         match(Tag.ELSE);
	         s2 = stmt();
	         return new Else(x, s1, s2);

	      case Tag.WHILE:
	         While whilenode = new While();
	         savedStmt = Stmt.Enclosing; Stmt.Enclosing = whilenode;
	         match(Tag.WHILE); match('('); x = bool(); match(')');
	         s1 = stmt();
	         whilenode.init(x, s1);
	         Stmt.Enclosing = savedStmt;  // reset Stmt.Enclosing
	         return whilenode;

      case Tag.DO:
         Do donode = new Do();
         savedStmt = Stmt.Enclosing; Stmt.Enclosing = donode;
         match(Tag.DO);
         s1 = stmt();
         match(Tag.WHILE); match('('); x = bool(); match(')'); match(';');
         donode.init(s1, x);
         Stmt.Enclosing = savedStmt;  // reset Stmt.Enclosing
         return donode;

      case Tag.FOR:
         For fornode = new For();
         savedStmt = Stmt.Enclosing; Stmt.Enclosing = fornode;
         match(Tag.FOR); match('(');
         // init expression (optional)
         Expr initExpr;
         if( look.tag == ';' ) {
            initExpr = null;
            move();  // consume ';'
         } else {
            initExpr = bool();
            match(';');
         }
         // condition expression (optional)
         Expr cond;
         if( look.tag == ';' ) {
            cond = null;
            move();  // consume ';'
         } else {
            cond = bool();
            match(';');
         }
         // update expression (optional)
         Expr update;
         if( look.tag == ')' ) {
            update = null;
         } else {
            update = bool();
         }
         match(')');
         s1 = stmt();
         fornode.init(initExpr, cond, update, s1);
         Stmt.Enclosing = savedStmt;  // reset Stmt.Enclosing
         return fornode;

      case Tag.BREAK:
         match(Tag.BREAK); match(';');
         return new Break();

	      case '{':
	         return block();

	      default:
	         return assign();
	      }
	   }

	   Stmt assign() throws IOException {
	      Stmt stmt;  Token t = look;
	      match(Tag.ID);
	      Id id = top.get(t);
	      if( id == null ) error(t.toString() + " undeclared");

	      if( look.tag == '=' ) {       // S -> id = E ;
	         move();  stmt = new Set(id, bool());
	      }
	      else {                        // S -> L = E ;
	         Access x = offset(id);
	         match('=');  stmt = new SetElem(x, bool());
	      }
	      match(';');
	      return stmt;
	   }

	   Expr bool() throws IOException {
	      Expr x = join();
	      while( look.tag == Tag.OR ) {
	         Token tok = look;  move();  x = new Or(tok, x, join());
	      }
	      return x;
	   }

	   Expr join() throws IOException {
	      Expr x = equality();
	      while( look.tag == Tag.AND ) {
	         Token tok = look;  move();  x = new And(tok, x, equality());
	      }
	      return x;
	   }

	   Expr equality() throws IOException {
	      Expr x = rel();
	      while( look.tag == Tag.EQ || look.tag == Tag.NE ) {
	         Token tok = look;  move();  x = new Rel(tok, x, rel());
	      }
	      return x;
	   }

	   Expr rel() throws IOException {
	      Expr x = expr();
	      switch( look.tag ) {
	      case '<': case Tag.LE: case Tag.GE: case '>':
	         Token tok = look;  move();  return new Rel(tok, x, expr());
	      default:
	         return x;
	      }
	   }

	   Expr expr() throws IOException {
	      Expr x = term();
	      while( look.tag == '+' || look.tag == '-' ) {
	         Token tok = look;  move();  x = new Arith(tok, x, term());
	      }
	      return x;
	   }

	   Expr term() throws IOException {
	      Expr x = unary();
	      while(look.tag == '*' || look.tag == '/' ) {
	         Token tok = look;  move();   x = new Arith(tok, x, unary());
	      }
	      return x;
	   }

	   Expr unary() throws IOException {
	      if( look.tag == '-' ) {
	         move();  return new Unary(Word.minus, unary());
	      }
	      else if( look.tag == '!' ) {
	         Token tok = look;  move();  return new Not(tok, unary());
	      }
	      else return factor();
	   }

	   Expr factor() throws IOException {
	      Expr x = null;
	      switch( look.tag ) {
	      case '(':
	         move(); x = bool(); match(')');
	         return x;
	      case Tag.NUM:
	         x = new Constant(look, Type.Int);    move(); return x;
	      case Tag.REAL:
	         x = new Constant(look, Type.Float);  move(); return x;
	      case Tag.TRUE:
	         x = Constant.True;                   move(); return x;
	      case Tag.FALSE:
	         x = Constant.False;                  move(); return x;
	      default:
	         error("syntax error");
	         return x;
	      case Tag.ID:
	         String s = look.toString();
	         Id id = top.get(look);
	         if( id == null ) error(look.toString() + " undeclared");
	         move();
	         if( look.tag != '[' ) return id;
	         else return offset(id);
	      }
	   }

	   Access offset(Id a) throws IOException {   // I -> [E] | [E] I
	      Expr i; Expr w; Expr t1, t2; Expr loc;  // inherit id

	      Type type = a.type;
	      match('['); i = bool(); match(']');     // first index, I -> [ E ]
	      type = ((Array)type).of;
	      w = new Constant(type.width);
	      t1 = new Arith(new Token('*'), i, w);
	      loc = t1;
	      while( look.tag == '[' ) {      // multi-dimensional I -> [ E ] I
	         match('['); i = bool(); match(']');
	         type = ((Array)type).of;
	         w = new Constant(type.width);
	         t1 = new Arith(new Token('*'), i, w);
	         t2 = new Arith(new Token('+'), loc, t1);
	         loc = t2;
	      }

	      return new Access(a, loc, type);
	   }
	}
