package jaco.cjava.struct;

import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.cjava.component.*;
import jaco.cjava.context.*;

public class CJTypeFactory extends TypeFactory implements CJType.Factory
{
    public CJTypeFactory(Types types)
    {
    super(types);
    }

    public Type CompoundType(Type[] components)
    {
    return CJType.CompoundType(components);
    }

    public Type AliasType(CJType.AliasCompleter completer)
    {
    return CJType.AliasType(completer);
    }
}
