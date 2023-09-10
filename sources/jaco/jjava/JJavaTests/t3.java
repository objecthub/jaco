class t3 {
    int i;
    signal some(int i);
    signal none();


    void add() & some(int i) {
        if (i == 100) 
            System.out.println("full");
        else
            some(i++);
    }


    int none() {
        System.out.println("This is executed immediately");
    }


    void add() & none() {
        some(1);
    }



    void take() & some() {
        if (i == 1) 
            none();
        else
            some(i--);
    }



    public static void main(String[] argv) {
        System.out.println("Hello");
    }
}
