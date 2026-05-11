package lexer;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Hashtable;

import symbols.Type;

public class Lexer {
	public static int line = 1;
	char peek = ' ';
	Hashtable words = new Hashtable();
	File file = new File("test.txt");
	Reader reader = null;
	int readcount = 0;
	void reserve(Word w)	{ words.put(w.lexeme, w); }
	
	public Lexer()	{
		reserve(new Word("if", Tag.IF));
		reserve(new Word("else", Tag.ELSE));
		reserve(new Word("while", Tag.WHILE));
		reserve(new Word("do", Tag.DO));
		reserve(new Word("break", Tag.BREAK));
		reserve(new Word("for", Tag.FOR));
		reserve(new Word("class", Tag.BASIC));
		reserve(new Word("void", Tag.BASIC));
		reserve(new Word("int", Tag.BASIC));
		reserve(new Word("double", Tag.BASIC));
		reserve(new Word("bool", Tag.BASIC));
		reserve(new Word("string", Tag.BASIC));
		reserve(new Word("null", Tag.ID));
		reserve(new Word("this", Tag.ID));
		reserve(new Word("extends", Tag.ID));
		reserve(new Word("return", Tag.ID));
		reserve(new Word("new", Tag.ID));
		reserve(new Word("NewArray", Tag.ID));
		reserve(new Word("Print", Tag.ID));
		reserve(new Word("ReadInteger", Tag.ID));
		reserve(new Word("ReadLine", Tag.ID));
		reserve(new Word("static", Tag.ID));
		reserve(new Word("New", Tag.ID));
		
		reserve(Word.True); reserve(Word.False);
		
		reserve(Type.Int); reserve(Type.Char);
		reserve(Type.Bool); reserve(Type.Float);
	}
	
	void readch() throws IOException {
		try {
			reader = new InputStreamReader(new FileInputStream(file));
			int temppeek;
			readcount++;
			for(int i=0;i<readcount;i++)
			{
				if((temppeek = reader.read()) != -1)
				{
					peek = (char)temppeek;
				}
				else
				{
					readcount = -1;
					break;
				}
			}
			reader.close();
		}
		catch (Exception e) {
			readcount = -1;
			e.printStackTrace();
		}
	}
	boolean readch(char c) throws IOException{
		readch();
		if(peek!=c) return false;
		peek = ' ';
		return true;
	}
	
	public Token scan() throws IOException{
		for(;;readch()){
			if(peek==' '||peek=='\t') continue;
			else if(peek=='\n') line = line + 1;
			else break;
		}
		switch(peek){
		case '&':
			if(readch('&')) return Word.and; else return new Token('&');
		case '|':
			if(readch('|')) return Word.or; else return new Token('|');
		case '=':
			if(readch('=')) return Word.eq; else return new Token('=');
		case '!':
			if(readch('=')) return Word.ne; else return new Token('!');
		case '<':
			if(readch('=')) return Word.le; else return new Token('<');
		case '>':
			if(readch('=')) return Word.ge; else return new Token('>');
		}
		
		if(Character.isDigit(peek)){
			int v = 0;
			do{
				v=10*v+Character.digit(peek, 10); readch();
			}while(Character.isDigit(peek));
			if(peek!='.') return new Num(v);
			float x = v; float d = 10;
			for(;;){
				readch();
				if(!Character.isDigit(peek)) break;
				x = x + Character.digit(peek, 10)/d; d=d*10;
			}
			return new Real(x);
		}
		
		if(Character.isLetter(peek)){
			StringBuffer b = new StringBuffer();
			do{
				b.append(peek); readch();
			}while(Character.isLetterOrDigit(peek));
			String s=b.toString();
			Word w = (Word)words.get(s);
			if(w!=null) return w;
			w = new Word(s, Tag.ID);
			words.put(s, w);
			return w;
		}
		
		Token tok = new Token(peek); peek=' ';
		return tok;
	}
}
