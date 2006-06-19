/*
  $Id: ecb_tbl.c,v 1.1 2006-06-19 23:33:20 rabbah Exp $

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
EMBED_RCS(ecb_tbl_c,
          "$Id: ecb_tbl.c,v 1.1 2006-06-19 23:33:20 rabbah Exp $")

int main(void) {
  int i, bitsPerShortKey, result, round, value;
  BLOCK plainText, cipherText;
  char asciiKey[HEX_DIGITS_PER_KEY+1];
  char buffer[MAX_CHARS_PER_LINE+1];
  keyInstance key;
  cipherInstance cipher;

  printf(
         "/* \n"
         "\n"
         "For each key size, make a key of all 0s and try out some\n"
         "'magic' plaintexts, coming from stdin, that have been\n"
         "precomputed in such a way that they will present specific\n"
         "inputs to the S-boxes.\n"
         "\n"
         "All the rounds, from 0 to 31, are exercised in turn. For each\n"
         "round, all the possible nibble inputs from 0 to f are presented\n"
         "in turn to each S-box in that round. The plaintexts with these\n"
         "properties have been precomputed in advance by working\n"
         "backwards through the cipher.\n"
         "\n"
         "*/\n"
         );

  printHeader("ecb_tbl", "Electronic Codebook (ECB) Mode",
              "Tables Known Answer Tests");

  
  result = cipherInit(&cipher, MODE_ECB, 0);
  if (result != TRUE) goto error;

  for(bitsPerShortKey=BITS_PER_SHORTEST_KEY; bitsPerShortKey<=BITS_PER_KEY;
      bitsPerShortKey+=BITS_PER_KEY_STEP) {

    printf("KEYSIZE=%d\n\n", bitsPerShortKey);

    /* Construct (backwards) an ascii key of all 0s, of length
       bitsPerShortKey bits. */
    /* XXX It would be neater to read the key from the input too, instead
       of hardcoding 0000...0 */
    i=bitsPerShortKey/BITS_PER_HEX_DIGIT;
    asciiKey[i] = 0; /* terminating null */ 
    for (i--; i >=0; i--) {
      asciiKey[i] = '0';
    }
    printf("KEY=%s\n\n", asciiKey);

    result = makeKey(&key, DIR_ENCRYPT, bitsPerShortKey, asciiKey);
    if (result != TRUE) goto error;
      
    i=1;
    for(round=0; round < r; round++) {
      for(value=0; value < (1<<BITS_PER_NIBBLE); value++, i++) {
        printf("I=%d Round=%d Input value=%d\n", i, round, value);

        /* read the next hex plaintext into buffer */
        scanf("%3s", buffer);
        if (strcmp(buffer, "PT=") != 0) {
          result = BAD_INPUT;
          goto error;
        }
        scanf("%32c", buffer); /*XXX format spec tied to 128 bit
                                           block size */

        result = stringToWords(buffer, plainText, WORDS_PER_BLOCK);
        if (result != TRUE) goto error;
        render("PT=", plainText, WORDS_PER_BLOCK);
      
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
        printf("\n");
      }
    }
    printf("==========\n\n");
  }
  exit(0);

 error:
  printf("Error %d (sorry, see serpent-api.h to see what this means)\n", 
         result);
  exit(result);
}
