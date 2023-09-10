package fake.test;
import java.io.*;
interface A1 {
 void a1();
 Object obj();
}
interface A2 {
    void a2();
}
interface A3 
{
}

public class aliasA2 = A2;
public class aliasA2C = [A2];
public interface aliasA3 = A3;
public class Test implements A1, A2
{
    public class aliasA1A2 = [A1, fake.test.aliasA2C];
    public class fakeObject = Object;
    public class mezigue = Test;
    public class hello = mezigue;
    public class aliasA1A22 = aliasA1A2;
    public Test() {}
    public void a1() {} 
    public void a2() {}
    public Object obj() { return new hello(); } 
    void dummy()
    {
    aliasA2 a2 = this;
    aliasA1A2 a1 = this;
    Object t = new fakeObject();
    if(a1 instanceof aliasA1A22)
        {
        a2.a2();
        a1.a2();
        a1.a1();
        //a1.a1();
        }
    
    }
    public class Toto implements aliasA1A22, aliasA3
    {
    public void a1() {} 
    public Object obj() {return null; }
    public void a2() {}
    void dummy()
    {
        A3 a3 = this;
    }
    }

    protected interface W
    {}

    public class W_ = W;
}
class Test2 extends Test.mezigue implements Test.aliasA1A2, Test.W_
{
    public void a1() {} 
    public void a2() {}
    public Object obj() { return new Test(); } 
    void dummy()
    {
    Test.mezigue t = new Test();
    }
}
