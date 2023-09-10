package jaco.cjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import java.io.*;
import Type.*;
import Definition.*;

import jaco.java.component.*;
import jaco.cjava.context.*;

/**
 * Add new attributes writted by CJAttributeReader.
 */
public class CJClassReader extends ClassReader implements CJAttributeConst
{
    public CJClassReader()
    {
    methodAttr |= CJAVA_REALSIG_ATTR;
    fieldAttr |= CJAVA_REALVARTYPE_ATTR;
    classAttr |= ALIAS_ATTR;
    }

    public String getName()
    {
    return "CJClassReader";
    }

    protected AttributeReader AttributeReader()
    {
    return new CJAttributeReader(this);
    }
}

