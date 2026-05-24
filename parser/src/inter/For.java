package inter;

import symbols.Type;

public class For extends Stmt {

   Expr initExpr; Expr cond; Expr update; Stmt stmt;

   public For() { initExpr = null; cond = null; update = null; stmt = null; }

   public void init(Expr i, Expr c, Expr u, Stmt s) {
      initExpr = i; cond = c; update = u; stmt = s;
      if( cond != null && cond.type != Type.Bool ) cond.error("boolean required in for");
   }
   
   public void gen(int b, int a) {
      after = a;                // save label after for break statements
      int label_init = newlabel();    // label for initialization
      int label_test = newlabel();    // label for condition test
      int label_body = newlabel();    // label for body
      int label_incr = newlabel();    // label for increment

      // Generate initialization code (expressions don't take labels, just gen())
      emitlabel(label_init);
      if( initExpr != null ) {
         initExpr.gen();
      }

      // Generate condition test
      emitlabel(label_test);
      if( cond != null ) cond.jumping(0, a);       // if false, goto after (exit loop)
      else emit("goto L" + label_body);            // no condition, always enter loop

      // Generate body
      emitlabel(label_body);
      stmt.gen(label_body, 0);

      // Generate increment (expressions don't take labels, just gen())
      emitlabel(label_incr);
      if( update != null ) {
         update.gen();
      }

      // Jump back to test
      emit("goto L" + label_test);
   }

   public void display() {
      emit("stmt : for begin");
      stmt.display();
      emit("stmt : for end");
   }
}
