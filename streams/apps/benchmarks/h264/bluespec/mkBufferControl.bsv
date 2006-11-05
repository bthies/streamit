//**********************************************************************
// Buffer Controller
//----------------------------------------------------------------------
//
//

package mkBufferControl;

import RegFile::*;
import H264Types::*;

import IBufferControl::*;
import FIFO::*;
import Vector::*;

import Connectable::*;
import GetPut::*;




//-----------------------------------------------------------
// Local Datatypes
//-----------------------------------------------------------

typedef union tagged                
{
 void     Idle;          //not working on anything in particular
 void     Y;
 void     U;
 void     V;
}
Outprocess deriving(Eq,Bits);


//-----------------------------------------------------------
// Helper functions



//-----------------------------------------------------------
// Buffer Controller  Module
//-----------------------------------------------------------


(* synthesize *)
module mkBufferControl( IBufferControl );

   FIFO#(DeblockFilterOT) infifo  <- mkFIFO();
   FIFO#(BufferControlOT) outfifo <- mkFIFO();

   Reg#(Bool) noMoreInput <- mkReg(False);
   Reg#(Bool) input1      <- mkReg(False);
   Reg#(Bool) inputdone   <- mkReg(False);
   Reg#(Outprocess) outprocess <- mkReg(Idle);

   RegFile#(Bit#(TAdd#(PicAreaSz,4)),Bit#(128)) yrfile0 <- mkRegFileFull();
   RegFile#(Bit#(TAdd#(PicAreaSz,4)),Bit#(128)) yrfile1 <- mkRegFileFull();
   RegFile#(Bit#(TAdd#(PicAreaSz,4)),Bit#(64)) uvrfile0 <- mkRegFileFull();
   RegFile#(Bit#(TAdd#(PicAreaSz,4)),Bit#(64)) uvrfile1 <- mkRegFileFull();
   
   Reg#(Bit#(PicWidthSz))  picWidth  <- mkReg(maxPicWidthInMB);
   Reg#(Bit#(PicHeightSz)) picHeight <- mkReg(0);

   Reg#(Bit#(TAdd#(PicAreaSz,6))) inlumacount <- mkReg(0);
   Reg#(Bit#(TAdd#(PicAreaSz,5))) inchromacount <- mkReg(0);
   Reg#(Bit#(PicAreaSz)) frameinmb <- mkReg(0);
   Reg#(Bit#(96)) tempinput <- mkReg(0);

   Reg#(Bit#(PicAreaSz)) outBase <- mkReg(0);
   Reg#(Bit#(PicWidthSz)) outOffset <- mkReg(0);
   Reg#(Bit#(4)) outLine <- mkReg(0);



   //-----------------------------------------------------------
   // Rules
   
   rule inputing ( !noMoreInput && !inputdone );
      case (infifo.first()) matches
	 tagged PicWidth .xdata :
	    begin
	       infifo.deq();
	       picWidth <= xdata;
	    end
	 tagged PicHeight .xdata :
	    begin
	       infifo.deq();
	       picHeight <= xdata;
	       frameinmb <= zeroExtend(picWidth)*zeroExtend(xdata);
	    end
	 tagged Luma .xdata :
	    begin
	       infifo.deq();
	       inlumacount <= inlumacount+1;
	       //$display( "TRACE Buffer Control: input Luma %0d %h %h", xdata.mb, xdata.pixel, xdata.data);
	       case(inlumacount[1:0])
		  0: tempinput <= {64'b0,xdata.data};
		  1: tempinput <= {32'b0,xdata.data,tempinput[31:0]};
		  2: tempinput <= {xdata.data,tempinput[63:0]};
		  3:
		  begin
		     Bit#(128) savedata = {xdata.data,tempinput};
		     Bit#(TAdd#(PicAreaSz,4)) addr = {xdata.mb,xdata.pixel[5:2]};
		     if(input1)
			yrfile1.upd(addr,savedata);
		     else
			yrfile0.upd(addr,savedata);
		  end
	       endcase
	    end
	 tagged Chroma .xdata :
	    begin
	       infifo.deq();
	       inchromacount <= inchromacount+1;
	       //$display( "TRACE Buffer Control: input Chroma %0d %0d %h %h", xdata.uv, xdata.mb, xdata.pixel, xdata.data);
	       if(inchromacount[0] == 0)
		  tempinput <= {64'b0,xdata.data};
	       else
		  begin
		     Bit#(64) savedata = {xdata.data,tempinput[31:0]};
		     Bit#(TAdd#(PicAreaSz,4)) addr = {xdata.uv,xdata.mb,xdata.pixel[3:1]};
		     if(input1)
			uvrfile1.upd(addr,savedata);
		     else
			uvrfile0.upd(addr,savedata);
		  end
	    end
	 tagged EndOfFrame :
	    begin
	       infifo.deq();
	       $display( "INFO Buffer Control: EndOfFrame reached");
	       inputdone <= True;
	    end
	 tagged EndOfFile :
	    begin
	       infifo.deq();
	       $display( "INFO Buffer Control: EndOfFile reached");
	       noMoreInput <= True;
	       //$finish(0);
	       //outfifo.enq(EndOfFile); 
	    end
	 default: infifo.deq();
      endcase
   endrule


   rule outputing ( outprocess != Idle );
      if(outprocess==Y)
	 begin
	    Bit#(TAdd#(PicAreaSz,4)) addr = {outBase+zeroExtend(outOffset),outLine};
	    Bit#(128) outdata;
	    if(input1)
	       outdata = yrfile0.sub(addr);
	    else
	       outdata = yrfile1.sub(addr);
	    outfifo.enq(Luma outdata);
	    if(outOffset==picWidth-1)
	       begin
		  outOffset <= 0;
		  if(outLine==15)
		     begin
			outLine <= 0;
			if(outBase+zeroExtend(picWidth)==frameinmb)
			   begin
			      outBase <= 0;
			      outprocess <= U;
			   end
			else
			   outBase <= outBase+zeroExtend(picWidth);
		     end
		  else
		     outLine <= outLine+1;
	       end
	    else
	       outOffset <= outOffset+1;
	 end
      else
	 begin
	    Bit#(TAdd#(PicAreaSz,4)) addr;
	    Bit#(64) outdata;
	    if(outprocess==U)
	       addr = {1'b0,outBase+zeroExtend(outOffset),outLine[2:0]};
	    else
	       addr = {1'b1,outBase+zeroExtend(outOffset),outLine[2:0]};
	    if(input1)
	       outdata = uvrfile0.sub(addr);
	    else
	       outdata = uvrfile1.sub(addr);
	    outfifo.enq(Chroma outdata);
	    if(outOffset==picWidth-1)
	       begin
		  outOffset <= 0;
		  if(outLine==7)
		     begin
			outLine <= 0;
			if(outBase+zeroExtend(picWidth)==frameinmb)
			   begin
			      outBase <= 0;
			      if(outprocess==U)
				 outprocess <= V;
			      else
				 outprocess <= Idle;
			   end
			else
			   outBase <= outBase+zeroExtend(picWidth);
		     end
		  else
		     outLine <= outLine+1;
	       end
	    else
	       outOffset <= outOffset+1;
	 end
   endrule


   rule switching ( outprocess==Idle && (noMoreInput || inputdone));
      if(noMoreInput)
	 outfifo.enq(EndOfFile);
      else
	 begin
	    inputdone <= False;
	    outprocess <= Y;
	    input1 <= !input1;
	 end
   endrule

   
   

   interface Put ioin  = fifoToPut(infifo);
   interface Get ioout = fifoToGet(outfifo);
      
endmodule

endpackage
