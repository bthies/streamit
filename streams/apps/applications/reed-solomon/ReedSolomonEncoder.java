/**
 * Sample ReedSolomon encoder. Basically just a streamit
 * wrapper for the ReedSolomonLibrary class which contains the code to
 * do all of the heavy lifting (eg the stuff I don't understand)
 *
 * Andrew Lamb aalamb@mit.edu 6/17/2002
 **/
import streamit.library.*;

class ReedSolomonEncoder extends Filter {

    ReedSolomonLibrary lib;
    public void init() {
	lib = new ReedSolomonLibrary();
	input  = new Channel(Character.TYPE, DataSource.MESSAGE_SIZE);
	output = new Channel(Character.TYPE, DataSource.ENCODED_SIZE);
    }
    public void work() {
	int i;
	char codeword[] = new char[255]; // space for the encoded message
	char msg[] = new char[DataSource.MESSAGE_SIZE];

	/** pop the data off of the input tape **/
	for (i=0; i<DataSource.MESSAGE_SIZE; i++) {
	    msg[i] = input.popChar();
	}
	
	/** Encode the data, ending up with NPAR parity bytes **/
	lib.encode_data(msg, msg.length, codeword);

	// print out the parity data to verify that the java version
	// does the same thing as the c version
	for (i=0; i<ReedSolomonLibrary.NPAR; i++) {
	    //System.out.println("Parity Byte " + i +
	    //	       "=" + (int)codeword[DataSource.MESSAGE_SIZE + 1 + i]);
	}

	/** push out the encoded message onto the tape **/
	for (i=0; i<DataSource.ENCODED_SIZE; i++) {
	    output.pushChar(codeword[i]);
	}
    }
}
    

    
