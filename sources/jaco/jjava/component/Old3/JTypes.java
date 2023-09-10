// JTypes
// Stewart Itzstein

package jaco.jjava.component;
 
import jaco.framework.*;
import jaco.java.component.*;
import jaco.jjava.struct.*;
import jaco.jjava.context.*;
import jaco.jjava.component.*;
 
// add SIGNAL in as a Type Constant 
public interface JTypeConst extends jaco.java.component.TypeConst
{
        static final int SIGNAL = 4;
}
 
public class JTypes extends jaco.java.component.Types implements JTypeConst
{
 
        public jaco.java.struct.Type                     signalType;
 
        public String getName()
        {
            return "JJavaTypes";
        }
 
        protected void initBasicTypes()
        {
                super.initBasicTypes();
                signalType      = enterBasicType(SIGNAL, "signal", make.VoidType(), "java.lang.Void");
        }
}
