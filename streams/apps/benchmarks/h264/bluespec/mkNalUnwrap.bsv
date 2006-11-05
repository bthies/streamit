//**********************************************************************
// NAL unit unwrapper implementation
//----------------------------------------------------------------------
//
//

package mkNalUnwrap;

import H264Types::*;
import INalUnwrap::*;
import FIFO::*;

import Connectable::*;
import GetPut::*;



//-----------------------------------------------------------
// NAL Unwrapper Module
//-----------------------------------------------------------

module mkNalUnwrap( INalUnwrap );

   FIFO#(InputGenOT)  infifo    <- mkFIFO;
   FIFO#(NalUnwrapOT) outfifo   <- mkFIFO;
   Reg#(Bit#(8))      buffera   <- mkReg(0);
   Reg#(Bit#(8))      bufferb   <- mkReg(0);
   Reg#(Bit#(8))      bufferc   <- mkReg(0);
   Reg#(Bit#(2))      bufcount  <- mkReg(0);
   Reg#(Bit#(27))     zerocount <- mkReg(0);

   
   //-----------------------------------------------------------
   // Rules
   rule fillbuffer (bufcount<3
		    &&& infifo.first() matches tagged DataByte .dbyte);
      bufferc  <= bufferb;
      bufferb  <= buffera;
      buffera  <= dbyte;
      bufcount <= bufcount+1;
      infifo.deq();
   endrule

   rule newnalunit (bufcount==3
		    &&& infifo.first() matches tagged DataByte .dbyte
		    &&& ((bufferc==0 && bufferb==0 && buffera==1)
			 || (bufferc==0 && bufferb==0 && buffera==0 && dbyte==1)));
      zerocount <= 0;
      if(bufferc==0 && bufferb==0 && buffera==1)
	 bufcount <= 0;
      else
	 begin
	    bufcount <= 0;
	    infifo.deq();
	 end
      outfifo.enq(NewUnit);
      $display("ccl1newunit");
   endrule

   rule remove3byte (bufcount==3
		     &&& infifo.first() matches tagged DataByte .dbyte
		     &&& (bufferc==0 && bufferb==0 && buffera==3 && dbyte<4));
      zerocount <= zerocount+2;
      bufcount  <= 0;
   endrule

   rule normalop (bufcount==3
		  &&& infifo.first() matches tagged DataByte .dbyte
		  &&& !(bufferc==0 && bufferb==0 && buffera==3 && dbyte<4)
		  &&& !((bufferc==0 && bufferb==0 && buffera==1)
			|| (bufferc==0 && bufferb==0 && buffera==0 && dbyte==1)));
      if(bufferc==0)
	 begin
	    zerocount <= zerocount+1;
	    bufferc  <= bufferb;
	    bufferb  <= buffera;
	    buffera  <= dbyte;
	    infifo.deq();
	 end
      else if(zerocount==0)
	 begin
	    outfifo.enq(RbspByte bufferc);
	    $display("ccl1rbspbyte %h", bufferc);
	    bufferc  <= bufferb;
	    bufferb  <= buffera;
	    buffera  <= dbyte;
	    infifo.deq();
	 end
      else
	 begin
	    zerocount <= zerocount-1;
	    outfifo.enq(RbspByte 0);
	    $display("ccl1rbspbyte 00");
	 end  
   endrule

   rule endfileop(infifo.first() matches tagged EndOfFile);
      case ( bufcount )
	 3:
	 begin
	    if(bufferc==0 && bufferb==0 && buffera<4)
	       begin
		  bufcount  <= 0;
		  zerocount <= 0;
	       end
	    else if(zerocount==0)
	       begin
		  bufcount <= 2;
		  outfifo.enq(RbspByte bufferc);
		  $display("ccl1rbspbyte %h", bufferc);
	       end
	    else
	       begin
		  zerocount <= zerocount-1;
		  outfifo.enq(RbspByte 0);
		  $display("ccl1rbspbyte 00");
	       end
	 end
	 2:
	 begin
	    bufcount  <= 1;
	    if(!(bufferb==0 && buffera==0))
	       outfifo.enq(RbspByte bufferb);
	       $display("ccl1rbspbyte %h", bufferb);
	 end
	 1:
	 begin
	    bufcount  <= 0;
	    if(!(buffera==0))
	       outfifo.enq(RbspByte buffera);
	       $display("ccl1rbspbyte %h", buffera);
	 end
	 0:
	 begin
	    infifo.deq();
	    outfifo.enq(EndOfFile);
	    $display("EndOfFile reached (NalUnwrap)");
	 end
      endcase
	 
   endrule

   
   interface Put ioin  = fifoToPut(infifo);
   interface Get ioout = fifoToGet(outfifo);
      
endmodule

endpackage
