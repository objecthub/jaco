public class C extends C1 implements I1, I2
{
    void dummy()
    {
	[I1, I2] a;
	[I1, I2, C1] b;
	
	b = this;
	a = b;
	b = null;
    }
}
