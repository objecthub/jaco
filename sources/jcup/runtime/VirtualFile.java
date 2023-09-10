package jcup.runtime;

import java.io.*;
import java.util.zip.*;
import java.util.*;


public abstract class VirtualFile
{
/** separator
 */
    protected char              separator = File.separatorChar;
    
/** table of all opened jar-files
 */
    protected static Hashtable  opened = new Hashtable();


/** get name of the file
 */
    public abstract String getName();

/** get path of the file
 */
    public abstract String getPath();

/** does the file exist?
 */
    public abstract boolean exists();

/** is the file a directory?
 */
    public abstract boolean isDirectory();

/** read content of the file into a byte[] buffer
 */
    public abstract byte[] read() throws IOException;

/** list contents of a directory
 */
    public abstract String[] list() throws IOException;

/** open a new file
 */
    public abstract VirtualFile open(String name);

/** return an input stream for the file
 */
    public InputStream getInputStream() throws IOException
    {
        return new ByteArrayInputStream(read());
    }
    
/** open file 'name' in directory 'dirname'
 */
    public static VirtualFile open(String dirname, String name)
    {
        VirtualFile res;
        if (dirname == null)
            res = new PlainFile(new File(name));
        else
        if (name == null)
            res = new PlainFile(new File(dirname));
        else
        if (dirname.endsWith(".zip") || dirname.endsWith(".jar"))
        {
            VirtualFile dir = (VirtualFile)opened.get(dirname);
            if (dir == null)
            {
                dir = new ZipDir(new File(dirname));
                if (dir.isDirectory())
                    opened.put(dirname, dir);
            }
            res = dir.open(name);
        }
        else
            res = new PlainFile(new File(dirname, name));
        if (!res.exists())
            res = null;
        return res;
    }
    
/** create file given by a fully qualified name from root directory `outdir';
 *  create intermediate directories if they do not exist already
 */
    public static File create(File outdir, String name, String suffix) throws IOException
    {
        int     start = 0;
        int     end = name.indexOf('.');
        while (end >= start)
        {
            outdir = new File(outdir, name.substring(start, end));
            if (!outdir.exists())
                outdir.mkdir();
            start = end + 1;
            end = name.indexOf('.', start);
        }
        return new File(outdir, name.substring(start) + suffix);
    }
}

class PlainFile extends VirtualFile
{
    File f;

    PlainFile(File f)
    {
        this.f = f;
    }
    
    public String getName()
    {
        return f.getName();
    }
    
    public String getPath()
    {
        return f.getPath();
    }

    public boolean exists()
    {
        return f.exists();
    }

    public boolean isDirectory()
    {
        return f.isDirectory();
    }

    public byte[] read() throws IOException
    {
        FileInputStream in = new FileInputStream(f);
        int             rest = in.available();
        byte[]          buf = new byte[rest];
        do
        {
            int res = in.read(buf, buf.length - rest, rest);
            if (res == -1)
                throw new IOException("read error");
            rest -= res;
        } while (rest > 0);
        in.close();
        return buf;
    }

    public String[] list() throws IOException
    {
        return f.list();
    }

    public VirtualFile open(String name)
    {
        return new PlainFile(new File(f, name));
    }
}

class ZippedFile extends VirtualFile
{
    ZipDir      dir;
    ZipEntry    zipEntry;
    
    {
        separator = '/';
    }
    
    ZippedFile(ZipDir dir, String name)
    {
        this.dir = dir;
        if (dir.zipFile != null)
        {
            name = name.replace(File.separatorChar, separator);
            zipEntry = this.dir.zipFile.getEntry(name);
            if (zipEntry == null)
                zipEntry = this.dir.zipFile.getEntry(name + separator);
        }
    }
    
    public String getName()
    {
        return zipEntry.getName();
    }
    
    public String getPath()
    {
        return dir.getPath() + "(" + zipEntry.getName() + ")";
    }
    
    public boolean exists()
    {
        return (zipEntry != null);
    }
    
    public boolean isDirectory()
    {
        return zipEntry.isDirectory();
    }
    
    public byte[] read() throws IOException
    {
        InputStream     in = dir.zipFile.getInputStream(zipEntry);
        int             rest = in.available();
        byte[]          buf = new byte[rest];
        do
        {
            int res = in.read(buf, buf.length - rest, rest);
            if (res == -1)
                throw new IOException("read error");
            rest -= res;
        } while (rest > 0);
        in.close();
        return buf;
    }
    
    public String[] list() throws IOException
    {
        if (!isDirectory())
            throw new IOException("not a directory");
        return dir.list(zipEntry.getName());
    }

    public VirtualFile open(String name)
    {
        String  pathname = zipEntry.getName();
        return new ZippedFile(dir, pathname + name);
    }
}

class ZipDir extends VirtualFile
{
    File        f;
    ZipFile     zipFile;
    
    {
        separator = '/';
    }
    
    ZipDir(File f)
    {
        this.f = f;
        try
        {
            zipFile = new ZipFile(f);
        }
        catch (ZipException e) {}
        catch (IOException e) {}
    }
    
    public String getName()
    {
        return f.getName();
    }
    
    public String getPath()
    {
        return f.getPath();
    }

    public boolean exists()
    {
        return (zipFile != null);
    }

    public boolean isDirectory()
    {
        return (zipFile != null);
    }

    public byte[] read() throws IOException
    {
        throw new IOException("cannot read directory");
    }

    public String[] list(String prefix)
    {
        int n = 0;
        for (Enumeration enum = zipFile.entries(); enum.hasMoreElements();)
        {
            ZipEntry    e = (ZipEntry)enum.nextElement();
            if (e.getName().startsWith(prefix))
            {
                String  candidate = e.getName().substring(prefix.length());
                if (candidate.indexOf(separator) < 0)
                    n++;
            }
        }
        String[] filenames = new String[n];
        n = 0;
        for (Enumeration enum = zipFile.entries(); enum.hasMoreElements();)
        {
            ZipEntry    e = (ZipEntry)enum.nextElement();
            if (e.getName().startsWith(prefix))
            {
                String  candidate = e.getName().substring(prefix.length());
                if (candidate.indexOf(separator) < 0)
                    filenames[n++] = candidate;
            }
        }
        return filenames;
    }

    public String[] list() throws IOException
    {
        return list("");
    }
    
    public VirtualFile open(String name)
    {
        return new ZippedFile(this, name);
    }
}
