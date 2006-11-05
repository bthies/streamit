//**********************************************************************
// H264 Test Bench
//----------------------------------------------------------------------
//
//

package mkTH;

import H264Types::*;
import IMemED::*;
import IInputGen::*;
import IBufferControl::*;
import IFinalOutput::*;
import IH264::*;
import mkMemED::*;
import mkInputGen::*;
import mkBufferControl::*;
import mkFinalOutput::*;
import mkH264::*;

import Connectable::*;
import GetPut::*;
import ClientServer::*;

(* synthesize *)
module mkTH( Empty );

   // Instantiate the modules

   IInputGen     inputgen    <- mkInputGen();
   IH264         h264        <- mkH264();
   IMemED#(TAdd#(PicWidthSz,1),20) memED          <- mkMemED();
   IMemED#(TAdd#(PicWidthSz,2),68) memP_intra     <- mkMemED();
   IMemED#(TAdd#(PicWidthSz,5),32) memD_data      <- mkMemED();
   IMemED#(PicWidthSz,13)          memD_parameter <- mkMemED();
   IBufferControl buffercontrol <- mkBufferControl();
   IFinalOutput   finaloutput   <- mkFinalOutput();

   // Cycle counter
   Reg#(Bit#(32)) cyclecount <- mkReg(0);

   rule countCycles ( True );
      if(cyclecount[9:0]==0) $display( "CCLCycleCount %0d", cyclecount );
      cyclecount <= cyclecount+1;
      if(cyclecount > 30000000)
	 begin
	    $display( "ERROR mkTH: time out" );
	    $finish(0);
	 end
   endrule
   
   // Internal connections
   
   mkConnection( inputgen.ioout, h264.ioin );
   mkConnection( h264.mem_clientED, memED.mem_server );
   mkConnection( h264.mem_clientP_intra, memP_intra.mem_server );
   mkConnection( h264.mem_clientD_data, memD_data.mem_server );
   mkConnection( h264.mem_clientD_parameter, memD_parameter.mem_server );
   mkConnection( h264.ioout, buffercontrol.ioin );
   mkConnection( buffercontrol.ioout, finaloutput.ioin );
   
endmodule

endpackage
