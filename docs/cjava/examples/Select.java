public class C extends C1 implements I1, I2
{
    [I1, I2] a;
    [I1, I2, C1] b;

    void dummy()
    {
	b = this;
	a = this;
	b.i1();
	b.i2();
	b.c1();
	a.i1();
	a.i2();
	int w = a.i1 + a.i2;
    }

    public void i1() 
    {
    }
    public void i2()
    {
    }
}
