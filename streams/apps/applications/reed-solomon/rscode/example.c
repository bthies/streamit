/* Example use of Reed-Solomon library 
 *
 * (C) Universal Access Inc. 1996
 *
 * This same code demonstrates the use of the encodier and 
 * decoder/error-correction routines. 
 *
 * We are assuming we have at least four bytes of parity (NPAR >= 4).
 * 
 * This gives us the ability to correct up to two errors, or 
 * four erasures. 
 *
 * In general, with E errors, and K erasures, you will need
 * 2E + K bytes of parity to be able to correct the codeword
 * back to recover the original message data.
 *
 * You could say that each error 'consumes' two bytes of the parity,
 * whereas each erasure 'consumes' one byte.
 *
 * Thus, as demonstrated below, we can inject one error (location unknown)
 * and two erasures (with their locations specified) and the 
 * error-correction routine will be able to correct the codeword
 * back to the original message.
 * */
 
#include <stdio.h>
#include <stdlib.h>
#include "ecc.h"
 
unsigned char msg[] = "Nervously I loaded the twin ducks aboard the revolving pl\
atform.";
unsigned char codeword[256];
 
/* Some debugging routines to introduce errors or erasures
   into a codeword. 
   */

/* Introduce a byte error at LOC */
void
byte_err (int err, int loc, unsigned char *dst)
{
  printf("Adding Error at loc %d, data %#x\n", loc, dst[loc-1]);
  dst[loc-1] ^= err;
}

/* Pass in location of error (first byte position is
   labeled starting at 1, not 0), and the codeword.
*/
void
byte_erasure (int loc, unsigned char dst[], int cwsize, int erasures[]) 
{
  printf("Erasure at loc %d, data %#x\n", loc, dst[loc-1]);
  dst[loc-1] = 0;
}


void
main (int argc, char *argv[])
{
  int i;
  int erasures[16];
  int nerasures = 0;

  /* Initialization the ECC library */
 
  initialize_ecc ();
 
  /* ************** */
 
  /* Encode data into codeword, adding NPAR parity bytes */
  encode_data(msg, sizeof(msg), codeword);
 
  printf("Encoded data is: \"%s\"\n", codeword);
  for (i=0; i<NPAR; i++) {
      printf("parity bit %i=%i\n", i, codeword[sizeof(msg) + 1 + i]);
  }
  
 
#define ML (sizeof (msg) + NPAR)


  /* Add one error and two erasures */
  byte_err(0x35, 3, codeword);

  byte_err(0x23, 17, codeword);
  byte_err(0x34, 19, codeword);


  printf("with some errors: \"%s\"\n", codeword);

  /* We need to indicate the position of the erasures.  Eraseure
     positions are indexed (1 based) from the end of the message... */

  erasures[nerasures++] = ML-17;
  erasures[nerasures++] = ML-19;

 
  /* Now decode -- encoded codeword size must be passed */
  decode_data(codeword, ML);

  /* check if syndrome is all zeros */
  if (check_syndrome () != 0) {
    correct_errors_erasures (codeword, 
			     ML,
			     nerasures, 
			     erasures);
 
    printf("Corrected codeword: \"%s\"\n", codeword);
  }
 
  exit(0);
}

