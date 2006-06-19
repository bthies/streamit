/*
  $Id: serpent-aux.c,v 1.1 2006-06-19 23:33:20 rabbah Exp $

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
EMBED_RCS(serpent_aux_c,
          "$Id: serpent-aux.c,v 1.1 2006-06-19 23:33:20 rabbah Exp $")

int checkHexNumber(char* s) {
  /* Check if string 's' contains a valid hex number and, if so, return the
     number of hex digits it contains. Otherwise return -1 and exit. Return
     0 for the "borderline" case of the empty string. */


  int i = 0;

  if (s) {
    while (s[i]) {
      if(isxdigit((int) s[i])) {
        i++;
      } else {
        return -1;
      }
    }
  }
  return i;
}

char hex(int n) {
  /* Return the hex digit corresponding to the value n ('0' for 0, 'A' for
     10 etc). Result undefined if n outside 0..15.*/
  if ((0 <= n)  && (n <= 9)) {
    return (char) (n + '0');
  } else if ((10<=n) && (n <= 15)) {
    return (char) (n - 10 + 'a');
  } else {
    printf("ERROR: %d can't be converted to a hex digit\n", n);
    exit(1);
  }
}

int stringToWords(char* s, WORD w[], int words) {
  /* Convert the (big-endian) hex number contained in string 's' into
     numeric (multi-word little-endian) format and put it into 'w'. Take 'w'
     to be 'words' words long. Reject 's' if it contains too many hex
     digits to fit into 'words' words, or if the digits it contains are not
     an exact multiple of the number of digits that fit in a word. Return
     TRUE if all goes well, BAD_HEX_DIGIT if 's' contains a bad hex digit,
     BAD_LENGTH if 's' contains an unacceptable number of hex digits.  */

  int digits, i, highestWordWithData;
  char buffer[HEX_DIGITS_PER_WORD+1];

  digits = checkHexNumber(s);

  if (digits < 0) {
    return BAD_HEX_DIGIT;
  }
  if ((digits > words*HEX_DIGITS_PER_WORD) || (digits%HEX_DIGITS_PER_WORD)) {
    return BAD_LENGTH;
  }

  /* Zero-fill the most significant words--the ones that won't receive any
     data from the string we're converting. */
  highestWordWithData = digits/HEX_DIGITS_PER_WORD;
  for (i = highestWordWithData; i < words; i++) {
    w[i] = 0;
  }

  /* From the least to the most significant digits (i.e. starting from the
     rightmost end of 's'), copy word-sized chunks of hex digits from the
     string, transform them into integers and store them in the relevant
     slots of 'words'. */
  buffer[HEX_DIGITS_PER_WORD] = 0;
  for (i = 0; i < highestWordWithData; i++) {
    /* Assert: within s[], the range from 0 to digits-1 is the characters we
       haven't converted yet. Anything from s[digits] onwards has already
       been converted. */

    digits -= HEX_DIGITS_PER_WORD; 
    /* Assert: digits >= 0 because it was a multiple of HEX_DIGITS_PER_WORD. */
    strncpy(buffer, &s[digits], HEX_DIGITS_PER_WORD);
    sscanf(buffer, "%lx", &w[i]);
  }

  return TRUE;
}

/* -------------------------------------------------- */
/* printing */

void printHeader(char* filename, char* mode, char* test) {
  printf(
         "=========================\n\n"
         "FILENAME:  \"%s.txt\"\n\n"
         "%s\n"
         "%s\n\n"
         "Algorithm Name: Serpent\n"
         "Principal Submitter: Ross Anderson, Eli Biham, Lars Knudsen\n\n"
         "==========\n\n",
         filename, mode, test
         );
}


char* wordsToString(WORD x[], int size, char* buffer) {
  /* Put a printable representation of x, which is made of 'size' words,
     into buffer, which must be big enough (i.e. at least
     size*HEX_DIGITS_PER_WORD+1 chars), and return buffer. */
  int i, j=0;
  for (i = size-1; i>=0; i--, j+=HEX_DIGITS_PER_WORD) {
    sprintf(&buffer[j], "%08lx", x[i]);
  }
  buffer[j] = 0;
  return buffer;
}

void render(char* label, WORD x[], int size) {
  /* Unconditionally print 'x', which is made of 'size' words, on stdout,
     in big-endian hex format, prefixing it with 'label' and adding a
     newline at the end. 

     NB: this doesn't call wordsToString to save a malloc which would
     otherwise be necessary. */
  int i;
  printf("%s", label);
  for (i = size-1; i>=0; i--) {
    printf("%08lx", x[i]);
  }
  printf("\n");
}

/*
void render(char* label, WORD x[], int size) {
  char* temp;
  temp = malloc(size*HEX_DIGITS_PER_WORD+1);
  printf("%s%s\n", label, wordsToString(x, size, temp));
  free(temp);
}
*/
