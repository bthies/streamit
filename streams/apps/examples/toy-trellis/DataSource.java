/*
 * Data source for trellis encoder example
 */

import streamit.library.*;

class DataSource extends Filter
{
    int x;
    public void init() {
	output = new Channel (Character.TYPE, 1);    /* pushes 1 */
	this.x = 65; // start with letter A
    }
    public void work ()
    {
	System.out.println("pushing: " + (char)x);
	output.pushChar ((char)x);
	this.x = (this.x + 1) % 90; // end with letter Z
	if (this.x == 0) {
	    this.x = 65;
	}
    }
}


