/* This is the comlex class, this performs the basic complex manipulation
 it is important to come with an efficient implementation of for complex numbers
 because they are widely used in signal processing and communication applications
*/

public class Comp {

    /*    public static void main(String[] args) {
	Complex d= new Complex();
	Complex a= new Complex();
	a.real=3;
	a.imag=2;
	d.imag=1;
	d.real=2;
	Comp.Println(d);
	Comp.Println(Comp.Add(d,d));
	Comp.Println(Comp.Mult(d,d));
	Comp.Println(Comp.RoutN(8));
	Comp.Println(Comp.Exp(d,3));
	Comp.Println(Comp.Div(a,b));
	
	
    }*/

  static  public Complex Add(Complex a, Complex b)
    { Complex c= new Complex();
	c.imag= a.imag+b.imag;
	c.real= b.real+a.real;
	return(c); 
    }

    static public Complex Make(float a, float b) {
	Complex c=new Complex();
	c.real=a;
	c.imag=b;
	return(c);
    }

    static public Complex Mult(Complex a, Complex b) {
	Complex c= new Complex();
	c.real=a.real*b.real -a.imag*b.imag;
	c.imag=a.imag+b.real+a.real+b.imag;
	return(c);
    }

    static public Complex RoutN(int N) {
	Complex c= new Complex();
	c.real=(float)Math.cos(Math.PI*2/N);
	c.imag=(float)Math.sin(Math.PI*2/N);
	return(c);
    }

    static public Complex etoj(float x) {
	Complex c= new Complex();
	c.real=(float) Math.cos(x);
	c.imag=(float) Math.sin(x);
	return(c);
    }

    static public Complex Mult(float r, Complex c) {
	Complex res = new Complex();
	res.real=c.real*r;
	res.imag=c.imag*r;
	return(res);
    }

    static public Complex Conj(Complex a) {
	Complex c= new Complex();
	c.real=a.real;
	c.imag=-a.imag;
	return(c);
    }

    static public Complex Div(Complex a, Complex b){
	Complex c= new Complex();
	float aux;
	aux=b.real*b.real+b.imag*b.imag;
	c=Comp.Mult(1/aux,Comp.Mult(a,Comp.Conj(b)));
	return(c);
    }

    static public Complex Div(Complex a, float b){
	Complex c= new Complex();
	c.real=a.real/b;
	c.imag=a.imag/b;
	return(c);
    }

    static public float Norm2(Complex a) {
	float x;
	x=a.real*a.real+a.imag*a.imag;
	return(x);
    }
 

    static public Complex Exp(Complex a,int N) {
	Complex c= new Complex();
	c.real=1;
	c.imag=0;
	for (int i=0; i<N; i++)
	    c=Comp.Mult(a,c);
	return(c);

    }

    static public void Println(Complex a) {
	System.out.print("= ");
	System.out.print(a.real);
	System.out.print(" + i");
	System.out.println(a.imag);
    }
    
}








