/*
  $Id: cbc_d_m.c,v 1.1 2006-06-19 23:33:20 rabbah Exp $

  # This file is part of the C reference implementation of Serpent.
  #
  # Written by Frank Stajano,
  # Olivetti Oracle Research Laboratory <http://www.orl.co.uk/~fms/> and
  # Cambridge University Computer Laboratory <http://www.cl.cam.ac.uk/~fms27/>.
  # 
  # (c) 1998 Olivetti Oracle Research Laboratory (ORL)
  #
  # Original (Python) Serpent reference development started on 1998 02 12.
  # C implementation development started on 1998 03 04.
  #
  # Serpent cipher invented by Ross Anderson, Eli Biham, Lars Knudsen.
  # Serpent is a candidate for the Advanced Encryption Standard.
  
*/

/* -------------------------------------------------- */
#include <assert.h>
#include "serpent-api.h"
#include "serpent-aux.h"
/* -------------------------------------------------- */
EMBED_RCS(cbc_d_m_c,
          "$Id: cbc_d_m.c,v 1.1 2006-06-19 23:33:20 rabbah Exp $")


/* The "NIST SPEC" markers refer to the pseudo-code instructions in figure
   6 (Monte Carlo Test - CBC Decryption) of the NIST document "Description
   of Known Answer Tests and Monte Carlo Tests for Advanced Encryption
   Standard (AES) Candidate Algorithm Submissions" as updated on February
   17, 1998 */

int main(void) {
  int i, j, k, bitsPerShortKey, result;
  BLOCK plainText, cipherText, PT_9998;
  KEY binaryKey;
  char asciiKey[HEX_DIGITS_PER_KEY+1];
  keyInstance key;
  cipherInstance cipher;


  /* The hack that remembers PT_9998 only works if... */
  assert(BITS_PER_KEY <= 2*BITS_PER_BLOCK);
  /* ...otherwise we'd have to remember more than just PT_9998. */

  printHeader("cbc_d_m", "Cipher Block Chaining (CBC) Mode - DECRYPTION",
              "Monte Carlo Test");

  for(bitsPerShortKey=BITS_PER_SHORTEST_KEY; bitsPerShortKey<=BITS_PER_KEY;
      bitsPerShortKey+=BITS_PER_KEY_STEP) {
    printf("KEYSIZE=%d\n\n", bitsPerShortKey);

    /* Construct (backwards) an ascii key of all 0s, of length
       bitsPerShortKey bits. */
    i=bitsPerShortKey/BITS_PER_HEX_DIGIT;
    asciiKey[i] = 0; /* terminating null */ 
    for (i--; i >=0; i--) {
      asciiKey[i] = '0';
    }
    result = makeKey(&key, DIR_DECRYPT, bitsPerShortKey, asciiKey);
    if (result != TRUE) goto error;

    result = stringToWords("00000000000000000000000000000000", cipherText,
                           WORDS_PER_BLOCK);
    if (result != TRUE) goto error;
    result = cipherInit(&cipher, MODE_CBC, "00000000000000000000000000000000");
    if (result != TRUE) goto error;

    for(i=0; i<OUTER_LOOP_MAX; i++) {
      /* NIST SPEC: if (i==0) CV_0 = IV_0 */
      /* This has already been done by cipherInit outside this loop. Our CV
         is held in cipher.IV. */

      /* NIST SPEC: Record i, KEY_i, CV_0, CT_0 */
      printf("I=%d\n", i);
      render("KEY=", key.userKey, bitsPerShortKey/BITS_PER_WORD);
      render("IV=", (WORD*) cipher.IV, WORDS_PER_BLOCK);
      render("CT=", cipherText, WORDS_PER_BLOCK);
      
      for (j=0; j<INNER_LOOP_MAX; j++) {
        /* NIST SPEC: IB_j = CT_j */
        /* This is done implicitly by passing cipherText as the input to
           blockDecrypt. */

        /* decrypt */
        result = blockDecrypt(&cipher, &key, (BYTE*) cipherText, 
                              BITS_PER_BLOCK, (BYTE*) plainText);
        if (result < 0) {
          goto error;
        } else if (result != BITS_PER_BLOCK) {
          result = BAD_NUMBER_OF_BITS_PROCESSED;
          goto error;
        }

        /* NIST SPEC: PT_j = OB_j xor CV_j */
        /* This is done automatically when setting mode to CBC; in fact we
           already get PT_j on output from blockDecrypt. */

        if (j == INNER_LOOP_MAX-2) {
          memcpy(PT_9998, plainText, BYTES_PER_BLOCK);
        }

        /* NIST SPEC: CV_j+1 = CT_j */
        /* This is done automatically when setting mode to CBC */

        /* NIST SPEC: CT_j+1 = PT_j */
        memcpy(cipherText, plainText, BYTES_PER_BLOCK);        
      }
      
      /* NIST SPEC: Record PT_j */
      render("PT=", plainText, WORDS_PER_BLOCK);
      printf("\n");

      /* NIST SPEC: KEY_i+1 = KEY_i xor last n bits of PT, where n=key size */
      /* First, juxtapose PT_9999 (least significant) and PT_9998 (most
         significant) into binaryKey; ...*/
      memcpy(binaryKey, PT_9998, BYTES_PER_BLOCK);
      memcpy(&binaryKey[WORDS_PER_BLOCK], plainText, BYTES_PER_BLOCK);
      memmove(binaryKey, 
             &binaryKey[(BITS_PER_KEY-bitsPerShortKey)/BITS_PER_WORD], 
             bitsPerShortKey/BITS_PER_BYTE);

      /* ...then, xor this stuff with the previously used key. */
      for (k=0; k<bitsPerShortKey/BITS_PER_WORD; k++) {
        binaryKey[k] ^= key.userKey[k];
      }
      
      /* NB: the NIST API does not provide callers with a way to specify a
         new key in binary format, so we have to go through the rigmarole
         of computing the new key in binary and converting it to ascii so
         that we can feed it to makeKey which will internally reconvert it
         back to binary--yechh. Note that just poking a new binary key in
         key.userKey won't work, as we need to invoke the routine that
         makes the subkeys. */
      wordsToString(binaryKey,
                    bitsPerShortKey/BITS_PER_WORD, asciiKey);
      result = makeKey(&key, DIR_DECRYPT, bitsPerShortKey, asciiKey);
      if (result != TRUE) goto error;

      /* NIST SPEC: CV_0 = CT_9999 */
      /* This was done automatically by blockDecrypt since we set it to CBC
         mode */

      /* NIST SPEC: CT_0 = PT_9999 */
      /* This was also done above as the last instruction of the inner (j)
         loop */
    }
      
    printf("==========\n\n");
  }
  exit(0);

error:
  printf("Error %d (sorry, look it up in serpent-api.h)\n", result);
  exit(result);
}
