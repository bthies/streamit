/* This is the comlex class, this performs the basic complex manipulation
 it is important to come with an efficient implementation of for complex numbers
 because they are widely used in signal processing and communication applications
*/

public class Comp {

    public static void main(String[] args) {
	Complex d= new Complex();
	d.imag=1;
	d.real=2;
	Comp.Println(d);
	Comp.Println(Comp.Add(d,d));
	Comp.Println(Comp.Mult(d,d));
	Comp.Println(Comp.RoutN(8));
	Comp.Println(Comp.Exp(d,3));
	
    }

  static  public Complex Add(Complex a, Complex b)
    { Complex c= new Complex();
	c.imag= a.imag+b.imag;
	c.real= b.real+a.real;
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






