package test.big;
import java.util.*;
public class Bug implements Named, Transformable
{
    protected Bug outer = this;
    private String name;
    private class BugLabel = GenericLabel;
    
    public Bug()
    {
    name = "bug";
    }
    public Label createLabel()
    {
    return new MyLabel(this);
    }
    public String getName()
    {
    return name;
    }
    public void setName(String _name)
    {
    name = _name;
    }
    public [Named, Transformable] transform()
    {
    [Named, Transformable] ant = new Ant();
    if(!name.equals("Bugs"))
    {
        ant.setName(getName());
    }
    return ant;
    }

    private class MyLabel extends GenericLabel
    {
    public MyLabel(Named orig)
    {
        super(orig);
    }
    }
}
