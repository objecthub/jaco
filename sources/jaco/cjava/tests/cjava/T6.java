interface I {
    void foo();
}
interface J {
}
class D {
    public static void main(String[] args) {
        C c = new C();
    C d = (C)c;
        [D, I, J] dij = ([D, I, J])c; // this is not legal according to
    }                                 // Buechi & Wecks paper (page 28, 2.)
}
class C extends D {
    int foo() {
        return 1;
    }
}
