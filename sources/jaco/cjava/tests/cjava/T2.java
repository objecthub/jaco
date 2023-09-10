interface I {
    void foo([I, J] x);
}
interface J {
}
class T2 implements I {
    public void foo([J, I] x) {
    }
}
