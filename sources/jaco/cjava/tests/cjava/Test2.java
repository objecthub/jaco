import java.util.Vector;
import java.lang.Exception;
public class Test2 
{
   void dummy() 
   {
       Stuff.aliasVectorAB vab = null;
       aliasVectorAB vab2 = vab;
       Stuff.aliasVectorB vb = vab;
       [aliasVector, A] va = vab;
       vab.addElement(va);
   }
}
