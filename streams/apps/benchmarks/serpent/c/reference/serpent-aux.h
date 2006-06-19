/*
  $Id: serpent-aux.h,v 1.1 2006-06-19 23:33:20 rabbah Exp $

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


#ifndef _SERPENT_AUX_
#define _SERPENT_AUX_

#include "serpent-api.h"

EMBED_RCS(serpent_aux_h,
          "$Id: serpent-aux.h,v 1.1 2006-06-19 23:33:20 rabbah Exp $")



int checkHexNumber(char* s);
char hex(int n);
int stringToWords(char* s, WORD w[], int words);
char* wordsToString(WORD x[], int size, char* buffer);
void printHeader(char* filename, char* mode, char* test);
void render(char* label, WORD x[], int size);

#endif


  
