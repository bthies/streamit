class RoundDemo {

    static int Y = 205;
    static int Cb = 121;
    static int Cr = 131;

    // default version through library
    static void workWithFloat() { // ColorSpace.str:86
	float Ey = ((Y - 16.0f) / 219.0f); // ColorSpace.str:91
	float Epb = ((Cb - 128.0f) / 224.0f); // ColorSpace.str:92
	float Epr = ((Cr - 128.0f) / 224.0f); // ColorSpace.str:93

	float EB = (((1.001856f * Ey) + (1.855625f * Epb)) + (0.001005f * Epr)); // ColorSpace.str:96
	int B = ((int)(Math.round((EB * 256.0f)))); // ColorSpace.str:101

	System.out.println();
	System.out.println("Java default:");
	System.out.println("Ey = " + Ey);
	System.out.println("Epb = " + Epb);
	System.out.println("Epr = " + Epr);
	System.out.println("EB = " + EB);
	System.out.println("EB*256 = " + (EB*256.0f));
	System.out.println("B = " + B);
    }

    // converted to operate on doubles
    static void workWithDouble() { // ColorSpace.str:86
	double Ey = ((Y - 16.0) / 219.0); // ColorSpace.str:91
	double Epb = ((Cb - 128.0) / 224.0); // ColorSpace.str:92
	double Epr = ((Cr - 128.0) / 224.0); // ColorSpace.str:93

	double EB = (((1.001856 * Ey) + (1.855625 * Epb)) + (0.001005 * Epr)); // ColorSpace.str:96
	int B = ((int)(Math.round((EB * 256.0)))); // ColorSpace.str:101

	System.out.println();
	System.out.println("Java as doubles:");
	System.out.println("Ey = " + Ey);
	System.out.println("Epb = " + Epb);
	System.out.println("Epr = " + Epr);
	System.out.println("EB = " + EB);
	System.out.println("EB*256 = " + (EB*256.0));
	System.out.println("B = " + B);
    }

    public static void main(String[] args) {
	workWithFloat();
	workWithDouble();
    }
}
