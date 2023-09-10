class Y {
	class A {
	}
}
class Z extends Y {
	class B extends A {
		B() {
			Y.this.super();	
		}
	}
}
