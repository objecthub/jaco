//      /   _ _      JaCo
//  \  //\ / / \     - predefined identifiers
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;


public interface PredefConst
{
    Name INIT_N = Name.fromString("<init>");
    Name CLINIT_N = Name.fromString("<clinit>");
    Name ERROR_N = Name.fromString("<error>");
    Name EMPTY_N = Name.fromString("");
    Name PERIOD_N = Name.fromString(".");
    Name DOLLAR_N = Name.fromString("$");
    Name COMMA_N = Name.fromString(",");
    Name SEMICOLON_N = Name.fromString(";");
    Name STAR_N = Name.fromString("*");
    Name THIS_N = Name.fromString("this");
    Name THIS0_N = Name.fromString("this0");
    Name VALD_N = Name.fromString("val$");
    Name SUPER_N = Name.fromString("super");
    Name NULL_N = Name.fromString("null");
    Name TRUE_N = Name.fromString("true");
    Name FALSE_N = Name.fromString("false");
    Name CLASS_N = Name.fromString("class");
    Name JAVA_LANG_N = Name.fromString("java.lang");
    Name JAVA_LANG_OBJECT_N = Name.fromString("java.lang.Object");
}
