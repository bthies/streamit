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

#define LOOPTIMES 1000000

#include "serpent.h"

char *serpent_convert_to_string();

main(int argc, char **argv)
{
  unsigned long x[4];
  int i, j;
  int loop_enc=LOOPTIMES, loop_dec=LOOPTIMES;
  char tmpstr[100];

  int rc;

  keyInstance keyI;
  cipherInstance cipherI;

  if(argc>1)
    loop_enc=atoi(argv[1]);
  if(argc>2)
    loop_dec=atoi(argv[2]);

  rc=cipherInit(&cipherI, MODE_ECB, "");
  if(rc<=0) exit(2);

  /* Let the plaintext be 0x00000000,0x00000001,0x00000002,0x00000003 */
  
  x[0] = 0; x[1] = 1; x[2] = 2; x[3] = 3;

  rc=makeKey(&keyI, DIR_ENCRYPT, 256,
            "0000000000000000000000000000000000000000000000000000000000000000");
  if(rc<=0) exit(2);

  printf("Plaintext          = %s\n",
	 serpent_convert_to_string(128, x, tmpstr));
  printf("Key                = %s\n",
	 serpent_convert_to_string(256, keyI.key, tmpstr));

  /* Repeatedly encrypt the plaintext LOOPTIMES times */
  for(i=0; i<loop_enc; i++)
    {
      rc=blockEncrypt(&cipherI, &keyI, (BYTE*)x, 128, (BYTE*)x);
      if(rc<=0) exit(2);
      if(i==0)
          printf("Ciphertext^%7d = %s\n",
                 i+1, serpent_convert_to_string(128, x, tmpstr));
    }

  printf("Ciphertext^%7d = %s\n",
         loop_enc, serpent_convert_to_string(128, x, tmpstr));


  /* Repeatedly decrypt the plaintext LOOPTIMES times */
  for(i=loop_dec-1; i>=0; i--)
    {
      rc=blockDecrypt(&cipherI, &keyI, (BYTE*)x, 128, (BYTE*)x);
      if(rc<=0) exit(2);

      if(i==1)
	printf("Decrypted-Ciphertext=%s\n",
	       serpent_convert_to_string(128, x, tmpstr));
    }

  printf("Decrypted-plaintext= %s\n",
         serpent_convert_to_string(128, x, tmpstr));

  exit(0);
}
