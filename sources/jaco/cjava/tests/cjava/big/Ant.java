package test.big;

public class Ant extends Bug
{
    public Ant()
    {
    super();
    setName("ant");
    }
    public [Named, Transformable] transform()
    {
    [Named, Transformable] bug = new Bug();
    bug.setName("Ex-ant");
    return bug;
    }

}
