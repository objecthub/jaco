//      /   _ _      JaCo
//  \  //\ / / \     - java classfile reader
//   \//  \\_\_/     
//         \         Matthias Zenger, 09/04/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import java.io.*;
import Type.*;
import Definition.*;


public class ClassReader extends Component
                         implements DefCompleter, DefinitionConst,
                                    ClassfileConst, ModifierConst, AttributeConst
{
/** other components
 */
    public Classfiles           classfiles;
    public ErrorHandler         report;
    public Trees                trees;
    public Definitions          definitions;
    public Types                types;
    public Constants            constants;
    public Mangler              mangler;
    public Signatures           signatures;
    
/** attribute reader
 */
    protected AttributeReader   attrib;
    public int                  classAttr  = SOURCEFILE_ATTR
                                           | INNERCLASSES_ATTR
                                           | SYNTHETIC_ATTR
                                           | DEPRECATED_ATTR;
    public int                  methodAttr = CODE_ATTR
                                           | EXCEPTIONS_ATTR
                                           | SYNTHETIC_ATTR 
                                           | DEPRECATED_ATTR;
    public int                  fieldAttr  = CONSTANT_VALUE_ATTR
                                           | SYNTHETIC_ATTR
                                           | DEPRECATED_ATTR;


/** the classpath and the sourcepath
 */
    protected ClassPath         classPath;
    protected ClassPath         sourcePath;
    
/** strategies for finding files
 */
    protected FindFileStrategy  classfileFinder;
    protected FindFileStrategy  sourcefileFinder;

/** new trees
 */
    protected TreeList          newTrees;
    
/** the current class which is completed
 */
    protected Definition        target;
    
    protected boolean           busy = false;

/** classfile input buffer
 */
    public ClassfileInput       in;
    
/** the objects of the constant pool
 */
    protected Object[]          poolObj;

/** for every constant pool entry, an index into in.buf where the
 *  defining section of the entry is found
 */
    protected int[]             poolIdx;

/** include files with these suffixes
 */
    public String[]             classSuffix = {".class"};
    public String[]             sourceSuffix = {".java"};
    
    
/** component name
 */
    public String getName()
    {
        return "JavaClassReader";
    }
    
/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
        classfiles = context.Classfiles();
        report = context.ErrorHandler();
        trees = context.Trees();
        definitions = context.Definitions();
        types = context.Types();
        constants = context.Constants();
        mangler = context.Mangler();
        signatures = context.Signatures();
        
        // use class reader directly
        definitions.completer = this;
        newTrees = new TreeList();
        
        sourceSuffix = context.settings.suffixes;
        setPaths();
        classfileFinder = ClassfileFinder();
        sourcefileFinder = SourcefileFinder();
    }

/** set paths
 */
    protected void setPaths()
    {
        String classpath = ((JavaSettings)context.settings).classpath;
        if ((classpath == null) || (classpath.length() == 0))
            classPath = ClassPath();
        else
            classPath = ClassPath(classpath);
        String  sourcepath = ((JavaSettings)context.settings).sourcepath;
        if (sourcepath == null)
        {
            String[] suffix = new String[classSuffix.length + sourceSuffix.length];
            System.arraycopy(classSuffix, 0, suffix, 0, classSuffix.length);
            System.arraycopy(sourceSuffix, 0, suffix, classSuffix.length, sourceSuffix.length);
            classSuffix = suffix;
            sourcePath = classPath;
        }
        else
            sourcePath = SourcePath(sourcepath);
    }
    
/** factory method for the class path
 */
    protected ClassPath ClassPath()
    {
        return new ClassPath();
    }
    
    protected ClassPath ClassPath(String spec)
    {
        return new ClassPath(spec);
    }
    
/** factory method for the source path
 */
    protected ClassPath SourcePath(String sourcepath)
    {
        return new ClassPath(sourcepath);
    }
    
/** factory method for the classfile find strategy
 */
    protected FindFileStrategy ClassfileFinder()
    {
        return new ClassPathFinder(classPath);
    }
    
/** factory method for the classfile find strategy
 */
    protected FindFileStrategy SourcefileFinder()
    {
        return new ClassPathFinder(sourcePath);
    }
    
/** factory method for attribute readers
 */
    protected AttributeReader AttributeReader()
    {
        return new AttributeReader(this);
    }
    
/** get new trees and reset newTrees variable
 */
    public TreeList fetchNewTrees()
    {
        TreeList    treelist = newTrees;
        newTrees = new TreeList();
        return treelist;
    }
    
    public void completeDef(Definition def)
    {
        try
        {
            completeClass(def);
        }
        catch (IOException e)
        {
            report.error(trees.getPos(), "cannot.read.class", def.fullname, e);
        }
    }
    
    
//////////// error reporting

    protected void unrecogized(Name attrName)
    {
        if (false /* Switches.checkClassFile */)
            report.error(Position.NOPOS, "unrecogized.attr", attrName);
    }


//////////// constant pool access

/** index all constant pool entries, writing their start addresses into poolIdx
 */
    protected void indexPool()
    {
        poolIdx = new int[in.nextChar()];
        poolObj = new Object[poolIdx.length];
        int i = 1;
        while (i < poolIdx.length)
        {
            poolIdx[i++] = in.bp;
            byte    tag = in.nextByte();
            switch (tag)
            {
                case CONSTANT_UTF8:
                case CONSTANT_UNICODE:
                {
                    int len = in.nextChar();
                    in.skip(len);
                    break;
                }
                
                case CONSTANT_CLASS:
                case CONSTANT_STRING:
                    in.skip(2);
                    break;
                
                case CONSTANT_FIELDREF:
                case CONSTANT_METHODREF:
                case CONSTANT_INTFMETHODREF:
                case CONSTANT_NAMEANDTYPE:
                case CONSTANT_INTEGER:
                case CONSTANT_FLOAT:
                    in.skip(4);
                    break;
            
                case CONSTANT_LONG:
                case CONSTANT_DOUBLE:
                    in.skip(8);
                    i++;
                    break;
                
                default:
                    throw new RuntimeException("bad constant pool tag: " + tag +
                        " at " + (in.bp - 1));
            }
        }
    }

/** if name is an array type or class signature, return the
 *  corresponding type; otherwise return a class definition with given name
 */
    protected Object classOrType(Name name)
    {
        if ((name.sub(0) == '[') || (name.sub(name.length() - 1) == ';'))
        {
            byte[] ascii = name.toAscii();
            return signatures.sigToType(ascii, 0, ascii.length);
        }
        else
        {
            return definitions.defineClass(name);
        }
    }

/** read constant pool entry at start address i, use poolObj as a cache.
 */
    public Object readPool(int i)
    {
        if (poolObj[i] != null)
            return poolObj[i];
        
        int index = poolIdx[i];
        if (index == 0)
            return null;
        
        switch (in.byteAt(index))
        {
            case CONSTANT_UTF8:
                poolObj[i] = Name.fromAscii(in.buf, index + 3, in.getChar(index + 1));
                break;
                
            case CONSTANT_UNICODE:
                throw new RuntimeException("can't read unicode strings in class files");
                
            case CONSTANT_CLASS:
                poolObj[i] = classOrType(readExternal(in.getChar(index + 1)));
                break;
            
            case CONSTANT_STRING:
                poolObj[i] = constants.toType(constants.make.
                                StringConst((Name)readPool(in.getChar(index + 1))));
                break;
            
            case CONSTANT_FIELDREF:
            {
                Definition  owner = (Definition)readPool(in.getChar(index + 1));
                NameAndType nt = (NameAndType)readPool(in.getChar(index + 3));
                poolObj[i] = definitions.make.VarDef(0,nt.name,
                                signatures.sigToType(Name.names, nt.sig.index, nt.sig.length()),
                                owner);
                break;
            }
            
            case CONSTANT_METHODREF:
            case CONSTANT_INTFMETHODREF:
            {
                Definition  owner = (Definition)readPool(in.getChar(index + 1));
                NameAndType nt = (NameAndType)readPool(in.getChar(index + 3));
                poolObj[i] = definitions.make.MethodDef(0, nt.name,
                                signatures.sigToType(Name.names, nt.sig.index, nt.sig.length()),
                                owner);
                break;
            }

            case CONSTANT_NAMEANDTYPE:
                poolObj[i] = new NameAndType((Name)readPool(in.getChar(index + 1)),
                                            readExternal(in.getChar(index + 3)));
                break;

            case CONSTANT_INTEGER:
                poolObj[i] = constants.toType(constants.make.
                                IntConst(in.getInt(index + 1)));
                break;
            
            case CONSTANT_FLOAT:
                poolObj[i] = constants.toType(constants.make.
                                FloatConst(in.getFloat(index + 1)));
                break;
            
            case CONSTANT_LONG:
                poolObj[i] = constants.toType(constants.make.
                                LongConst(in.getLong(index + 1)));
                break;
            
            case CONSTANT_DOUBLE:
                poolObj[i] = constants.toType(constants.make.
                                DoubleConst(in.getDouble(index + 1)));
                break;
            
            default:
                throw new RuntimeException("bad constant pool tag: " + in.byteAt(index));
        }
        return poolObj[i];
    }

/** read a constant pool string and convert to internal representation.
 */
    protected Name readExternal(int i)
    {
        if (poolObj[i] == null)
        {
            int index = poolIdx[i];
            if (in.byteAt(index) == CONSTANT_UTF8)
            {
                int len = in.getChar(index + 1);
                byte[] translated = classfiles.internalize(in.buf, index + 3, len);
                poolObj[i] = Name.fromAscii(translated, 0, len);
            }
        }
        return (Name)poolObj[i];
    }


//////////// Reading Types

/** read a constant pool string and convert to a type
 */
    public Type readType(int i)
    {
        Name    sig = readExternal(i);
        return signatures.sigToType(Name.names, sig.index, sig.length());
    }


//////////// Reading Definitions

/** read a class name
 */
    public Name readClassName(int i)
    {
        return ((Definition)readPool(i)).fullname;
    }

/** read a field
 */
    public Definition readField()
    {
        int modifiers = in.nextChar();
        Name name = (Name)readPool(in.nextChar());
        Type type = readType(in.nextChar());
        Definition v = definitions.make.VarDef(modifiers, name, type, target);
        return attrib.readAttributes(v, fieldAttr);
    }

/** read a method
 */
    public Definition readMethod()
    {
        int modifiers = in.nextChar();
        Name name = (Name)readPool(in.nextChar());
        Type ftype = readType(in.nextChar());
        Definition  f = definitions.make.MethodDef(modifiers & (~0x040), name, ftype, target);
        if ((modifiers & 0x040) != 0) {
            attrib.readAttributes(f, methodAttr);
        	return null;
        } else
        	return attrib.readAttributes(f, methodAttr);
    }
	
    protected Definition readClass(Definition c)
    {
        Scope       locals = new Scope(null, c);
        c.setLocals(locals);
        target = c;
        
        c.modifiers = in.nextChar();
        Name ownName = readClassName(in.nextChar());
        if (c.fullname != ownName)
            throw new RuntimeException("class file '" + c.fullname +
                                       "' contains wrong class " + ownName);
        
        int sci = in.nextChar();
        c.setSupertype((sci == 0) ? null :
                                    definitions.defineClass(readClassName(sci)).type);
    
        Type[] is = new Type[in.nextChar()];
        c.setInterfaces(is);
        for  (int i = 0; i < is.length; i++)
            is[i] = definitions.defineClass(readClassName(in.nextChar())).type;

        char fieldCount = in.nextChar();
        for (int i = 0; i < fieldCount; i++)
            locals.enter(readField());
        
        char methodCount = in.nextChar();
        for (int i = 0; i < methodCount; i++) {
        	Definition def = readMethod();
        	if (def != null)
            	locals.enter(def);
        }
		
        return attrib.readAttributes(c, classAttr);
    }

/** read a class file
 */
    protected void readClassfileStruct(Definition c) throws IOException
    {
        try
        {
            int magic = in.nextInt();
            if (magic != JAVA_MAGIC)
                throw new RuntimeException("illegal start of class file");
            int minorVersion = in.nextChar();
            int majorVersion = in.nextChar();
            if ((majorVersion < JAVA_MAJOR_VERSION) ||
                ((majorVersion == JAVA_MAJOR_VERSION) &&
                 (minorVersion < JAVA_MINOR_VERSION)))
                throw new RuntimeException("class file has wrong version " +
                        majorVersion + "." + minorVersion + ", should be at least " +
                        JAVA_MAJOR_VERSION + "." + JAVA_MINOR_VERSION);
            else
            if (false /* Switches.checkClassFile */ && (minorVersion > JAVA_MINOR_VERSION))
            {
                report.print("class file has later version than expected: " +
                            minorVersion);
            }
            indexPool();
            readClass(c);
            Mangler.Mangle  info = mangler.get(c.fullname);
            if (info != null)
                c.modifiers = info.mods;
        }
        catch (RuntimeException ex)
        {
            ex.printStackTrace();
            throw new IOException("bad class file (" + ex + ")");
        }
    }


////////// Loading Classes

/** check there is no class & package with same name
 */
    protected void checkNotInClasses(Definition p) throws IOException
    {
        if (definitions.getClass(p.fullname) != null)
            throw new IOException("package and class with same name: " + p.fullname);
    }
    
    protected void checkNotInPackages(Definition p) throws IOException
    {
        if (definitions.getPackage(p.fullname) != null)
            throw new IOException("class and package with same name: " + p.fullname);
    }
    
    protected CompilationEnv makeTopLevelEnv(VirtualFile file) throws IOException
    {
        Sourcefile  source = new Sourcefile(file);
        if (((JavaSettings)context.settings).encoding != null)
            source.setEncoding(((JavaSettings)context.settings).encoding);
        return ((MainContext)context).CompilationEnv(source);
    }
    
/** complete a class definition by reading the classfile
 */
    public void readClassfile(Definition c) throws IOException
    {
        busy = true;
        try
        {
            String      filename = classfiles.externalizeFileName(c.fullname) +
                                   ".class";
            VirtualFile f = null;
            long        msec = System.currentTimeMillis();
            if (c.owner == definitions.emptyPackage)
                f = VirtualFile.open(null, filename);
            if (f == null)
                f = classfileFinder.getFile(filename);
            in = new ClassfileInput(f);
            attrib = AttributeReader();
            readClassfileStruct(c);
            report.operation("loaded " + f.getPath(), msec);
            busy = false;
            for (Definition e = c.locals().elems; e != null; e = e.sibling)
                if (e.def.kind == TYP)
                    e.def.complete();
        }
        finally
        {
            busy = false;
        }
    }
    
/** complete a class definition by reading the sourcefile
 */
    public void readSourcefile(Definition c) throws IOException
    {
        String      filename = classfiles.externalizeFileName(c.fullname) + ".java";
        VirtualFile f = null;
        if (c.owner == definitions.emptyPackage)
            f = VirtualFile.open(null, filename);
        if (f == null)
            f = sourcefileFinder.getFile(filename);
        CompilationEnv  info = makeTopLevelEnv(f);
        Tree            cu = trees.make.CompilationUnit(null, info);
        TreeList        treelist = new TreeList(cu);
        info.shortRun = true;
        try
        {
            cu = treelist.process(((MainContext)context).Compiler()).chain().head;
        }
        catch (AbortCompilation e)
        {
            if (e.info instanceof Tree)
                cu = (Tree)e.info;
        }
        info.reset();
        newTrees.append(cu);
        if (((ClassDef)c).locals == null) {
            c.setLocals(new Scope(null, c));
            report.error(Position.NOPOS, "wrong.type.in.file", filename, c.fullname, c.owner);
        }
    }
    
/** complete loading class c
 */
    public void completeClass(Definition c) throws IOException
    {
        report.assert(!busy);
        try
        {
            readClassfile(c);
        }
        catch (IOException e)
        {
            try
            {
                readSourcefile(c);
            }
            catch (IOException f)
            {
                throw e;
            }
        }
    }
    
/** load a class with name classname, where pname is the name of the current
 *  package. If classname is unqualfified - i.e. does not contain a '.',
 *  prefix pname to it. Also load all referenced classes
 */
    public Definition loadClass(Name fullname) throws IOException
    {
        Definition  c = definitions.getClass(fullname);
        boolean     absent = (c == null);
        if (absent)
            c = definitions.defineClass(fullname);
        if (c.completer != null)
        {
            try
            {
                c.completer = null;
                completeClass(c);
                checkNotInPackages(c);
            }
            catch (IOException e)
            {
                if (absent)
                    definitions.removeClass(c.fullname);
                else if (((ClassDef)c).locals == null)
                    c.setLocals(new Scope(null, c));
                throw e;
            }
            busy = false;
        }
        return c;
    }

/** establish reference to an existsing package
 */
    public Definition loadPackage(Name fullname) throws IOException
    {
        Definition  p = definitions.getPackage(fullname);
        if (p == null)
        {
            p = definitions.definePackage(fullname);
            checkNotInClasses(p);
        }
        return p;
    }

/** read directory of a classpath directory and include classes
 *  in package scope
 */
    protected void includeClasses(VirtualFile dir, Definition p, String[] suffix)
    {
        if (dir != null)
        {
            String[] filenames = null;
            try
            {
                filenames = dir.list();
            }
            catch (IOException e) {}
            if (filenames != null)
            {
                Scope   locals = p.locals();
                for (int j = 0; j < filenames.length; j++)
                {
                    String  fname = filenames[j];
                    for (int i = 0; i < suffix.length; i++)
                        if (fname.endsWith(suffix[i]))
                        {
                            locals.enterIfAbsent(definitions.defineClass(
                                definitions.formFullName(
                                    Name.fromString(fname.substring(
                                        0, fname.length() - suffix[i].length())), p)));
                            break;
                        }
                }
            }
        }
    }
    
/** load directory of package and extract scope of package
 */
    public Scope directory(Definition p, ClassPath classPath, String[] filter) {
        if (p.locals() != null)
            return p.locals();
        String  dirname;
        Scope   res;
        p.setLocals(res = new Scope(null, p));
        if (p.fullname.length() == 0) {
            includeClasses(VirtualFile.open(null, "."), p, filter);
            dirname = null;
        } else {
            dirname = classfiles.externalizeFileName(p.fullname);
            if (!dirname.endsWith("/"))
                dirname += "/";
        }
        String[]    base = classPath.decompose();
        for (int i = 0; i < base.length; i++)
            includeClasses(VirtualFile.open(base[i], dirname), p, filter);
        return res;
    }
    
    public Scope directory(Definition p)
    {
        Scope   s = directory(p, classPath, classSuffix);
        if (classPath == sourcePath)
            return s;
        else
            return directory(p, sourcePath, sourceSuffix);
    }
}
