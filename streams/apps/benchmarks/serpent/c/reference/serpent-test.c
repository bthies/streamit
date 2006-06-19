/*
  $Id: serpent-test.c,v 1.1 2006-06-19 23:33:20 rabbah Exp $

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
EMBED_RCS(serpent_test_c,
          "$Id: serpent-test.c,v 1.1 2006-06-19 23:33:20 rabbah Exp $")


/* Stuff to exercise the NIST API */
int doOneBlockViaNIST(BLOCK source, char* rawUserKey, BLOCK dest,
                      BYTE direction, int printKey) {
  /* Using the functions exported by the NIST API, take the binary 'source'
     block, encrypt it or decrypt it (as indicated by 'direction', which
     must be one of DIR_ENCRYPT or DIR_DECRYPT) under the key described in
     ASCII by 'rawUserKey' and yield the binary 'dest' block. If printKey
     is TRUE, print out the key after having converted it from its ascii
     representation.

     Return TRUE for successful operation or one of the codes returned by
     the NIST API functions in case of error. */

  keyInstance key;
  cipherInstance cipher;
  int result;

  result = makeKey(&key, direction, strlen(rawUserKey)*4, rawUserKey);
  if (result != TRUE) {
    goto cleanUp;
  }
  if (printKey) {
    render("userKey=", key.userKey, 8);
  }

  result = cipherInit(&cipher, MODE_ECB, 0);
  if (result != TRUE) {
    goto cleanUp;
  }
  if (key.direction == DIR_ENCRYPT) {
    result = blockEncrypt(&cipher, &key, (BYTE*)source, BITS_PER_BLOCK,
                          (BYTE*)dest);
  } else {
    result = blockDecrypt(&cipher, &key, (BYTE*)source, BITS_PER_BLOCK,
                          (BYTE*)dest);
  }
  if (result >= 0) {
    /* It's actually number of bits processed instead of an error code */
    if (result == 128) {
      result = TRUE;
    } else {
      result = BAD_NUMBER_OF_BITS_PROCESSED;
    }
  }

cleanUp:
  /* Yes, I am shamelessly using a goto. Better that, and providing a clean
     single-point-of-exit, than to repeat the following code after every
     NIST API function. Of course if we were writing this in C++ we'd have
     destructors to do the job for us behind the scenes...*/

  return result;
}

/* -------------------------------------------------- */
/* WARNING: No clever crypto stuff below this line. Just boring command
   line option parsing. It's a dirty job, but someone must do it if we want
   a program YOU can use straight away, without having to edit main() to
   poke in your own values. */

void help(void) {
  printf(
         "Serpent Reference Implementation\n"
         "Simple manual one-block ECB test\n"
         "\n"
         "Encrypts or decrypts one block of data using the Serpent cipher.\n"
         "\n"
         "\n"
         "SYNTAX: serpent-test mode [options]\n"
         "\n"
         "MODE is one of the following:\n"
         "-e -> encrypt\n"
         "-d -> decrypt\n"
         "-h -> help (the text you're reading right now)\n"
         "\n"
         "OPTIONS are:\n"
         "-p plainText  -> The 128-bit value to be encrypted.\n"
         "                 Required in mode -e. Ignored otherwise.\n"
         "-c cipherText -> The 128-bit value to be decrypted.\n"
         "                 Required in mode -d. Ignored otherwise.\n"
         "-k key        -> The 256-bit value of the key. Required in modes\n"
         "                 -e and -d.\n"
         "\n"
         "I/O FORMAT:\n"
         "Each value is read/printed as one long big-endian hexadecimal\n"
         "number (the leftmost hex digit is the most significant),\n"
         "with a fixed number of digits depending on its intended size\n"
         "(e.g. 32 digits for 128-bit values).\n"
         "To help you catching typos, the program\n"
         "insists that the exact number of digits be entered,\n"
         "so values must be left-filled with 0s where appropriate.\n"
         );
}

void exitMentioningHelp(void) {
  printf("Try 'serpent-test -h | more' for help.\n");
  exit(1);
}


void assignStringToUniqueOption(char** p, char* target, char* key) {
  /* The idea is to make a string variable (say, plainTextString) point to
     a value inside argv[]. 'p' points to that string variable; 'target'
     points to the relevant argv entry; 'key' (say, -p) is the name of the
     option whose value is held in 'target'.

     If 'target' is null, complain and exit (it should never be, because
     the 'key' option is supposed to have a value). If the char* pointed by
     'p' is null, make 's' point to 'target'; otherwise, complain that the
     option 'key' has already been seen, and exit. */
  if (!target) {
    printf("Option without value: %s\n", key);
    exitMentioningHelp();
  }
  if (*p) {
    printf("Multiple occurrences of %s\n", key);
    exitMentioningHelp();
  } else {
    *p = target;
  }
}


int main(int argc, char* argv[]) {
  /* Process the command line options, point out the errors if they're
     badly formed and obey them if they're ok. WARNING: this accesses, and
     even writes to, the global variables 'tag*'. */

  int i;
  char* userKeyString = 0;
  char* plainTextString = 0;
  char* cipherTextString = 0;
  char* formatString = 0;
  char* mode = 0;
  char* msg = 0;

  WORD plainText[4], cipherText[4];
  int result=TRUE;


  if (sizeof(WORD) < 4) {
    printf("ERROR: on this architecture 'WORD' is %d bits (need at least 32)\n",
           (int) (sizeof(WORD)*8));
    exit(1);
  }

  i = 1;
  while (argv[i]) {
    if (strcmp(argv[i], "-k") == 0) {
      assignStringToUniqueOption(&userKeyString, argv[++i], "-k");
    } else if (strcmp(argv[i], "-p") == 0) {
      assignStringToUniqueOption(&plainTextString, argv[++i], "-p");
    } else if (strcmp(argv[i], "-c") == 0) {
      assignStringToUniqueOption(&cipherTextString, argv[++i], "-c");
    } else if (strcmp(argv[i], "-f") == 0) {
      assignStringToUniqueOption(&formatString, argv[++i], "-f");
    } else if (strcmp(argv[i], "-e") == 0 || strcmp(argv[i], "-d") == 0
               || strcmp(argv[i], "-s") == 0 || strcmp(argv[i], "-h") == 0) {
      if (mode) {
        printf("You can only specify one mode\n");
        exitMentioningHelp();
      } else {
        mode = argv[i];
      }
    } else if (strcmp(argv[i], "-t") == 0) {
      if (!argv[++i]) {
        printf("Option without value\n");
        exitMentioningHelp();
      }
    } else {
      printf("Unrecognised option: '%s'\n", argv[i]);
      exitMentioningHelp();
    }

    i++;
  }

  if (!mode) {
    printf("Mode required.\n");
    exitMentioningHelp();
  }

  if (strcmp(mode, "-h") == 0) {
    help();
    exit(0);
  }
  if ((strcmp(mode, "-e") == 0) || (strcmp(mode, "-d") == 0)) {
    if (!userKeyString) {
      printf("-k (key) required when doing -e (encrypt) or -d (decrypt)\n");
      exitMentioningHelp();
    }
  }
  if (strcmp(mode, "-e") == 0) {
    if (plainTextString) {
      result  = stringToWords(plainTextString, plainText, 4);
      if (result != TRUE) {
        printf("Error while converting -p parameter\n");
        exitMentioningHelp();
      }
    } else {
      printf("-p (plaintext) required when doing -e (encrypt)\n");
      exitMentioningHelp();
    }
  }
  if (strcmp(mode, "-d") == 0) {
    if (cipherTextString) {
      result = stringToWords(cipherTextString, cipherText, 4);
      if (result != TRUE) {
        printf("Error while converting -c parameter\n");
        exitMentioningHelp();
      }
    } else {
      printf("-c (ciphertext) required when doing -d (decrypt)\n");
      exitMentioningHelp();
    }
  }


  /* At last we're ready to DO the thing! */

  if (strcmp(mode, "-e") == 0) {
    render("plainText=", plainText, 4);
    result = doOneBlockViaNIST(plainText, userKeyString, cipherText, 
                               DIR_ENCRYPT, TRUE);
    if (result == TRUE) {
      render("cipherText=", cipherText, 4);
    }
  } else if (strcmp(mode, "-d") == 0) {
    render("cipherText=", cipherText, 4);
    result = doOneBlockViaNIST(cipherText, userKeyString, plainText, 
                               DIR_DECRYPT, TRUE);
    if (result == TRUE) {
      render("plainText=", plainText, 4);
    }
  }
  switch(result) {
    case TRUE: 
      msg = 0; break;
    case BAD_KEY_DIR: 
      msg = "BAD_KEY_DIR"; break;
    case BAD_KEY_MAT: 
      msg = "BAD_KEY_MAT"; break;
    case BAD_KEY_INSTANCE: 
      msg = "BAD_KEY_INSTANCE"; break;
    case BAD_CIPHER_MODE: 
      msg = "BAD_CIPHER_MODE"; break;
    case BAD_CIPHER_STATE: 
      msg = "BAD_CIPHER_STATE"; break;
    case BAD_IV: 
      msg = "BAD_IV"; break;
    case BAD_HEX_DIGIT: 
      msg = "BAD_HEX_DIGIT"; break;
    case BAD_LENGTH: 
      msg = "BAD_LENGTH"; break;
    case BAD_NUMBER_OF_BITS_PROCESSED: 
      msg = "BAD_NUMBER_OF_BITS_PROCESSED"; break;
    default: 
      msg = "UNRECOGNISED_RESULT"; break;
  }
  if (msg) {
    printf("Error %d: %s\n", result, msg);
  }

  return 0;
}

