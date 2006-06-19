/*
  $Id: ecb_iv.c,v 1.1 2006-06-19 23:33:20 rabbah Exp $

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
EMBED_RCS(ecb_iv_c,
          "$Id: ecb_iv.c,v 1.1 2006-06-19 23:33:20 rabbah Exp $")



int main(void) {
  int bitsPerShortKey, result;
  BLOCK T, plainText, cipherText, recoveredPlainText, recoveredCipherText;
  char asciiKey[HEX_DIGITS_PER_KEY+1];
  char asciiT[HEX_DIGITS_PER_BLOCK+1];
  keyInstance key;
  cipherInstance cipher;
  char* masterAsciiPattern = 
    "0123456789abcdeffedcba9876543210"
    "00112233445566778899aabbccddeeff"
    "ffeeddccbbaa99887766554433221100";

  assert(strlen(masterAsciiPattern) 
         >= HEX_DIGITS_PER_BLOCK+HEX_DIGITS_PER_KEY);
  /* ...otherwise we need to put more hex digits in it! */

  printf(
         "/*\n"
         "\n"
         "For each key size, this test program picks a key K and a\n"
         "block-sized test pattern T (not all 0s: we use an asymmetric\n"
         "pattern to highlight any word swaps). It then encrypts T under K\n"
         "and decrypts the result, showing all the intermediate values\n"
         "along the way; it then DEcrypts T under K and encrypts the\n"
         "result, again showing all intermediate values.\n"
         "\n"
         "The intermediate values shown are: the 256-bit long key (LONG_KEY)\n"
         "corresponding to the supplied key; all the subkeys of the key\n"
         "schedule, both in bitslice (SK[]) and in standard (SK^[])\n"
         "format, and the outputs of all the rounds (R[], or Rinv[] for\n"
         "the inverse rounds while decrypting). The relevant round number\n"
         "for each result appears within the square brackets.\n"
         "\n"
         "Note that this reference implementation, since it does not\n"
         "implement the fast bitslice variant, only uses the standard keys\n"
         "(SK^[]) in its rounds. However the algorithm's description\n"
         "defines those in terms of the bitslice keys (SK[]), which need\n"
         "to be precomputed first, so these are shown as well.\n"
         "\n"
         "The subkeys are all precomputed within makeKey(), since they\n"
         "remain the same for all the blocks processed under the same key;\n"
         "for this reason, they all appear at the beginning instead of\n"
         "being interleaved with the round values.\n"
         "\n"
         "In keeping with the convention adopted in other NIST example\n"
         "files, there is a blank line between the output of different\n"
         "blocks. There are no blank lines between internal results\n"
         "pertaining to the same block.\n"
         "\n"
         "Note also that printing of intermediate values can be turned on\n"
         "or off for *any* test run (not that you'd want to do it in those\n"
         "that run millions of encryptions, though...) simply by linking\n"
         "the desired main program with serpent-reference-show-internals.o\n"
         "instead of the regular serpent-reference.o. As you might have\n"
         "guessed, you obtain the former by compiling serpent-reference.c\n"
         "with -DSHOW_INTERNALS. Conversely, this same test can be run\n"
         "with just the top-level results (and no intermediate printouts)\n"
         "by simply linking it with serpent-reference.o. See the Makefile\n"
         "for more details.\n"
         "\n"
         "*/\n"
         "\n"
         );

  printHeader("ecb_iv", "Electronic Codebook (ECB) Mode",
              "Intermediate Values Known Answer Tests");

  strncpy(asciiT, masterAsciiPattern, HEX_DIGITS_PER_BLOCK);
  asciiT[HEX_DIGITS_PER_BLOCK] = 0;
  result = stringToWords(asciiT, T, WORDS_PER_BLOCK);
  if (result != TRUE) goto error;

  for(bitsPerShortKey=BITS_PER_SHORTEST_KEY; bitsPerShortKey<=BITS_PER_KEY;
      bitsPerShortKey+=BITS_PER_KEY_STEP) {

    /* make the key and set things up */
    printf("KEYSIZE=%d\n\n", bitsPerShortKey);
    strncpy(asciiKey, &masterAsciiPattern[HEX_DIGITS_PER_BLOCK],
            bitsPerShortKey/BITS_PER_HEX_DIGIT);
    asciiKey[bitsPerShortKey/BITS_PER_HEX_DIGIT] = 0;
    printf("KEY=%s\n\n", asciiKey);
    result = makeKey(&key, DIR_ENCRYPT, bitsPerShortKey, asciiKey);
    if (result != TRUE) goto error;
    printf("\n");
    result = cipherInit(&cipher, MODE_ECB, 0);
    if (result != TRUE) goto error;

    /* encrypt T */
    key.direction = DIR_ENCRYPT;
    render("PT=", T, WORDS_PER_BLOCK);
    result = blockEncrypt(&cipher, &key, (BYTE*) T, BITS_PER_BLOCK,
                          (BYTE*) cipherText);
    if (result < 0) {
      goto error;
    } else if (result != BITS_PER_BLOCK) {
      result = BAD_NUMBER_OF_BITS_PROCESSED;
      goto error;
    }
    render("CT=", cipherText, WORDS_PER_BLOCK);
    printf("\n");

    /* decrypt and see if it comes out the same */
    key.direction = DIR_DECRYPT;
    render("CT=", cipherText, WORDS_PER_BLOCK);
    result = blockDecrypt(&cipher, &key, (BYTE*) cipherText, BITS_PER_BLOCK,
                          (BYTE*) recoveredPlainText);
    if (result < 0) {
      goto error;
    } else if (result != BITS_PER_BLOCK) {
      result = BAD_NUMBER_OF_BITS_PROCESSED;
      goto error;
    }
    render("PT=", recoveredPlainText, WORDS_PER_BLOCK);
    if (memcmp((BYTE*)T, (BYTE*)recoveredPlainText, BYTES_PER_BLOCK)) {
      result = DECRYPTION_MISMATCH;
      goto error;
    }
    printf("\n");

    /* decrypt T */
    key.direction = DIR_DECRYPT;
    render("CT=", T, WORDS_PER_BLOCK);
    result = blockDecrypt(&cipher, &key, (BYTE*) T, BITS_PER_BLOCK,
                          (BYTE*) plainText);
    if (result < 0) {
      goto error;
    } else if (result != BITS_PER_BLOCK) {
      result = BAD_NUMBER_OF_BITS_PROCESSED;
      goto error;
    }
    render("PT=", plainText, WORDS_PER_BLOCK);
    printf("\n");

    /* encrypt and see if it comes out the same */
    key.direction = DIR_ENCRYPT;
    render("PT=", plainText, WORDS_PER_BLOCK);
    result = blockEncrypt(&cipher, &key, (BYTE*) plainText, BITS_PER_BLOCK,
                          (BYTE*) recoveredCipherText);
    if (result < 0) {
      goto error;
    } else if (result != BITS_PER_BLOCK) {
      result = BAD_NUMBER_OF_BITS_PROCESSED;
      goto error;
    }
    render("CT=", recoveredCipherText, WORDS_PER_BLOCK);
    if (memcmp((BYTE*)recoveredCipherText, (BYTE*)T, BYTES_PER_BLOCK)) {
      result = ENCRYPTION_MISMATCH;
      goto error;
    }
    printf("\n");

    printf("==========\n\n");
  }
  exit(0);

error:
  printf("Error %d (sorry, see serpent-api.h to see what this means)\n",
         result);
  exit(result);
}
