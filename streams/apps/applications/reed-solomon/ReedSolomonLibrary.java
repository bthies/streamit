/**
 * Code and data used for Reed-Solomon encoding and decoding
 * (All this code was taken from rscode.sourceforge.net)
 * and is copyright Henry Minsky. The code in this file
 * is directly copied, with appropriate syntax changes to make
 * it valid java/StreamIT code.
 *
 * -Andrew Lamb aalamb@mit.edu 6/17/2002
 **/

import streamit.library.*;

public class ReedSolomonLibrary {
    /** constructor that runs the appropriate initialization routines **/
    public ReedSolomonLibrary() {
	// initialize the data structures that galois field arithmetic needs
	// I don't pretend to know the details of what that means
	this.initialize_ecc();

    }
    
    /*---------------------ecc.h-------------------------*/
    
    /* Reed Solomon Coding for glyphs
     * 
     * (c) Henry Minsky (hqm@ua.com), Universal Access Inc.  (1991-1996)
     *     
     *
     */
    /****************************************************************/
    /* Below is NPAR, the only compile-time parameter you should have to
       modify.
       
       It is the number of parity bytes which will be appended to
       your data to create a codeword.
       
       Note that the maximum codeword size is 255, so the
       sum of your message length plus parity should be less than
       or equal to this maximum limit.
       
       In practice, you will get slooow error correction and decoding
       if you use more than a reasonably small number of parity bytes.
       (say, 10 or 20)
       
    */    
  public static final int NPAR = 8;
  
  final boolean DEBUG = false;
  /****************************************************************/
  final int TRUE = 1;
  final int FALSE = 0;
  
  //typedef unsigned long BIT32;
  //typedef unsigned short BIT16;
  
  /* **************************************************************** */
  
  /* Maximum degree of various polynomials. */
  final int MAXDEG = (NPAR*2);
  
  /*---------------------galois.c-------------------------*/
  /*****************************
   * 
   *
   * Multiplication and Arithmetic on Galois Field GF(256)
   *
   * From Mee, Daniel, "Magnetic Recording, Volume III", Ch. 5 by Patel.
   * 
   * (c) 1991 Henry Minsky
   *
   *
   ******************************/
  
  /* This is one of 14 irreducible polynomials
   * of degree 8 and cycle length 255. (Ch 5, pp. 275, Magnetic Recording)
   * The high order 1 bit is implicit */
  /* x^8 + x^4 + x^3 + x^2 + 1 */
  int PPOLY = 0x1D; 
  int gexp[] = new int [512];
  int glog[] = new int [256];
  
  void init_galois_tables ()
  {	
    /* initialize the table of powers of alpha */
    init_exp_table();
  }
  
  
  void init_exp_table ()
  {
    int i, z;
    int pinit,p1,p2,p3,p4,p5,p6,p7,p8;
    
    pinit = p2 = p3 = p4 = p5 = p6 = p7 = p8 = 0;
    p1 = 1;
    
    gexp[0] = 1;
    gexp[255] = gexp[0];
    glog[0] = 0;			/* shouldn't log[0] be an error? */
    
    for (i = 1; i < 256; i++) {
      pinit = p8;
      p8 = p7;
      p7 = p6;
      p6 = p5;
      p5 = p4 ^ pinit;
      p4 = p3 ^ pinit;
      p3 = p2 ^ pinit;
      p2 = p1;
      p1 = pinit;
      gexp[i] = p1 + p2*2 + p3*4 + p4*8 + p5*16 + p6*32 + p7*64 + p8*128;
      gexp[i+255] = gexp[i];
    }
    
    for (i = 1; i < 256; i++) {
      for (z = 0; z < 256; z++) {
	if (gexp[z] == i) {
	  glog[i] = z;
	  break;
	}
      }
    }
  }
  
  /* multiplication using logarithms */
  int gmult(int a, int b)
  {
    int i,j;
    if (a==0 || b == 0) return (0);
    i = glog[a];
    j = glog[b];
    return (gexp[i+j]);
  }
  
  
  int ginv (int elt) 
  { 
    return (gexp[255-glog[elt]]);
  }
  
  
  /*---------------------berlekamp.c-------------------------*/
  /***********************************************************************
   * Berlekamp-Peterson and Berlekamp-Massey Algorithms for error-location
   *
   * From Cain, Clark, "Error-Correction Coding For Digital Communications", pp. 205.
   *
   * This finds the coefficients of the error locator polynomial.
   *
   * The roots are then found by looking for the values of a^n
   * where evaluating the polynomial yields zero.
   *
   * Error correction is done using the error-evaluator equation  on pp 207.
   *
   * hqm@ai.mit.edu   Henry Minsky
   */
  
  /* The Error Locator Polynomial, also known as Lambda or Sigma. Lambda[0] == 1 */
  int Lambda[] = new int[MAXDEG];

  /* The Error Evaluator Polynomial */
  int Omega[] = new int[MAXDEG];
  
  /* local ANSI declarations */
  //int compute_discrepancy(int lambda[], int S[], int L, int n);
  //void init_gamma(int gamma[]);
  //void compute_modified_omega (void);
  //void mul_z_poly (int src[]);
  
  /* error locations found using Chien's search*/
  int ErrorLocs[] = new int[256];
  int NErrors;
  
  /* erasure flags */
  int ErasureLocs[] = new int[256];
  int NErasures;

  /* From  Cain, Clark, "Error-Correction Coding For Digital Communications", pp. 216. */
  void Modified_Berlekamp_Massey ()
  {	
    int n, L, L2, k, d, i;
    int psi[] = new int[MAXDEG];
    int psi2[] = new int[MAXDEG];
    int D[] = new int[MAXDEG];
    int gamma[] = new int[MAXDEG];
    
    /* initialize Gamma, the erasure locator polynomial */
    init_gamma(gamma);
    
    /* initialize to z */
    copy_poly(D, gamma);
    mul_z_poly(D);
    
    copy_poly(psi, gamma);	
    k = -1; L = NErasures;
    
    for (n = NErasures; n < NPAR; n++) {
      
      d = compute_discrepancy(psi, synBytes, L, n);
      
      if (d != 0) {
	
	/* psi2 = psi - d*D */
	for (i = 0; i < MAXDEG; i++) psi2[i] = psi[i] ^ gmult(d, D[i]);
	
	
	if (L < (n-k)) {
	  L2 = n-k;
	  k = n-L;
	  /* D = scale_poly(ginv(d), psi); */
	  for (i = 0; i < MAXDEG; i++) D[i] = gmult(psi[i], ginv(d));
	  L = L2;
	}
	
	/* psi = psi2 */
	for (i = 0; i < MAXDEG; i++) psi[i] = psi2[i];
      }
      
      mul_z_poly(D);
    }
    
    for(i = 0; i < MAXDEG; i++) Lambda[i] = psi[i];
    compute_modified_omega();
  }
  
  /* given Psi (called Lambda in Modified_Berlekamp_Massey) and synBytes,
     compute the combined erasure/error evaluator polynomial as 
     Psi*S mod z^4
  */
  void compute_modified_omega ()
  {
    int i;
    int product[] = new int[MAXDEG*2];
    
    mult_polys(product, Lambda, synBytes);	
    zero_poly(Omega);
    for(i = 0; i < NPAR; i++) Omega[i] = product[i];
    
  }

  /* polynomial multiplication */
  void mult_polys (int dst[], int p1[], int p2[])
  {
    int i, j;
    int tmp1[] = new int[MAXDEG*2];
    
    for (i=0; i < (MAXDEG*2); i++) dst[i] = 0;
    
    for (i = 0; i < MAXDEG; i++) {
      for(j=MAXDEG; j<(MAXDEG*2); j++) tmp1[j]=0;
      
      /* scale tmp1 by p1[i] */
      for(j=0; j<MAXDEG; j++) tmp1[j]=gmult(p2[j], p1[i]);
      /* and mult (shift) tmp1 right by i */
      for (j = (MAXDEG*2)-1; j >= i; j--) tmp1[j] = tmp1[j-i];
      for (j = 0; j < i; j++) tmp1[j] = 0;
      
      /* add into partial product */
      for(j=0; j < (MAXDEG*2); j++) dst[j] ^= tmp1[j];
    }
  }


	
  /* gamma = product (1-z*a^Ij) for erasure locs Ij */
  void init_gamma (int gamma[])
  {
    int e;
    int tmp[] = new int[MAXDEG];
    
    zero_poly(gamma);
    zero_poly(tmp);
    gamma[0] = 1;
    
    for (e = 0; e < NErasures; e++) {
      copy_poly(tmp, gamma);
      scale_poly(gexp[ErasureLocs[e]], tmp);
      mul_z_poly(tmp);
      add_polys(gamma, tmp);
    }
  }
  
	
  void compute_next_omega (int d, int A[], int dst[], int src[])
  {
    int i;
    for ( i = 0; i < MAXDEG;  i++) {
      dst[i] = src[i] ^ gmult(d, A[i]);
    }
  }
  
  int compute_discrepancy (int lambda[], int S[], int L, int n)
  {
    int i, sum=0;
    
    for (i = 0; i <= L; i++) 
      sum ^= gmult(lambda[i], S[n-i]);
    return (sum);
  }
  
  /********** polynomial arithmetic *******************/
  void add_polys (int dst[], int src[]) 
  {
    int i;
    for (i = 0; i < MAXDEG; i++) dst[i] ^= src[i];
  }
  
  void copy_poly (int dst[], int src[]) 
  {
    int i;
    for (i = 0; i < MAXDEG; i++) dst[i] = src[i];
  }
  
  void scale_poly (int k, int poly[]) 
  {	
    int i;
    for (i = 0; i < MAXDEG; i++) poly[i] = gmult(k, poly[i]);
  }
  
  
  void zero_poly (int poly[]) 
  {
    int i;
    for (i = 0; i < MAXDEG; i++) poly[i] = 0;
  }
  

  /* multiply by z, i.e., shift right by 1 */
  void mul_z_poly (int src[])
  {
    int i;
    for (i = MAXDEG-1; i > 0; i--) src[i] = src[i-1];
    src[0] = 0;
  }


  /* Finds all the roots of an error-locator polynomial with coefficients
   * Lambda[j] by evaluating Lambda at successive values of alpha. 
   * 
   * This can be tested with the decoder's equations case.
   */
  void Find_Roots ()
  {
    int sum, r, k;	
    NErrors = 0;
    
    for (r = 1; r < 256; r++) {
      sum = 0;
      /* evaluate lambda at r */
      for (k = 0; k < NPAR+1; k++) {
	sum ^= gmult(gexp[(k*r)%255], Lambda[k]);
      }
      if (sum == 0) 
	{ 
	  ErrorLocs[NErrors] = (255-r); NErrors++; 
	  if (DEBUG) System.out.println("Root found at r = " + r + ", (255-r) = " + (255-r) + "\n");
	}
    }
  }
  
  /* Combined Erasure And Error Magnitude Computation 
   * 
   * Pass in the codeword, its size in bytes, as well as
   * an array of any known erasure locations, along the number
   * of these erasures.
   * 
   * Evaluate Omega(actually Psi)/Lambda' at the roots
   * alpha^(-i) for error locs i. 
   *
   * Returns 1 if everything ok, or 0 if an out-of-bounds error is found
   *
   */
  
  int correct_errors_erasures (char codeword[], 
			       int csize,
			       int nerasures,
			       int erasures[])
  {
    int r, i, j, err;
    
    /* If you want to take advantage of erasure correction, be sure to
       set NErasures and ErasureLocs[] with the locations of erasures. 
    */
    NErasures = nerasures;
    for (i = 0; i < NErasures; i++) ErasureLocs[i] = erasures[i];
    
    Modified_Berlekamp_Massey();
    Find_Roots();
    
    
    if ((NErrors <= NPAR) && NErrors > 0) { 
      
      /* first check for illegal error locs */
      for (r = 0; r < NErrors; r++) {
	if (ErrorLocs[r] >= csize) {
	  if (DEBUG) System.out.println("Error loc i=" + i + " outside of codeword length " + csize + "\n");
	  return(0);
	}
      }
      
      for (r = 0; r < NErrors; r++) {
	int num, denom;
	i = ErrorLocs[r];
	/* evaluate Omega at alpha^(-i) */
	
	num = 0;
	for (j = 0; j < MAXDEG; j++) 
	  num ^= gmult(Omega[j], gexp[((255-i)*j)%255]);
	
	/* evaluate Lambda' (derivative) at alpha^(-i) ; all odd powers disappear */
	denom = 0;
	for (j = 1; j < MAXDEG; j += 2) {
	  denom ^= gmult(Lambda[j], gexp[((255-i)*(j-1)) % 255]);
	}
	
	err = gmult(num, ginv(denom));
	if (DEBUG) System.out.println("Error magnitude " + err + " at loc " + (csize-1) + "\n");
	
	codeword[csize-i-1] ^= err;
      }
      return(1);
    }
    else {
      if (DEBUG && (NErrors>0)) System.out.println("Uncorrectable codeword\n");
      return(0);
    }
  }


  /*---------------------rs.c-------------------------*/
  /* 
   * Reed Solomon Encoder/Decoder 
   *
   * (c) Henry Minsky (hqm@ua.com), Universal Access 1991-1995
   */
  
  /* Encoder parity bytes */
  int pBytes[] = new int[MAXDEG];
  
  /* Decoder syndrome bytes */
  int synBytes[] = new int[MAXDEG];
  
  /* generator polynomial */
  int genPoly[] = new int[MAXDEG*2];
  
  /* Initialize lookup tables, polynomials, etc. */
  void initialize_ecc ()
  {
    /* Initialize the galois field arithmetic tables */
    init_galois_tables();
    
    /* Compute the encoder generator polynomial */
    compute_genpoly(NPAR, genPoly);
  }

  void zero_fill_from (char buf[], int from, int to)
  {
    int i;
    for (i = from; i < to; i++) buf[i] = 0;
  }
  
  /* debugging routines */
  void print_parity ()
  { 
    int i;
    System.out.println("Parity Bytes: ");
    for (i = 0; i < NPAR; i++) 
      System.out.println("[" + i + "]:" + pBytes[i] + "%x, ");
    System.out.println("\n");
  }
  
  
  void print_syndrome ()
  { 
    int i;
    System.out.println("Syndrome Bytes: ");
    for (i = 0; i < NPAR; i++) 
      System.out.println("[" + i + "]:" + synBytes[i]);
    System.out.println("\n");
  }
  
  /* Append the parity bytes onto the end of the message */
  void build_codeword (char msg[], int nbytes, char dst[])
  {
    int i;
    
    for (i = 0; i < nbytes; i++) dst[i] = msg[i];
    
    for (i = 0; i < NPAR; i++) {
      dst[i+nbytes] = (char)pBytes[NPAR-1-i];
    }
  }
  
  /**********************************************************
   * Reed Solomon Decoder 
   *
   * Computes the syndrome of a codeword. Puts the results
   * into the synBytes[] array.
   */
  
  void decode_data(char data[], int nbytes)
  {
    int i, j, sum;
    for (j = 0; j < NPAR;  j++) {
      sum	= 0;
      for (i = 0; i < nbytes; i++) {
	sum = data[i] ^ gmult(gexp[j+1], sum);
      }
      synBytes[j]  = sum;
    }
  }


  /* Check if the syndrome is zero */
  int check_syndrome ()
  {
    int i, nz = 0;
    for (i =0 ; i < NPAR; i++) {
      if (synBytes[i] != 0) nz = 1;
    }
    return nz;
  }
  

void debug_check_syndrome ()
  {	
    int i;
    
    for (i = 0; i < 3; i++) {
      System.out.println(" inv log S[" + i + "]/S[" + (i+1) + "] = " +
			 glog[gmult(synBytes[i], ginv(synBytes[i+1]))] + "\n"); 
    }
  }
  
  
  /* Create a generator polynomial for an n byte RS code. 
   * The coefficients are returned in the genPoly arg.
   * Make sure that the genPoly array which is passed in is 
   * at least n+1 bytes long.
   */
  
  void compute_genpoly (int nbytes, int genpoly[])
  {
    int i;
    int tp[] = new int[256];
    int tp1[] = new int[256];
    
    /* multiply (x + a^n) for n = 1 to nbytes */
    
    zero_poly(tp1);
    tp1[0] = 1;
    
    for (i = 1; i <= nbytes; i++) {
      zero_poly(tp);
      tp[0] = gexp[i];		/* set up x+a^n */
      tp[1] = 1;
      
      mult_polys(genpoly, tp, tp1);
      copy_poly(tp1, genpoly);
    }
  }
  
  /* Simulate a LFSR with generator polynomial for n byte RS code. 
   * Pass in a pointer to the data array, and amount of data. 
   *
   * The parity bytes are deposited into pBytes[], and the whole message
   * and parity are copied to dest to make a codeword.
   * 
   */
  
  void encode_data (char msg[], int nbytes, char dst[])
  {
    int i;
    int LFSR[] = new int[NPAR+1];
    int dbyte, j;
    
    for(i=0; i < NPAR+1; i++) LFSR[i]=0;
    
    for (i = 0; i < nbytes; i++) {
      dbyte = msg[i] ^ LFSR[NPAR-1];
      for (j = NPAR-1; j > 0; j--) {
	LFSR[j] = LFSR[j-1] ^ gmult(genPoly[j], dbyte);
      }
      LFSR[0] = gmult(genPoly[0], dbyte);
    }
    
    for (i = 0; i < NPAR; i++) 
      pBytes[i] = LFSR[i];
    
    build_codeword(msg, nbytes, dst);
  }

}
