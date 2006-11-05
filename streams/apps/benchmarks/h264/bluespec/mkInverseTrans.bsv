//**********************************************************************
// Inverse Quantizer and Inverse Transformer implementation
//----------------------------------------------------------------------
//
//

package mkInverseTrans;

import H264Types::*;

import IInverseTrans::*;
import FIFO::*;
import Vector::*;

import Connectable::*;
import GetPut::*;
import ClientServer::*;


//-----------------------------------------------------------
// Local Datatypes
//-----------------------------------------------------------

typedef union tagged                
{
 void     Start;            //not working on anything in particular
 void     Intra16x16DC;
 void     Intra16x16;
 void     ChromaDC;
 void     Chroma;
 void     Regular4x4;
}
State deriving(Eq,Bits);

typedef union tagged                
{
// void     Initializing;     //not working on anything in particular		  
 void     Passing;          //not working on anything in particular
 void     LoadingDC;
 void     Scaling;          //does not include scaling for DC (just loading in that case)
 void     Transforming;
 void     ScalingDC;
 void     Outputing;
}
Process deriving(Eq,Bits);

      
//-----------------------------------------------------------
// Helper functions

function Bit#(6) qpi_to_qpc( Bit#(6) qpi );//mapping from qpi to qpc
   case ( qpi )
      30: return 29;
      31: return 30;
      32: return 31;
      33: return 32;
      34: return 32;
      35: return 33;
      36: return 34;
      37: return 34;
      38: return 35;
      39: return 35;
      40: return 36;
      41: return 36;
      42: return 37;
      43: return 37;
      44: return 37;
      45: return 38;
      46: return 38;
      47: return 38;
      48: return 39;
      49: return 39;
      50: return 39;
      51: return 39;
      default: return qpi;
   endcase
endfunction


//function Bit#(4) inverseZigZagScan( Bit#(4) idx );
//   case ( idx )
//      0: return 0;
//      1: return 1;
//      2: return 4;
//      3: return 8;
//      4: return 5;
//      5: return 2;
//      6: return 3;
//      7: return 6;
//      8: return 9;
//      9: return 12;
//      10: return 13;
//      11: return 10;
//      12: return 7;
//      13: return 11;
//      14: return 14;
//      15: return 15;
//   endcase
//endfunction


function Bit#(4) reverseInverseZigZagScan( Bit#(4) idx );
   case ( idx )
      0: return 15;
      1: return 14;
      2: return 11;
      3: return 7;
      4: return 10;
      5: return 13;
      6: return 12;
      7: return 9;
      8: return 6;
      9: return 3;
      10: return 2;
      11: return 5;
      12: return 8;
      13: return 4;
      14: return 1;
      15: return 0;
   endcase
endfunction


//-----------------------------------------------------------
// Inverse Quantizer and Inverse Transformer Module
//-----------------------------------------------------------


(* synthesize *)
module mkInverseTrans( IInverseTrans );

   FIFO#(EntropyDecOT) infifo      <- mkFIFO;
   FIFO#(EntropyDecOT) outfifo     <- mkFIFO;
   Reg#(Bit#(4))       blockNum    <- mkReg(0);
   Reg#(Bit#(4))       pixelNum    <- mkReg(0);//also used as a regular counter during inverse transformation
   Reg#(State)         state       <- mkReg(Start);
   Reg#(Process)       process     <- mkReg(Passing);

   Reg#(Bit#(5))        chroma_qp_index_offset <- mkReg(0);
   Reg#(Bit#(6))        ppspic_init_qp   <- mkReg(0);
   Reg#(Bit#(6))        slice_qp         <- mkReg(0);
   Reg#(Bit#(6))        qpy              <- mkReg(0);//Calculating it requires 8 bits, but value only 0 to 51
   Reg#(Bit#(6))        qpc              <- mkReg(0);
   Reg#(Bit#(3))        qpymod6          <- mkReg(0);
   Reg#(Bit#(3))        qpcmod6          <- mkReg(0);
   Reg#(Bit#(4))        qpydiv6          <- mkReg(0);
   Reg#(Bit#(4))        qpcdiv6          <- mkReg(0);

   Reg#(Vector#(16,Bit#(16))) workVector       <- mkRegU();
   Reg#(Vector#(16,Bit#(16))) storeVector      <- mkRegU();

   

   //-----------------------------------------------------------
   // Rules

   
   rule passing (process matches Passing);
      case (infifo.first()) matches
	 tagged NewUnit . xdata :
	    begin
	       infifo.deq();
	       outfifo.enq(infifo.first());
	       $display("ccl3newunit");
	       $display("ccl3rbspbyte %h", xdata);
	    end
	 tagged SDMmbtype .xdata :
	    begin
	       infifo.deq();
	       outfifo.enq(infifo.first());
	       $display( "INFO InverseTrans: SDMmbtype %0d", xdata);
	       if(mbPartPredMode(xdata,0) == Intra_16x16)
		  state <= Intra16x16DC;
	       else
		  state <= Regular4x4;
	    end
	 tagged PPSpic_init_qp .xdata :
	    begin
	       infifo.deq();
	       ////outfifo.enq(infifo.first());
	       ppspic_init_qp <= truncate(xdata);
	    end
	 tagged SHslice_qp_delta .xdata :
	    begin
	       infifo.deq();
	       ////outfifo.enq(infifo.first());
	       slice_qp <= ppspic_init_qp+truncate(xdata);
	       qpy <= ppspic_init_qp+truncate(xdata);
	    end
	 tagged SDMmb_qp_delta .xdata :
	    begin
	       infifo.deq();
	       ////outfifo.enq(infifo.first());
	       Bit#(8) qpytemp = zeroExtend(qpy) + zeroExtend(xdata+52);
	       Bit#(6) qpynext;
	       if(qpytemp >= 104)
		  qpynext = truncate(qpytemp - 104);
	       else if(qpytemp >= 52)
		  qpynext = truncate(qpytemp - 52);
	       else
		  qpynext = truncate(qpytemp);
	       qpy <= qpynext;
	       
	       //$display( "TRACE InverseTrans: qpy %0d", qpynext );

	       qpydiv6 <= truncate(qpynext/6);
	       qpymod6 <= truncate(qpynext%6);

	       Bit#(7) qpitemp = zeroExtend(chroma_qp_index_offset+12) + zeroExtend(qpynext);
	       Bit#(6) qpi;
	       if(qpitemp < 12)
		  qpi = 0;
	       else if(qpitemp > 63)
		  qpi = 51;
	       else
		  qpi = truncate(qpitemp-12);
	       qpc <= qpi_to_qpc(qpi);
	       outfifo.enq(IBTmb_qp {qpy:qpynext,qpc:qpi_to_qpc(qpi)});
	    end
	 tagged PPSchroma_qp_index_offset .xdata :
	    begin
	       infifo.deq();
	       ////outfifo.enq(infifo.first());
	       chroma_qp_index_offset <= xdata;
	    end
	 tagged SDMRcoeffLevel .xdata :
	    begin
	       blockNum <= 0;
	       pixelNum <= 0;
	       if(state == Intra16x16DC)
		  begin
		     $display( "INFO InverseTrans: 16x16 MB" );
		     process <= LoadingDC;
		  end
	       else
		  begin
		     $display( "INFO InverseTrans: Non-16x16 MB" );
		     process <= Scaling;
		  end
	       workVector <= replicate(0);
	       qpcdiv6 <= truncate(qpc/6);
	       qpcmod6 <= truncate(qpc%6);
	    end
	 tagged SDMRcoeffLevelZeros .xdata :
	    begin
	       blockNum <= 0;
	       pixelNum <= 0;
	       if(state == Intra16x16DC)
		  begin
		     $display( "INFO InverseTrans: 16x16 MB" );
		     process <= LoadingDC;
		  end
	       else
		  begin
		     $display( "INFO InverseTrans: Non-16x16 MB" );
		     process <= Scaling;
		  end
	       workVector <= replicate(0);
	       qpcdiv6 <= truncate(qpc/6);
	       qpcmod6 <= truncate(qpc%6);
	    end
	 tagged EndOfFile :
	    begin
	       infifo.deq();
	       outfifo.enq(infifo.first());
	       $display( "INFO InverseTrans: EndOfFile reached" );
	       //$finish(0);///////////////////////////////
	    end
	 default:
	    begin
	       infifo.deq();
	       outfifo.enq(infifo.first());
	    end
      endcase
   endrule


   rule loadingDC (process matches LoadingDC);
      Vector#(16,Bit#(16)) workVectorTemp = workVector;

      case (infifo.first()) matches
	 tagged SDMRcoeffLevelZeros .xdata :
	    begin
	       infifo.deq();
	       pixelNum <= pixelNum+truncate(xdata);
	       if((state==ChromaDC && zeroExtend(pixelNum)+xdata==8) || zeroExtend(pixelNum)+xdata==16)
		  process <= Transforming;
	       else if((state==ChromaDC && zeroExtend(pixelNum)+xdata>8) || zeroExtend(pixelNum)+xdata>16)
		  $display( "ERROR InverseTrans: loadingDC index overflow" );
	    end
	 tagged SDMRcoeffLevel .xdata :
	    begin
	       infifo.deq();
	       Bit#(16) workValue = signExtend(xdata);
	       if(state==ChromaDC)
		  begin
		     if(pixelNum<4)
			workVector <= update(workVectorTemp, 3-pixelNum, workValue);
		     else
			workVector <= update(workVectorTemp, 11-pixelNum, workValue);
		  end
	       else
		  workVector <= update(workVectorTemp, reverseInverseZigZagScan(pixelNum), workValue);
	       pixelNum <= pixelNum+1;
	       if((state==ChromaDC && pixelNum==7) || pixelNum==15)
		  process <= Transforming;
	       else if((state==ChromaDC && pixelNum>7) || pixelNum>15)
		  $display( "ERROR InverseTrans: loadingDC index overflow" );
	    end
	 default: process <= Passing;
      endcase
   endrule

   
   rule scaling (process matches Scaling);
      Vector#(16,Bit#(16)) workVectorTemp = workVector;
      Vector#(16,Bit#(16)) storeVectorTemp = storeVector;

      case (infifo.first()) matches
	 tagged SDMRcoeffLevelZeros .xdata :
	    begin
	       infifo.deq();
	       if(zeroExtend(pixelNum)+xdata==16 || (zeroExtend(pixelNum)+xdata==15 && (state==Chroma || state==Intra16x16)))
		  begin
		     Bit#(16) prevValue0=0;
		     if(state==Intra16x16)
			prevValue0 = select(storeVectorTemp, {blockNum[3],blockNum[1],blockNum[2],blockNum[0]});
		     else if(state==Chroma)
			prevValue0 = select(storeVectorTemp, blockNum);
		     if(xdata==16 || (xdata==15 && (state==Chroma || state==Intra16x16) && prevValue0==0))
			begin
			   outfifo.enq(SDMRcoeffLevelZeros 16);
			   ////$display("ccl3IBTresidualZeros %0d", 16);
			   workVector <= replicate(0);
			   if(state==Chroma)
			      begin
				 if(blockNum<7)
				    blockNum <= blockNum+1;
				 else if (blockNum==7)
				    begin
				       blockNum <= 0;
				       process <= Passing;
				    end
				 else
				    $display( "ERROR InverseTrans: scaling outputing chroma unexpected blockNum" );
			      end
			   else
			      begin
				 blockNum <= blockNum+1;
				 if(blockNum==15)
				    begin
				       state <= ChromaDC;
				       process <= LoadingDC;
				    end
			      end
			end
		     else
			process <= Transforming;
		     pixelNum <= 0;
		  end
	       else if(zeroExtend(pixelNum)+xdata>16 || (zeroExtend(pixelNum)+xdata>15 && (state==Chroma || state==Intra16x16)))
		  $display( "ERROR InverseTrans: scaling index overflow" );
	       else
		  pixelNum <= pixelNum+truncate(xdata);
	       //$display( "TRACE InverseTrans: coeff zeros %0d", xdata );
	    end
	 tagged SDMRcoeffLevel .xdata :
	    begin
	       infifo.deq();
	       Bit#(6)  qp;
	       Bit#(4)  qpdiv6;
	       Bit#(3)  qpmod6;
	       if(state==Chroma)
		  begin
		     qp = qpc;
		     qpdiv6 = qpcdiv6;
		     qpmod6 = qpcmod6;
		  end
	       else
		  begin
		     qp = qpy;
		     qpdiv6 = qpydiv6;
		     qpmod6 = qpymod6;
		  end
	       Bit#(5) levelScaleValue=0;
	       if(pixelNum==15 || pixelNum==12 || pixelNum==10 || pixelNum==4)
		  begin
		     case(qpmod6)
			0: levelScaleValue = 10;
			1: levelScaleValue = 11;
			2: levelScaleValue = 13;
			3: levelScaleValue = 14;
			4: levelScaleValue = 16;
			5: levelScaleValue = 18;
			default: $display( "ERROR InverseTrans: levelScaleGen case default" );
		     endcase
		  end
	       else if(pixelNum==11 || pixelNum==5 || pixelNum==3 || pixelNum==0)
		  begin
		     case(qpmod6)
			0: levelScaleValue = 16;
			1: levelScaleValue = 18;
			2: levelScaleValue = 20;
			3: levelScaleValue = 23;
			4: levelScaleValue = 25;
			5: levelScaleValue = 29;
			default: $display( "ERROR InverseTrans: levelScaleGen case default" );
		     endcase
		  end
	       else
		  begin
		     case(qpmod6)
			0: levelScaleValue = 13;
			1: levelScaleValue = 14;
			2: levelScaleValue = 16;
			3: levelScaleValue = 18;
			4: levelScaleValue = 20;
			5: levelScaleValue = 23;
			default: $display( "ERROR InverseTrans: levelScaleGen case default" );
		     endcase
		  end
	       Bit#(16) workValueTemp = zeroExtend(levelScaleValue)*signExtend(xdata);
	       Bit#(16) workValue;
	       workValue = workValueTemp << zeroExtend(qpdiv6);
	       workVector <= update(workVectorTemp, reverseInverseZigZagScan(pixelNum), workValue);
	       if(pixelNum==15 || (pixelNum==14 && (state==Chroma || state==Intra16x16)))
		  begin
		     process <= Transforming;
		     pixelNum <= 0;
		  end
	       else
		  pixelNum <= pixelNum+1;
	    end
	 default: process <= Passing;
      endcase
   endrule


   rule transforming (process matches Transforming);
      Vector#(16,Bit#(16)) workVectorTemp = workVector;
      Vector#(16,Bit#(16)) workVectorNew = workVector;
      Vector#(16,Bit#(16)) storeVectorTemp = storeVector;

      if(state == ChromaDC)
	 begin
	    case ( pixelNum )
	       8:
	       begin
		  workVectorNew[0] = workVectorTemp[0] + workVectorTemp[2];
		  workVectorNew[1] = workVectorTemp[1] + workVectorTemp[3];
		  workVectorNew[2] = workVectorTemp[0] - workVectorTemp[2];
		  workVectorNew[3] = workVectorTemp[1] - workVectorTemp[3];
		  pixelNum <= pixelNum+1;
	       end
	       9:
	       begin
		  workVectorNew[0] = workVectorTemp[0] + workVectorTemp[1];
		  workVectorNew[1] = workVectorTemp[0] - workVectorTemp[1];
		  workVectorNew[2] = workVectorTemp[2] + workVectorTemp[3];
		  workVectorNew[3] = workVectorTemp[2] - workVectorTemp[3];
		  pixelNum <= pixelNum+1;
	       end
	       10:
	       begin
		  workVectorNew[4] = workVectorTemp[4] + workVectorTemp[6];
		  workVectorNew[5] = workVectorTemp[5] + workVectorTemp[7];
		  workVectorNew[6] = workVectorTemp[4] - workVectorTemp[6];
		  workVectorNew[7] = workVectorTemp[5] - workVectorTemp[7];
		  pixelNum <= pixelNum+1;
	       end
	       11:
	       begin
		  workVectorNew[4] = workVectorTemp[4] + workVectorTemp[5];
		  workVectorNew[5] = workVectorTemp[4] - workVectorTemp[5];
		  workVectorNew[6] = workVectorTemp[6] + workVectorTemp[7];
		  workVectorNew[7] = workVectorTemp[6] - workVectorTemp[7];
		  pixelNum <= 0;
		  process <= ScalingDC;
	       end
	       default:
	          $display( "ERROR InverseTrans: transforming ChromaDC unexpected pixelNum" );
	    endcase
	    workVector <= workVectorNew;
	 end
      else if(state == Intra16x16DC)
	 begin
	    case ( pixelNum )
	       0:
	       begin
		  workVectorNew[0]  = workVectorTemp[0] + workVectorTemp[4] + workVectorTemp[8] + workVectorTemp[12];
		  workVectorNew[4]  = workVectorTemp[0] + workVectorTemp[4] - workVectorTemp[8] - workVectorTemp[12];
		  workVectorNew[8]  = workVectorTemp[0] - workVectorTemp[4] - workVectorTemp[8] + workVectorTemp[12];
		  workVectorNew[12] = workVectorTemp[0] - workVectorTemp[4] + workVectorTemp[8] - workVectorTemp[12];
	       end
	       1:
	       begin
		  workVectorNew[1]  = workVectorTemp[1] + workVectorTemp[5] + workVectorTemp[9] + workVectorTemp[13];
		  workVectorNew[5]  = workVectorTemp[1] + workVectorTemp[5] - workVectorTemp[9] - workVectorTemp[13];
		  workVectorNew[9]  = workVectorTemp[1] - workVectorTemp[5] - workVectorTemp[9] + workVectorTemp[13];
		  workVectorNew[13] = workVectorTemp[1] - workVectorTemp[5] + workVectorTemp[9] - workVectorTemp[13];
	       end
	       2:
	       begin
		  workVectorNew[2]  = workVectorTemp[2] + workVectorTemp[6] + workVectorTemp[10] + workVectorTemp[14];
		  workVectorNew[6]  = workVectorTemp[2] + workVectorTemp[6] - workVectorTemp[10] - workVectorTemp[14];
		  workVectorNew[10] = workVectorTemp[2] - workVectorTemp[6] - workVectorTemp[10] + workVectorTemp[14];
		  workVectorNew[14] = workVectorTemp[2] - workVectorTemp[6] + workVectorTemp[10] - workVectorTemp[14];
	       end
	       3:
	       begin
		  workVectorNew[3]  = workVectorTemp[3] + workVectorTemp[7] + workVectorTemp[11] + workVectorTemp[15];
		  workVectorNew[7]  = workVectorTemp[3] + workVectorTemp[7] - workVectorTemp[11] - workVectorTemp[15];
		  workVectorNew[11] = workVectorTemp[3] - workVectorTemp[7] - workVectorTemp[11] + workVectorTemp[15];
		  workVectorNew[15] = workVectorTemp[3] - workVectorTemp[7] + workVectorTemp[11] - workVectorTemp[15];
	       end
	       4:
	       begin
		  workVectorNew[0] = workVectorTemp[0] + workVectorTemp[1] + workVectorTemp[2] + workVectorTemp[3];
		  workVectorNew[1] = workVectorTemp[0] + workVectorTemp[1] - workVectorTemp[2] - workVectorTemp[3];
		  workVectorNew[2] = workVectorTemp[0] - workVectorTemp[1] - workVectorTemp[2] + workVectorTemp[3];
		  workVectorNew[3] = workVectorTemp[0] - workVectorTemp[1] + workVectorTemp[2] - workVectorTemp[3];
	       end
	       5:
	       begin
		  workVectorNew[4] = workVectorTemp[4] + workVectorTemp[5] + workVectorTemp[6] + workVectorTemp[7];
		  workVectorNew[5] = workVectorTemp[4] + workVectorTemp[5] - workVectorTemp[6] - workVectorTemp[7];
		  workVectorNew[6] = workVectorTemp[4] - workVectorTemp[5] - workVectorTemp[6] + workVectorTemp[7];
		  workVectorNew[7] = workVectorTemp[4] - workVectorTemp[5] + workVectorTemp[6] - workVectorTemp[7];
	       end
	       6:
	       begin
		  workVectorNew[8]  = workVectorTemp[8] + workVectorTemp[9] + workVectorTemp[10] + workVectorTemp[11];
		  workVectorNew[9]  = workVectorTemp[8] + workVectorTemp[9] - workVectorTemp[10] - workVectorTemp[11];
		  workVectorNew[10] = workVectorTemp[8] - workVectorTemp[9] - workVectorTemp[10] + workVectorTemp[11];
		  workVectorNew[11] = workVectorTemp[8] - workVectorTemp[9] + workVectorTemp[10] - workVectorTemp[11];
	       end
	       7:
	       begin
		  workVectorNew[12] = workVectorTemp[12] + workVectorTemp[13] + workVectorTemp[14] + workVectorTemp[15];
		  workVectorNew[13] = workVectorTemp[12] + workVectorTemp[13] - workVectorTemp[14] - workVectorTemp[15];
		  workVectorNew[14] = workVectorTemp[12] - workVectorTemp[13] - workVectorTemp[14] + workVectorTemp[15];
		  workVectorNew[15] = workVectorTemp[12] - workVectorTemp[13] + workVectorTemp[14] - workVectorTemp[15];
	       end
	       default:
	          $display( "ERROR InverseTrans: transforming Intra16x16DC unexpected pixelNum" );
	    endcase
	    workVector <= workVectorNew;
	    if(pixelNum == 7)
	       begin
		  pixelNum <= 0;
		  process <= ScalingDC;
	       end
	    else
	       pixelNum <= pixelNum+1;
	 end
      else
	 begin
	    case ( pixelNum )
	       0:
	       begin
		  Bit#(16) prevValue0;
		  if(state==Intra16x16)
		     prevValue0 = select(storeVectorTemp, {blockNum[3],blockNum[1],blockNum[2],blockNum[0]});
		  else if(state==Chroma)
		     prevValue0 = select(storeVectorTemp, blockNum);
		  else
		     prevValue0 = workVectorTemp[0];
		  Bit#(16) workValue0 = prevValue0 + workVectorTemp[2];
		  Bit#(16) workValue1 = prevValue0 - workVectorTemp[2];
		  Bit#(16) workValue2 = signedShiftRight(workVectorTemp[1], 1) - workVectorTemp[3];
		  Bit#(16) workValue3 = workVectorTemp[1] + signedShiftRight(workVectorTemp[3], 1);
		  workVectorNew[0] = workValue0 + workValue3;
		  workVectorNew[1] = workValue1 + workValue2;
		  workVectorNew[2] = workValue1 - workValue2;
		  workVectorNew[3] = workValue0 - workValue3;
	       end
	       1:
	       begin
		  Bit#(16) workValue4 = workVectorTemp[4] + workVectorTemp[6];
		  Bit#(16) workValue5 = workVectorTemp[4] - workVectorTemp[6];
		  Bit#(16) workValue6 = signedShiftRight(workVectorTemp[5], 1) - workVectorTemp[7];
		  Bit#(16) workValue7 = workVectorTemp[5] + signedShiftRight(workVectorTemp[7], 1);
		  workVectorNew[4] = workValue4 + workValue7;
		  workVectorNew[5] = workValue5 + workValue6;
		  workVectorNew[6] = workValue5 - workValue6;
		  workVectorNew[7] = workValue4 - workValue7;
	       end
	       2:
	       begin
		  Bit#(16) workValue8  = workVectorTemp[8] + workVectorTemp[10];
		  Bit#(16) workValue9  = workVectorTemp[8] - workVectorTemp[10];
		  Bit#(16) workValue10 = signedShiftRight(workVectorTemp[9], 1) - workVectorTemp[11];
		  Bit#(16) workValue11 = workVectorTemp[9] + signedShiftRight(workVectorTemp[11], 1);
		  workVectorNew[8]  = workValue8 + workValue11;
		  workVectorNew[9]  = workValue9 + workValue10;
		  workVectorNew[10] = workValue9 - workValue10;
		  workVectorNew[11] = workValue8 - workValue11;
	       end
	       3:
	       begin
		  Bit#(16) workValue12 = workVectorTemp[12] + workVectorTemp[14];
		  Bit#(16) workValue13 = workVectorTemp[12] - workVectorTemp[14];
		  Bit#(16) workValue14 = signedShiftRight(workVectorTemp[13], 1) - workVectorTemp[15];
		  Bit#(16) workValue15 = workVectorTemp[13] + signedShiftRight(workVectorTemp[15], 1);
		  workVectorNew[12] = workValue12 + workValue15;
		  workVectorNew[13] = workValue13 + workValue14;
		  workVectorNew[14] = workValue13 - workValue14;
		  workVectorNew[15] = workValue12 - workValue15;
	       end
	       4:
	       begin
		  Bit#(16) workValue0  = workVectorTemp[0] + workVectorTemp[8];
		  Bit#(16) workValue4  = workVectorTemp[0] - workVectorTemp[8];
		  Bit#(16) workValue8  = signedShiftRight(workVectorTemp[4], 1) - workVectorTemp[12];
		  Bit#(16) workValue12 = workVectorTemp[4] + signedShiftRight(workVectorTemp[12], 1);
		  workVectorNew[0]  = workValue0 + workValue12;
		  workVectorNew[4]  = workValue4 + workValue8;
		  workVectorNew[8]  = workValue4 - workValue8;
		  workVectorNew[12] = workValue0 - workValue12;
	       end
	       5:
	       begin
		  Bit#(16) workValue1  = workVectorTemp[1] + workVectorTemp[9];
		  Bit#(16) workValue5  = workVectorTemp[1] - workVectorTemp[9];
		  Bit#(16) workValue9  = signedShiftRight(workVectorTemp[5], 1) - workVectorTemp[13];
		  Bit#(16) workValue13 = workVectorTemp[5] + signedShiftRight(workVectorTemp[13], 1);
		  workVectorNew[1]  = workValue1 + workValue13;
		  workVectorNew[5]  = workValue5 + workValue9;
		  workVectorNew[9]  = workValue5 - workValue9;
		  workVectorNew[13] = workValue1 - workValue13;
	       end
	       6:
	       begin
		  Bit#(16) workValue2  = workVectorTemp[2] + workVectorTemp[10];
		  Bit#(16) workValue6  = workVectorTemp[2] - workVectorTemp[10];
		  Bit#(16) workValue10 = signedShiftRight(workVectorTemp[6], 1) - workVectorTemp[14];
		  Bit#(16) workValue14 = workVectorTemp[6] + signedShiftRight(workVectorTemp[14], 1);
		  workVectorNew[2]  = workValue2 + workValue14;
		  workVectorNew[6]  = workValue6 + workValue10;
		  workVectorNew[10] = workValue6 - workValue10;
		  workVectorNew[14] = workValue2 - workValue14;
	       end
	       7:
	       begin
		  Bit#(16) workValue3  = workVectorTemp[3] + workVectorTemp[11];
		  Bit#(16) workValue7  = workVectorTemp[3] - workVectorTemp[11];
		  Bit#(16) workValue11 = signedShiftRight(workVectorTemp[7], 1) - workVectorTemp[15];
		  Bit#(16) workValue15 = workVectorTemp[7] + signedShiftRight(workVectorTemp[15], 1);
		  workVectorNew[3]  = workValue3 + workValue15;
		  workVectorNew[7]  = workValue7 + workValue11;
		  workVectorNew[11] = workValue7 - workValue11;
		  workVectorNew[15] = workValue3 - workValue15;
	       end
	       default:
	          $display( "ERROR InverseTrans: transforming regular unexpected pixelNum" );
	    endcase
	    workVector <= workVectorNew;
	    if(pixelNum == 7)
	       begin
		  pixelNum <= 0;
		  process <= Outputing;
	       end
	    else
	       pixelNum <= pixelNum+1;
	 end
   endrule

   
   rule scalingDC (process matches ScalingDC);
      Bit#(6)  qp;
      Bit#(4)  qpdiv6;
      Bit#(3)  qpmod6;
      Bit#(6)  workOne = 1;
      Bit#(16) workValue;
      Bit#(22) storeValueTemp;
      Bit#(16) storeValue;
      Vector#(16,Bit#(16)) workVectorTemp = workVector;
      Vector#(16,Bit#(16)) storeVectorTemp = storeVector;

      if(state==ChromaDC)
	 begin
	    qp = qpc;
	    qpdiv6 = qpcdiv6;
	    qpmod6 = qpcmod6;
	 end
      else
	 begin
	    qp = qpy;
	    qpdiv6 = qpydiv6;
	    qpmod6 = qpymod6;
	 end
      workValue = select(workVectorTemp, pixelNum);
      Bit#(5) levelScaleValue=0;
      case(qpmod6)
	 0: levelScaleValue = 10;
	 1: levelScaleValue = 11;
	 2: levelScaleValue = 13;
	 3: levelScaleValue = 14;
	 4: levelScaleValue = 16;
	 5: levelScaleValue = 18;
	 default: $display( "ERROR InverseTrans: scalingDC levelScaleGen case default" );
      endcase
      storeValueTemp = zeroExtend(levelScaleValue)*signExtend(workValue);
      if(state==ChromaDC)
	 storeValue = truncate( (storeValueTemp << zeroExtend(qpdiv6)) >> 1 );
      else
	 begin
	    if(qp >= 36)
	       storeValue = truncate( storeValueTemp << zeroExtend(qpdiv6 - 2) );
	    else
	       storeValue = truncate( ((storeValueTemp << 4) + zeroExtend(workOne << zeroExtend(5-qpdiv6))) >> zeroExtend(6 - qpdiv6) );
	 end
      storeVector <= update(storeVectorTemp, pixelNum, storeValue);
      if((state==ChromaDC && pixelNum==7) || pixelNum==15)
	 begin
	    blockNum <= 0;
	    pixelNum <= 0;
	    workVector <= replicate(0);
	    if(state==ChromaDC)
	       state <= Chroma;
	    else
	       state <= Intra16x16;
	    process <= Scaling;
	 end
      else if((state==ChromaDC && pixelNum>7) || pixelNum>15)
	 $display( "ERROR InverseTrans: scalingDC index overflow" );
      else
	 pixelNum <= pixelNum+1;
   endrule


   rule outputing (process matches Outputing);
      Bit#(16) workValue;
      Bit#(10) outputValue;
      Vector#(16,Bit#(16)) workVectorTemp = workVector;

      workValue = select(workVectorTemp, pixelNum);
      outputValue = truncate((workValue+32) >> 6);
      outfifo.enq(ITBresidual outputValue);
      Int#(10) tempint = unpack(outputValue);
      $display("ccl3IBTresidual %0d", tempint);
      pixelNum <= pixelNum+1;
      if(pixelNum==15)
	 begin
	    workVector <= replicate(0);
	    if(state==Chroma)
	       begin
		  if(blockNum<7)
		     begin
			blockNum <= blockNum+1;
			process <= Scaling;
		     end
		  else if (blockNum==7)
		     begin
			blockNum <= 0;
			process <= Passing;
		     end
		  else
		     $display( "ERROR InverseTrans: outputing chroma unexpected blockNum" );
	       end
	    else
	       begin
		  blockNum <= blockNum+1;
		  if(blockNum==15)
		     begin
			state <= ChromaDC;
			process <= LoadingDC;
		     end
		  else
		     process <= Scaling;
	       end
	 end
   endrule 

   
   
   interface Put ioin  = fifoToPut(infifo);
   interface Get ioout = fifoToGet(outfifo);

      
endmodule

endpackage
