/*
  $Id: cbc_e_m.c,v 1.1 2006-06-19 23:33:20 rabbah Exp $

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
EMBED_RCS(cbc_e_m_c,
          "$Id: cbc_e_m.c,v 1.1 2006-06-19 23:33:20 rabbah Exp $")


int main(void) {
  int i, j, bitsPerShortKey, result;
  BLOCK plainText, cipherText, IV, CV, CT_9998;
  KEY binaryKey;
  char asciiKey[HEX_DIGITS_PER_KEY+1];
  char asciiIV[HEX_DIGITS_PER_BLOCK+1];
  keyInstance key;
  cipherInstance cipher;


  /* The hack that remembers CT_9998 only works if... */
  assert(BITS_PER_KEY <= 2*BITS_PER_BLOCK);
  /* ...otherwise we'd have to remember more than just CT_9998. */

  printHeader("cbc_e_m", "Cipher Block Chaining (CBC) Mode - ENCRYPTION",
              "Monte Carlo Test");

  for(bitsPerShortKey=BITS_PER_SHORTEST_KEY; bitsPerShortKey<=BITS_PER_KEY;
      bitsPerShortKey+=BITS_PER_KEY_STEP) {
    result = stringToWords("00000000000000000000000000000000", plainText,
                           WORDS_PER_BLOCK);
    if (result != TRUE) goto error;

    printf("KEYSIZE=%d\n\n", bitsPerShortKey);

    /* Construct (backwards) an ascii key of all 0s, of length
       bitsPerShortKey bits. */
    i=bitsPerShortKey/BITS_PER_HEX_DIGIT;
    asciiKey[i] = 0; /* terminating null */ 
    for (i--; i >=0; i--) {
      asciiKey[i] = '0';
    }

    /* Same for the initialisation vector. */
    i=BITS_PER_BLOCK/BITS_PER_HEX_DIGIT;
    asciiIV[i] = 0; /* terminating null */ 
    for (i--; i >=0; i--) {
      asciiIV[i] = '0';
    }

    result = cipherInit(&cipher, MODE_CBC, asciiIV);
    if (result != TRUE) goto error;
    
    /* now that we've made it into binary, let's remember this as IV */
    memcpy(IV, cipher.IV, BYTES_PER_BLOCK);
    /* dodgy */

    result = makeKey(&key, DIR_ENCRYPT, bitsPerShortKey, asciiKey);
    if (result != TRUE) goto error;

    for(i=0; i<OUTER_LOOP_MAX; i++) {
      /* NIST SPEC: if (i==0) CV_0 = IV */
      if (i == 0) {
        memcpy(cipher.IV, IV, BYTES_PER_BLOCK);
      }
      /* dodgy */

      /* NIST SPEC: Record i, KEY_i, CV_0, PT_0 */
      printf("I=%d\n", i);
      render("KEY=", key.userKey, bitsPerShortKey/BITS_PER_WORD);
      render("IV=", (WORD*) cipher.IV, WORDS_PER_BLOCK);
      render("PT=", plainText, WORDS_PER_BLOCK);
      
      for (j=0; j<INNER_LOOP_MAX; j++) {
        memcpy(CV, cipher.IV, BYTES_PER_BLOCK);

        /* NIST SPEC: IB_j = PT_j xor CV_j */
        /* This is done automatically when setting mode to CBC */

        /* encrypt */
        result = blockEncrypt(&cipher, &key, (BYTE*) plainText, BITS_PER_BLOCK,
                              (BYTE*) cipherText);
        if (result < 0) {
          goto error;
        } else if (result != BITS_PER_BLOCK) {
          result = BAD_NUMBER_OF_BITS_PROCESSED;
          goto error;
        }

        /* NIST SPEC: if (j==0) PT_j+1 = CV_0 else PT_j+1 = CT_j-1 */
        memcpy(plainText, CV, BYTES_PER_BLOCK);
          
        /* NIST SPEC: CV_j+1 = CT_j */
        /* This is done automatically when setting mode to CBC */

        if (j == INNER_LOOP_MAX-2) {
          memcpy(CT_9998, cipherText, BYTES_PER_BLOCK);
        }
      }
      
      /* NIST SPEC: Record CT_j */
      render("CT=", cipherText, WORDS_PER_BLOCK);
      printf("\n");

      /* NIST SPEC: KEY_i+1 = KEY_i xor last n bits of CT, where n=key size */
      /* First, juxtapose CT_9999 (least significant) and CT_9998 (most
         significant) into binaryKey; ...*/
      memcpy(binaryKey, CT_9998, BYTES_PER_BLOCK);
      memcpy(&binaryKey[WORDS_PER_BLOCK], cipherText, BYTES_PER_BLOCK);
      memmove(binaryKey, 
             &binaryKey[(BITS_PER_KEY-bitsPerShortKey)/BITS_PER_WORD], 
             bitsPerShortKey/BITS_PER_BYTE);

      /* ...then, xor this stuff with the previously used key. */
      for (j=0; j<bitsPerShortKey/BITS_PER_WORD; j++) {
        binaryKey[j] ^= key.userKey[j];
      }
      
      /* NB: the NIST API does not provide callers with a way to specify a
         new key in binary format, so we have to go through the rigmarole
         of computing the new key in binary and converting it to ascii so
         that we can feed it to makeKey which will internally reconvert it
         back to binary--yechh. Note that just poking a new binary key in
         key.userKey won't work, as we need to invoke the routine that
         makes the subkeys. */
      wordsToString(binaryKey, bitsPerShortKey/BITS_PER_WORD, asciiKey);
      result = makeKey(&key, DIR_ENCRYPT, bitsPerShortKey, asciiKey);
      if (result != TRUE) goto error;

      /* NIST SPEC: PT_0 = CT_9998 */
      memcpy(plainText, CT_9998, BYTES_PER_BLOCK);

      /* NIST SPEC: CV_0 = CT_9999 */
      memcpy(cipher.IV, cipherText, BYTES_PER_BLOCK);
    }
      
    printf("==========\n\n");
  }
  exit(0);

error:
  printf("Error %d (sorry, look it up in serpent-api.h)\n", result);
  exit(result);
}
