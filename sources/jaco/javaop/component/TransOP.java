//      /   _ _      JaCo
//  \  //\ / / \     - OP translator
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.javaop.component;

import java.util.*;
import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.javaop.context.*;
import Tree.*;


public class TransOP extends jaco.java.component.Translator {
/** components
 */
        protected OPOperators operators;
        protected OPNameResolver nameresolv;
        protected HashMap tempVars;

/** component name
 */
        public String getName()
        {
                return "TransOP";
        }

/** the debugging name
 */ 
    public String getDebugName()
    {
        return "transop";
    }

/** component initialization
 */
        public void init(OPCompilerContext context)
        {
            super.init(context);
            operators = (OPOperators)(context.mainContext.Operators());
            nameresolv = (OPNameResolver)(context.mainContext.NameResolver());
            tempVars = new HashMap();
        }
        
/** return descrition of translator
 */
        public String getDescription()
        {
                return "translating OP";
        }

/** the translation methods
 */
    protected Tree translateDecl(Tree tree, Env env) {
                switch (tree) {

                        case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                                                        Tree[] thrown, Tree[] stats, _):
                        // cache tempVars
                        HashMap cache = tempVars;
                        tempVars = new HashMap();

                        
                        tree = super.translateDecl(tree, env);

                        // create VarDecls for all entries in tempVars
                        Iterator it = tempVars.entrySet().iterator();
                        while( it.hasNext() ) {
                            Map.Entry entry = (Map.Entry)it.next();

                            Name vname = Name.fromString((String)entry.getKey());
                            Tree vtype = trees.toTree((Type)entry.getValue());
                            Tree vard  = trees.at(tree).make(
                                    newdef.VarDecl(vname, 0, vtype, null));
                            System.out.println("*** " + vard + " " + vtype);//DEBUG
                            if ( null != ((MethodDecl)tree).stats ) {
                                ((MethodDecl)tree).stats = trees.append( vard, ((MethodDecl)tree).stats );
                            }

                        }

                        // restore tempVars
                        tempVars = cache;

                        return tree;
                        


                        default:
                        return super.translateDecl(tree, env);
                }
    }
        
    protected Tree translateStat(Tree tree, Env env) {
                switch (tree) {
                        default:
                        return super.translateStat(tree, env);
                }
    }
        
    protected Tree translateExpr(Tree tree, Env env) {
               switch (tree) {
                        case Binop(int opcode, Tree lhs, Tree rhs, _):                      
                            switch( ((Binop)tree).operator ) {
                                   case MethodDef(Code code) :
                                       
                                       System.out.println("Translating Binop (op ov) " + tree);
                                       
                                       Name opname = ((Binop)tree).operator.name;
                                       boolean bLeftAssoc = operators.isMangledNameLeftAssoc(opname);

                                       Tree selected =  bLeftAssoc ? transExpr(lhs, env ) : transExpr(rhs, env);
                                       Tree select = trees.at(tree).make( newdef.Select( selected, opname ) );
                                          
                                       if ( bLeftAssoc ) {

                                           Tree[] args = new Tree[]{transExpr(rhs, env)};
                                           return trees.at(tree).make(newdef.Apply(select, args));

                                       } else {

                                           // a trick to preserve left to right order of evaluation
                                           // the expr is replaced by ((tmp = lhs) == tmp) ? rhs.op(tmp) : rhs.op(tmp)
                                           
                                           Definition def = lhs.def();
                                           String name = "OP$$TMP$" + (1 + tempVars.size());
                                           tempVars.put( name, def.type );
                                           
                                           Tree tmp    = trees.at(tree).make(newdef.Ident (Name.fromString(name)));

                                           Tree assign = trees.at(tree).make(newdef.Assign(tmp, 
                                                                                           transExpr(lhs, env)));

                                           Tree cond   = trees.at(tree).make(newdef.Binop (OperatorConst.EQ, 
                                                                                           assign,
                                                                                           trees.copy(tmp)));

                                           Tree[] args = new Tree[]{trees.copy(tmp)};

                                           Tree prev   = trees.at(tree).make(newdef.Apply (select, 
                                                                                           args));

                                           return        trees.at(tree).make(newdef.If    (cond,
                                                                                           prev,
                                                                                           trees.copy(prev)));
                                       }
                                       

                                       // Tree[] args = bLeftAssoc ? new Tree[]{rhs} : new Tree[]{lhs};

                                       //return trees.at(tree).make( redef.Apply(transExpr(select, env), 
                                       //                                      transExprs(args, env)));
                                      
                            
                                   default:
                                       return super.translateExpr(tree, env);
                            }
                          

                        default:
                        return super.translateExpr(tree, env);
                }
    }
       
    public TreeList exit(TreeList treelist)  throws AbortCompilation
    {
        TreeList tl = super.exit(treelist);
        nameresolv.active = false;
        return tl;
    }
}










