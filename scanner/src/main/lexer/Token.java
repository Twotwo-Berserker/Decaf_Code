package lexer;

public class Token {

	public final int tag;

	public Token(int t) 
	{
		tag = t;
		//System.out.println("\nTag = "+t);
	}

	public String toString() {
		return "" + (char) tag;
	}
}
