import streamit.*;

class FusionTest extends StreamIt {

    public static void main(String[] args) {
	new FusionTest().run(args);
    }

    public void init() {
	add(new FloatOneSource());
	add(new FloatPrinter());
    }
    
}
