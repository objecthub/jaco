class Test {
    public static void main(String[] args) {
        int i = 200;
        byte b = (byte)(i & 0xff);
        System.out.println("i = " + ((i & 0xff) - 128));
        System.out.println("b = " + (byte)(b + 256));
    }
}
