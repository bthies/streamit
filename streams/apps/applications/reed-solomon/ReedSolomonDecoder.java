/**
 * Sample ReedSolomon dencoder. Basically just a streamit
 * wrapper for the ReedSolomonLibrary class which contains the code to
 * do all of the heavy lifting (eg the stuff I don't understand)
 *
 * Andrew Lamb aalamb@mit.edu 6/17/2002
 **/
import streamit.library.*;

class ReedSolomonDecoder extends Filter {

    ReedSolomonLibrary lib;
    public void init() {
	lib    = new ReedSolomonLibrary();
	input  = new Channel(Character.TYPE, DataSource.ENCODED_SIZE);
	output = new Channel(Character.TYPE, DataSource.MESSAGE_SIZE);
    }
    public void work() {
	int i;
	char codeword[] = new char[255]; // space for the coded

	int erasures[] = new int[16];
	int nerasures = 0;

	
	/** pop the (encoded) data codeword off of the input tape **/
	for (i=0; i<DataSource.ENCODED_SIZE; i++) {
	    codeword[i] = input.popChar();
	}
	
	/** Decode the data, ending up with NPAR parity bytes **/
	lib.decode_data(codeword, DataSource.ENCODED_SIZE);

	/* check if syndrome is all zeros */
	if (lib.check_syndrome () != 0) {
	    lib.correct_errors_erasures (codeword, 
					 DataSource.ENCODED_SIZE,
					 nerasures, 
					 erasures);
	    
	    System.out.println("(Detected errors)");
	}
	
	/** push out the decoded message **/
	for (i=0; i<DataSource.MESSAGE_SIZE; i++) {
	    output.pushChar(codeword[i]);
	}
    }
}
    

    
