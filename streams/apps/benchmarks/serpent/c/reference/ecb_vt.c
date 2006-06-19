/*
  $Id: ecb_vt.c,v 1.1 2006-06-19 23:33:20 rabbah Exp $

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
#include "serpent-api.h"
#include "serpent-aux.h"
/* -------------------------------------------------- */
EMBED_RCS(ecb_vt_c,
          "$Id: ecb_vt.c,v 1.1 2006-06-19 23:33:20 rabbah Exp $")


int main(void) {
  int i, bitsPerShortKey, result;
  BLOCK plainText, cipherText, recoveredPlainText;
  char asciiKey[HEX_DIGITS_PER_KEY+1];
  char asciiPlainText[HEX_DIGITS_PER_BLOCK+1];
  keyInstance key;
  cipherInstance cipher;

  printHeader("ecb_vt", "Electronic Codebook (ECB) Mode",
              "Variable Text Known Answer Tests");

  /* Construct (backwards) an ascii plainText of all 0s, of length
     BITS_PER_BLOCK bits. */
  i=BITS_PER_BLOCK/BITS_PER_HEX_DIGIT;
  asciiPlainText[i] = 0; /* terminating null */ 
  for (i--; i >=0; i--) {
    asciiPlainText[i] = '0';
  }

  result = cipherInit(&cipher, MODE_ECB, 0);
  if (result != TRUE) goto error;

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
    printf("KEY=%s\n\n", asciiKey);

    result = makeKey(&key, DIR_ENCRYPT, bitsPerShortKey, asciiKey);
    if (result != TRUE) goto error;
      
    for(i=BITS_PER_BLOCK-1; i>=0; i--) {
      /* assert: asciiPlainText is all 0s */

      /* prepare plain text for this round: in the ascii big-endian
         representation, set bit number i.  */
      asciiPlainText[(BITS_PER_BLOCK-1-i)/BITS_PER_HEX_DIGIT] = 
        hex(1 << (i%BITS_PER_HEX_DIGIT));
      result = stringToWords(asciiPlainText, plainText, WORDS_PER_BLOCK);
      if (result != TRUE) goto error;
      /* assert: plainText has a 1 bit in its ith position */

      printf("I=%d\n", BITS_PER_BLOCK-i);
      printf("PT=%s\n", asciiPlainText);

      /* restore ascii plain text for next round */
      asciiPlainText[(BITS_PER_BLOCK-1-i)/BITS_PER_HEX_DIGIT] = '0';
      /* assert: asciiKey is all 0s again */

      
      /* encrypt */
      key.direction = DIR_ENCRYPT;
      result = blockEncrypt(&cipher, &key, (BYTE*) plainText, BITS_PER_BLOCK,
                            (BYTE*) cipherText);
      if (result < 0) {
        goto error;
      } else if (result != BITS_PER_BLOCK) {
        result = BAD_NUMBER_OF_BITS_PROCESSED;
        goto error;
      }
      render("CT=", cipherText, WORDS_PER_BLOCK);

      /* decrypt and see if it comes out the same */
      key.direction = DIR_DECRYPT;
      result = blockDecrypt(&cipher, &key, (BYTE*) cipherText, BITS_PER_BLOCK,
                            (BYTE*) recoveredPlainText);
      if (result < 0) {
        goto error;
      } else if (result != BITS_PER_BLOCK) {
        result = BAD_NUMBER_OF_BITS_PROCESSED;
        goto error;
      }

      if (memcmp((BYTE*)plainText, (BYTE*)recoveredPlainText,
                 BYTES_PER_BLOCK)) {
        result = DECRYPTION_MISMATCH;
        goto error;
      }

      printf("\n");
    }
    printf("==========\n\n");
  }
  exit(0);

error:
  printf("Error %d (sorry, see serpent-api.h to see what this means)\n", 
         result);
  exit(result);
}
