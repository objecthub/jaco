
public interface I1
{
    int i1 = 1;
    void i1();
}

public interface I2
{
    int i2 = 2;
    void i2();
}

/* package-local */
interface I3
{
    int i3 = 3;
    void i3();
}

/* package-local */
interface I4
{
    int i4 = 4;
    void i4();
}

public class C1
{
    void c1()
    {
	System.out.println("C1");
    }
    int getvalue()
    {
	return 1;
    }
}

public class C1Subclass extends C1
{
    void c1()
    {
	System.out.println("C1Subclass");
    }
    int getvalue()
    {
	return 0;
    }
}

public class C2
{
    void c2()
    {
	System.out.println("C2");
    }
    int getvalue()
    {
	return 2;
    }
}


public class C3
{
    void c3()
    {
	System.out.println("C3");
    }
    int getvalue()
    {
	return 3;
    }
}

