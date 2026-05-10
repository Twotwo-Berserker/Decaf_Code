package inter;

import symbols.Type;

public class For extends Stmt {

   Expr initExpr; Expr cond; Expr update; Stmt stmt;

   public For() { initExpr = null; cond = null; update = null; stmt = null; }

   public void init(Expr i, Expr c, Expr u, Stmt s) {
      initExpr = i; cond = c; update = u; stmt = s;
      if( cond != null && cond.type != Type.Bool ) cond.error("boolean required in for");
   }
   public void gen(int b, int a) {}
   
   public void display() {
   emit("stmt : for begin");
   stmt.display();
   emit("stmt : for end");
   }
}
