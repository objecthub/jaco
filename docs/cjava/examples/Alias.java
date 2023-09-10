
public class aI1I2 = [I1, I2];
public class Test 
{
    private class aI3I4 = [I3, I4];

    aI3I4 a;

    aI1I2 dummy([I3, I4] arg)
    {
	a = arg;
	return (aI1I2)arg;
    }

    void dummy(Object i)
    {
	class aI1I3 = [I1, I3];
	aI1I3 w = (aI1I3)i;
    }
}
