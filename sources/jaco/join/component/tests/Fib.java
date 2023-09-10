final class Fib {
    private static Integer fib(int n) {
        if (n == 0)
            return new Integer(1);
        else if (n == 1)
            return new Integer(1);
        else
            return new Integer(fib(n-1).intValue() + fib(n-2).intValue());
    }
    public static void main(String[] args) {
        fib(4);
        long msec = System.currentTimeMillis();
        int r = fib(25).intValue();
        System.out.println("[runtime: " + (System.currentTimeMillis() - msec) + "ms]");
        System.out.println(r);
    }
}
