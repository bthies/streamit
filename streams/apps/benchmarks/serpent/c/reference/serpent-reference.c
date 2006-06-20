/*
  $Id: serpent-reference.c,v 1.2 2006-06-20 00:28:49 rabbah Exp $

  This is a reference implementation of the Serpent cipher invented by Ross
  Anderson, Eli Biham, Lars Knudsen. It is written for the human reader
  more than for the machine and, as such, it is optimised for clarity
  rather than speed. ("Premature optimisation is the root of all evil.")

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
#include "serpent-tables.h"
#include "serpent-reference.h"
#include "serpent-aux.h"
/* -------------------------------------------------- */
EMBED_RCS(serpent_reference_c,
          "$Id: serpent-reference.c,v 1.2 2006-06-20 00:28:49 rabbah Exp $")

/* NIST API functions */

int makeKey(keyInstance *key, BYTE direction, int keyLen,
			char *keyMaterial) {

/* 
   Initializes a keyInstance with the following information. The
   fields followed by (*) get their value copied verbatim from the input
   parameters to this function. The others are calculated from them.

   direction (*): DIR_ENCRYPT or DIR_DECRYPT
   keyLen (*): The key length (in bits) of the key
   keyMaterial (*): a string with the raw key information 
     (BITS_PER_BLOCK/4 ASCII characters representing the hex values for 
     the key).
   userKey: the long (256 bit) form of the key in little-endian binary format.
   KHat: a series of subkeys generated from the user key.
   
   Parameters:
   key: a structure that holds the keyInstance information
   direction: the key is being setup for encryption or decryption
   keyLen: an integer value that indicates the length of the key in bits.
   keyMaterial: the raw key information (keyLen/4 ASCII characters
   representing the hex values for the key). For example,
   "0123456789abcdef0123456789abcdef" is the string for a key with the
   binary value:
   0000000100100011010001010110011110001001101010111100...

   Returns:
   TRUE - on success
   BAD_KEY_DIR - direction is invalid (e.g., unknown value)
   BAD_KEY_MAT - keyMaterial is invalid (e.g., wrong length)
   
   */

  int result;

  if ((keyLen%BITS_PER_WORD) 
      || (keyLen > BITS_PER_KEY) 
      || (keyLen < BITS_PER_SHORTEST_KEY)) {
    return BAD_KEY_MAT;
  }
  key->direction = direction;
  key->keyLen = keyLen;

  if (keyMaterial) {
    strncpy(key->keyMaterial, keyMaterial, HEX_DIGITS_PER_KEY + 1);
  }

  result = stringToWords(key->keyMaterial, key->userKey, WORDS_PER_KEY);
  if (result != TRUE) {
    return BAD_KEY_MAT;
  }

  if (keyLen < BITS_PER_KEY) {
    shortToLongKey(key->userKey, keyLen);
  }

  makeSubkeys(key->userKey, key->KHat);
  return TRUE;
}


int cipherInit(cipherInstance* cipher, BYTE mode, char* IV) {
  /*
    Initializes the cipher with the mode and, if present, sets the
    Initialization Vector. If any algorithm specific setup is necessary,
    cipherInit() must take care of that as well. The IV parameter passed to
    cipherInit() is an ASCII hex string representation of the IV, i.e. the
    IV passed as a parameter will typically be 32 bytes long. The IV field
    of the cipherInstance structure is the binary value of the IV, i.e. it
    will typically be 16 bytes long.  Algorithm specific parameters must be
    loaded into the cipherInstance structure before calling
    cipherInit(). For example, if the algorithm can use other block sizes
    than 128-bits, a field should be added to the cipherInstance structure
    and the value being used should be loaded into the cipher parameter
    before calling cipherInit().

    Parameters:
    cipher - the cipherInstance being loaded
    mode - the operation mode of this cipher (this is one of
    MODE_ECB, MODE_CBC, or MODE_CFB1)
    IV - the cipher initialization vector, necessary for some modes

    Returns:
    TRUE - on success
    BAD_CIPHER_MODE - the mode passed is unknown.
    */

  int result;

  cipher->mode = mode;

  switch (mode) {
    case MODE_ECB:
      return TRUE;
    case MODE_CFB1:
    case MODE_CBC:
      /* Convert the initialisation vector from ascii to bin and store it
         in cipher. */
      result = stringToWords(IV, (WORD*) cipher->IV, WORDS_PER_IV);
      if (result != TRUE) {
        return BAD_IV;
      }
      return TRUE;
    default:
      return BAD_CIPHER_MODE;
  }
}



int blockEncrypt(cipherInstance* cipher, keyInstance* key, BYTE* input, int
                 inputLen, BYTE* outBuffer) {
  /* Uses the cipherInstance object and the keyInstance object to encrypt
    one block of data in the input buffer. The output (the encrypted data)
    is returned in outBuffer, which is the same size as inputLen. The
    routine returns the number of bits enciphered. inputLen will typically
    be 128 bits, but some algorithms may handle additional block
    sizes. Additionally, it is acceptable to use this routine to encrypt
    multiple "blocks" of data with one call. For example, if your algorithm
    has a block size of 128 bits, it is acceptable to pass n*128 bits to
    blockEncrypt().

    Parameters:
    cipher - the cipherInstance to be used
    key - the ciphering key
    input - the input buffer
    inputLen - the input length, in bits
    outBuffer - contains the encrypted data
    
    Returns:
    The number of bits ciphered, or
    BAD_CIPHER_STATE - cipher in bad state (e.g., not initialized)
    BAD_KEY_MAT - direction not set for DIR_ENCRYPT

  */

  /* Why does NIST insist on having both a key->direction indicator and
     separate encrypt and decrypt calls? This redundancy just opens the
     door to inconsistency and confusion, which would be avoided if the
     caller only had one way to specify the desired direction.
     
     Note also that, in case of inconsistency, we return BAD_KEY_MAT as
     requested, though it would make more sense to return BAD_KEY_DIR.
     */
  if (key->direction != DIR_ENCRYPT) {
    return BAD_KEY_MAT;
  }
  return blockEncryptOrDecrypt(cipher, key, input, inputLen, outBuffer);
}



int blockDecrypt(cipherInstance* cipher, keyInstance* key, BYTE* input, int
                 inputLen, BYTE* outBuffer) {
  /* Uses the cipherInstance object and the keyInstance object to decrypt
     one block of data in the input buffer. The output (the decrypted data)
     is returned in outBuffer, which is the same size as inputLen. The
     routine returns the number of bits deciphered. inputLen will typically
     be 128 bits, but some algorithms may handle additional block
     sizes. Additionally, it is acceptable to use this routine to decrypt
     multiple "blocks" of data with one call. For example, if your
     algorithm has a block size of 128 bits, it is acceptable to pass n*128
     bits to blockDecrypt().

     Parameters: 
     cipher - the cipherInstance to be used 
     key - the ciphering key 
     input - the input buffer 
     inputLen - the input length, in bits
     outBuffer - contains the decrypted data

     Returns:
     The number of bits deciphered, or
     BAD_CIPHER_STATE - cipher in bad state (e.g., not initialized)
     BAD_KEY_MAT - direction not set for DIR_DECRYPT
     */

  if (key->direction != DIR_DECRYPT) {
    return BAD_KEY_MAT; /* see comments in blockEncrypt */
  }
  return blockEncryptOrDecrypt(cipher, key, input, inputLen, outBuffer);
}

/* -------------------------------------------------- */
/* Stuff called by the NIST API */

int blockEncryptOrDecrypt(cipherInstance* cipher, keyInstance* key, BYTE*
                          input, int inputLen, BYTE* outBuffer) {

  int i, numBlocks, bytesLeftOver, result;

  numBlocks = inputLen / BITS_PER_BLOCK;
  bytesLeftOver = inputLen % BITS_PER_BLOCK;
  /* I'd like to return an error if this is != 0, but it's not clear to me
     from the NIST spec whether I'm allowed to or not. So I'll settle for
     just not processing the leftover bits (and the caller will have to
     check for himself whether the nmber of bytes processed is or isn't the
     same as inputLen). */
  
  for (i=0; i<numBlocks; i++) {
    result = doOneBlock((WORD*)(input + i*WORDS_PER_BLOCK), 
               (WORD*)(outBuffer + i*WORDS_PER_BLOCK), cipher, key);
    if (result != TRUE) {
      return BAD_CIPHER_STATE;
    }
  }
  return numBlocks*BITS_PER_BLOCK;
}


int doOneBlock(BLOCK input, BLOCK output, 
               cipherInstance* cipher, keyInstance* key) {
  /* Encrypt or decrypt one block, given by 'input', and put the result in
     'output'. 'cipher' points to the cipher instance to be used
     (containing among other things the initialisation vector for CBC) and
     'key' points to the key instance to be used. Return TRUE if all goes
     well, BAD_CIPHER_MODE if the required encryption or decryption mode is
     not supported, */

  int i;
  BIT plainTextBit, cipherTextBit;
  BLOCK temp;

  switch (key->direction) {
    case DIR_ENCRYPT:
      switch (cipher->mode) {
        case MODE_ECB: 
          encryptGivenKHat(input, key->KHat, output);
          break;
        case MODE_CBC:
          for (i=0; i < WORDS_PER_BLOCK; i++) {
            temp[i] = input[i] ^ ((WORD*) cipher->IV)[i];
          }
          encryptGivenKHat(temp, key->KHat, output);
          for (i=0; i < WORDS_PER_BLOCK; i++) {
            ((WORD*) (cipher->IV))[i] = output[i];
          }
          break;
        case MODE_CFB1:
          /* We use cipher->IV as the shift register. New data comes in at
             the LSB end and old data is dropped out of the MSB end. This
             is what is traditionally called a "left shift", though in our
             case we are actually shifting towards the right (MSB end)
             since we store multiword quantities in little-endian
             format. 

             The first bit of the plaintext to be encrypted is bit 0
             (LSB). This is stored in bit 0 of the output ciphertext
             block. Similarly, every bit of the plaintext is stored, after
             encryption, in the corresponding bit of the output block.

             Note that, due to the way it's called, this routine will
             always encrypt an entire block, though in theory MODE_CFB1
             could also encrypt a non-round number of bits. */

          for (i=0; i<BITS_PER_BLOCK; i++) {
            encryptGivenKHat((WORD*)(cipher->IV), key->KHat, temp);
            plainTextBit = getBit(input, i);
            cipherTextBit = getBit(temp, BITS_PER_BLOCK-1) ^ plainTextBit;
            setBit(output, i, cipherTextBit);
            shiftBlockLeft((WORD*)(cipher->IV), cipherTextBit);
          }
          break;
        default:
          return BAD_CIPHER_MODE;
      }
      break;
    case DIR_DECRYPT: 
      switch (cipher->mode) {
        case MODE_ECB: 
          decryptGivenKHat(input, key->KHat, output);
          break;
        case MODE_CBC:
          decryptGivenKHat(input, key->KHat, temp);
          for (i=0; i < WORDS_PER_BLOCK; i++) {
            output[i] = temp[i] ^ ((WORD*) cipher->IV)[i];
          }
          for (i=0; i < WORDS_PER_BLOCK; i++) {
            ((WORD*) cipher->IV)[i] = input[i];
          }
          break;
        case MODE_CFB1:
          /* The comments on the encryption side apply. See above. */
          for (i=0; i<BITS_PER_BLOCK; i++) {
            encryptGivenKHat((WORD*)(cipher->IV), key->KHat, temp);
            /* NB: yes, in CFB the cipher is used in encryption mode even
               when decrypting. */
            cipherTextBit = getBit(input, i);
            plainTextBit = getBit(temp, BITS_PER_BLOCK-1) ^ cipherTextBit;
            setBit(output, i, plainTextBit);
            shiftBlockLeft((WORD*)(cipher->IV), cipherTextBit);
          }
          break;
        default:
          return BAD_CIPHER_MODE;
      }
      break;
    default: 
      return BAD_KEY_DIR;
  }
  return TRUE;
}


/* -------------------------------------------------- */
/* Auxiliary bit-twiddling stuff */

void setBit(WORD x[], int p, BIT v) {
  /* Set the bit at position 'p' of little-endian word array 'x' to 'v'. */

  if (v) {
    x[p/BITS_PER_WORD] |= ((WORD) 0x1 << p%BITS_PER_WORD);
  } else {
    x[p/BITS_PER_WORD] &= ~((WORD) 0x1 << p%BITS_PER_WORD);
  }
}

BIT getBit(WORD x[], int p) {
  /* Return the value of the bit at position 'p' in little-endian word
     array 'x'. */

  return (BIT) ((x[p/BITS_PER_WORD] 
                 & ((WORD) 0x1 << p%BITS_PER_WORD)) >> p%BITS_PER_WORD);
}

/* They should all be just getBit, but no overloading in C... */
BIT getBitFromWord(WORD x, int p) {
  return (BIT) ((x & ((WORD) 0x1 << p)) >> p);
}
BIT getBitFromNibble(NIBBLE x, int p) {
  return (BIT) ((x & (0x1 << p)) >> p);
}

NIBBLE getNibble(WORD x, int p) {
  /* Return the nibble at position 'p' (in 0..7) in 'x'. */

  return (NIBBLE) (0xf & (x >> (p*BITS_PER_NIBBLE)));
}

NIBBLE makeNibble(BIT b0, BIT b1, BIT b2, BIT b3) {
  /* Return a nibble made of the given bits (b0 being the least significant). */

  return (NIBBLE) (b0 | (b1 << 1) | (b2 << 2) | (b3 << 3));
}

void xorBlock(BLOCK in1, BLOCK in2, BLOCK out) {
  /* Xor 'in1' and 'in2', yielding 'out'. */
  int i;
  for (i = 0; i < WORDS_PER_BLOCK; i++) {
    out[i] = in1[i] ^ in2[i];
  }
}

void applyPermutation(permutationTable t, BLOCK input, BLOCK output) {
  /* Apply the permutation defined by 't' to 'input' and return the result
     in 'output'. */

  int p;
  for (p=0; p<WORDS_PER_BLOCK; p++) {
    output[p] = 0;
  }
  for (p=0; p<BITS_PER_BLOCK; p++) {
    setBit(output, p, getBit(input, t[p]));
  }
}

void applyXorTable(xorTable t, BLOCK input, BLOCK output) {
  /* Apply the linear transformation defined by 't' to 'input', yielding
     'output'. Each bit in 'output' is the xor of the bits from 'input'
     given in the corresponding row of 't'. */

  int i, j;
  BIT b;
  for (i = 0; i < BITS_PER_BLOCK; i++) {
    b = 0;
    j = 0;
    while (t[i][j] != MARKER) {
      b ^= getBit(input, t[i][j]);
      j++;
    }
    setBit(output, i, b);
  }
}

NIBBLE S(int box, NIBBLE input) {
  /* Apply S-box number 'box' (0..31) to 'input', returning its output. */

  return SBox[box][input];
}

NIBBLE SInverse(int box, NIBBLE output) {
  /* Apply S-box number 'box' (0..31) in reverse to 'output', returning its
     input. */

  return SBoxInverse[box][output];
}

WORD rotateLeft(WORD x, int p) {
  /* Return word 'x' rotated left by 'p' places. The leftmost (i.e. most
     significant) 'p' bits fall off the edge and come back in from the
     other side. */
  return ((x << p) | (x >> (BITS_PER_WORD-p))) & 0xffffffff;
}

void shiftBlockLeft(BLOCK b, BIT in) {
  /* Return block 'b' shifted left by one place. The leftmost (i.e. most
     significant) bit falls off the edge and is lost, while bit 'in' is
     inserted at the rightmost (least significant) position to make up for
     it. */
  int i;
  for (i=WORDS_PER_BLOCK-1; i>0; i--) {
    b[i] <<= 1;
    setBit(&b[i], 0, getBit(&b[i-1], BITS_PER_WORD-1));
  }
  b[0] <<= 1;
  setBit(&b[0], 0, in);
}

/* -------------------------------------------------- */
/* Short key support */

void shortToLongKey(KEY key, int bitsInShortKey) {
  /* Take a short key (stored in key) and transform it (in place) into its
     long form (and write it to longKey). The number of significant bits in
     the supplied short key is given by bitsInShortKey. The caller is
     responsible for ensuring that all the remaining (most significant)
     bits in key from position bitsInShortKey upwards are 0 (as they should
     be anyway since key is declared to be WORDS_PER_KEY words long). */

  key[bitsInShortKey/BITS_PER_WORD] |= 
    ((WORD) 1) << (bitsInShortKey%BITS_PER_WORD);
}

/* -------------------------------------------------- */
/* Functions used in the formal description of the cipher */

void IP(BLOCK input, BLOCK output) {
  /* Apply the Initial Permutation to 'input', yielding 'output'. */
  applyPermutation(IPTable, input, output);
}

void FP(BLOCK input, BLOCK output) {
  /* Apply the Final Permutation to 'input', yielding 'output'. */
  applyPermutation(FPTable, input, output);
}

void IPInverse(BLOCK output, BLOCK input) {
  /* Apply the Initial Permutation in reverse to 'output', yielding 'input'. */
  applyPermutation(FPTable, output, input);
}

void FPInverse(BLOCK output, BLOCK input) {
  /* Apply the Final Permutation in reverse to 'output', yielding 'input'. */
  applyPermutation(IPTable, output, input);
}

void SHat(int box, BLOCK input, BLOCK output) {
  /* Apply a parallel array of 32 copies of S-box number 'box' to 'input',
     yielding 'output'. */
  int iWord, iNibble;
  for (iWord = 0; iWord < WORDS_PER_BLOCK; iWord++) {
    output[iWord] = 0;
    for (iNibble = 0; iNibble < NIBBLES_PER_WORD; iNibble++) {
      output[iWord] |= ((WORD) S(box, getNibble(input[iWord], iNibble)))
                        << (iNibble*BITS_PER_NIBBLE);
    }
  }
}

void SHatInverse(int box, BLOCK output, BLOCK input) {
  /* Apply, in reverse, a parallel array of 32 copies of S-box number 'box'
     to 'output', yielding 'input'. */
  int iWord, iNibble;
  for (iWord = 0; iWord < WORDS_PER_BLOCK; iWord++) {
    input[iWord] = 0;
    for (iNibble = 0; iNibble < NIBBLES_PER_WORD; iNibble++) {
      input[iWord] |= ((WORD) SInverse(box, getNibble(output[iWord], iNibble)))
                        << (iNibble*BITS_PER_NIBBLE);
    }
  }
}

void LT(BLOCK input, BLOCK output) {
  /* Apply the table-based version of the linear transformation to 'input',
     yielding 'output'. */

  applyXorTable(LTTable, input, output);
}

void LTInverse(BLOCK output, BLOCK input) {
  /* Apply, in reverse, the table-based version of the linear
     transformation to 'output', yielding 'input'. */

  applyXorTable(LTTableInverse, output, input);
}

void R(int i, BLOCK BHati, keySchedule KHat, BLOCK BHatiPlus1) {
  /* Apply round 'i' to 'BHati', yielding 'BHatiPlus1'. Do this using the
    appropriately numbered subkey(s) from 'KHat'. NB: it is allowed for
    BHatiPlus1 to point to the same memory as BHati. */

  BLOCK xored, SHati;

  xorBlock(BHati, KHat[i], xored);
  SHat(i, xored, SHati);

  if ( (0 <= i) && (i <= r-2) ) {
    LT(SHati, BHatiPlus1);
  } else if (i == r-1) {
    xorBlock(SHati, KHat[r], BHatiPlus1);
  } else {
    printf("ERROR: round %d is out of 0..%d range", i, r-1);
    exit(1);
    /* Printf and exit is disgusting--if we were programming in a sensible
       language, we'd have exceptions. Shall I make the code less readable
       with ugly return codes that no caller ever checks anyway?
       Naaah... */
  }

#ifdef SHOW_INTERNALS
  printf("R[%d]", i);
  render("=", BHatiPlus1, WORDS_PER_BLOCK);
#endif
}

void RInverse(int i, BLOCK BHatiPlus1, keySchedule KHat, BLOCK BHati) {
  /* Apply round 'i' in reverse to 'BHatiPlus1', yielding 'BHati'. Do this
    using the appropriately numbered subkey(s) from 'KHat'. NB: it is
    allowed for BHati to point to the same memory as BHatiPlus1. */

  BLOCK xored, SHati;

  if ( (0 <= i) && (i <= r-2) ) {
    LTInverse(BHatiPlus1, SHati);
  } else if (i == r-1) {
    xorBlock(BHatiPlus1, KHat[r], SHati);
  } else {
    printf("ERROR: round %d is out of 0..%d range", i, r-1);
    exit(1);
  }
  SHatInverse(i, SHati, xored);
  xorBlock(xored, KHat[i], BHati);

#ifdef SHOW_INTERNALS
  printf("Rinv[%d]", i);
  render("=", BHati, WORDS_PER_BLOCK);
#endif
}



void makeSubkeysBitslice(KEY userKey, keySchedule K) {
  /* Given 'userKey' (shown as K in the paper, but we can't use that name
     because of a collision with K[i] used later for something else),
     produce the array 'K' of 33 128-bit subkeys. */

  int i, j, l, whichS;
  NIBBLE input, output;
  WORD k[132], raw_w[140];
  /* The w array, in the notation of the paper, is supposed to span the
     range -8..131. But we can't have negative array indices in C. NOTE
     that this is a typical place where one can make trivial mistakes in
     the implementation when setting j = i+8 and so on (I know -- I did
     too).  So what do I do here? My little hack (all for the sake of
     readability) is to define a raw_w array in the range 0..139 and to
     make a "proper" w alias on top of that by starting from the 8th
     element, so that w[i] is redirected to raw_w[i+8], which works for i
     in -8..131. */
  WORD* w = &raw_w[8];

  /* "We write the key as 8 32-bit words w-8 ... w-1" */
  for (i = -8; i < 0; i++) {
    w[i] = userKey[i+8];
  }

  /* "We expand these to a prekey w0 ... w131 with the affine recurrence" */
  for (i = 0; i < 132; i++) {
    w[i] = rotateLeft(w[i-8] ^ w[i-5] ^ w[i-3] ^ w[i-1] ^ phi ^ i, 11);
  }

  /* The round keys are now calculated from the prekeys using the S-boxes
     in bitslice mode. */
  for (i = 0; i < r+1; i++) {
    whichS = (r + 3 - i) % r;
    k[0+4*i] = k[1+4*i] = k[2+4*i] = k[3+4*i] = 0;
    for (j = 0; j < 32; j++) {
      input = makeNibble(getBitFromWord(w[0+4*i], j),
                         getBitFromWord(w[1+4*i], j),
                         getBitFromWord(w[2+4*i], j),
                         getBitFromWord(w[3+4*i], j));
      output = S(whichS, input);
      for (l = 0; l < 4; l++) {
        k[l+4*i] |= ((WORD) getBitFromNibble(output, l)) << j;
      }
    }
  }

  /* We then renumber the 32 bit values k_j as 128 bit subkeys K_i. */
  for (i = 0; i < 33; i++) {
    for (j = 0; j < 4; j++) {
      K[i][j] = k[4*i+j];
    }

#ifdef SHOW_INTERNALS
    printf("SK[%d]", i);
    render("=", K[i], WORDS_PER_BLOCK);
#endif
  }
}


void makeSubkeys(KEY userKey, keySchedule KHat) {
  /* Given 'userKey' (shown as K in the paper, but we can't use that name
     because of a collision with K[i] used later for something else),
     produce the array 'KHat' of 33 128-bit subkeys. */

  int i;
  keySchedule K;

#ifdef SHOW_INTERNALS
  render("LONG_KEY=", userKey, WORDS_PER_KEY);
#endif

  makeSubkeysBitslice(userKey, K);

  /* We now apply IP to the round key in order to place the key bits in the
     correct column. */
  for (i = 0; i < 33; i++) {
      IP(K[i], KHat[i]);

#ifdef SHOW_INTERNALS
    printf("SK^[%d]", i);
    render("=", KHat[i], WORDS_PER_BLOCK);
#endif
  }

}


void encryptGivenKHat(BLOCK plainText, keySchedule KHat, BLOCK cipherText) {
  /* Encrypt 'plainText' with 'KHat', using the normal (non-bitslice)
     algorithm, yielding 'cipherText'. */

  BLOCK BHat;
  int i;

  IP(plainText, BHat);
  for (i = 0; i < r; i++) {
    R(i, BHat, KHat, BHat);
  }
  FP(BHat, cipherText);
}

void decryptGivenKHat(BLOCK cipherText, keySchedule KHat, BLOCK plainText) {
  /* Decrypt 'cipherText' with 'KHat', using the normal (non-bitslice)
     algorithm, yielding 'plainText'. */

  BLOCK BHat;
  int i;

  FPInverse(cipherText, BHat);
  for (i = r-1; i >=0; i--) {
    RInverse(i, BHat, KHat, BHat);
  }
  IPInverse(BHat, plainText);
}

