class reference {

    //signal state();
    signal bing();

    reference(Object init) {
        state(init);
    }
    
    Object get() & state(Object x) {
        state(x);
        //return x;
    }


    void state(Object x) & bing() {
    }


    void put(Object y) & state(Object x) {
        state(y);
    }
}
