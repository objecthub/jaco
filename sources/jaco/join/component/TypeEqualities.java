package jaco.join.component;

import jaco.framework.*;
import jaco.join.struct.*;
import jaco.java.struct.*;

class TypeEqualities {
    static JTypes types;
    JType left;
    JType right;
    TypeEqualities next;
    
    TypeEqualities(JTypes types) {
        TypeEqualities.types = types;
    }
    
    TypeEqualities(JType left, JType right, TypeEqualities next) {
        this.left = left;
        this.right = right;
        this.next = next;
    }
    
    TypeEqualities add(JType left, JType right) {
        return new TypeEqualities(left, right, this);
    }
    
    boolean find(JType left, JType right) {
        if (next == null)
            return false;
        return ((types.equal(this.left, left) && types.equal(this.right, right)) ||
                (types.equal(this.left, right) && types.equal(this.right, left)) ||
                next.find(left, right));
    }
}
