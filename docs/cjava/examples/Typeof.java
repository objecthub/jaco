public class C 
{
    [I1, I2] a;
    [I1, I2, C1] b;

    void dummy(Object o)
    {
	if( o instanceof [I1, I2])
	    {
	    }
		
	if(getObject() instanceof [I1, I2, C1])
	    {
	    }

	if(a instanceof [I1, I2, C1])
	    {
	    }
    }

    Object getObject()
    {
	return this;
    }
}
