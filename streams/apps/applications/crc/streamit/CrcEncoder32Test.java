/**
 *   CRCEncoder32.java:
 *   CRC Encoder using a 32 bit
 *   Generator Polynomial
 *   NOTE:  Until finite input lengths are accepted by StreamIt,
 *          this will not completely work as expected.
 *   - J. Wong
 */

import streamit.library.*;
import java.lang.*;
import streamit.library.io.*;
import java.io.*;

/*
class EncoderInput
{

    //member variables!
    public static int mInputLength = 5;


    public int[] readFile() 
  {
    int[] input = new int[mInputLength];
    try
    {
	File f1 = new File("CrcEncoderInput1");
	java.io.FileReader fr = new java.io.FileReader(f1);
	BufferedReader br = new BufferedReader(fr);
	//DataInputStream data = new DataInputStream(new FileInputStream(f1));
	//read the sucker!
	//int[] input = new int[4];
	for(int i = 0; i < input.length; i++)
	    {
		String j = br.readLine();
		
		if (j.equals("1"))
		    {
			input[i] = 1;
		    }
		else
		    { 
			input[i] = 0;
		    }		
		
	    }
	br.close();
    }
    catch (IOException e)
    {
	System.out.println("Blah! IO error!");
    }

    return input;
  }//readFile

}//class EncoderInput
*/

class AdditionFilter extends Filter
{
    //inputs:
    int mInitFeedbackData;
    int mShiftRegisterInput;
    //outputs
    //output 1: mInitFeedbackData
    int mAdditionOutput;


    public void init()
    {
	input = new Channel(Integer.TYPE, 2);
	output = new Channel(Integer.TYPE, 2);
    }

    public void work()
    {
	mInitFeedbackData = input.popInt();
	mShiftRegisterInput = input.popInt();
	
	if(((mInitFeedbackData == 1) && (mShiftRegisterInput == 1)) || ((mInitFeedbackData == 0) && (mShiftRegisterInput == 0)))
	{
	    mAdditionOutput = 0;
	}
	else
	{
	    mAdditionOutput = 1;    
	} 
	output.pushInt(mInitFeedbackData);
	output.pushInt(mAdditionOutput);
    }

}//AdditionFilter

class InitialAdditionFilter extends Filter

{
    //inputs:
    int mInitFeedbackData;
    int mFileInput;
    //outputs
    //output 1: mInitFeedbackData
    int mAdditionOutput;


    public void init()
    {
	input = new Channel(Integer.TYPE, 2);
	output = new Channel(Integer.TYPE, 2);
    }

    public void work()
    {
	mFileInput = input.popInt();
	mInitFeedbackData = input.popInt();
	//System.err.println("File Input is " + mFileInput + " Feedback data is " + mInitFeedbackData);
	if(((mInitFeedbackData == 1) && (mFileInput == 1)) || ((mInitFeedbackData == 0) && (mFileInput == 0)))
	{
	    mAdditionOutput = 0;
	}
	else
	{
	    mAdditionOutput = 1;    
	} 
	output.pushInt(mAdditionOutput);
	output.pushInt(mAdditionOutput);
    }

}//InitialAdditionFilter

class ZeroAdditionFilter extends Filter
{
    //inputs:
    int mInitFeedbackData;
    int mShiftRegisterInput;
    //outputs
    //output 1: mInitFeedbackData
    int mAdditionOutput;


    public void init()
    {
	input = new Channel(Integer.TYPE, 2);
	output = new Channel(Integer.TYPE, 2);
    }

    public void work()
    {
	mInitFeedbackData = input.popInt();
	mShiftRegisterInput = input.popInt();
	
	 
	output.pushInt(mInitFeedbackData);
	output.pushInt(mShiftRegisterInput);
    }

}//ZeroAdditionFilter  //i.e. the identity filter
    
class ShiftRegisterFilter extends Filter
{
    
    //private register variable:
    int mRegisterNumber;
    int mRegisterContents;
    //inputs:
    int mInitFeedbackData;
    int mDataStream;
    //output:
    int mRegisterOutput;
    int mTerminationCounter;
    
    public ShiftRegisterFilter(int number)
    {
        super(number);
    }
    public void init(int number)
    {
	mRegisterNumber = number;
	mTerminationCounter = 0;
	input = new Channel(Integer.TYPE, 2);
	output = new Channel(Integer.TYPE, 2);
	mRegisterContents = 0; //initial value
    }

    public void work()
    {	
	mInitFeedbackData = input.popInt();
	mDataStream = input.popInt();
	mRegisterOutput = mRegisterContents;   //move datastream value into Shift Register
	mRegisterContents = mDataStream;       //move original contents to output
	mTerminationCounter++;
	/*
	if(mTerminationCounter < EncoderInput.mInputLength + 1)
	{
	    System.err.println("Shift Register Contents of Filter " + mRegisterNumber + " at iteration " + mTerminationCounter + " is " + mRegisterContents);
	    System.err.println("Shift Register Output of Filter " + mRegisterNumber + " at iteration " + mTerminationCounter + " is " + mRegisterOutput);
	    
	}
	*/
	output.pushInt(mInitFeedbackData);
	output.pushInt(mRegisterOutput);
    }
}//ShiftRegisterFilter

class CrcInputFilter extends Filter
{
    // int[] mdata;
    boolean counter;
    // EncoderInput mencodefile;

    public void init()
    {
	counter = false;
	// mencodefile = new EncoderInput();
	// mdata = new int[mencodefile.mInputLength];
	output = new Channel(Integer.TYPE, 1);
    }

    public void work()
    {
	if(!counter)
	{
	    counter = true;
	    output.pushInt(0);
	}
	else
	{
	    output.pushInt(1);
	}     
    }
	//mdata = mencodefile.readFile();
	   //for (int i = 0; i < mencodefile.mInputLength; i++)
	   //{
	    //System.errprintln("inputting " + mdata[i]);
    //output.pushInt(mdata[i]);
       //}
    

}//CrcInputFilter

class FeedbackEndFilter extends Filter
{
    int mRegisterOutput;
    int mInitFeedbackData;

    public void init()
    {
	input = new Channel(Integer.TYPE, 2);
	output = new Channel(Integer.TYPE, 1);
    }
    
    public void work()
    {
	mInitFeedbackData = input.popInt();
	mRegisterOutput = input.popInt();
	//System.err.println("Shift Register 3 is " + mRegisterOutput);
	output.pushInt(mRegisterOutput);
    }
}//FeedbackEndFilter

class IntPrinter extends Filter {
    public void init()
    {
	input = new Channel(Integer.TYPE, 1);
    }
    
    public void work()
    {
	System.out.println(input.popInt());
    }
}

class CrcFeedbackLoop extends FeedbackLoop
{
    public void init()
    {
	this.setDelay(1);
	this.setJoiner(WEIGHTED_ROUND_ROBIN (1, 1));  //sequence: inputdata, fbdata
	this.setBody(new FeedbackBodyStream());
	this.setSplitter(DUPLICATE ());
	this.setLoop(new Identity(Integer.TYPE));
    }

    public int initPathInt(int index)
    {
	return 0;
    }
}//CrcFeedbackLoop

class FeedbackBodyStream extends Pipeline
{
    public void init()
    {
	this.add(new InitialAdditionFilter());
	this.add(new ShiftRegisterFilter(1));
	this.add(new AdditionFilter());
	this.add(new ShiftRegisterFilter(2));
	this.add(new AdditionFilter()); //x^2
	this.add(new ShiftRegisterFilter(3));
	//x^3
	this.add(new ShiftRegisterFilter(4));
	this.add(new AdditionFilter()); //x^4
	this.add(new ShiftRegisterFilter(5));
	this.add(new AdditionFilter()); //x^5
	this.add(new ShiftRegisterFilter(6));
	//x^6
	this.add(new ShiftRegisterFilter(7));
	this.add(new AdditionFilter()); //x^7
	this.add(new ShiftRegisterFilter(8));
	this.add(new AdditionFilter()); //x^8
	this.add(new ShiftRegisterFilter(9));
	//x^9
	this.add(new ShiftRegisterFilter(10));
	this.add(new AdditionFilter()); //x^10
	this.add(new ShiftRegisterFilter(11));
	this.add(new AdditionFilter()); //x^11
	this.add(new ShiftRegisterFilter(12));
	this.add(new AdditionFilter()); //x^12
	this.add(new ShiftRegisterFilter(13));
	//x^13
	this.add(new ShiftRegisterFilter(14));
	//x^14
	this.add(new ShiftRegisterFilter(15));
	//x^15
	this.add(new ShiftRegisterFilter(16));
	this.add(new AdditionFilter()); //x^16
	this.add(new ShiftRegisterFilter(17));
	//x^17
	this.add(new ShiftRegisterFilter(18));
	//x^18
	this.add(new ShiftRegisterFilter(19));
	//x^19
	this.add(new ShiftRegisterFilter(20));
	//x^20
	this.add(new ShiftRegisterFilter(21));
	//x^21
	this.add(new ShiftRegisterFilter(22));
	this.add(new AdditionFilter()); //x^22
	this.add(new ShiftRegisterFilter(23));
	this.add(new AdditionFilter()); //x^23
	this.add(new ShiftRegisterFilter(24));
	//x^24
	this.add(new ShiftRegisterFilter(25));
	//x^25
	this.add(new ShiftRegisterFilter(26));
	this.add(new AdditionFilter()); //x^26
	this.add(new ShiftRegisterFilter(27));
	//x^27
	this.add(new ShiftRegisterFilter(28));
	//x^28
	this.add(new ShiftRegisterFilter(29));
	//x^29
	this.add(new ShiftRegisterFilter(30));
	//x^30
	this.add(new ShiftRegisterFilter(31));
	//x^31
	this.add(new Identity(Integer.TYPE)); 

	//this.add(new ShiftRegisterFilter(3));
	this.add(new FeedbackEndFilter());
    }
   
}//FeedbackBodyStream

public class CrcEncoder32Test extends StreamIt
{
    public static void main(String args[])
    {
	new CrcEncoder32Test().run(args);
    }

    public void init()
    {
	this.add(new CrcInputFilter());
	this.add(new CrcFeedbackLoop());
	this.add(new IntPrinter());
    }
}//class CrcEncoder32Test





