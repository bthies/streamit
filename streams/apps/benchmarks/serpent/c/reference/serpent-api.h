/*
  $Id: serpent-api.h,v 1.1 2006-06-19 23:33:20 rabbah Exp $

  This file is derived from the NIST-supplied sample-c-api.txt dated April
  15 1998, with the appropriate modifications for Serpent.
  
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

#ifndef _SERPENT_API_
#define _SERPENT_API_



/*  aes.h  */

/*  AES Cipher header file for ANSI C Submissions
    Lawrence E. Bassham III
    Computer Security Division
    National Institute of Standards and Technology
    
    April 15, 1998
    
    This sample is to assist implementers developing to the Cryptographic 
    API Profile for AES Candidate Algorithm Submissions.  Please consult this 
    document as a cross-reference.
    
    ANY CHANGES, WHERE APPROPRIATE, TO INFORMATION PROVIDED IN THIS FILE
    MUST BE DOCUMENTED.  CHANGES ARE ONLY APPROPRIATE WHERE SPECIFIED WITH
    THE STRING "CHANGE POSSIBLE".  FUNCTION CALLS AND THEIR PARAMETERS CANNOT 
    BE CHANGED.  STRUCTURES CAN BE ALTERED TO ALLOW IMPLEMENTERS TO INCLUDE 
    IMPLEMENTATION SPECIFIC INFORMATION.
    */

/*  Includes:
	Standard include files
    */

#include <stdio.h>
#include <ctype.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

/*  Defines:
	Add any additional defines you need
    */

#define     DIR_ENCRYPT     0    /*  Are we encrpyting?  */
#define     DIR_DECRYPT     1    /*  Are we decrpyting?  */
#define     MODE_ECB        1    /*  Are we ciphering in ECB mode?   */
#define     MODE_CBC        2    /*  Are we ciphering in CBC mode?   */
#define     MODE_CFB1       3    /*  Are we ciphering in 1-bit CFB mode? */
#define     TRUE            1
#define     FALSE           0

/*  Error Codes - CHANGE POSSIBLE: inclusion of additional error codes  */
#define     BAD_KEY_DIR        -1  /*  Key direction is invalid, e.g.,
                                       unknown value */
#define     BAD_KEY_MAT        -2  /*  Key material not of correct 
                                       length */
#define     BAD_KEY_INSTANCE   -3  /*  Key passed is not valid  */
#define     BAD_CIPHER_MODE    -4  /*  Params struct passed to 
                                       cipherInit invalid */
#define     BAD_CIPHER_STATE   -5  /*  Cipher in wrong state (e.g., not 
                                       initialized) */

#define DECRYPTION_MISMATCH -7 /* Test decryption doesn't recover plaintext */
#define ENCRYPTION_MISMATCH -8 /* Test encryption doesn't recover ciphertext */
#define BAD_HEX_DIGIT -9 /* Not a valid hex digit */
#define BAD_LENGTH -10 /* Not the required length */
#define BAD_IV -11 /* initialisation vector not valid */
#define BAD_NUMBER_OF_BITS_PROCESSED -12 /* Expecting something different */
#define BAD_INPUT -13 /* Label on this line does not match what we expected. */


/*  CHANGE POSSIBLE:  inclusion of algorithm specific defines  */

#define     MAX_KEY_SIZE	64  /* # of ASCII char's needed to
                                   represent a key */
#define     MAX_IV_SIZE		16  /* # bytes needed to
                                   represent an IV  */
/* I don't like these constants named "size" because they don't make the
   unit of "size" explicit when you look through the code that uses them. I
   find it clearer to use BITS_PER_*, BYTES_PER_* etc. So I leave these
   here since they're NIST-defined, but I use the more readable ones in the
   code. */


#define MAX_XOR_TAPS_PER_BIT 7 /* Maximum number of entries per row in the
                                  Linear Transformation table, i.e. maximum
                                  # of input bits to be xored together to
                                  yield an output bit. */

#define r 32 /* # of rounds */
#define phi 0x9e3779b9 /* magic number used in key schedule initialisation */

/* primitive lengths */
#define BITS_PER_BYTE 8
#define BITS_PER_NIBBLE 4
#define BYTES_PER_WORD 4
#define WORDS_PER_BLOCK 4
#define WORDS_PER_KEY 8 /* Unless otherwise specified, in these constants
                            KEY stands for "long key", which is the only
                            internal representation we use (see Serpent
                            paper: shorter keys all have a corresponding
                            long representation). */

/* derived lengths */
#define BITS_PER_HEX_DIGIT BITS_PER_NIBBLE
#define NIBBLES_PER_WORD (BITS_PER_WORD/BITS_PER_NIBBLE)
#define BITS_PER_WORD (BITS_PER_BYTE*BYTES_PER_WORD)
#define BYTES_PER_IV MAX_IV_SIZE
#define BYTES_PER_BLOCK (BITS_PER_BLOCK/BITS_PER_BYTE) 
#define BYTES_PER_KEY_SCHEDULE (BYTES_PER_WORD*WORDS_PER_KEY_SCHEDULE)
#define HEX_DIGITS_PER_WORD (BITS_PER_WORD/BITS_PER_HEX_DIGIT)
#define HEX_DIGITS_PER_BLOCK (HEX_DIGITS_PER_WORD*WORDS_PER_BLOCK)
#define HEX_DIGITS_PER_KEY (HEX_DIGITS_PER_WORD*WORDS_PER_KEY)
/* ASSERT: HEX_DIGITS_PER_KEY == MAX_KEY_SIZE */
#define BITS_PER_BLOCK (BITS_PER_WORD*WORDS_PER_BLOCK) 
#define WORDS_PER_IV (BYTES_PER_IV/BYTES_PER_WORD)
#define BITS_PER_KEY (BITS_PER_WORD*WORDS_PER_KEY)
#define WORDS_PER_KEY_SCHEDULE ((r+1)*WORDS_PER_BLOCK)


/* Used in looping over all the accepted (short) key lengths */
#define BITS_PER_SHORTEST_KEY 128 
#define BITS_PER_KEY_STEP 64

/* Used in the KAT and Monte Carlo */
#define OUTER_LOOP_MAX 400
#define INNER_LOOP_MAX 10000


#define MAX_CHARS_PER_LINE 80

/*  Typedefs:
    
	Typedef'ed data storage elements.  Add any algorithm specific 
    parameters at the bottom of the structs as appropriate.
    */

typedef unsigned char BYTE;
typedef unsigned long WORD;
/* This must be at least 32 bits. If the machine has a longer word size,
   the top bits will just be ignored. */

/* C's weak type checking won't help us much with the following
   pseudo-types: I use them mostly to make it clear to the human reader
   what the parameters to the functions are _meant_ to be, not to expect
   help from the compiler. */
typedef unsigned char NIBBLE; /* convention: only the bottom 4 bits are used */
typedef unsigned char BIT; /* convention: only the bottom bit is used */

typedef int permutationTable[BITS_PER_BLOCK];
typedef BYTE xorTable[BITS_PER_BLOCK][MAX_XOR_TAPS_PER_BIT+1];

typedef WORD keySchedule[r+1][WORDS_PER_BLOCK];
typedef WORD BLOCK[WORDS_PER_BLOCK];
typedef WORD KEY[WORDS_PER_KEY];

/*  The structure for key information */
typedef struct {
  BYTE  direction;	/*  Key used for encrypting or decrypting? */
  int   keyLen;	/*  Length of the key  */
  char  keyMaterial[MAX_KEY_SIZE+1];  /*  Raw key data in ASCII,
                                          e.g., user input or KAT values */
  /*  The following parameters are algorithm dependent, replace or
      add as necessary  */
  KEY userKey; /* (long) user key in little-endian binary form */
  keySchedule KHat; /* series of subkeys */
} keyInstance;

/*  The structure for cipher information */
typedef struct {
  BYTE  mode;            /* MODE_ECB, MODE_CBC, or MODE_CFB1 */
  BYTE  IV[MAX_IV_SIZE]; /* A possible Initialization Vector for 
                            ciphering */
  /*  Add any algorithm specific parameters needed here  */
  int   blockSize;    	/* Sample: Handles non-128 bit block sizes
                           (if available) */
} cipherInstance;


/*  Function prototypes  */

/* NIST API */
int makeKey(keyInstance* key, BYTE direction, int keyLen,
			char* keyMaterial);
int cipherInit(cipherInstance* cipher, BYTE mode, char* IV);
int blockEncrypt(cipherInstance* cipher, keyInstance* key, BYTE* input, 
                 int inputLen, BYTE* outBuffer);
int blockDecrypt(cipherInstance* cipher, keyInstance* key, BYTE* input,
                 int inputLen, BYTE* outBuffer);


/* Embedding RCS info from all used sources in the object files */
#define EMBED_RCS(name, id) \
static char* _##name = id; \
static void* f_##name(void) {return (void*) _##name;}
/* The string will be embedded in the program's binary, whence it will be
   extractable with "strings ... | fgrep $Id". The function is never used
   and is there as a hack to stop the compiler complaining about the string
   not being used. */

EMBED_RCS(serpent_api_h,
          "$Id: serpent-api.h,v 1.1 2006-06-19 23:33:20 rabbah Exp $")

#endif
