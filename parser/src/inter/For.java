package inter;

import symbols.Type;

public class For extends Stmt {
	Expr expr; Stmt stmt1; Stmt stmt2; Stmt stmt3; 
	
	public For() { expr = null; stmt1 = null; stmt2 = null; stmt3 = null; }
	
	public void init(Stmt s1, Expr x, Stmt s2, Stmt s3) {
		expr = x; stmt1 = s1; stmt2 = s2; stmt3 = s3;
		if( expr.type != Type.Bool ) expr.error("boolean required in for");
	}
	
	public void gen(int b, int a) {
		after = a;                // save label after for break statements
		int label_init = newlabel();    // label for initialization
		int label_test = newlabel();    // label for condition test
		int label_body = newlabel();    // label for body
		int label_incr = newlabel();    // label for increment
		
		// Generate initialization code
		emitlabel(label_init);
		stmt1.gen(label_init, 0);
		
		// Generate condition test
		emitlabel(label_test);
		expr.jumping(0, a);       // if false, goto after (exit loop)
		
		// Generate body
		emitlabel(label_body);
		stmt3.gen(label_body, 0);
		
		// Generate increment
		emitlabel(label_incr);
		stmt2.gen(label_incr, 0);
		
		// Jump back to test
		emit("goto L" + label_test);
	}

	public void display() {
   emit("stmt : for begin");
   stmt3.display();
   emit("stmt : for end");
   }
}
