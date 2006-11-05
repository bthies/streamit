//**********************************************************************
// final output implementation
//----------------------------------------------------------------------
//
//

package mkFinalOutput;

import H264Types::*;
import IFinalOutput::*;
import FIFO::*;

import Connectable::*;
import GetPut::*;

//-----------------------------------------------------------
// Final Output Module
//-----------------------------------------------------------

module mkFinalOutput( IFinalOutput );

   FIFO#(BufferControlOT)  infifo    <- mkFIFO;

   //-----------------------------------------------------------
   // Rules
   rule finalout (True);
      if(infifo.first() matches tagged Luma .xdata)
         begin
	    $display("ccl5finalout %h", xdata[7:0]);
	    $display("ccl5finalout %h", xdata[15:8]);
	    $display("ccl5finalout %h", xdata[23:16]);
	    $display("ccl5finalout %h", xdata[31:24]);
	    $display("ccl5finalout %h", xdata[39:32]);
	    $display("ccl5finalout %h", xdata[47:40]);
	    $display("ccl5finalout %h", xdata[55:48]);
	    $display("ccl5finalout %h", xdata[63:56]);
	    $display("ccl5finalout %h", xdata[71:64]);
	    $display("ccl5finalout %h", xdata[79:72]);
	    $display("ccl5finalout %h", xdata[87:80]);
	    $display("ccl5finalout %h", xdata[95:88]);
	    $display("ccl5finalout %h", xdata[103:96]);
	    $display("ccl5finalout %h", xdata[111:104]);
	    $display("ccl5finalout %h", xdata[119:112]);
	    $display("ccl5finalout %h", xdata[127:120]);
	    infifo.deq();
	 end
      else if(infifo.first() matches tagged Chroma .xdata)
	 begin
	    $display("ccl5finalout %h", xdata[7:0]);
	    $display("ccl5finalout %h", xdata[15:8]);
	    $display("ccl5finalout %h", xdata[23:16]);
	    $display("ccl5finalout %h", xdata[31:24]);
	    $display("ccl5finalout %h", xdata[39:32]);
	    $display("ccl5finalout %h", xdata[47:40]);
	    $display("ccl5finalout %h", xdata[55:48]);
	    $display("ccl5finalout %h", xdata[63:56]);
	    infifo.deq();
	 end
      else
	 $finish(0);
   endrule


   interface Put ioin  = fifoToPut(infifo);

endmodule

endpackage