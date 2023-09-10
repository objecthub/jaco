interface I {
    void foo(I x);
}
interface J {
}
class T3 implements I {
    public void foo([I, I] x) {
    }
}
