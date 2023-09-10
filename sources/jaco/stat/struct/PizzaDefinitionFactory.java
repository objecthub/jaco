//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import Definition.*;


public class PizzaDefinitionFactory extends DefinitionFactory
{
    public PizzaDefinitionFactory(Definitions definitions, Types types)
    {
        super(definitions, types);
    }
    
    public Definition ClassDef(int mods, Name name, Type type, Definition owner)
    {
        ClassDef    def = new CDef(name, mods, type, owner);
        def.fullname = definitions.formFullName(name, owner);
        return def;
    }
    
    public Definition ClassDef(int mods, Name name, Definition owner)
    {
        ClassDef    def = new CDef(name, mods, null, owner);
        def.type = Type.ClassType(types.packageType).setDef(def);
        def.fullname = definitions.formFullName(name, owner);
        return def;
    }
    
    public Definition MethodDef(int mods, Name name, Type type, Definition owner)
    {
        MethodDef   def = new MDef(name, mods, type, owner);
        def.fullname = definitions.formFullName(name, owner);
        return def;
    }
    
    public Definition VarDef(int mods, Name name, Type type, Definition owner)
    {
        VarDef      def = new VDef(name, mods, type, owner);
        def.fullname = definitions.formFullName(name, owner);
        return def;
    }
}
