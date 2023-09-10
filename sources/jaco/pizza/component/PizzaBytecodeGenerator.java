//      /   _ _      JaCo
//  \  //\ / / \     - extended bytecode generator
//   \//  \\_\_/     
//         \         Matthias Zenger, 14/02/02

package jaco.pizza.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.pizza.struct.*;
import jaco.pizza.context.*;
import java.io.IOException;
import Type.*;
import Definition.*;
import Tree.*;
import Item.*;


public class PizzaBytecodeGenerator extends BytecodeGenerator
                                    implements PizzaModifierConst {
    
/** other components
 */
    protected PizzaClassWriter writer;
    
/** the list of the current bridge methods
 */
    protected TreeList bridges;
        
/** component name
 */
    public String getName() {
        return "PizzaBytecodeGenerator";
    }
    
/** component initialization
 */
    public void init(BackendContext context) {
        super.init(context);
        writer = (PizzaClassWriter)((PizzaCompilerContext)
            context.compilerContext).ClassWriter();
    }
    
    protected Tree[] normalizeDecls(CompilationEnv env, Tree[] defs, ClassDef c) {
        for (int i = 0; i < defs.length; i++)
            switch (defs[i]) {
                case MethodDecl(_, _, _, _, _, _, MethodDef f):
                    ((MDef)f).metaData = ((PizzaTrees)trees).getMetaData(defs[i]);
                    break;
                case VarDecl(_, _, _, _, VarDef v):
                    ((VDef)v).metaData = ((PizzaTrees)trees).getMetaData(defs[i]);
            }
        return super.normalizeDecls(env, defs, c);
    }
    
/** generate code for declarations
 */
    protected void genDecl(Tree tree, GenEnv env) {
        switch (tree) {
            case PackageDecl(_):
            case Import(_, _):
                break;
            case ClassDecl(_, _, _, _, Tree[] defs, ClassDef c):
                ((ClassDecl)tree).members = defs = normalizeDecls(env.toplevel.info, defs, c);
                c.pool = coder.newPool();
                TreeList oldbridges = bridges;
                bridges = new TreeList();
                GenEnv localEnv = env.dup(tree);
                localEnv.enclClass = (ClassDecl)tree;
                if (debug(4))
                    pretty.printDecl(tree);
                for (int j = 0; j < defs.length; j++)
                    genDecl(defs[j], localEnv);
                ((ClassDecl)tree).members = bridges.append(defs).toArray();
                if (debug(8))
                    pretty.printDecl(tree);
                bridges = oldbridges;
                ++report.classes;
                break;
            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                            Tree[] thrown, Tree[] stats, MethodDef d):
                GenEnv localEnv = env.dup(tree);
                localEnv.enclMethod = (MethodDecl)tree;
                genTry(localEnv, false);
                ++report.methods;
                // check if a bridge has to be inserted
                BridgeMethodSet bmset = (BridgeMethodSet)writer.bridges.get(d);
                if (bmset != null) {
                    Type[] ts = bmset.types();
                    for (int i = 0; i < ts.length; i++) {
                        //System.out.println("insert bridge method for " + ts[i]);
                        // create a new MethodDef for the bridge method
                        MethodDef def = (MethodDef)definitions.make.MethodDef(
                            d.modifiers | SYNTHETIC | BRIDGE, name, ts[i], d.owner);
                        d.owner.locals().enter(def);
                        // create a tree node (only for pretty printing support)
                        MethodDecl decl = (MethodDecl)trees.at(tree.pos).make(
                            trees.newdef.MethodDecl(name, d.modifiers | SYNTHETIC,
                                trees.toTree(ts[i].restype()),
                                (VarDecl[])trees.copy(params, new VarDecl[params.length]),
                                trees.copy(thrown),
                                new Tree[0]));
                        // fill in real types
                        decl.type = ts[i];
                        decl.def = def;
                        // generate code
                        def.code = coder.newCode(false, def);
                        int thisAdr = coder.newThis(def);
                        // set "this"
                        def.code.setStartPc(thisAdr);
                        // define parameters
                        for (int j = 0; j < params.length; j++) {
                            coder.newLocal(params[j].def);
                            coder.letInit(params[j].def.adr);
                        }
                        // load this.method
                        items.load(items.make.SelfItem(false));
                        Item f = items.make.MemberItem(tree.pos, def,
                            (d.modifiers & PRIVATE) != 0, true);
                        // push arguments
                        Type[] that = ts[i].argtypes();
                        for (int k = 0; k < params.length; k++) {
                            int reg = coder.regOf(params[k].def.adr);
                            if (reg <= 3)
                                coder.emitop(iload_0 +
                                    coder.truncate(items.make.typecode(that[k])) * 4 + reg);
                            else
                                coder.emitop1w(iload +
                                    coder.truncate(items.make.typecode(that[k])), reg);
                        }
                        // invoke method
                        items.load(items.invoke(f));
                        // return from method
                        coder.emitop(ireturn + coder.truncate(
                            items.make.typecode(def.type.restype())));
                        // code generation finished
                        def.code.setEndPc(thisAdr);
                        coder.endScopes(0);
                        bridges.append(decl);
                    }
                }
                break;
            default:
                throw new InternalError();
        }
    }
}
