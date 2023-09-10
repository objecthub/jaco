//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import jaco.framework.*;
import jaco.java.component.*;
import Definition.*;


public class DefinitionFactory implements Definition.Factory, DefinitionConst
{
    public Definitions  definitions;
    public Types        types;
    
    
    public DefinitionFactory(Definitions definitions, Types types)
    {
        this.definitions = definitions;
        this.types = types;
    }
    
    public Definition PackageDef(Name name, Type type, Definition owner)
    {
        PackageDef  def = new PackageDef(null);
        def.kind = PCK;
        def.name = name;
        def.type = type;
        def.owner = owner;
        def.def = def;
        def.fullname = definitions.formFullName(name, owner);
        type.setDef(def);
        return def;
    }
    
    public Definition ClassDef(int mods, Name name, Type type, Definition owner)
    {
        ClassDef    def = new ClassDef(null, types.noTypes, null,
                                              null, null);
        def.kind = TYP;
        def.modifiers = mods;
        def.name = name;
        def.type = type;
        def.owner = owner;
        def.def = def;
        def.fullname = definitions.formFullName(name, owner);
        return def;
    }
    
    public Definition ClassDef(int mods, Name name, Definition owner)
    {
        ClassDef    def = new ClassDef(null, types.noTypes, null,
                                              null, null);
        def.kind = TYP;
        def.modifiers = mods;
        def.name = name;
        def.type = types.make.ClassType(types.packageType).setDef(def);
        def.owner = owner;
        def.def = def;
        def.fullname = definitions.formFullName(name, owner);
        return def;
    }
    
    public Definition MethodDef(int mods, Name name, Type type, Definition owner)
    {
        MethodDef   def = new MethodDef(null);
        def.kind = FUN;
        def.modifiers = mods;
        def.name = name;
        def.type = type;
        def.owner = owner;
        def.def = def;
        def.fullname = definitions.formFullName(name, owner);
        return def;
    }
    
    public Definition ConstructorDef(int mods, Type type, Definition owner)
    {
        return MethodDef(mods, PredefConst.INIT_N, type, owner);
    }
    
    public Definition OperatorDef(Name name, Type type, int opcode)
    {
        OperatorDef def = new OperatorDef(opcode);
        def.kind = FUN;
        def.modifiers = ModifierConst.PUBLIC | ModifierConst.STATIC;
        def.name = name;
        def.type = type;
        def.owner = definitions.predefClass;
        def.def = def;
        def.fullname = def.name;
        return def;
    }
    
    public Definition VarDef(int mods, Name name, Type type, Definition owner)
    {
        VarDef      def = new VarDef(null, -1, -1);
        def.kind = VAR;
        def.modifiers = mods;
        def.name = name;
        def.type = type;
        def.owner = owner;
        def.def = def;
        def.fullname = definitions.formFullName(name, owner);
        return def;
    }
    
    public Definition ErrorDef(Definition owner)
    {
        ErrorDef    err = new ErrorDef();
        err.kind = ANY;
        err.modifiers = ModifierConst.PUBLIC | ModifierConst.STATIC;
        err.name = Name.fromString("<error def>");
        err.type = Type.ErrType;
        err.owner = owner;
        err.shadowed = Scope.sentinel;
        err.fullname = err.name;
        err.def = err;
        return err;
    }
}
