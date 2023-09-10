package join.runtime;

public interface Bytecodes {
    byte NOP = 0;
    byte FORK = 1;
    byte RET = 2;
    byte STOP = 3;
    byte UNIT = 4;
    byte BYTE = 5;
    byte INT = 6;
    byte TRUE = 7;
    byte FALSE = 8;
    byte TUPEL = 9;
    byte DECOMP = 10;
    byte FUN = 11;
    byte TOKEN = 12;
    byte LOAD = 13;
    byte STORE = 14;
    byte DUP = 15;
    byte POP = 16;
    byte ENTER = 17;
    byte LEAVE = 18;
    byte GOTO = 19;
    byte JMPT = 20;
    byte JMPF = 21;
    byte APPLY = 22;
    byte YIELD = 23;
    byte ADD = 24;
    byte SUB = 25;
    byte MULT = 26;
    byte DIV = 27;
    byte EQL = 28;
    byte NOT = 29;
    byte ERROR = 30;
}
