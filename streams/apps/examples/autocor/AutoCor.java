import streamit.library.*;

/* n is the dataSize. nlags is the # of dot-products to be done.
 * eg. (n, nlags) can be (32,8) or (512,32) ...
 *
for (lag = 0; lag < nlags; lag++) {
    sum = 0;
    for (i = 0; i < (n - lag); i++)
	sum += A[i] * A[i+lag];
    acc[lag] = sum;
}

total elements being summed = nlags * (n - nlags / 2)
*/

public class AutoCor extends StreamIt {
    
    public static void main(String[] args) {
	new AutoCor().run(args);
    }

    public void init() {
	add(new OneSource());
	add(new Cor1(32, 8));
	add(new FloatPrinter());
    }

}

// this puts each inner loop in a filter
class Cor1 extends SplitJoin {
    public Cor1(int a, int b) {
	super(a,b);
    }
    public void init(final int N, final int NFLAGS) {
	    setSplitter(DUPLICATE());
	    for (int l=0; l<NFLAGS; l++) {
		final int lag = l;
		add(new Filter() {
			public void init() {
			    input = new Channel(Float.TYPE, N);
			    output = new Channel(Float.TYPE, 1);
			}
			public void work() {
			    float sum = 0;
			    for (int i=0; i<(N-lag); i++)
				sum += input.peekFloat(i)
				    *input.peekFloat(i+lag);
			    for (int i=0; i<N; i++)
				input.popFloat();
			    output.pushFloat(sum);
			}
		    });
	    }
	    setJoiner(ROUND_ROBIN());
    }
}


class Adder extends Filter {
    int N;

    public Adder(int N) {
	super(N);
    }

    public void init (int n) {
        input = new Channel (Float.TYPE, n, n);
        output = new Channel (Float.TYPE, 1);
	this.N = n;
    }

    public void work() {
	float result = 0;
	int i;
	for (i=0; i<N; i++) {
	    result += input.popFloat();
	}
	output.pushFloat(result);
    }
}

class IdentityFloat extends Filter {
    public void init ()
    {
        input = new Channel(Float.TYPE, 1);
        output = new Channel(Float.TYPE, 1);
    }

    public void work() {
        output.pushFloat(input.popFloat());
    }
}

class OneSource extends Filter
{
    public void init ()
    {
        output = new Channel(Float.TYPE, 1);
	this.n = 0;
    }
    float n;
    public void work()
    {
        output.pushFloat(n);
	n += 0.01;
    }
}

class FloatPrinter extends Filter
{
    public void init ()
    {
        input = new Channel(Float.TYPE, 1);
    }
    public void work ()
    {
        System.out.println (input.popFloat ());
    }
}

