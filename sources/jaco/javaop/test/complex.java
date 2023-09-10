
class complex {

    float re, im;

    complex(float re, float im) {
    this.re = re;
    this.im = im;
    }

    complex this # (complex c) {
    return new complex(c.re + re, c.im + im);
    }

    complex (float re) + this {
    return new complex(this.re + re, im);
    }

    complex (complex c) + this {
    return c;
    }

    complex this + (int i) {
    return new complex(42,42);;
    }

    public static void main(String[] args) {
    complex c = new complex(1,1);
    c = c + c;

    int j = 3;

    c = c + j;
    c = j + c;

    c = j + (j + c);

    String str = "" + c + " " + j;
    System.out.println(str + " coucou");

    }

}
