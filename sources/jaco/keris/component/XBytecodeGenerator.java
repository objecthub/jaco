//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    bytecode generator: covariant return types
//                           
//  [XBytecodeGenerator.java (5317) 2-Apr-01 00:45 -> 23-Jun-01 00:02]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.keris.struct.*;
import jaco.keris.context.*;
import java.io.IOException;
import Type.*;
import Definition.*;
import Tree.*;
import Item.*;


public class XBytecodeGenerator extends BytecodeGenerator
                                implements XModifierConst {
    
/** other components
 */
    protected XClassWriter writer;
    
/** the list of the current bridge methods
 */
    protected TreeList bridges;
        
/** component name
 */
    public String getName() {
        return "XBytecodeGenerator";
    }
    
/** component initialization
 */
    public void init(BackendContext context) {
        super.init(context);
        writer = (XClassWriter)((XCompilerContext)context.compilerContext).ClassWriter();
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
                break;
            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                            Tree[] thrown, Tree[] stats, MethodDef d):
                GenEnv localEnv = env.dup(tree);
                localEnv.enclMethod = (MethodDecl)tree;
                genTry(localEnv, false);
                // check if a bridge has to be inserted
                BridgeMethodSet bmset = (BridgeMethodSet)writer.bridges.get(d);
                if (bmset != null) {
                    Type[] ts = bmset.types();
                    for (int i = 0; i < ts.length; i++) {
                        //System.out.println("insert bridge method into " +
                        //                   d.owner + " for " + name + ": " + ts[i]);
                        // create a new MethodDef for the bridge method
                        MethodDef def = (MethodDef)definitions.make.MethodDef(
                            d.modifiers | SYNTHETIC,
                            name, ts[i], d.owner);
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
                        Item f = items.make.MemberItem(tree.pos, d,
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
