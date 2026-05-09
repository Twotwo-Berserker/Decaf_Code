package lexer;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader; 
import java.util.Hashtable;

import javax.sound.sampled.BooleanControl;

public class Lexer {

	public static int line = 1;
	char peek = ' ';
	short readstatus = 0;
	Hashtable words = new Hashtable();
	File file = new File("test.TXT");
	Reader reader = null;
	int readcount=0;

	void reserve(Word w) {
		words.put(w.lexeme, w);
	}

	int hexTest(char c) //十六进制检测
	{
		switch(c)
		{
			case '0': return 0;
			case '1': return 1;
			case '2': return 2;
			case '3': return 3;
			case '4': return 4;
			case '5': return 5;
			case '6': return 6;
			case '7': return 7;
			case '8': return 8;
			case '9': return 9;
			case 'a': case 'A': return 10;
			case 'b': case 'B': return 11;
			case 'c': case 'C': return 12;
			case 'd': case 'D': return 13;
			case 'e': case 'E': return 14;
			case 'f': case 'F': return 15;
			default: return -1;
		}
	}

	public Lexer() {
		reserve(new Word("if", Tag.IF));
		reserve(new Word("else", Tag.ELSE));
		reserve(new Word("while", Tag.WHILE));
		reserve(new Word("do", Tag.DO));
		reserve(new Word("break", Tag.BREAK));
		reserve(new Word("class", Tag.CLASS));
		reserve(new Word("void", Tag.VOID));
		reserve(new Word("int", Tag.INT));
		reserve(new Word("double", Tag.DOUBLE));
		reserve(new Word("bool", Tag.BOOL));		
		reserve(new Word("string", Tag.STRING));
		reserve(new Word("null", Tag.NULL));
		reserve(new Word("this", Tag.THIS));
		reserve(new Word("extends", Tag.EXTENDS));
		reserve(new Word("for", Tag.FOR));
		reserve(new Word("return", Tag.RETURN));
		reserve(new Word("new", Tag.NEW));
		reserve(new Word("NewArray", Tag.NEWARRAY));
		reserve(new Word("Print", Tag.PRINT));
		reserve(new Word("ReadInteger", Tag.READINTEGER));
		reserve(new Word("ReadLine", Tag.READLINE));
		
		reserve(Word.True);
		reserve(Word.False);
	}


	public void readch() throws IOException {
		try{
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

	boolean readch(char c) throws IOException {
		readch();
		if (peek != c) {
			return false;
		}
		//peek = ' ';
		return true;
	}


	public Token scan() throws IOException {
	
		for(;;readch()) {
			if (peek == ' ' || peek == '\t')
				continue;
			else if (peek == '\n') 
			{
				line += 1;
			}
			else 
				break;
			if(readstatus != 3)
			{				
				Token nn = new Token(13);
				readch();
				readstatus = 0;
				return nn;
			}
			}

		//readstatus 标识需要检测多个字符的状态 在这种状态中放一些循环来检测多个字符 使其不会读到一个字符马上就传给token 
		//1 蛇=字符串
		//2 眼镜蛇=注释
		//3 响尾蛇=注释
		//4 检测到了0后面的X
		//5 检测到了0后面的x
		//6 有0X 但是后面是错误的十六进制数的格式 前面状态4发现格式错误的话先返回0的token 这里返回X的token
		//7 同6 X大写变成小写
		switch (readstatus)
		{
			//snake: string between quotes 
			case 1:
			StringBuffer snakeBuffer = new StringBuffer();
			do {
				snakeBuffer.append(peek);
				readch();
			} while (peek != '"' && ( !Character.isWhitespace(peek) || peek == ' ' || peek == '\t') );
			readstatus = 0;
			readch();
			String snake = snakeBuffer.toString();
			Word w1 = new Word(snake, Tag.STRINGS);
			words.put(snake, w1);
			return w1;

			//cobra: comments (line)
			case 2:
			StringBuffer cobraBuffer = new StringBuffer();
			for(;;readch()) 
			{
				if( Character.isWhitespace(peek) && peek != ' ' && peek != '\t' ) 
					break;
				cobraBuffer.append(peek);
			}
			readstatus = 0;
			String cobra = cobraBuffer.toString();
			Word w2 = new Word(cobra, Tag.COMMENTS);
			words.put(cobra, w2);
			return w2;

			//crotalus: comments (range)
			case 3:
			StringBuffer crotalusBuffer = new StringBuffer();
			for(;;readch()) 
			{
				if(peek == '*')
				{
					if(readch('/'))
					{
						readch();
						String crotalus = crotalusBuffer.toString();
						Word w3 = new Word(crotalus, Tag.COMMENTS);
						words.put(crotalus, w3);
						readstatus = 0;
						return w3;
					}
					else
						crotalusBuffer.append('*');
				}
				crotalusBuffer.append(peek);
			}
			// < */ >
			
			//hex prefix detected
			case 4: case 5:
			//System.out.println("peek="+peek);
			readch();
			if(hexTest(peek)<0)
			{
				if(readstatus == 4)
				{
					readstatus += 2;
					return new Num(0);
				}
			}
			else
			{
				int h = 0;
				do {
					h = 16 * h + hexTest(peek);
					readch();
				} while (hexTest(peek) >= 0);
				readstatus = 0;
				return new Num(h);
			}

			case 6: case 7:  //0x detected but not a real hex
				StringBuffer fHexBuffer = new StringBuffer();
				if(readstatus == 6)
					fHexBuffer.append('x');
				if(readstatus == 7)
					fHexBuffer.append('X');
				readstatus = 0;
				do {
					fHexBuffer.append(peek);
					readch();
					} while (Character.isLetterOrDigit(peek));
			String fHex = fHexBuffer.toString();
			Word w = (Word) words.get(fHex);
			if (w != null)
				return w;
			w = new Word(fHex, Tag.ID);
			words.put(fHex, w);
			return w;
		}



		//peek=下一个字符
		switch (peek) {
		case '&':
			if (readch('&'))
			{
				readch();
				return Word.and;
			}
			else
				return new Token('&');
		case '|':
			if (readch('|'))
			{
				readch();
				return Word.or;
			}
			else
				return new Token('|');
		case '=':
			if (readch('='))
			{
				readch();
				return Word.eq;
			}
			else
				return new Token('=');
		case '!':
			if (readch('='))
			{
				readch();
				return Word.ne;
			}
			else
				return new Token('!');
		case '<':
			if (readch('='))
			{
				readch();
				return Word.le;
			}
			else
				return new Token('<');
		case '>':
			if (readch('='))
			{
				readch();
				return Word.ge;
			}
			else
				return new Token('>');
		//comment ready
		case '/':
			if (readch('/') && readstatus == 0)
			{
				readch();
				readstatus = 2;
				return Word.cmt;
			}
			else if (peek == '*' && readstatus == 0)
			{
				readch();
				readstatus = 3;
				//System.out.println("[/*] get\n");
				return Word.cmt;
			}
			else
				return new Token('/');
		case '0':
			//System.out.println("[hex 0]");
			readch();
			if (peek != 'x' && peek != 'X')
			{
				//System.out.println("[hex not], peek="+peek);
				float dn = 0;
				float dd = 10;
				if(peek == '.')
				{
					for (;;) 
					{
					readch();
					if (!Character.isDigit(peek))
						break;
					dn = dn + Character.digit(peek, 10) / dd;
					dd = dd * 10;
					}
				if (peek != 'E')
					return new Real(dn);
				readch();
				if (peek =='+' || peek == '-' || Character.isDigit(peek))
				{
					int i = 0;
					if (peek!='-'){
					//System.out.println("get E+");
					if(peek=='+')
					readch();
					do {
						i = 10 * i + Character.digit(peek, 10);
						readch();
					} while (Character.isDigit(peek));
					if(i==0) return new Real(1);
					for(;i>0;i--)
					dn *= 10;
				}
				else if (peek=='-'){
					//System.out.println("get E-");
					readch();
					do {
						i = 10 * i + Character.digit(peek, 10);
						readch();
					} while (Character.isDigit(peek));
					if(i==0) return new Real(1);
					for(;i>0;i--)
					dn /= 10;
				}
				return new Real(dn);
			}
			else
			{
				return new Word("E",Tag.ID);
			}
				}
				else if(!Character.isDigit(peek))
					return new Num(0);
			}
			else if(peek == 'x')
			{
				//System.out.println("[hex x]");
				readstatus = 4;
				return new Token(400);
			}
			else if(peek == 'X')
			{
				//System.out.println("[hex X]");
				readstatus = 5;
				return new Token(400);
			}
/* ddd aaa bbb/* sss fff*/

		}

//检查数字开头 遇到小数点前都认为是NUM 遇到字母标点停止检索
//遇到小数点后认为是REAL 遇到字母标点停止检索（除了字母E）
//如果REAL状态遇到字母E 检查下一个字符是否是+或- 如果是的话再检查后面是不是NUM 如果是就是科学计数法
//十六进制以状态表现
		if (Character.isDigit(peek)) {
			int v = 0;
			do {
				v = 10 * v + Character.digit(peek, 10);
				readch();
			} while (Character.isDigit(peek));
			if (peek != '.')
				return new Num(v);
			float x = v;
			//float
			float d = 10;
			for (;;) {
				readch();
				if (!Character.isDigit(peek))
					break;
				x = x + Character.digit(peek, 10) / d;
				d = d * 10;
			}
			//x10
			if (peek != 'E')
				return new Real(x);
			readch();
			if (peek =='+' || peek == '-' || Character.isDigit(peek))
			{
				int i = 0;
				if (peek!='-'){
					//System.out.println("get E+");
					if(peek=='+')
					readch();
					do {
						i = 10 * i + Character.digit(peek, 10);
						readch();
					} while (Character.isDigit(peek));
					if(i==0) return new Real(1);
					for(;i>0;i--)
					x *= 10;
				}
				else if (peek=='-'){
					//System.out.println("get E-");
					readch();
					do {
						i = 10 * i + Character.digit(peek, 10);
						readch();
					} while (Character.isDigit(peek));
					if(i==0) return new Real(1);
					for(;i>0;i--)
					x /= 10;
				}
				return new Real(x);
			}
			else
			{
				return new Word("E",Tag.ID);
			}
		}
		if (Character.isLetter(peek)) { 
			//变量名 字母开头 直到遇到空格 把所有扫描到的东西存储至一个字符串 最后检查字符串内容是不是一个关键字
			StringBuffer b = new StringBuffer();
			do {
				b.append(peek);
				readch();
			} while (Character.isLetterOrDigit(peek));
			String s = b.toString();
			Word w = (Word) words.get(s);
			if (w != null)
				return w;
			w = new Word(s, Tag.ID);
			words.put(s, w);
			return w;
		}
		//string
		if(peek == '\"' && readstatus == 0)
		{
			readstatus = 1;
		}

		Token tok = new Token(peek);
		peek = ' ';
		return tok;
	}
	
	public void out() {
		System.out.println(words.size());
		
	}

	public char getPeek() { //访问peek
		return peek;
	}

	public int changePeek() { //访问peek的ascii
		int peekasc = (int)peek;
		return peekasc;
	}

	public void setPeek(char peek) {
		this.peek = peek;
	}

	public void startcheck() //开始扫描文件，将readcount置0说明从第0个字符开始读文件
	{
		readcount = 0;
	}

	public boolean endcheck() //结束扫描文件,检查验证文件末尾的readcount
	{
		if(readcount<0)
		return false;
		else return true;
	}
}
