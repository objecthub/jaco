class t2 {

    void normalMethod1(int x) {
        x = 3;
    }


    void add() & some() {
        int i=0;
        if (i == 100) 
            System.out.println("full");
        //else
            //some();
    }


    void add2() & some() {
    }

    void normalMethod2(int y) {
        y = 9;
    }


    void add() & none(){
    }

    public static void main(String[] argv) {
        System.out.println("Hello");
    }
}
