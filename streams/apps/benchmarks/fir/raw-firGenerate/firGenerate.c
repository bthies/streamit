/************************************************************************
*    File Name:  firGenerate.c
*      Version:  1
*         Date:  November 12, 2003
*  Description:  Generates assembly files for Raw FIR
* Dependencies:  
*
*       Author:  Nathan R. Shnidman
*        Email:  naters@cag.lcs.mit.edu
*        Phone:  (617) 253-2785
*      Company:  M.I.T.
*
*        Notes:  
*
*                Copyright (c) 2003 Nathan R. Shnidman
*                All rights reserved
************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <time.h>

#define TRUE -1
#define FALSE 0

struct proc {
  FILE *fp;
  char fileName[256];
  char weight[256];
  char input[16];
  char input2[16];
  char output[16];
  char output2[16];
};

static int order[16] = {0, 4, 8, 12, 13, 9, 5, 1, 2, 6, 10, 14, 15, 11, 7, 3};
static int rorder[16] = {0, 7, 8, 15, 1, 6, 9, 14, 2, 5, 10, 13, 3, 4, 11, 12};

void print_usage(int long_version);
int procCode(struct proc *tile, int i);
int switchCode(struct proc *tile, int i);
int setupTiles(struct proc *tile, int i);

int main(int argc, char *argv[]) {  
  FILE *fWeights;
  FILE *fTestFile; // DEBUG ONLY
  int i, j, k;
  struct proc tile[16];
  char genTime[26];
  time_t timer;

  // Open link to input and output files.
  if ((fWeights = fopen("weights.txt", "r")) == NULL) {
    fprintf(stderr, "%s: can't open %s\n", "firGenerate", "weights.txt");
    exit(1);
  } 
  for (i=0;i<16;i++) {
    sprintf(tile[i].fileName, "fir1fp_%d.S", i);
    if ((tile[i].fp = fopen(tile[i].fileName, "w+")) == NULL) {
      fprintf(stderr, "%s: can't open file %s\n", "firGenerate", 
              tile[i].fileName);
      exit(1);
    } 
    // Setup tile structures
    setupTiles(tile, i);
  }
  /* DEBUG ONLY  */
  if ((fTestFile = fopen("TestFile", "w+")) == NULL) {
    fprintf(stderr, "%s: can't open %s\n", "firGenerate", "TestFile.txt");
    exit(1);
  }

  // Gather Weights
  for (i=0;i<16;i++) {
    if (fscanf(fWeights, "%s", &(tile[order[i]].weight)) != EOF) {}
    else {fprintf(stderr, "Error reading in weights, only read in %d values", i);}
  }

  // Get current date/time
  timer=time(NULL);
  sprintf(genTime, "%s", asctime(localtime(&timer)));
  // Generate Tile code
  for (i=0;i<16;i++) {
    // Generate file headers
    fprintf(tile[i].fp, "# %s\n", tile[i].fileName);
    fprintf(tile[i].fp, "# \n");
    fprintf(tile[i].fp, "# Nathan R. Shnidman\n");
    fprintf(tile[i].fp, "# Origin Date: October 19, 2003\n");
    fprintf(tile[i].fp, "# Generated: %s", genTime);
    fprintf(tile[i].fp, "# \n");
    fprintf(tile[i].fp, "# This app implements part of a floating point FIR filter.\n");
    fprintf(tile[i].fp, "# \n");
    fprintf(tile[i].fp, " \n");
    fprintf(tile[i].fp, "#include \"module_test.h\"\n");
    fprintf(tile[i].fp, "#define WEIGHT %s\n", tile[i].weight);
    fprintf(tile[i].fp, "#define W%d $2\n", i);
    fprintf(tile[i].fp, " \n");
    fprintf(tile[i].fp, ".text\n");
    fprintf(tile[i].fp, ".global begin\n");
    fprintf(tile[i].fp, "begin:\n");
    fprintf(tile[i].fp, "\tMTSRi\tSW_PC, %clo(switch_begin)\n", '%');
    fprintf(tile[i].fp, "\tMTSRi\tSW_FREEZE, 0\n");
    fprintf(tile[i].fp, "\t\n");
    fprintf(tile[i].fp, "\tli\tW%d, WEIGHT\n", i);
    // Generate Tile specific processor code
    procCode(tile, i);
    // Generate Tile specific switch code
    switchCode(tile, i);
  }

  for (i=0;i<16;i++) {
    fclose(tile[i].fp);
  }
  fclose(fTestFile); // DEBUG ONLY
  return 1;
}

int setupTiles(struct proc *tile, int i) {
  if (i == 0 || i == 2) {
    sprintf(tile[i].input, "%s", "$cWi");
    sprintf(tile[i].input2, "%s", "$cWi");
    sprintf(tile[i].output, "%s", "$cSo");
    sprintf(tile[i].output2, "%s", "$cSo");
  }
  else if (i==4 || i==8 || i==6 || i==10) {
    sprintf(tile[i].input, "%s", "$cNi");
    sprintf(tile[i].input2, "%s", "$cNi");
    sprintf(tile[i].output, "%s", "$cSo");
    sprintf(tile[i].output2, "%s", "$cSo");
  }
  else if (i==9 || i==5 || i==7 || i==11) {
    sprintf(tile[i].input, "%s", "$cSi");
    sprintf(tile[i].input2, "%s", "$cSi");
    sprintf(tile[i].output, "%s", "$cNo");
    sprintf(tile[i].output2, "%s", "$cNo");
  }
  else if (i==12 || i==14) {
    sprintf(tile[i].input, "%s", "$cNi");
    sprintf(tile[i].input2, "%s", "$cNi");
    sprintf(tile[i].output, "%s", "$cEo");
    sprintf(tile[i].output2, "%s", "$cEo");
  }
  else if (i==13 || i==15) {
    sprintf(tile[i].input, "%s", "$cWi");
    sprintf(tile[i].input2, "%s", "$cWi");
    sprintf(tile[i].output, "%s", "$cNo");
    sprintf(tile[i].output2, "%s", "$cNo");
  }
  else if (i==1) {
    sprintf(tile[i].input, "%s", "$cSi");
    sprintf(tile[i].input2, "%s", "$cSi");
    sprintf(tile[i].output, "%s", "$cEo");
    sprintf(tile[i].output2, "%s", "$cEo");
  }
  else { // i==3
    sprintf(tile[i].input, "%s", "$cSi");
    sprintf(tile[i].input2, "%s", "$cSi");
    sprintf(tile[i].output, "%s", "$cEo");
    sprintf(tile[i].output2, "%s", "$cEo");
  }

  return 1;
}


int procCode(struct proc *tile, int i) {
  if (i==0) {
    fprintf(tile[i].fp, "	li	$csto, 0x00000401 # we send a bogus" 
            " word out to the switch\n");
    fprintf(tile[i].fp, "	\n");
    fprintf(tile[i].fp, "	move	$csto, $0\n");
    fprintf(tile[i].fp, "stream_loop:\n");
    fprintf(tile[i].fp, "	mul.s	$3, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$4, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$5, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$6, W%d, $csti\n", i);
    fprintf(tile[i].fp, " 	add.s	$csto, $3, $0\n");
    fprintf(tile[i].fp, " 	add.s	$csto, $4, $0\n");
    fprintf(tile[i].fp, " 	add.s	$csto, $5, $0\n");
    fprintf(tile[i].fp, "	add.s	$csto, $6, $0\n");
    fprintf(tile[i].fp, "	mul.s	$3, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$4, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$5, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$6, W%d, $csti\n", i);
    fprintf(tile[i].fp, " 	add.s	$csto, $3, $0\n");
    fprintf(tile[i].fp, " 	add.s	$csto, $4, $0\n");
    fprintf(tile[i].fp, " 	add.s	$csto, $5, $0\n");
    fprintf(tile[i].fp, "	add.s	$csto, $6, $0\n");
    fprintf(tile[i].fp, "	mul.s	$3, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$4, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$5, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$6, W%d, $csti\n", i);
    fprintf(tile[i].fp, " 	add.s	$csto, $3, $0\n");
    fprintf(tile[i].fp, " 	add.s	$csto, $4, $0\n");
    fprintf(tile[i].fp, " 	add.s	$csto, $5, $0\n");
    fprintf(tile[i].fp, "	add.s	$csto, $6, $0\n");
    fprintf(tile[i].fp, "	j	stream_loop\n");
  }
  else {
    fprintf(tile[i].fp, "\t\n");
    if (i!=3) {fprintf(tile[i].fp, "	move	$csto, $0\n");}
    if (0){ //i==4 || i==8) { // I Turned This OFF
      fprintf(tile[i].fp, "	mul.s	$3, W%d, $csti\n", i);
      fprintf(tile[i].fp, "	mul.s	$4, W%d, $csti\n", i);
      fprintf(tile[i].fp, "	mul.s	$5, W%d, $csti\n", i);
      fprintf(tile[i].fp, "	mul.s	$6, W%d, $csti\n", i);
      fprintf(tile[i].fp, "	add.s!	$7, $3, $csti\n");
      fprintf(tile[i].fp, "	add.s!	$8, $4, $csti\n");
      fprintf(tile[i].fp, "	add.s!	$9, $5, $csti\n");
      fprintf(tile[i].fp, "	add.s!	$10, $6, $csti\n");
      fprintf(tile[i].fp, "	PASS_REG($7)\n");
      fprintf(tile[i].fp, "	PASS_REG($8)\n");
      fprintf(tile[i].fp, "	PASS_REG($9)\n");
      fprintf(tile[i].fp, "	PASS_REG($10)\n");
      fprintf(tile[i].fp, "	nop	\n");
    }
    fprintf(tile[i].fp, "stream_loop:\n");
    fprintf(tile[i].fp, "	mul.s	$3, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$4, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$5, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$6, W%d, $csti\n", i);
    fprintf(tile[i].fp, " 	add.s	$csto, $3, $csti\n");
    fprintf(tile[i].fp, " 	add.s	$csto, $4, $csti\n");
    fprintf(tile[i].fp, " 	add.s	$csto, $5, $csti\n");
    fprintf(tile[i].fp, "	add.s	$csto, $6, $csti\n");
    fprintf(tile[i].fp, "	mul.s	$3, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$4, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$5, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$6, W%d, $csti\n", i);
    fprintf(tile[i].fp, " 	add.s	$csto, $3, $csti\n");
    fprintf(tile[i].fp, " 	add.s	$csto, $4, $csti\n");
    fprintf(tile[i].fp, " 	add.s	$csto, $5, $csti\n");
    fprintf(tile[i].fp, "	add.s	$csto, $6, $csti\n");
    fprintf(tile[i].fp, "	mul.s	$3, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$4, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$5, W%d, $csti\n", i);
    fprintf(tile[i].fp, "	mul.s	$6, W%d, $csti\n", i);
    fprintf(tile[i].fp, " 	add.s	$csto, $3, $csti\n");
    fprintf(tile[i].fp, " 	add.s	$csto, $4, $csti\n");
    fprintf(tile[i].fp, " 	add.s	$csto, $5, $csti\n");
    fprintf(tile[i].fp, "	add.s	$csto, $6, $csti\n");
    fprintf(tile[i].fp, "	j	stream_loop\n");
  }
  fprintf(tile[i].fp, "\t\n");

  return 1;
}


int switchCode(struct proc *tile, int i) {
  char input[16], input2[16], output[16], output2[16];
  int j;

  sprintf(input, "%s", tile[i].input);
  sprintf(input2, "%s", tile[i].input2);
  sprintf(output, "%s", tile[i].output);
  sprintf(output2, "%s", tile[i].output2);

  fprintf(tile[i].fp, ".swtext\n");
  fprintf(tile[i].fp, ".global switch_begin\n");
  fprintf(tile[i].fp, "switch_begin:\n");
  // Startup Code
  if (i==0) {
    fprintf(tile[i].fp, "	# this word is sent to the serial rom to activate it.\n");
    fprintf(tile[i].fp, "	nop     \t	route $csto->$cWo\n");
    //   Mul inputs
    fprintf(tile[i].fp, "	nop	\t	route %s->$csti, %s->$swo2\n", input, input);
    for (j=0;j<3;j++) {
      fprintf(tile[i].fp, "	nop	\t	route %s->$csti, %s->$swo2, $swi2->$swo1\n", input, input); }
    fprintf(tile[i].fp, "	nop	\t	route $csto->%s\n", output);
    for (j=0;j<1;j++) { //CHANGE 3->1
      fprintf(tile[i].fp, "	nop	\t	route $swi1->%s\n", output); }
  }
  else if (i==3) {
    //   Mul inputs
    fprintf(tile[i].fp, "	nop	\t	route %s->$csti\n", input);
    for (j=0;j<3;j++) {
      fprintf(tile[i].fp, "	nop	\t	route %s->$csti\n", input); }
    //   Add inputs
    for (j=0;j<4;j++) {
      fprintf(tile[i].fp, "	nop	\t	route %s->$csti\n", input2); }
  }
  else { // Everybody except tiles 0 (input) and 3 (output)
    //   Mul inputs
    fprintf(tile[i].fp, "	nop	\t	route %s->$csti, %s->$swo2\n", input, input);
    for (j=0;j<3;j++) {
      fprintf(tile[i].fp, "	nop	\t	route %s->$csti, %s->$swo2, $swi2->$swo1\n", input, input); }
    //   Add inputs
    fprintf(tile[i].fp, "	nop	\t	route %s->$csti, $csto->%s\n", input2, output);
    for (j=0;j<1;j++) {//CHANGE 3->1
      fprintf(tile[i].fp, "	nop	\t	route %s->$csti, $swi1->%s\n", input2, output); }
    for (j=0;j<2;j++) {//CHANGE 1->2
      fprintf(tile[i].fp, "	nop	\t	route %s->$csti\n", input2); }
  }

  // Main Loop
  fprintf(tile[i].fp, "switch_loop:	\n");
  if (i==0) {
    //   Mul Inputs
    for (j=0;j<2;j++) {//CHANGE 1->2
      fprintf(tile[i].fp, "	nop	\t	route %s->$csti, %s->$swo2, $swi1->%s, $swi2->$swo1\n", input, input, output); }
    for (j=0;j<2;j++) {//CHANGE 4->2
      fprintf(tile[i].fp, "	nop	\t	route %s->$csti, %s->$swo2, $csto->%s, $swi2->$swo1\n", input, input, output2); }
    for (j=0;j<2;j++) {//CHANGE 1->2
      fprintf(tile[i].fp, "	nop	\t	route $csto->%s\n", output2); }
    for (j=0;j<1;j++) {//CHANGE 3->1
      fprintf(tile[i].fp, "	nop	\t	route $swi1->%s\n", output); }
    fprintf(tile[i].fp, "	j	switch_loop	route $swi1->%s\n", output); }
  else if (i==3) {
    //   Mul Inputs
    for (j=0;j<4;j++) {
      fprintf(tile[i].fp, "	nop	\t	route %s->$csti\n", input); }
    //   Add Inputs
    for (j=0;j<3;j++) {
      fprintf(tile[i].fp, "	nop	\t	route %s->$csti, $csto->%s\n", input2, output2); }
    fprintf(tile[i].fp, "	j	switch_loop	route %s->$csti, $csto->%s\n", input2, output2); }
  else { // Everybody except tiles 0 (input) and 3 (output)
    //   Mul inputs
    for (j=0;j<2;j++) {//CHANGE 1->2
      fprintf(tile[i].fp, "	nop	\t	route %s->$csti, %s->$swo2, $swi1->%s, $swi2->$swo1\n", input, input, output); }
    for (j=0;j<2;j++) {//CHANGE 4->2    
      fprintf(tile[i].fp, "	nop	\t	route %s->$csti, %s->$swo2, $csto->%s, $swi2->$swo1\n", input, input, output2); }
    //   Add inputs
    for (j=0;j<2;j++) {//CHANGE 1->2
      fprintf(tile[i].fp, "	nop	\t	route %s->$csti, $csto->%s\n", input2, output2); }
    for (j=0;j<1;j++) {//CHANGE 3->1
      fprintf(tile[i].fp, "	nop	\t	route %s->$csti, $swi1->%s\n", input2, output); }
    fprintf(tile[i].fp, "	j	switch_loop	route %s->$csti, $swi1->%s\n", input2, output); }
  fprintf(tile[i].fp, "\t\n");

  return 1;
}


void print_usage(int long_version) {
  if (long_version ==  TRUE) {
  printf("\n FIR Tile Assembly Code Generator. Version 1.0\n");
  printf(" Written by Nathan R. Shnidman (naters@mit.edu)\n");
  printf(" Copyright (c) 2003   All rights reserved\n");
  }
//   printf("\n  Usage: 802.11a [-i inputFile] [-o outputFile] ");
//   printf("[-x tranmitterOutputFile]\n\t\t[-x ReciverInputFile]");
//   printf("[-l length (in 2xbytes)] [-r data_rate]\n\n");
//   printf("\t\t-h         Prints extended help info.\n");
//   printf("\n");

  if (long_version == TRUE) {
    printf(" This program generates Raw assembly code which implements.\n");
    printf("   a 16-tap FIR fileter.\n\n");
  }
}
