package jaco.jjava.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import java.util.*;

public class JTreeFactory extends TreeFactory {
    
    public Tree MethodDecl(Name name, int mods, Tree restype,
                           Tree.VarDecl[] params, Tree[] thrown, Tree[] stats)
    {
        return new JMethodDecl(name, mods, restype, params,
                               thrown, stats, null, null);
    }
    
    public JMethodDecl JMethodDecl(Name name, int mods, Tree restype,
                                   Tree.VarDecl[] params, Tree[] thrown,
                                   Tree[] stats, LinkedList joinpatterns)
    {
        return new JMethodDecl(name, mods, restype, params, thrown,
                           stats, null, joinpatterns);
    }
}

public class JTreeCreate extends TreeCreate {
    
    public Tree MethodDecl(Name name, int mods, Tree restype,
                           Tree.VarDecl[] params, Tree[] thrown, Tree[] stats)
    {
        Tree.MethodDecl t = new JMethodDecl(name, mods, restype, params,
                                        thrown, stats, null, null);
        t.pos = protoPos;
        return t;
    }
}


public class JTreeRedef extends TreeRedef
{
    public Tree MethodDecl(Name name, int mods, Tree restype,
                           Tree.VarDecl[] params, Tree[] thrown, Tree[] stats)
    {
        Tree.MethodDecl t = (Tree.MethodDecl)protoTree;
        t = new JMethodDecl(name, mods, restype, params, thrown, stats,
                            t.def, null);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
}
