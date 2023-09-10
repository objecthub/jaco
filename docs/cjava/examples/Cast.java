public class C 
{
    [I1, I2] a;
    [I1, I2, C1] b;

    void dummy(Object o)
    {
	[I1, I2, I3] e = ([I1, I2, I3])b;
	e = ([I1, I2, I3])a;
    }
}
