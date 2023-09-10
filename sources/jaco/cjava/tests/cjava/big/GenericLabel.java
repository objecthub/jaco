package test.big;
import java.io.*;

abstract class GenericLabel implements Label
{
    protected String name;
    protected Named origin;

    public GenericLabel(Named _origin)
    {
    origin = _origin;
    name = _origin.getName();
    }
    
    public void update()
    {
    name = origin.getName();
    }

    public void print(PrintStream writer)
    {
    writer.println(name);
    }
}
