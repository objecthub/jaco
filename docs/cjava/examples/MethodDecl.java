public class C
{
    int val()
    {
	return 1;
    }
    int val(int base)
    {
	return base+val();
    }
    int val(Object o)
    {
	return o.hashCode();
    }
    int val(I1 i)
    {
	return i.i1*2 + val((Object)i);
    }
    int val(I2 i)
    {
	return i.i2*2 + val((Object)i);
    }
    int val([I1, I2] i)
    {
	return val((I1)i) + val((I2)i);
    }
}

class C2 extends C
{
    int val([I3, I1, I2] i)
    {
	return i.i3 + val(([I2, I1])i);
    }
}

