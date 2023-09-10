package test.big;
import java.util.*;
public class Main
{
    public static void main(String[] argv)
    {
    Main m = new Main();
    m.start();
    }

    private class ListImpl = LinkedList; // list of Label
    private [Named, Transformable] initial = new Bug();
    private List list = new ListImpl();
    public void start()
    {
    class Working = [Named, Transformable];
    Working working = initial;
    int i=0;
    for(i=0; i<3; i++)
        {
        Label label = working.createLabel();
        working.setName("Working " + i);
        list.add(label);
        working = working.transform();
        }
    Iterator iter = list.iterator();
    i=0;
    while(iter.hasNext())
        {
        Label lab = (Label)iter.next();
        System.out.println("[" + i + "] :");
        System.out.print(' ');
        lab.print(System.out);
        System.out.print(' ');
        lab.update();
        lab.print(System.out);
        i++;
        }
    }

}
