/*
 * Data source that outputs the example data string from
 * http://pw1.netcom.com/~chip.f/viterbi/algrthms.html
 */

import streamit.library.*;

class ExampleSource extends Filter
{
    int SIZE = 17;
    int currentIndex;
    int[] data = {0,1,0,1,1,1,0,0,1,0,1,0,0,0,1,0,0};

    public void init() {
	output = new Channel (Integer.TYPE, 1);    /* pushes 1 integer per cycle */
    }
    public void work ()
    {
	output.pushInt(this.data[this.currentIndex]);
	currentIndex = (currentIndex + 1) % SIZE;
    }
}




