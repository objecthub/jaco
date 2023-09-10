//      /   _ _      JaCo
//  \  //\ / / \     - processor that generates classfiles
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import java.io.*;
import Type.*;
import Definition.*;
import Constant.*;
import Tree.*;


public class ClassWriter extends Processor
                         implements AttributeConst, TypeConst,
                                    ModifierConst, DefinitionConst,
                                    ClassfileConst
{
/** other components
 */
    public Mangler              mangler;
    public Classfiles           classfiles;
    public Signatures           signatures;
    public Definitions          definitions;
    
/** the classfile output buffer
 */
    public ClassfileOutput      out;

/** attribute writer
 */
    protected AttributeWriter   attrib;
    public int                  classAttr  = SOURCEFILE_ATTR
                                           | SYNTHETIC_ATTR
                                           | DEPRECATED_ATTR
                                           | INNERCLASSES_ATTR;
    public int                  methodAttr = CODE_ATTR
                                           | EXCEPTIONS_ATTR
                                           | SYNTHETIC_ATTR
                                           | DEPRECATED_ATTR
                                           | BRIDGE_ATTR;
    public int                  fieldAttr  = CONSTANT_VALUE_ATTR
                                           | SYNTHETIC_ATTR
                                           | DEPRECATED_ATTR;

/** current constant pool
 */
    public Pool                 pool;

/** output directory
 */
    protected String            outDir;
    

/** component name
 */
    public String getName()
    {
        return "JavaClassWriter";
    }
    
/** description of tree processor
 */
    public String getDescription()
    {
        return "writing classfiles";
    }
    
/** component initialization
 */
    public void init(CompilerContext context)
    {
        super.init(context);
        mangler = context.mainContext.Mangler();
        classfiles = context.mainContext.Classfiles();
        signatures = context.mainContext.Signatures();
        definitions = context.mainContext.Definitions();
        
        out = new ClassfileOutput();
        outDir = ((JavaSettings)context.settings).outpath;
        
    }

/** factory method for attribute writers
 */
    public AttributeWriter AttributeWriter()
    {
        return new AttributeWriter(this);
    }
    

//////////// Signature Generation

/** Given a type t, return its extended class name:
 */
    public Name xClassName(Type t)
    {
        switch (t.deref())
        {
            case ClassType(_):
                return t.tdef().fullname;
            
            case ArrayType(_):
                return signatures.typeToSig(t);
            
            default:
                throw new InternalError("xClassName");
        }
    }


//////////// Writing the Constant Pool

/** write constant pool to active output buffer;
 *  note: during writing, constant pool might grow since some parts of constants
 *        still need to be entered
 */
    protected void writePool(Pool pool)
    {
        int adr = attrib.writeZeroChar();
        int i = 1;
        while (i < pool.pp)
            i = writePoolEntry(pool.pool[i], i);
        attrib.fixupChar(adr, pool.pp);
    }
    
    protected strictfp int writePoolEntry(Object value, int pp)
    {
        report.assert(value);
        if (value instanceof Name)
        {
            byte[]  ascii = ((Name)value).toAscii();
            out.writeByte(CONSTANT_UTF8);
            out.writeChar(ascii.length);
            out.writeBytes(ascii, 0, ascii.length);
        }
        else
        if (value instanceof NameAndType)
        {
            NameAndType nt = (NameAndType)value;
            out.writeByte(CONSTANT_NAMEANDTYPE);
            out.writeChar(pool.put(nt.name));
            out.writeChar(pool.put(nt.sig));
        }
        else
        if (value instanceof Definition)
        {
            Definition  def = (Definition)value;
            switch (def)
            {
                case ClassDef(_, _, _, _, _):
                    byte[]  ascii = def.fullname.toAscii();
                    out.writeByte(CONSTANT_CLASS);
                    out.writeChar(
                        pool.put(
                            Name.fromAscii(classfiles.externalize(ascii, 0,
                                    ascii.length), 0, ascii.length)));
                    break;
                    
                case MethodDef(_):
                    if ((def.owner.modifiers & INTERFACE) != 0)
                        out.writeByte(CONSTANT_INTFMETHODREF);
                    else
                        out.writeByte(CONSTANT_METHODREF);
                    out.writeChar(pool.put(def.owner));
                    out.writeChar(pool.put(nameType(def)));
                    break;
                
                case VarDef(_, _, _):
                    out.writeByte(CONSTANT_FIELDREF);
                    out.writeChar(pool.put(def.owner));
                    out.writeChar(pool.put(nameType(def)));
                    break;
                    
                default:
                    throw new InternalError();
            }
        }
        else
        if (value instanceof Constant)
        {
            Constant    c = (Constant)value;
            switch (c)
            {
                case IntConst(int val):
                    out.writeByte(CONSTANT_INTEGER);
                    out.writeInt(val);
                    break;
                
                case LongConst(long val):
                    out.writeByte(CONSTANT_LONG);
                    out.writeLong(val);
                    pp++;
                    break;
                
                case FloatConst(float val):
                    out.writeByte(CONSTANT_FLOAT);
                    out.writeFloat(val);
                    break;
                
                case DoubleConst(double val):
                    out.writeByte(CONSTANT_DOUBLE);
                    out.writeDouble(val);
                    pp++;
                    break;
                
                case StringConst(Name str):
                    out.writeByte(CONSTANT_STRING);
                    out.writeChar(pool.put(str));
                    break;
                
                default:
                    throw new InternalError();
            }
        }
        else
        if (value instanceof Type)
        {
//TODO: check whether this still works with inner classes
            Type type = (Type)value;
            out.writeByte(CONSTANT_CLASS);
            byte[]  ascii = xClassName((Type)value).toAscii();
            out.writeChar(
                pool.put(
                    Name.fromAscii(classfiles.externalize(ascii, 0, ascii.length),
                                       0, ascii.length)));
        }
        else
            throw new InternalError("writePoolEntry (" + value + ")");
        return pp + 1;
    }


//////////// Writing Objects

/** given a field, return its name
 */
    protected Name fieldName(Definition def)
    {
        if ((((def.modifiers & PRIVATE) != 0) &&
            false /* Switches.scramble */) ||
            ((def.modifiers & (PROTECTED | PUBLIC)) == 0 &&
            false /* Switches.scrambleAll */))
            return Name.fromString("_$" + def.name.index);
        else
            return def.name;
    }

/** given a definition def, return its name-and-type
 */
    protected NameAndType nameType(Definition def)
    {
        return new NameAndType(fieldName(def), signatures.typeToSig(def.type));
    }

/** write definitions of a special kind
 */
    public void writeDefinitions(Definition root, int kind)
    {
        int             adr = attrib.writeZeroChar();
        int             ndefs = 0;
        Definition[]    defs = new Definition[64];
        
        while (root != null)
        {
            if (root.def.kind == kind)
                try
                {
                    defs[ndefs++] = root.def;
                }
                catch (ArrayIndexOutOfBoundsException e)
                {
                    Definition[]    newDefs = new Definition[defs.length * 2];
                    System.arraycopy(defs, 0, newDefs, 0, defs.length);
                    defs = newDefs;
                    defs[ndefs - 1] = root.def;
                }
            root = root.sibling;
        }
        for (int i = ndefs - 1; i >= 0; i--)
            writeDefinition(defs[i].def);
        attrib.fixupChar(adr, ndefs);
    }


/** write definition, entering all references into given pool
 */
    protected void writeDefinition(Definition def)
    {
        switch (def)
        {
            case MethodDef(_):
                out.writeChar(def.modifiers & LANGUAGE_MODS);
                out.writeChar(pool.put(fieldName(def)));
                out.writeChar(pool.put(signatures.typeToSig(def.type)));
                break;
            
            case VarDef(_, _, _):
                out.writeChar(def.modifiers & LANGUAGE_MODS);
                out.writeChar(pool.put(fieldName(def)));
                out.writeChar(pool.put(signatures.typeToSig(def.type)));
                break;
                
            default:
                throw new InternalError();
        }
        writeAttributes(def);
    }
    
/** write the attributes of a definition
 */
    public void writeAttributes(Definition def)
    {
        switch (def)
        {
            case ClassDef(_, _, _, _, _):
                attrib.writeAttributes(classAttr, def);
                break;
                
            case MethodDef(Code code):
                attrib.writeAttributes(methodAttr, def);
                break;
                
            case VarDef(_, _, _):
                attrib.writeAttributes(fieldAttr, def);
                break;
                
            default:
                throw new InternalError();
        }
    }

/** write class information; return index of nattrs field for later
 *  modification if InnerClasses needs to be generated
 */
    public void writeClass(ClassDef c)
    {
        out.writeChar(((c.modifiers & LANGUAGE_MODS) & ~ STRICTFP) | ACC_SUPER);
        out.writeChar(c.pool.put(c));           // write reference to pool
        if (c.supertype() == null)              // write superclass
            out.writeChar(0);
        else
            out.writeChar(c.pool.put(c.supertype().tdef()));
        Type[] is = c.interfaces();             // write interfaces
        out.writeChar(is.length);
        for (int i = 0; i < is.length; i++)
            out.writeChar(c.pool.put(is[i].tdef()));
        Definition  elems = c.locals().elems;
        writeDefinitions(elems, VAR);           // write fields
        writeDefinitions(elems, FUN);           // write methods
        writeAttributes(c);                     // write class attributes
    }
    
/** write class c to outstream
 */
    public void writeClassFile(ClassDef c) throws IOException
    {
        long    msec = System.currentTimeMillis();
        pool = c.pool;
        out.recycle();
        attrib = AttributeWriter();
        writeClass(c);
        if (c.pool.pp > 0xffff)
            throw new IOException("too many constants in the constant pool");
        out.usePoolBuffer();
        out.writeInt(JAVA_MAGIC);
        out.writeChar(JAVA_MINOR_VERSION);
        out.writeChar(JAVA_MAJOR_VERSION);
        writePool(c.pool);
        out.appendOtherBuffer();
        File    outfile = openOutput(c.sourcefile, c.fullname, classSuffix());
        out.writeBuffer(new FileOutputStream(outfile)).close();
        report.operation("wrote " + outfile.getPath(), msec);
    }

/** write all classfiles
 */
    public void writeClasses(Tree[] decls)
    {
        for (int i = 0; i < decls.length; i++)
            switch (decls[i])
            {
                case ClassDecl(_, _, _, _, Tree[] members, ClassDef def):
                    try
                    {
                        writeClassFile(def);
                    }
                    catch (IOException e)
                    {
                        report.error(decls[i].pos, "cannot.write.class",
                                     def.name + classSuffix(), e);
                    }
                    writeClasses(members);
                    break;
            }
    }

/** suffix for classfiles
 */
    public String classSuffix()
    {
        return ".class";
    }
    
/** open output file for a given class
 */
    protected File openOutput(Name sourcefile, Name fullname, String suffix) throws IOException
    {
        if (outDir == null)
        {
            String  filename = mangler.shortName(fullname) + suffix;
            String  sourcedir = new File(sourcefile.toString()).getParent();
            if (sourcedir == null)
                return new File(filename);
            else
                return new File(sourcedir, filename);
        }
        else
            return VirtualFile.create(new File(outDir), fullname.toString(), suffix);
    }

/** the debugging name
 */ 
    public String getDebugName()
    {
        return "writer";
    }

/** the classwriter as a tree processor
 */
    protected Tree process(CompilationUnit tree) throws AbortCompilation
    {
        writeClasses(tree.decls);
        return tree;
    }
    
    protected boolean needsProcessing(CompilationEnv info)
    {
        return info.codeGenerated;
    }
}
