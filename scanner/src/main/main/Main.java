package main;

import java.io.IOException;
import java.util.Hashtable;

import lexer.Lexer;
import lexer.Token;

public class Main {
	public static void main(String[] args) throws IOException {
		Lexer lexer = new Lexer();
		lexer.startcheck();
		char c;
		do {
			Token token = lexer.scan();
			switch (token.tag) {
				case 34:
				case 400:
					break;
				case 33: // !
				case 37: // %
				case 40: // (
				case 41: // )
				case 42: // *
				case 43: // +
				case 44: // ,
				case 45: // -
				case 46: // .
				case 47: // /
				case 59: // ;
				case 60: // <
				case 61: // =
				case 62: // >
				case 91: // [
				case 92: // \
				case 93: // ]
				case 123: // {
				case 125: // }
					System.out.println("(SYM , "+token.toString()+")");
					break;
				case 270: //
				case 272:
					System.out.println("(NUM , "+token.toString()+")");
					break;
				case 264:
					System.out.println("(ID , "+token.toString()+")");
					break;
				case 256:
				case 257:
				case 258:
				case 259:
				case 260:
				case 261:
				case 262:
				case 263:
				case 265:
				case 266:
				case 267:
				case 268:
				case 269:
				case 271:
				case 274:
				case 275:
				case 276:
				case 277:
				case 278:
				case 279:
				case 280:
				case 281:
				case 282:
				case 283:
				case 284:
				case 285:
				case 286:
				case 287:
				case 288:
				case 289:
				case 290:
				case 291:
					System.out.println("(KEY , "+token.toString()+")");
					break;
				case 300:
					System.out.println("(STR , "+token.toString()+")");
					break;
				case 301:
					System.out.println("(CMT , "+token.toString()+")");
					break;
				case 13:
					break;
				default:
					if(token.tag != -1)
						System.out.println("ERR: invalid decaf character \'"+token.toString()+"\' , line:"+lexer.line);
					break;
			}

		} while (lexer.endcheck());
	}
}
