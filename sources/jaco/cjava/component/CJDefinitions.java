package jaco.cjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.cjava.struct.*;
import jaco.cjava.context.*;

public class CJDefinitions extends Definitions
{
    /** use the cj definition factory.
     *
     * @see CJDefinition.Factory
     */
    protected DefinitionFactory DefinitionFactory(Types types)
    {
    return new CJDefinitionFactory(this, types, TwoFaced());
    }

    /** initialization */
    public void init(MainContext context)
    {
    super.init(context);

    }

    /**
     * Two-Faced Definitions registery
     */
    protected TwoFacedRegistery twofaced;
    /**
     * Get the Two-Faced Definitions registery.
     *
     * Create the registery, if necessary.
     * @result the registery
     */
    public TwoFacedRegistery TwoFaced()
    {
    if(twofaced==null)
        twofaced = new TwoFacedRegistery();
    return twofaced;
    }

    /* Sometimes, defineClass is called with slash as a separator, sometimes
     * (most of the time) with dot.
     *
     * is that a bug in jaco ? is that normal ? 
     * see check3/Import; it fails without this fix.
     */
    public Definition defineClass(Name fullname)
    {
    fullname = fullname.replace((byte)'/', (byte)'.');
    return super.defineClass(fullname);
    }
}
