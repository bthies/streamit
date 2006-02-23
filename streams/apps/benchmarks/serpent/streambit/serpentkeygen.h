#ifndef KEYSCHED_OUTPUT_H
#define KEYSCHED_OUTPUT_H

void KG_INIT(FilterState * local_state);


/*   KeySched filter: Input Rate = 256
      Output Rate = 4224     */
extern int KG_OUTPUT_RATE; 
extern int KG_INPUT_RATE; 


void KeySched(unsigned *in_Input_0_0  , unsigned *out_Output_0_1  , FilterState * local_state);


#endif 
