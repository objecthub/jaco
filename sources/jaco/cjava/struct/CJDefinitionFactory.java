package jaco.cjava.struct;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.cjava.component.*;
import jaco.java.struct.*;
import CJDefinition.*;
import Definition.*;


/**
 * Create the appropriate definitions of CJava.
 *
 */
public class CJDefinitionFactory extends DefinitionFactory implements CJDefinition.Factory
{
    /**
     * The two-faced registery to use.
     */
    protected TwoFacedRegistery twofaced;

    /**
     * Create a new definition factory.
     *
     * @param definitions the definitions component
     * @param types the component
     * @param twofaced the two-faced registery to use
     */
    public CJDefinitionFactory(Definitions definitions, Types types, TwoFacedRegistery _twofaced)
    {
    super(definitions, types);
    twofaced = _twofaced;
    }

    /**
     * Return a special version of MethodDef that can have
     * two signatures.
     *
     * It returns a TwoFacedDefinition that descends from MethodDef. It
     * is put into standard representation by default.
     */
    public Definition MethodDef(int mods, Name name, Type type, Definition owner)
    {
    TwoFacedMethodDef   def = new TwoFacedMethodDef(null);
    def.init(twofaced);
    def.kind = FUN;
    def.modifiers = mods;
    def.name = name;
    def.type = type;
    def.owner = owner;
    def.def = def;
    def.fullname = definitions.formFullName(name, owner);
    return def;
    }
    /**
     * Return a special version of VarDef that can have
     * two signatures.
     *
     * It returns a TwoFacedDefinition that descends from VarDef. It
     * is put into standard representation by default.
     */
    public Definition VarDef(int mods, Name name, Type type, Definition owner)
    {
    TwoFacedVarDef      def = new TwoFacedVarDef(null, -1, -1);
    def.init(twofaced);
    def.kind = VAR;
    def.modifiers = mods;
    def.name = name;
    def.type = type;
    def.owner = owner;
    def.def = def;
    def.fullname = definitions.formFullName(name, owner);
    return def;
    }
    
    /**
     * Return a compound definition.
     *
     * @param name the name of the compound definition (mostly ignored)
     * @param type the type; its definition will be set to the new compounddef, it type is non-null
     * @param superclass the class that is part of the compound, or null
     * @param interfaces the list of interfaces that are part of the compound
     * @param owner the owner of the definition (class, package or method)
     */
    public Definition CompoundDef(Name name, CJType.CompoundType type, Type superclass, Type[] interfaces, Definition owner)
    {
    CompoundDef def = new CompoundDef(superclass, interfaces);
    def.modifiers = ModifierConst.PUBLIC|ModifierConst.ABSTRACT|ModifierConst.FINAL;
    def.kind = TYP;
    def.name = name;
    def.type = type;
    def.owner = owner;
    def.def = def;
    def.fullname = name;
    if(type!=null)
        type.setDef(def);
    return def;
    }

    /**
     * Return a class definition that is a CJClassDef.
     *
     * @see CJClassDef
     * @see CJBasicClassDef
     * @see TwoFacedDefinition
     */
    public Definition ClassDef(int mods, Name name, Type type, Definition owner)
    {
    CJClassDef  def = new CJClassDef(null, types.noTypes, null,
                         null, null);
    def.init(twofaced);
    def.kind = TYP;
    def.modifiers = mods;
    def.name = name;
    def.type = type;
    def.owner = owner;
    def.def = def;
    def.fullname = definitions.formFullName(name, owner);
    return def;
    }

    /**
     * Return a class definition that is a CJClassDef.
     *
     * @see CJClassDef
     * @see CJBasicClassDef
     * @see TwoFacedDefinition
     */
    public Definition ClassDef(int mods, Name name, Definition owner)
    {
    CJClassDef  def = new CJClassDef(null, types.noTypes, null,
                         null, null);
    def.init(twofaced);
    def.kind = TYP;
    def.modifiers = mods;
    def.name = name;
    def.type = Type.ClassType(types.packageType).setDef(def);
    def.owner = owner;
    def.def = def;
    def.fullname = definitions.formFullName(name, owner);
    return def;
    }
    
}
