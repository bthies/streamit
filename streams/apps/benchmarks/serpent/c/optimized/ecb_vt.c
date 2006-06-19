/* Copyright (C) 1998 Ross Anderson, Eli Biham, Lars Knudsen
 * All rights reserved.
 *
 * This code is freely distributed for AES selection process.
 * No other use is allowed.
 * 
 * Copyright remains of the copyright holders, and as such any Copyright
 * notices in the code are not to be removed.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted only for the AES selection process, provided
 * that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHORS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * The licence and distribution terms for any publically available version or
 * derivative of this code cannot be changed without the authors permission.
 *  i.e. this code cannot simply be copied and put under another distribution
 * licence [including the GNU Public Licence.]
 */

#include "serpent.h"

char *serpent_convert_to_string();

main(int argc, char **argv)
{
  unsigned long plaintext[4];
  unsigned long plaintext2[4];
  unsigned long ciphertext[4];
  int i, j;
  char tmpstr[100];
  int keylen;

  printf("=========================\n");
  printf("\n");
  printf("FILENAME:  \"ecb_vt.txt\"\n");
  printf("\n");
  printf("Electronic Codebook (ECB) Mode\n");
  printf("Variable Text Known Answer Tests\n");
  printf("\n");
  printf("Algorithm Name: Serpent\n");
  printf("Principal Submitter: Ross Anderson, Eli Biham, Lars Knudsen\n");
  printf("\n");
  printf("==========\n");
  printf("\n");

  for(keylen=128; keylen<=256; keylen+=64)
    {
      int i;
      int rc;

      keyInstance keyI;
      cipherInstance cipherI;

      char kkk[300];

      strcpy(kkk,"0000000000000000000000000000000000000000000000000000000000000000");
      rc=makeKey(&keyI, DIR_ENCRYPT, keylen, kkk);
      if(rc<=0) exit(2);

      rc=cipherInit(&cipherI, MODE_ECB, "");
      if(rc<=0) exit(2);

      printf("KEYSIZE=%d\n", keylen);
      printf("\n");
      printf("KEY=%s\n", serpent_convert_to_string(keylen, keyI.key, tmpstr));
      printf("\n");

      for(i=127; i>=0; i--)
	{
	  serpent_convert_from_string(128, "00000000000000000000000000000000",
				      plaintext);
	  plaintext[i/32] = 1L<<(i&31);

	  printf("I=%d\n", 128-i);
	  printf("PT=%s\n", serpent_convert_to_string(128, plaintext, tmpstr));
	  rc=blockEncrypt(&cipherI, &keyI, (BYTE*)plaintext, 128, (BYTE*)ciphertext);
	  if(rc<=0) exit(2);
	  printf("CT=%s\n", serpent_convert_to_string(128, ciphertext, tmpstr));
	  rc=blockDecrypt(&cipherI, &keyI, (BYTE*)ciphertext, 128, (BYTE*)plaintext2);
	  if(rc<=0) exit(2);
	  printf("\n");
	  if(memcmp(plaintext, plaintext2, 16)!=0)
	    {
	      printf("Error\n");
	      exit(2);
	    }
	}
      printf("==========\n");
      printf("\n");
    }
  exit(0);
}
