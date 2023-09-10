interface I {
    void foo(I x);
}
class T4 implements I {
    public void foo([I] x) {
    }
}
