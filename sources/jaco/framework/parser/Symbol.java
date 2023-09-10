//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.framework.parser;

/*
 * Defines the Symbol class, which is used to represent all terminals
 * and nonterminals while parsing.  The lexer should pass CUP Symbols 
 * and CUP returns a Symol
 *
 * @version last updated: 7/3/96
 * @author  Frank Flannery
 * @author  Matthias Zenger
 */

public final class Symbol
{
/** the symbol number of the terminal or non terminal being represented
 */
    public int sym;

/** the parse state to be recorded on the parse stack with this symbol.
 *  This field is for the convenience of the parser and shouldn't be 
 *  modified except by the parser. 
 */
    public int parse_state;

/** the data passed to the parser
 */
    public int      left, right;
    public Object   value;
    
    
    public Symbol(int id, int l, int r, Object o)
    {
        sym = id;
        parse_state = -1;
        left = l;
        right = r;
        value = o;
    }
    
    public Symbol(int id, Object o)
    {
        sym = id;
        parse_state = -1;
        left = -1;
        right = -1;
        value = o;
    }
    
    public Symbol(int sym_num, int l, int r)
    {
        sym = sym_num;
        left = l;
        right = r;
    }
    
    public Symbol(int sym_num)
    {
        sym = sym_num;
        parse_state = -1;
        left = -1;
        right = -1;
    }

    public Symbol(int sym_num, int state)
    {
        sym = sym_num;
        parse_state = state;
    }
}
