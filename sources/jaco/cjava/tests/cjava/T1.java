interface I {
    [I, J] bar();
}
interface J {
    int foo();
}
abstract class T implements I {
}
