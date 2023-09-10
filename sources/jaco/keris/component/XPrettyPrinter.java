//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    pretty printer
//                           
//  [XPrettyPrinter.java (5869) 24-Apr-01 17:39 -> 23-Jun-01 00:10]

package jaco.keris.component;

import jaco.framework.*;
import jaco.keris.component.*;
import jaco.java.component.*;
import jaco.keris.struct.*;
import jaco.java.struct.*;
import XTree.*;
import Tree.*;
import java.io.*;


public class XPrettyPrinter extends PrettyPrinter
                            implements XModifierConst, XTreeConst {
    
    public XPrettyPrinter() {
        super();
    }
    
    public XPrettyPrinter(PrintWriter out) {
        super(out);
    }
    
    public XPrettyPrinter(String suffix) {
        super(suffix);
    }
    
    public String getName() {
        return "XPrettyPrinter";
    }
    
    public PrettyPrinter printDecl(Tree tree) {
        switch ((XTree)tree) {
            case ClassDecl(Name name, int mods, Tree extending,
                           Tree[] implementing, Tree[] members, _):
                XClassDecl xcd = (XClassDecl)tree;
                Name prev = classname;
                classname = name;
                print(modifiers.toString(mods & ~INTERFACE));
                if ((mods & CLASSFIELD) != 0) {
                    print(modifiers.toString(mods)).print("class " + name);
                    if (extending != null)
                        switch (extending) {
                            case CompoundType(Tree[] ts):
                                print(" extends ").printExprs(ts);
                                break;
                            default:
                                print(" extends ").printExpr(extending);
                        }
                    if ((implementing != null) && (implementing.length > 0))
                        print(" implements ").printExprs(implementing);
                    if (xcd.superimpl == null) {
                        print(" =");
                        if (members == null) {
                            classname = prev;
                            return print(";");
                        }
                    } else {
                        print(" = ").printExpr(xcd.superimpl);
                        if ((xcd.included != null) && (xcd.included.length > 0))
                            print(" includes ").printExprs(xcd.included);
                        if (members == null) {
                            classname = prev;
                            return print(";");
                        }
                    }
                } else if ((mods & MODULE) != 0) {
                    if ((mods & INTERFACE) != 0) {
                        print("module interface " + name);
                        if (implementing.length > 0) {
                            if ((mods & SPECIALIZES) != 0)
                                print(" inherits ").printExprs(implementing);
                            else
                                print(" extends ").printExprs(implementing);
                        }
                        implementing = ((XTree.XClassDecl)tree).required;
                        if ((implementing != null) &&
                            (implementing.length > 0))
                            print(" requires ").printExprs(implementing);
                    } else {
                        print("module " + name);
                        if (extending != null) {
                            if ((mods & SPECIALIZES) != 0)
                                print(" specializes ").printExpr(extending);
                            else
                                print(" refines ").printExpr(extending);
                        }
                        if (implementing.length > 0)
                            print(" implements ").printExprs(implementing);
                        implementing = ((XTree.XClassDecl)tree).required;
                        if ((implementing != null) &&
                            (implementing.length > 0))
                            print(" requires ").printExprs(implementing);
                    }
                } else if ((mods & INTERFACE) != 0) {
                    print("interface " + name);
                    if (implementing.length > 0)
                        print(" extends ").printExprs(implementing);
                } else if (((mods & ALGEBRAIC) != 0) && (members.length > 0)) {
                    print("class " + name);
                    if (extending != null)
                        print(" extends ").printExpr(extending);
                    println();
                    indent();
                    for (int i = 0; i < members.length - 1; i++) {
                        align();
                        printDecl(members[i]).print(",").println();
                    }
                    align();
                    printDecl(members[members.length - 1]).print(";").println();
                    undent();
                    align();
                    classname = prev;
                    return this;
                } else {
                    if ((mods & XModifierConst.FRAGMENT) != 0)
                        print("fragment " + name);
                    else
                        print("class " + name);
                    if (extending != null)
                        print(" extends ").printExpr(extending);
                    if ((implementing != null) &&
                        (implementing.length > 0))
                        print(" implements ").printExprs(implementing);
                    if ((xcd.required != null) &&
                        (xcd.required.length > 0))
                        print(" requires ").printExprs(xcd.required);
                    if ((xcd.included != null) &&
                        (xcd.included.length > 0))
                        print(" includes ").printExprs(xcd.included);
                }
                printClassBlock(members);
                classname = prev;
                return this;
            case ModuleFieldDecl(int mods, Tree impl, Tree[] intf):
                print(modifiers.toString(mods)).print("module ");
                printExpr(impl);
                if (intf == null)
                    return print(";");
                else
                    return print(" implements ").printExprs(intf).print(";");
            case Import(int tag, Tree qualid):
                if ((tag & IMPORTMODULE) != 0)
                    return print("import ").printExpr(qualid).print(";");
                else if ((tag & IMPORTMODULESTAR) != 0)
                    return print("import ").printExpr(qualid).print("::*;");
                else if ((tag & INCLUDEMODULE) != 0)
                    return print("include ").printExpr(qualid).print(";");
                else
                    return super.printDecl(tree);
            case CaseDecl(Name name, int mods, VarDecl[] fields,
                          Tree[] inits, _):
                print(modifiers.toString(mods)).print("case ").print(name);
                if (fields != null)
                    print("(").printExprs(fields).print(")");
                return this;
            case Include(Tree fragment, Tree[] args, Tree[] classes):
                print("include ").printExpr(fragment);
                print("(").printExprs(args).print(") in ");
                return printExprs(classes).print(";");
        }
        return super.printDecl(tree);
    }
    
    public PrettyPrinter printExpr(Tree tree, int prec) {
        if (tree == null)
            return print("<null>");
        switch ((XTree)tree) {
            case VarDecl(Name name, int mods, Tree vartype, Tree init, _):
                if (name == null)
                    return print("_");
                else
                    return super.printExpr(tree, prec);
            case Overrides(Tree overriding, Tree[] overridden):
                return printExpr(overriding).print(" for ")
                        .printExprs(overridden);
            case ModuleSelect(Tree from, Name selector, _):
                return printExpr(from, 11).print("::" + selector);
            case CompoundType(Tree[] fields):
                print("[");
                if (fields.length == 0)
                    return print("]");
                printExpr(fields[0]);
                for (int i = 1; i < fields.length; i++)
                    print(", ").printExpr(fields[i]);
                return print("]");
            case Self(Tree clazz, int tag, _):
                if (clazz != null)
                    printExpr(clazz, operators.postfixPrec).print(".");
                if (tag == XTreeConst.SUPER)
                    return print("super");
                else if (tag == XTreeConst.FRAGMENT)
                    return print("fragment");
                else
                    return print("this");
            default:
                return super.printExpr(tree, prec);
        }
    }
    
    public PrettyPrinter printStat(Tree tree) {
        switch (tree) {
            case Block(int mods, Tree[] stats):
                if ((mods & CASEBLOCK) != 0)
                    print("/* case */ ");
                return printBlock(stats);
            default:
                return super.printStat(tree);
        }
    }
}
