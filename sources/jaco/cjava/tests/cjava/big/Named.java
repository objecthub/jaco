package test.big;

public interface Label = [Updatable, Printable];
public interface Named
{
    String getName();
    void setName(String name);
    Label createLabel();
}

