package jaco.join.struct;

public interface JoinConst {

/** binary operators
 */
    int PLUS = 1;
    int MINUS = 2;
    int TIMES = 3;
    int DIV = 4;
    int AND = 5;
    int OR = 6;
    int EQ = 7;
    int NOTEQ = 8;
    int GT = 9;
    int LT = 10;
    int GTEQ = 11;
    int LTEQ = 12;

/** unary operators
 */
    int NOT = 100;
    int NEG = 101;

/** special literals
 */
    int ERROR = 200;
    int FALSE = 201;
    int TRUE = 202;
}
