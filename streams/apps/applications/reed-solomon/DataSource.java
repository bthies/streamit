/*
 * Data source for Reed-Solomon encoder/decoder example.
 */
import streamit.library.*;

class DataSource extends Filter
{
    public static final int MESSAGE_SIZE = 66;
    public static final int ENCODED_SIZE = DataSource.MESSAGE_SIZE + ReedSolomonLibrary.NPAR;
    public char[] message;  
    public void init() {
	int i;
	char temp[] = ("Nervously I loaded the twin ducks aboard the revolving platform.\n").toCharArray();

	this.message = new char[MESSAGE_SIZE];

	for (i=0; i<MESSAGE_SIZE-1; i++) {
	    this.message[i] = temp[i];
	}
	// append trailing 0;
	this.message[MESSAGE_SIZE-1] = (char)0;
	    
	output = new Channel (Character.TYPE, MESSAGE_SIZE);    /* pushes entire message */
    }
    
    public void work ()
    {
	int i;
	for (i=0; i<this.MESSAGE_SIZE;i++) {
	    output.pushChar (this.message[i]);
	}
    }
}
