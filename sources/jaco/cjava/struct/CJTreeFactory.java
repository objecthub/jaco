package jaco.cjava.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import Tree.*;
import CJTree.*;
import Definition.*;


public class CJTreeFactory extends TreeFactory implements CJTree.Factory
{
    public CJTree CompoundType(Tree[] trees)
    {
    return new CompoundType(trees);
    }

    public CJTree AliasDecl(Name name, int mods, Tree type)
    {
    return new AliasDecl(name, mods, type, null);
    }
}

public class CJTreeCreate extends TreeCreate implements CJTree.Factory
{
    public CJTree CompoundType(Tree[] types)
    {
    CompoundType    t = new CompoundType(types);
    t.pos = protoPos;
    return t;
    }

    public CJTree AliasDecl(Name name, int mods, Tree type)
    {
    AliasDecl t= new AliasDecl(name, mods, type, null);
    t.pos = protoPos;
    return t;
    }
}

public class CJTreeRedef extends TreeRedef implements CJTree.Factory
{
    public CJTree CompoundType(Tree[] types)
    {
    CompoundType    t = (CompoundType)protoTree;
    t = new CompoundType(types);
    t.pos = protoTree.pos;
    t.type = protoTree.type;
    return t;
    }

    public CJTree AliasDecl(Name name, int mods, Tree type)
    {
    AliasDecl t= new AliasDecl(name, mods, type, null);
    t.pos = protoTree.pos;
    t.type = protoTree.type;
    return t;
    }

}
