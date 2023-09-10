public class C 
{
    [I1, I2] i1i2;
    [I1, I2, C1] i1i2c1;
    [I1, C3] i1;
    [Object, I2] i2;

    [[I1], [Object, I3]] i1i3;

    void dummy()
	{
	    [I1, I2] w;
	}
}
