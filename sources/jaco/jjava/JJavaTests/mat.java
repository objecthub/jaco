class reference {
    reference(Object init) {
        state(init);
    }
    
    Object get() & state(Object x) {
        state(x);
        return x;
    }

    void put(Object y) & state(Object x) {
        state(y);
    }
}
