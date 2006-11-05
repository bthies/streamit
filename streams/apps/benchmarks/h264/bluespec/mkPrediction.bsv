//**********************************************************************
// Prediction
//----------------------------------------------------------------------
//
//

package mkPrediction;

import H264Types::*;

import IPrediction::*;
import FIFO::*;
import FIFOF::*;
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
 void     Intra;
 void     Inter;
}
State deriving(Eq,Bits);

typedef union tagged                
{
 void     Start;            //not working on anything in particular
 void     Intra16x16;
 void     Intra4x4;
 void     IntraPCM;
}
IntraState deriving(Eq,Bits);

typedef union tagged                
{
 void     Start;            //not working on anything in particular
 void     InterP16x16;
 void     InterP16x8;
 void     InterP8x16;
 void     InterP8x8;
 void     InterPskip;
}
InterState deriving(Eq,Bits);

      
//-----------------------------------------------------------
// Helper functions

function Bit#(8) intra4x4SelectTop( Bit#(72) valVector, Bit#(4) idx );
   case(idx)
      0: return valVector[15:8];
      1: return valVector[23:16];
      2: return valVector[31:24];
      3: return valVector[39:32];
      4: return valVector[47:40];
      5: return valVector[55:48];
      6: return valVector[63:56];
      7: return valVector[71:64];
      default: return valVector[7:0];
   endcase
endfunction

function Bit#(8) intra4x4SelectLeft( Bit#(40) valVector, Bit#(3) idx );
   case(idx)
      0: return valVector[15:8];
      1: return valVector[23:16];
      2: return valVector[31:24];
      3: return valVector[39:32];
      default: return valVector[7:0];
   endcase
endfunction

function Bit#(8) select32to8( Bit#(32) valVector, Bit#(2) idx );
   case(idx)
      0: return valVector[7:0];
      1: return valVector[15:8];
      2: return valVector[23:16];
      3: return valVector[31:24];
   endcase
endfunction

function Bit#(8) select16to8( Bit#(16) valVector, Bit#(1) idx );
   case(idx)
      0: return valVector[7:0];
      1: return valVector[15:8];
   endcase
endfunction


//-----------------------------------------------------------
// Prediction Module
//-----------------------------------------------------------


(* synthesize *)
module mkPrediction( IPrediction );

   //Common state
   FIFO#(EntropyDecOT) infifo      <- mkFIFO;
   FIFO#(EntropyDecOT) outfifo     <- mkFIFO;
   Reg#(Bit#(4))       blockNum    <- mkReg(0);
   Reg#(Bit#(4))       pixelNum    <- mkReg(0);
   Reg#(State)         state       <- mkReg(Start);

   Reg#(Bit#(PicWidthSz)) picWidth  <- mkReg(maxPicWidthInMB);
   Reg#(Bit#(PicAreaSz))  firstMb   <- mkReg(0);
   Reg#(Bit#(PicAreaSz))  currMb    <- mkReg(0);
   Reg#(Bit#(PicAreaSz))  currMbHor <- mkReg(0);//horizontal position of currMb

   Reg#(Bit#(4))   outBlockNum    <- mkReg(0);
   Reg#(Bit#(4))   outPixelNum    <- mkReg(0);
   FIFOF#(Bit#(8)) predictedfifo  <- mkFIFOF;
   Reg#(Bit#(1))   outChromaFlag  <- mkReg(0);
   Reg#(Bit#(5))   outZeroCount   <- mkReg(0);

   //Reg#(Vector#(16,Bit#(8))) workVector       <- mkRegU();

   //Intra state
   Reg#(IntraState)     intrastate      <- mkReg(Start);
   Reg#(Bit#(1))        intraChromaFlag <- mkReg(0);
   FIFO#(MemReq#(TAdd#(PicWidthSz,2),68)) intraMemReqQ  <- mkFIFO;
   Reg#(MemReq#(TAdd#(PicWidthSz,2),68)) intraMemReqQdelay <- mkRegU;
   FIFO#(MemResp#(68))  intraMemRespQ <- mkFIFO;
   Reg#(Vector#(4,Bit#(4))) intra4x4typeLeft <- mkRegU();//15=unavailable, 14=inter-MB, 13=intra-non-4x4
   Reg#(Vector#(4,Bit#(4))) intra4x4typeTop  <- mkRegU();//15=unavailable, 14=inter-MB, 13=intra-non-4x4
   Reg#(Bit#(1)) ppsconstrained_intra_pred_flag <- mkReg(0);
   Reg#(Vector#(4,Bit#(40))) intraLeftVal <- mkRegU();
   Reg#(Vector#(9,Bit#(8))) intraLeftValChroma0 <- mkRegU();
   Reg#(Vector#(9,Bit#(8))) intraLeftValChroma1 <- mkRegU();
   Reg#(Vector#(5,Bit#(32))) intraTopVal <- mkRegU();
   Reg#(Vector#(4,Bit#(16))) intraTopValChroma0 <- mkRegU();
   Reg#(Vector#(4,Bit#(16))) intraTopValChroma1 <- mkRegU();
   Reg#(Bit#(32)) intraLeftValNext <- mkReg(0);
   Reg#(Bit#(24)) intraTopValNext <- mkReg(0);
   Reg#(Bit#(8)) intraTopValChroma0Next <- mkReg(0); 
   Reg#(Bit#(8)) intraTopValChroma1Next <- mkReg(0);
   Reg#(Bit#(2)) intra16x16_pred_mode <- mkReg(0);
   FIFO#(Bit#(4)) rem_intra4x4_pred_mode <- mkSizedFIFO(16);
   Reg#(Bit#(2)) intra_chroma_pred_mode <- mkReg(0);
   Reg#(Bit#(4)) cur_intra4x4_pred_mode <- mkReg(0);
   Reg#(Bit#(1)) intraChromaTopAvailable <- mkReg(0);
   Reg#(Bit#(1)) intraChromaLeftAvailable <- mkReg(0);

   Reg#(Bit#(3)) intraReqCount <- mkReg(0);
   Reg#(Bit#(3)) intraRespCount <- mkReg(0);
   Reg#(Bit#(4)) intraStepCount <- mkReg(0);
   Reg#(Bit#(13)) intraSumA <-  mkReg(0);
   Reg#(Bit#(15)) intraSumB <-  mkReg(0);
   Reg#(Bit#(15)) intraSumC <-  mkReg(0);
   

   //-----------------------------------------------------------
   // Rules

   //////////////////////////////////////////////////////////////////////////////
//   rule stateMonitor ( True );
//      if(predictedfifo.notEmpty())
//	 $display( "TRACE Prediction: stateMonitor predictedfifo.first() %0d", predictedfifo.first());////////////////////
//      if(infifo.first() matches tagged ITBresidual .xdata)
//	 $display( "TRACE Prediction: stateMonitor infifo.first() %0d", xdata);////////////////////
//      if(infifo.first() matches tagged ITBresidual .xdata)
//	 $display( "TRACE Prediction: stateMonitor outBlockNum outPixelNum outChromaFlag %0d %0d", outBlockNum, outPixelNum, outChromaFlag);////////////////////
//   endrule
   //////////////////////////////////////////////////////////////////////////////
   
   rule passing ( True );
      Bit#(1) outputFlag = 0;
      Bit#(8) outputValue = 0;
      
      case (infifo.first()) matches
	 tagged NewUnit . xdata :
	    begin
	       infifo.deq();
	       outfifo.enq(infifo.first());
	       $display("ccl4newunit");
	       $display("ccl4rbspbyte %h", xdata);
	    end
	 tagged SPSpic_width_in_mbs .xdata :
	    begin
	       infifo.deq();
	       outfifo.enq(infifo.first());
	       picWidth <= xdata;
	    end
	 tagged PPSconstrained_intra_pred_flag .xdata :
	    begin
	       infifo.deq();
	       ////outfifo.enq(infifo.first());
	       ppsconstrained_intra_pred_flag <= xdata;
	    end
	 tagged SHfirst_mb_in_slice .xdata :
	    begin
	       infifo.deq();
	       outfifo.enq(infifo.first());
	       firstMb   <= xdata;
	       currMb    <= xdata;
	       currMbHor <= xdata;
	       intra4x4typeLeft <= replicate(15);
	    end
	 tagged SDmb_skip_run .xdata :
	    begin
	       $display( "ERROR Prediction: P_SKIP not implemented yet");
	       //$finish;////////////////////////////////////////////////////////////////////////////////////////
	    end
	 tagged SDMmbtype .xdata :
	    begin
	       infifo.deq();
	       outfifo.enq(infifo.first());
	       $display( "INFO Prediction: SDMmbtype %0d", xdata);
	       if(mbPartPredMode(xdata,0) == Intra_16x16)
		  begin
		     state <= Intra;
		     intrastate <= Intra16x16;
		     if(xdata matches tagged I_16x16 {intra16x16PredMode:.tempv1, codedBlockPatternChroma:.tempv2, codedBlockPatternLuma:.tempv3})
			intra16x16_pred_mode <= tempv1;
		     else
			$display( "ERROR EntropyDec: MacroblockLayer 5 sdmmbtype not I_16x16" );
		     intraReqCount <= 1;
		     intraRespCount <= 1;
		  end
	       else if(xdata==I_NxN)
		  begin
		     state <= Intra;
		     intrastate <= Intra4x4;
		     intraReqCount <= 1;
		     intraRespCount <= 1;
		  end
	       else if(xdata==I_PCM)
		  begin
		     $display( "ERROR Prediction: I_PCM not implemented yet");
		     //$finish;////////////////////////////////////////////////////////////////////////////////////////
		  end
	       else
		  begin
		     $display( "ERROR Prediction: inter prediction not implemented yet");
		     //$finish;////////////////////////////////////////////////////////////////////////////////////////
		  end
	    end
	 tagged SDMMrem_intra4x4_pred_mode .xdata :
	    begin
	       infifo.deq();
	       ////outfifo.enq(infifo.first());
	       rem_intra4x4_pred_mode.enq(xdata);
	    end
	 tagged SDMMintra_chroma_pred_mode .xdata :
	    begin
	       infifo.deq();
	       ////outfifo.enq(infifo.first());
	       intra_chroma_pred_mode <= xdata;
	    end
	 tagged ITBresidual .xdata :
	    begin
	       //$display( "TRACE Prediction: passing IBTresidual %0d %0d", outBlockNum, outPixelNum);////////////////////
	       Bit#(11) tempOutputValue = signExtend(xdata) + zeroExtend(predictedfifo.first());
	       if(tempOutputValue[10]==1)
		  outputValue = 0;
	       else if(tempOutputValue[9:0] > 255)
		  outputValue = 255;
	       else
		  outputValue = tempOutputValue[7:0];
	       infifo.deq();
	       predictedfifo.deq();
	       outputFlag = 1;
	    end
	 tagged SDMRcoeffLevelZeros .xdata :
	    begin
	       if(outZeroCount < xdata-1)
		  outZeroCount <= outZeroCount + 1;
	       else
		  begin
		     outZeroCount <= 0;
		     infifo.deq();
		  end
	       outputValue = predictedfifo.first();
	       predictedfifo.deq();
	       outputFlag = 1;
	    end
	 tagged EndOfFile :
	    begin
	       infifo.deq();
	       outfifo.enq(infifo.first());
	       $display( "INFO Prediction: EndOfFile reached" );
	       //$finish(0);////////////////////////////////
	    end
	 default:
	    begin
	       infifo.deq();
	       outfifo.enq(infifo.first());
	    end
      endcase

      if(outputFlag == 1)
	 begin
	    $display("ccl4PBoutput %0d", outputValue);
	    outfifo.enq(PBoutput outputValue);	    
	    Bit#(2) blockHor = {outBlockNum[2],outBlockNum[0]};
	    Bit#(2) blockVer = {outBlockNum[3],outBlockNum[1]};
	    Bit#(2) pixelHor = {outPixelNum[1],outPixelNum[0]};
	    Bit#(2) pixelVer = {outPixelNum[3],outPixelNum[2]};
	    Bit#(4) totalHor = {blockHor,pixelHor};
	    Bit#(4) totalVer = {blockVer,pixelVer};

	    if(outBlockNum==0 && outPixelNum==0 && outChromaFlag==0 && currMb!=firstMb && picWidth>1)
	       begin
		  intraMemReqQ.enq(intraMemReqQdelay);
		  //$display( "TRACE Prediction: passing storing addr data");//////////////////
	       end
	    
	    if(pixelHor==3 && (blockHor==3 || (blockHor[0]==1 && outChromaFlag==1) || (intrastate==Intra4x4 && outChromaFlag==0)))
	       begin
		  if(outChromaFlag==0)
		     begin
			Bit#(32) intraLeftValNextTemp = intraLeftValNext;
			if(totalVer==0 || (intrastate==Intra4x4 && pixelVer==0))
			   begin
			      Bit#(32) tempValSet = select(intraTopVal,zeroExtend(blockHor));
			      intraLeftValNextTemp = zeroExtend(tempValSet[31:24]);
			   end
			case(pixelVer)
			   0:intraLeftValNext <= {intraLeftValNextTemp[31:16],outputValue,intraLeftValNextTemp[7:0]};
			   1:intraLeftValNext <= {intraLeftValNextTemp[31:24],outputValue,intraLeftValNextTemp[15:0]};
			   2:intraLeftValNext <= {outputValue,intraLeftValNextTemp[23:0]};
			   3:
			   begin
			      intraLeftVal <= update(intraLeftVal,blockVer,{outputValue,intraLeftValNextTemp});
			      intraLeftValNext <= zeroExtend(outputValue);
			      if(intrastate==Intra4x4)
				 intra4x4typeLeft <= update(intra4x4typeLeft,blockVer,cur_intra4x4_pred_mode);
			      else if(state==Intra)
				 intra4x4typeLeft <= update(intra4x4typeLeft,blockVer,13);
			      else
				 intra4x4typeLeft <= update(intra4x4typeLeft,blockVer,14);
			   end
			endcase
		     end
		  else
		     begin
			if(outBlockNum[2]==0)
			   intraLeftValChroma0 <= update(intraLeftValChroma0,totalVer+1,outputValue);
			else
			   intraLeftValChroma1 <= update(intraLeftValChroma1,totalVer+1,outputValue);
		     end
	       end
			   
	    if(pixelVer==3 && (blockVer==3 || (blockVer[0]==1 && outChromaFlag==1) || (intrastate==Intra4x4 && outChromaFlag==0)))
	       begin
		  if(outChromaFlag==0)
		     begin
			Bit#(24) intraTopValNextTemp = intraTopValNext;
			case(pixelHor)
			   0:intraTopValNext <= zeroExtend(outputValue);
			   1:intraTopValNext <= {intraTopValNextTemp[23:16],outputValue,intraTopValNextTemp[7:0]};
			   2:intraTopValNext <= {outputValue,intraTopValNextTemp[15:0]};
			   3:
			   begin
			      intraTopVal <= update(intraTopVal,zeroExtend(blockHor),{outputValue,intraTopValNextTemp});
			      if(intrastate==Intra4x4)
				 intra4x4typeTop <= update(intra4x4typeTop,blockHor,cur_intra4x4_pred_mode);
			      else if(state==Intra)
				 intra4x4typeTop <= update(intra4x4typeTop,blockHor,13);
			      else
				 intra4x4typeTop <= update(intra4x4typeTop,blockHor,14);
			   end
			endcase
		     end
		  else
		     begin
			if(outBlockNum[2]==0)
			   begin
			      if(pixelHor[0]==0)
				 intraTopValChroma0Next <= outputValue;
			      else
				 intraTopValChroma0 <= update(intraTopValChroma0,totalHor[2:1],{outputValue,intraTopValChroma0Next});
			   end
			else
			   begin
			      if(pixelHor[0]==0)
				 intraTopValChroma1Next <= outputValue;
			      else
				 begin
				    intraTopValChroma1 <= update(intraTopValChroma1,totalHor[2:1],{outputValue,intraTopValChroma1Next});
				    Bit#(4)  intra4x4typeTopStore = select(intra4x4typeTop,totalHor[2:1]);
				    if(state == Inter)
				       intra4x4typeTopStore = 14;
				    else if(intrastate != Intra4x4)
				       intra4x4typeTopStore = 13;
				    Bit#(32) intraTopValStore = select(intraTopVal,zeroExtend(totalHor[2:1]));
				    Bit#(16) intraTopValChroma0Store = select(intraTopValChroma0,totalHor[2:1]);
				    Bit#(16) intraTopValChroma1Store = {outputValue,intraTopValChroma1Next};
				    Bit#(68) intraStore = {intra4x4typeTopStore,intraTopValChroma1Store,intraTopValChroma0Store,intraTopValStore};
				    Bit#(PicWidthSz) tempStoreAddr = truncate(currMbHor);
				    if({blockHor[0],pixelHor[1]}==3 && picWidth>1)
				       intraMemReqQdelay <= StoreReq {addr:{tempStoreAddr,blockHor[0],pixelHor[1]},data:intraStore};
				    else
				       begin
					  intraMemReqQ.enq(StoreReq {addr:{tempStoreAddr,blockHor[0],pixelHor[1]},data:intraStore});
					  //$display( "TRACE Prediction: passing storing addr data %0d %h",{tempStoreAddr,blockHor[0],pixelHor[1]},intraStore);//////////////////
				       end
				 end
			   end
		     end
	       end
	       
	    outPixelNum <= outPixelNum+1;
	    if(outPixelNum == 15)
	       begin
		  if(outChromaFlag==0)
		     begin
			outBlockNum <= outBlockNum+1;
			if(outBlockNum == 15)
			   outChromaFlag <= 1;
		     end
		  else
		     begin
			if(outBlockNum == 7)
			   begin
			      outBlockNum <= 0;
			      outChromaFlag <= 0;
			      currMb <= currMb+1;
			      currMbHor <= currMbHor+1;
			   end
			else
			   outBlockNum <= outBlockNum+1;
		     end
	       end
	 end
   endrule


   rule currMbHorUpdate( !(currMbHor<zeroExtend(picWidth)) );
      Bit#(PicAreaSz) temp = zeroExtend(picWidth);
      if((currMbHor >> 3) >= temp)
	 currMbHor <= currMbHor - (temp << 3);
      else
	 currMbHor <= currMbHor - temp;
   endrule


   rule intraSendReq ( intraReqCount>0 && currMbHor<zeroExtend(picWidth) );
      Bit#(PicWidthSz) temp2 = truncate(currMbHor);
      Bit#(TAdd#(PicWidthSz,2)) temp = 0;
      Bit#(1) noMoreReq = 0;
      if( currMb-firstMb < zeroExtend(picWidth) )
	 noMoreReq = 1;
      else
	 begin
	    if(intraReqCount<5)
	       begin
		  Bit#(2) temp3 = truncate(intraReqCount-1);
		  temp = {temp2,temp3};
	       end
	    else if(intraReqCount==5)
	       begin
		  if((currMbHor+1)<zeroExtend(picWidth) && intrastate==Intra4x4)
		     temp = {(temp2+1),2'b00};
		  else if(currMbHor>0 && currMb-firstMb>zeroExtend(picWidth))
		     temp = {(temp2-1),2'b11};
		  else
		     noMoreReq = 1;
	       end
	    else if(intraReqCount==6)
	       begin
		  if((currMbHor+1)<zeroExtend(picWidth) && intrastate==Intra4x4 && currMbHor>0 && currMb-firstMb>zeroExtend(picWidth))
		     temp = {(temp2-1),2'b11};
		  else
		     noMoreReq = 1;
	       end
	    else
	       noMoreReq = 1;
	 end
      if(noMoreReq == 0)
	 begin
      	    intraMemReqQ.enq(LoadReq temp);
	    intraReqCount <= intraReqCount+1;
	    //$display( "TRACE Prediction: intraSendReq addr %0d",temp);///////////////////////
	 end
      else
	 intraReqCount <= 0;
   endrule


   rule intraReceiveNoResp ( intraRespCount>0 && currMbHor<zeroExtend(picWidth) && currMb-firstMb<zeroExtend(picWidth) );
      intra4x4typeTop <= replicate(15);
      intraRespCount <= 0;
      intraStepCount <= 1;
      blockNum <= 0;
      pixelNum <= 0;
   endrule

   
   rule intraReceiveResp ( intraRespCount>0 && intraRespCount<7 && currMbHor<zeroExtend(picWidth) &&& intraMemRespQ.first() matches tagged LoadResp .data);
      Bit#(1) noMoreResp = 0;
      Bit#(2) temp2bit = 0;
      if(intraRespCount<5)
	 begin
	    temp2bit = truncate(intraRespCount-1);
	    intra4x4typeTop <= update(intra4x4typeTop, temp2bit, data[67:64]);
	    if(intraRespCount==4)
	       begin
		  Vector#(5,Bit#(32)) intraTopValTemp = intraTopVal;
		  intraTopValTemp[3] = data[31:0];
		  intraTopValTemp[4] = {data[31:24],data[31:24],data[31:24],data[31:24]};
		  intraTopVal <= intraTopValTemp;
		  if(!((currMbHor+1)<zeroExtend(picWidth) && intrastate==Intra4x4) && !(currMbHor>0 && currMb-firstMb>zeroExtend(picWidth)))
		     noMoreResp = 1;
	       end
	    else
	       intraTopVal <= update(intraTopVal, intraRespCount-1, data[31:0]);
	    intraTopValChroma0 <= update(intraTopValChroma0, temp2bit, data[47:32]);
	    intraTopValChroma1 <= update(intraTopValChroma1, temp2bit, data[63:48]);
	 end
      else if(intraRespCount==5)
	 begin
	    if((currMbHor+1)<zeroExtend(picWidth) && intrastate==Intra4x4)
	       begin
		  if(!(data[67:64]==15 || (data[67:64]==14 && ppsconstrained_intra_pred_flag==1)))
		     intraTopVal <= update(intraTopVal, 4, data[31:0]);
		  if(!(currMbHor>0 && currMb-firstMb>zeroExtend(picWidth)))
		     noMoreResp = 1;
	       end
	    else
	       begin
		  Bit#(40) temp2 = intraLeftVal[0];
		  intraLeftVal <= update(intraLeftVal, 0, {temp2[39:8],data[31:24]});
		  intraLeftValChroma0 <= update(intraLeftValChroma0, 0, data[47:40]);
		  intraLeftValChroma1 <= update(intraLeftValChroma1, 0, data[63:56]);
		  noMoreResp = 1;
	       end
	 end
      else
	 begin
	    Bit#(40) temp2 = intraLeftVal[0];
	    intraLeftVal <= update(intraLeftVal, 0, {temp2[39:8],data[31:24]});
	    intraLeftValChroma0 <= update(intraLeftValChroma0, 0, data[47:40]);
	    intraLeftValChroma1 <= update(intraLeftValChroma1, 0, data[63:56]);
	    noMoreResp = 1;
	 end
      intraMemRespQ.deq();
      //$display( "TRACE Prediction: intraReceiveResp data %h",data);///////////////////////
      if(noMoreResp == 0)
	 intraRespCount <= intraRespCount+1;
      else
	 begin
	    intraRespCount <= 0;
	    intraStepCount <= 1;
	    blockNum <= 0;
	    pixelNum <= 0;
	 end
   endrule

   
   rule intraPredTypeStep ( intraStepCount==1 && (intrastate!=Intra4x4 || !predictedfifo.notEmpty()));
      Bit#(2) blockHor = {blockNum[2],blockNum[0]};
      Bit#(2) blockVer = {blockNum[3],blockNum[1]};
      Bit#(4) topType = select(intra4x4typeTop, blockHor);
      Bit#(4) leftType;
      if(currMbHor!=0 || blockNum!=0)
	 leftType = select(intra4x4typeLeft, blockVer);
      else
	 begin
	    leftType = 15;
	    intra4x4typeLeft <= replicate(15);
	 end
      if(intrastate!=Intra4x4)
	 intraStepCount <= intraStepCount+1;
      else if(!predictedfifo.notEmpty())
	 begin
	    Bit#(1) topAvailable;
	    Bit#(1) leftAvailable;
	    if(topType==15 || (topType==14 && ppsconstrained_intra_pred_flag==1))
	       topAvailable = 0;
	    else
	       topAvailable = 1;
	    if(leftType==15 || (leftType==14 && ppsconstrained_intra_pred_flag==1))
	       leftAvailable = 0;
	    else
	       leftAvailable = 1;
	    Bit#(4) predType = 0;
	    Bit#(4) remType = rem_intra4x4_pred_mode.first();
	    Bit#(4) curType = 0;
	    rem_intra4x4_pred_mode.deq();
	    if(topAvailable==0 || leftAvailable==0)
	       predType = 2;
	    else
	       begin
		  Bit#(4) topType2 = topType;
		  Bit#(4) leftType2 = leftType;
		  if(topType>8)
		     topType2 = 2;
		  if(leftType>8)
		     leftType2 = 2;
		  if(topType2 > leftType2)
		     predType = leftType2;
		  else
		     predType = topType2;
	       end
	    if(remType[3] == 1)
	       curType = predType;
	    else if(remType < predType)
	       curType = remType;
	    else
	       curType = remType+1;
	    cur_intra4x4_pred_mode <= curType;
	    intraStepCount <= intraStepCount+1;

	    //$display( "TRACE Prediction: intraPredTypeStep currMbHor blockNum topType leftType predType remType curType %0d %0d %0d %0d %0d %0d %0d",currMbHor,blockNum,topType,leftType,predType,remType,curType);//////////////////
	 end
   endrule


   rule intraProcessStep ( intraStepCount>1 );
      //$display( "TRACE Prediction: intraProcessStep %0d %0d", blockNum, pixelNum);////////////////////
      //$display( "TRACE Prediction: intraProcessStep intraTopVal %h %h %h %h %h",intraTopVal[4],intraTopVal[3],intraTopVal[2],intraTopVal[1],intraTopVal[0]);/////////////////
      Bit#(1) outFlag  = 0;
      Bit#(4) nextIntraStepCount = intraStepCount+1;
      Bit#(2) blockHor = {blockNum[2],blockNum[0]};
      Bit#(2) blockVer = {blockNum[3],blockNum[1]};
      Bit#(2) pixelHor = {pixelNum[1],pixelNum[0]};
      Bit#(2) pixelVer = {pixelNum[3],pixelNum[2]};

      Bit#(4) topType = select(intra4x4typeTop, blockHor);
      Bit#(4) leftType = select(intra4x4typeLeft, blockVer);
      Bit#(1) topAvailable;
      Bit#(1) leftAvailable;
      if(topType==15 || (topType==14 && ppsconstrained_intra_pred_flag==1))
	 topAvailable = 0;
      else
	 topAvailable = 1;
      if(leftType==15 || (leftType==14 && ppsconstrained_intra_pred_flag==1))
	 leftAvailable = 0;
      else
	 leftAvailable = 1;
      if(blockNum==0 && pixelNum==0 && intraChromaFlag==0)
	 begin
	    intraChromaTopAvailable <= topAvailable;
	    intraChromaLeftAvailable <= leftAvailable;
	 end
      if(intrastate==Intra4x4 && intraChromaFlag==0)
	 begin
	    if(intraStepCount==2)
	       begin
		  outFlag = 1;
		  Bit#(40) leftValSet = select(intraLeftVal,blockVer);
		  Bit#(32) topMidValSet = select(intraTopVal,zeroExtend(blockHor));
		  Bit#(32) topRightValSet = select(intraTopVal,{1'b0,blockHor}+1);
		  Bit#(72) topValSet;
		  if((blockNum[3:2]==3 && blockNum[0]==1) || blockNum[1:0]==3)
		     topValSet = {topMidValSet[31:24],topMidValSet[31:24],topMidValSet[31:24],topMidValSet[31:24],topMidValSet,leftValSet[7:0]};
		  else
		     topValSet = {topRightValSet,topMidValSet,leftValSet[7:0]};
		  //$display( "TRACE Prediction: intraProcessStep intra4x4 %0d %0d %h %h", cur_intra4x4_pred_mode, blockNum, leftValSet, topValSet);////////////////////
		  Bit#(4) topSelect1 = 0;
		  Bit#(4) topSelect2 = 0;
		  Bit#(4) topSelect3 = 0;
		  Bit#(3) leftSelect1 = 0;
		  Bit#(3) leftSelect2 = 0;
		  Bit#(3) leftSelect3 = 0;
		  Bit#(10) tempVal1 = 0;
		  Bit#(10) tempVal2 = 0;
		  Bit#(10) tempVal3 = 0;
		  case(cur_intra4x4_pred_mode)
		     0://vertical
		     begin
			topSelect1 = zeroExtend(pixelHor);
			Bit#(8) topVal = intra4x4SelectTop(topValSet,topSelect1);
			predictedfifo.enq(topVal);
		     end
		     1://horizontal
		     begin
			leftSelect1 = zeroExtend(pixelVer);
			Bit#(8) leftVal = intra4x4SelectLeft(leftValSet,leftSelect1);
			predictedfifo.enq(leftVal);
		     end
		     2://dc
		     begin
			Bit#(10) tempTopSum = zeroExtend(topValSet[15:8])+zeroExtend(topValSet[23:16])+zeroExtend(topValSet[31:24])+zeroExtend(topValSet[39:32]) + 2;
			Bit#(10) tempLeftSum = zeroExtend(leftValSet[15:8])+zeroExtend(leftValSet[23:16])+zeroExtend(leftValSet[31:24])+zeroExtend(leftValSet[39:32]) + 2;
			Bit#(11) tempTotalSum = zeroExtend(tempTopSum)+zeroExtend(tempLeftSum);
			Bit#(8) topSum = tempTopSum[9:2];
			Bit#(8) leftSum = tempLeftSum[9:2];
			Bit#(8) totalSum = tempTotalSum[10:3];
			if(topAvailable==1 && leftAvailable==1)
			   predictedfifo.enq(totalSum);
			else if(topAvailable==1)
			   predictedfifo.enq(topSum);
			else if(leftAvailable==1)
			   predictedfifo.enq(leftSum);
			else
			   predictedfifo.enq(8'b10000000);
		     end
		     3://diagonal down left
		     begin
			Bit#(4) selectNum = zeroExtend(pixelHor)+zeroExtend(pixelVer);
			if(pixelHor==3 && pixelVer==3)
			   begin
			      topSelect1 = 6;
			      topSelect2 = 7;
			      topSelect3 = 7;
			   end
			else
			   begin
			      topSelect1 = selectNum;
			      topSelect2 = selectNum+1;
			      topSelect3 = selectNum+2;
			   end
			tempVal1 = zeroExtend(intra4x4SelectTop(topValSet,topSelect1));
			tempVal2 = zeroExtend(intra4x4SelectTop(topValSet,topSelect2));
			tempVal3 = zeroExtend(intra4x4SelectTop(topValSet,topSelect3));
			Bit#(10) predVal = tempVal1 + (tempVal2<<1) + tempVal3 + 2;
			predictedfifo.enq(predVal[9:2]);
		     end
		     4://diagonal down right
		     begin
			if(pixelHor > pixelVer)
			   begin
			      topSelect3 = zeroExtend(pixelHor)-zeroExtend(pixelVer);
			      topSelect2 = topSelect3-1;
			      topSelect1 = topSelect3-2;
			      tempVal1 = zeroExtend(intra4x4SelectTop(topValSet,topSelect1));
			      tempVal2 = zeroExtend(intra4x4SelectTop(topValSet,topSelect2));
			      tempVal3 = zeroExtend(intra4x4SelectTop(topValSet,topSelect3));
			   end
			else if(pixelHor < pixelVer)
			   begin
			      leftSelect3 = zeroExtend(pixelVer)-zeroExtend(pixelHor);
			      leftSelect2 = leftSelect3-1;
			      leftSelect1 = leftSelect3-2;
			      tempVal1 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect1));
			      tempVal2 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect2));
			      tempVal3 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect3));
			   end
			else
			   begin
			      leftSelect1 = 0;
			      leftSelect2 = -1;
			      topSelect1 = 0;
			      tempVal1 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect1));
			      tempVal2 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect2));
			      tempVal3 = zeroExtend(intra4x4SelectTop(topValSet,topSelect1));
			   end
			Bit#(10) predVal = tempVal1 + (tempVal2<<1) + tempVal3 + 2;
			predictedfifo.enq(predVal[9:2]);
		     end
		     5://vertical right
		     begin
			Bit#(4) tempPixelHor = zeroExtend(pixelHor);
			Bit#(4) zVR = (tempPixelHor<<1)-zeroExtend(pixelVer);
			if(zVR<=6 && zVR>=0)
			   begin
			      topSelect3 = zeroExtend(pixelHor)-zeroExtend(pixelVer>>1);
			      topSelect2 = topSelect3-1;
			      if(zVR==1 || zVR==3 || zVR==5)
				 topSelect1 = topSelect3-2;
			      else
				 topSelect1 = topSelect3;
			      tempVal1 = zeroExtend(intra4x4SelectTop(topValSet,topSelect1));
			      tempVal2 = zeroExtend(intra4x4SelectTop(topValSet,topSelect2));
			      tempVal3 = zeroExtend(intra4x4SelectTop(topValSet,topSelect3));
			   end
			else if(zVR==-1)
			   begin
			      leftSelect1 = 0;
			      leftSelect2 = -1;
			      topSelect1 = 0;
			      tempVal1 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect1));
			      tempVal2 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect2));
			      tempVal3 = zeroExtend(intra4x4SelectTop(topValSet,topSelect1));
			   end
			else
			   begin
			      leftSelect1 = zeroExtend(pixelVer)-1;
			      leftSelect2 = leftSelect1-1;
			      leftSelect3 = leftSelect1-2;
			      tempVal1 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect1));
			      tempVal2 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect2));
			      tempVal3 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect3));
			   end
			Bit#(10) predVal = tempVal1 + (tempVal2<<1) + tempVal3 + 2;
			predictedfifo.enq(predVal[9:2]);
		     end
		     6://horizontal down
		     begin
			Bit#(4) tempPixelVer = zeroExtend(pixelVer);
			Bit#(4) zHD = (tempPixelVer<<1)-zeroExtend(pixelHor);
			if(zHD<=6 && zHD>=0)
			   begin
			      leftSelect3 = zeroExtend(pixelVer)-zeroExtend(pixelHor>>1);
			      leftSelect2 = leftSelect3-1;
			      if(zHD==1 || zHD==3 || zHD==5)
				 leftSelect1 = leftSelect3-2;
			      else
				 leftSelect1 = leftSelect3;
			      tempVal1 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect1));
			      tempVal2 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect2));
			      tempVal3 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect3));
			   end
			else if(zHD==-1)
			   begin
			      leftSelect1 = 0;
			      leftSelect2 = -1;
			      topSelect1 = 0;
			      tempVal1 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect1));
			      tempVal2 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect2));
			      tempVal3 = zeroExtend(intra4x4SelectTop(topValSet,topSelect1));
			   end
			else
			   begin
			      topSelect1 = zeroExtend(pixelHor)-1;
			      topSelect2 = topSelect1-1;
			      topSelect3 = topSelect1-2;
			      tempVal1 = zeroExtend(intra4x4SelectTop(topValSet,topSelect1));
			      tempVal2 = zeroExtend(intra4x4SelectTop(topValSet,topSelect2));
			      tempVal3 = zeroExtend(intra4x4SelectTop(topValSet,topSelect3));
			   end
			Bit#(10) predVal = tempVal1 + (tempVal2<<1) + tempVal3 + 2;
			predictedfifo.enq(predVal[9:2]);
		     end
		     7://vertical left
		     begin
			topSelect1 = zeroExtend(pixelHor)+zeroExtend(pixelVer>>1);
			topSelect2 = topSelect1+1;
			if(pixelVer==1 || pixelVer==3)
			   topSelect3 = topSelect1+2;
			else
			   topSelect3 = topSelect1;
			tempVal1 = zeroExtend(intra4x4SelectTop(topValSet,topSelect1));
			tempVal2 = zeroExtend(intra4x4SelectTop(topValSet,topSelect2));
			tempVal3 = zeroExtend(intra4x4SelectTop(topValSet,topSelect3));
			Bit#(10) predVal = tempVal1 + (tempVal2<<1) + tempVal3 + 2;
			predictedfifo.enq(predVal[9:2]);
		     end
		     8://horizontal up
		     begin
			Bit#(4) tempPixelVer = zeroExtend(pixelVer);
			Bit#(4) zHU = (tempPixelVer<<1)+zeroExtend(pixelHor);
			if(zHU<=4)
			   begin
			      leftSelect1 = zeroExtend(pixelVer)+zeroExtend(pixelHor>>1);
			      leftSelect2 = leftSelect1+1;
			      if(zHU==1 || zHU==3)
				 leftSelect3 = leftSelect1+2;
			      else
				 leftSelect3 = leftSelect1;
			   end
			else
			   begin
			      if(zHU==5)
				 leftSelect1 = 2;
			      else
				 leftSelect1 = 3;
			      leftSelect2 = 3;
			      leftSelect3 = 3;
			   end
			tempVal1 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect1));
			tempVal2 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect2));
			tempVal3 = zeroExtend(intra4x4SelectLeft(leftValSet,leftSelect3));
			Bit#(10) predVal = tempVal1 + (tempVal2<<1) + tempVal3 + 2;
			predictedfifo.enq(predVal[9:2]);
		     end
		     default: $display( "ERROR Prediction: intraProcessStep intra4x4 unknown cur_intra4x4_pred_mode");
		  endcase
	       end
	    else
	       $display( "ERROR Prediction: intraProcessStep intra4x4 unknown intraStepCount");
	 end
      else if(intrastate==Intra16x16  && intraChromaFlag==0)
	 begin
	    //$display( "TRACE Prediction: intraProcessStep intra16x16 %0d %0d %0d %h", intra16x16_pred_mode, currMb, blockNum, select(intraTopVal,blockHor));///////////////////
	    case(intra16x16_pred_mode)
	       0://vertical
	       begin
		  Bit#(32) topValSet = select(intraTopVal,blockHor);
		  Bit#(8) topVal = select32to8(topValSet,pixelHor);
		  predictedfifo.enq(topVal);
		  outFlag = 1;
	       end
	       1://horizontal
	       begin
		  Bit#(40) leftValSet = select(intraLeftVal,blockVer);
		  Bit#(8) leftVal = intra4x4SelectLeft(leftValSet,zeroExtend(pixelVer));
		  predictedfifo.enq(leftVal);
		  outFlag = 1;
	       end
	       2://dc
	       begin
		  case(intraStepCount)
		     2:
		     begin
			if(topAvailable == 1)
			   begin
			      Bit#(32) topValSet = select(intraTopVal,0);
			      intraSumA <= zeroExtend(topValSet[7:0])+zeroExtend(topValSet[15:8])+zeroExtend(topValSet[23:16])+zeroExtend(topValSet[31:24]);
			   end
			else
			   begin
			      intraSumA <= 0;
			      nextIntraStepCount = 6;
			   end
		     end
		     3:
		     begin
			Bit#(32) topValSet = select(intraTopVal,1);
			intraSumA <= intraSumA+zeroExtend(topValSet[7:0])+zeroExtend(topValSet[15:8])+zeroExtend(topValSet[23:16])+zeroExtend(topValSet[31:24]);
		     end
		     4:
		     begin
			Bit#(32) topValSet = select(intraTopVal,2);
			intraSumA <= intraSumA+zeroExtend(topValSet[7:0])+zeroExtend(topValSet[15:8])+zeroExtend(topValSet[23:16])+zeroExtend(topValSet[31:24]);
		     end
		     5:
		     begin
			Bit#(32) topValSet = select(intraTopVal,3);
			intraSumA <= intraSumA+zeroExtend(topValSet[7:0])+zeroExtend(topValSet[15:8])+zeroExtend(topValSet[23:16])+zeroExtend(topValSet[31:24])+8;
		     end
		     6:
		     begin
			if(leftAvailable == 1)
			   begin
			      Bit#(40) leftValSet = select(intraLeftVal,0);
			      intraSumA <= intraSumA+zeroExtend(leftValSet[15:8])+zeroExtend(leftValSet[23:16])+zeroExtend(leftValSet[31:24])+zeroExtend(leftValSet[39:32]);
			   end
			else
			   nextIntraStepCount = 10;
		     end
		     7:
		     begin
			Bit#(40) leftValSet = select(intraLeftVal,1);
			intraSumA <= intraSumA+zeroExtend(leftValSet[15:8])+zeroExtend(leftValSet[23:16])+zeroExtend(leftValSet[31:24])+zeroExtend(leftValSet[39:32]);
		     end
		     8:
		     begin
			Bit#(40) leftValSet = select(intraLeftVal,2);
			intraSumA <= intraSumA+zeroExtend(leftValSet[15:8])+zeroExtend(leftValSet[23:16])+zeroExtend(leftValSet[31:24])+zeroExtend(leftValSet[39:32]);
		     end
		     9:
		     begin
			Bit#(40) leftValSet = select(intraLeftVal,3);
			intraSumA <= intraSumA+zeroExtend(leftValSet[15:8])+zeroExtend(leftValSet[23:16])+zeroExtend(leftValSet[31:24])+zeroExtend(leftValSet[39:32])+8;
		     end
		     10:
		     begin
			if(leftAvailable == 1 && topAvailable == 1)
			   intraSumA <= intraSumA >> 5;
			else if(leftAvailable == 1 || topAvailable == 1)
			   intraSumA <= intraSumA >> 4;
			else
			   intraSumA <= 128;
		     end
		     11:
		     begin
			predictedfifo.enq(intraSumA[7:0]);
			outFlag = 1;
		     end
		     default: $display( "ERROR Prediction: intraProcessStep intra16x16 DC unknown intraStepCount");
		  endcase
	       end
	       3://plane
	       begin
		  if(intraStepCount == 2)
		     begin
			Bit#(32) topValSet = select(intraTopVal,3);
			Bit#(8) topVal = select32to8(topValSet,3);
			Bit#(40) leftValSet = select(intraLeftVal,3);
			Bit#(8) leftVal = intra4x4SelectLeft(leftValSet,3);
			Bit#(13) tempVal = zeroExtend(topVal) + zeroExtend(leftVal);
			intraSumA <= tempVal << 4;
			intraSumB <= 0;
			intraSumC <= 0;
		     end
		  else if(intraStepCount < 11)
		     begin
			Bit#(4) xyPlusOne = intraStepCount-2;
			Bit#(4) xyPlusEight = intraStepCount+5;
			Bit#(4) sixMinusXY = 9-intraStepCount;
			Bit#(32) topValSet1 = select(intraTopVal,xyPlusEight[3:2]);
			Bit#(8) topVal1 = select32to8(topValSet1,xyPlusEight[1:0]);
			Bit#(40) leftValSet1 = select(intraLeftVal,xyPlusEight[3:2]);
			Bit#(8) leftVal1 = intra4x4SelectLeft(leftValSet1,zeroExtend(xyPlusEight[1:0]));
			Bit#(32) topValSet2=0;
			Bit#(8) topVal2;
			Bit#(40) leftValSet2;
			Bit#(8) leftVal2;
			if(intraStepCount==10)
			   begin
			      leftValSet2 = select(intraLeftVal,0);
			      leftVal2 = intra4x4SelectLeft(leftValSet2,-1);
			      topVal2 = leftVal2;
			   end
			else
			   begin
			      topValSet2 = select(intraTopVal,sixMinusXY[3:2]);
			      topVal2 = select32to8(topValSet2,sixMinusXY[1:0]);
			      leftValSet2 = select(intraLeftVal,sixMinusXY[3:2]);
			      leftVal2 = intra4x4SelectLeft(leftValSet2,zeroExtend(sixMinusXY[1:0]));
			   end
			Bit#(15) diffH = zeroExtend(topVal1) - zeroExtend(topVal2);
			Bit#(15) diffV = zeroExtend(leftVal1) - zeroExtend(leftVal2);
			intraSumB <= intraSumB + (zeroExtend(xyPlusOne) * diffH);
			intraSumC <= intraSumC + (zeroExtend(xyPlusOne) * diffV);
		     end
		  else if(intraStepCount == 11)
		     begin
			Bit#(18) tempSumB = (5*signExtend(intraSumB)) + 32;
			Bit#(18) tempSumC = (5*signExtend(intraSumC)) + 32;
			intraSumB <= signExtend(tempSumB[17:6]);
			intraSumC <= signExtend(tempSumC[17:6]);
		     end
		  else if(intraStepCount == 12)
		     begin
			Bit#(5)  positionHor = {1'b0,blockHor,pixelHor};
			Bit#(5)  positionVer = {1'b0,blockVer,pixelVer};
			Bit#(16) tempProductB = signExtend(intraSumB) * signExtend(positionHor-7);
			Bit#(16) tempProductC = signExtend(intraSumC) * signExtend(positionVer-7);
			Bit#(16) tempTotal = tempProductB + tempProductC + zeroExtend(intraSumA) + 16;
			if(tempTotal[15]==1)
			   predictedfifo.enq(0);
			else if(tempTotal[14:5] > 255)
			   predictedfifo.enq(255);
			else
			   predictedfifo.enq(tempTotal[12:5]);
			outFlag = 1;
		     end
		  else
		     $display( "ERROR Prediction: intraProcessStep intra16x16 plane unknown intraStepCount");
	       end
	    endcase
	 end
      else if(intraChromaFlag==1)
	 begin
	    //$display( "TRACE Prediction: intraProcessStep intraChroma %0d %0d %0d %0d %0d %0d %h %h %h %h %h %h %h %h",intra_chroma_pred_mode,intraChromaTopAvailable,intraChromaLeftAvailable,currMb,blockNum,pixelNum,pack(intraLeftValChroma0),pack(intraTopValChroma0),pack(intraLeftValChroma1),pack(intraTopValChroma1),intraLeftValChroma0[0],intraTopValChroma0[3][15:8],intraLeftValChroma1[0],intraTopValChroma1[3][15:8]);///////////////////
	    Vector#(9,Bit#(8)) tempLeftVec;
	    Vector#(4,Bit#(16)) tempTopVec;
	    if(blockNum[2] == 0)
	       begin
		  tempLeftVec = intraLeftValChroma0;
		  tempTopVec = intraTopValChroma0;
	       end
	    else
	       begin
		  tempLeftVec = intraLeftValChroma1;
		  tempTopVec = intraTopValChroma1;
	       end
	    case(intra_chroma_pred_mode)
	       0://dc
	       begin
		  if(intraStepCount == 2)
		     begin
			Bit#(1) useTop=0;
			Bit#(1) useLeft=0;
			if(blockNum[1:0] == 0 || blockNum[1:0] == 3)
			   begin
			      useTop = intraChromaTopAvailable;
			      useLeft = intraChromaLeftAvailable;
			   end
			else if(blockNum[1:0] == 1)
			   begin
			      if(intraChromaTopAvailable == 1)
				 useTop = 1;
			      else if(intraChromaLeftAvailable == 1)
				 useLeft = 1;
			   end
			      else if(blockNum[1:0] == 2)
				 begin
				    if(intraChromaLeftAvailable == 1)
				       useLeft = 1;
				    else if(intraChromaTopAvailable == 1)
				       useTop = 1;
				 end
		        else
			   $display( "ERROR Prediction: intraProcessStep intraChroma dc unknown blockNum");
			Bit#(10) topSum;
			Bit#(10) leftSum;
			Bit#(11) totalSum;
			if(blockHor[0] == 0)
			   topSum = zeroExtend(tempTopVec[0][15:8])+zeroExtend(tempTopVec[0][7:0])+zeroExtend(tempTopVec[1][15:8])+zeroExtend(tempTopVec[1][7:0])+2;
			else
			   topSum = zeroExtend(tempTopVec[2][15:8])+zeroExtend(tempTopVec[2][7:0])+zeroExtend(tempTopVec[3][15:8])+zeroExtend(tempTopVec[3][7:0])+2;
			if(blockVer[0] == 0)
			   leftSum = zeroExtend(tempLeftVec[1])+zeroExtend(tempLeftVec[2])+zeroExtend(tempLeftVec[3])+zeroExtend(tempLeftVec[4])+2;
			else
			   leftSum = zeroExtend(tempLeftVec[5])+zeroExtend(tempLeftVec[6])+zeroExtend(tempLeftVec[7])+zeroExtend(tempLeftVec[8])+2;
			totalSum = zeroExtend(topSum) + zeroExtend(leftSum);
			if(useTop==1 && useLeft==1)
			   intraSumA <= zeroExtend(totalSum[10:3]);
			else if(useTop==1)
			   intraSumA <= zeroExtend(topSum[9:2]);
			else if(useLeft==1)
			   intraSumA <= zeroExtend(leftSum[9:2]);
			else
			   intraSumA <= zeroExtend(8'b10000000);
		     end
		  else if(intraStepCount == 3)
		     begin
			predictedfifo.enq(intraSumA[7:0]);
			outFlag = 1;
		     end
		  else
		     $display( "ERROR Prediction: intraProcessStep intraChroma dc unknown intraStepCount");
	       end
	       1://horizontal
	       begin
		  Bit#(4) tempLeftIdx = {1'b0,blockVer[0],pixelVer} + 1;
		  predictedfifo.enq(select(tempLeftVec,tempLeftIdx));
		  outFlag = 1;
	       end
	       2://vertical
	       begin
		  Bit#(16) tempTopVal = select(tempTopVec,{blockHor[0],pixelHor[1]});
		  if(pixelHor[0] == 0)
		     predictedfifo.enq(tempTopVal[7:0]);
		  else
		     predictedfifo.enq(tempTopVal[15:8]);
		  outFlag = 1;
	       end
	       3://plane
	       begin
		  if(intraStepCount == 2)
		     begin
			Bit#(16) topValSet = tempTopVec[3];
			Bit#(8) topVal = topValSet[15:8];
			Bit#(8) leftVal = tempLeftVec[8];
			Bit#(13) tempVal = zeroExtend(topVal) + zeroExtend(leftVal);
			intraSumA <= tempVal << 4;
			intraSumB <= 0;
			intraSumC <= 0;
		     end
		  else if(intraStepCount < 7)
		     begin
			Bit#(3) xyPlusOne = truncate(intraStepCount)-2;
			Bit#(3) xyPlusFour = truncate(intraStepCount)+1;
			Bit#(4) twoMinusXY = 5-intraStepCount;
			Bit#(16) topValSet1 = select(tempTopVec,xyPlusFour[2:1]);
			Bit#(8) topVal1 = select16to8(topValSet1,xyPlusFour[0]);
			Bit#(4) tempLeftIdx1 = {1'b0,xyPlusFour} + 1;
			Bit#(8) leftVal1 = select(tempLeftVec,tempLeftIdx1);
			
			Bit#(16) topValSet2 = select(tempTopVec,twoMinusXY[2:1]);
			Bit#(8) topVal2;
			Bit#(8) leftVal2 = select(tempLeftVec,twoMinusXY+1);
			if(intraStepCount==6)
			   topVal2 = leftVal2;
			else
			   topVal2 = select16to8(topValSet2,twoMinusXY[0]);
			Bit#(15) diffH = zeroExtend(topVal1) - zeroExtend(topVal2);
			Bit#(15) diffV = zeroExtend(leftVal1) - zeroExtend(leftVal2);
			intraSumB <= intraSumB + (zeroExtend(xyPlusOne) * diffH);
			intraSumC <= intraSumC + (zeroExtend(xyPlusOne) * diffV);
			Int#(15) tempDisplayH = unpack(zeroExtend(xyPlusOne) * diffH);
			Int#(15) tempDisplayV = unpack(zeroExtend(xyPlusOne) * diffV);
			//$display( "TRACE Prediction: intraProcessStep intraChroma plane partH partV %0d %0d",tempDisplayH,tempDisplayV);////////////////////
		     end
		  else if(intraStepCount == 7)
		     begin
			Int#(15) tempDisplayH = unpack(intraSumB);
			Int#(15) tempDisplayV = unpack(intraSumC);
			//$display( "TRACE Prediction: intraProcessStep intraChroma plane H V %0d %0d",tempDisplayH,tempDisplayV);////////////////////
			Bit#(19) tempSumB = (34*signExtend(intraSumB)) + 32;
			Bit#(19) tempSumC = (34*signExtend(intraSumC)) + 32;
			intraSumB <= signExtend(tempSumB[18:6]);
			intraSumC <= signExtend(tempSumC[18:6]);
		     end
		  else if(intraStepCount == 8)
		     begin
			Bit#(4)  positionHor = {1'b0,blockHor[0],pixelHor};
			Bit#(4)  positionVer = {1'b0,blockVer[0],pixelVer};
			Bit#(17) tempProductB = signExtend(intraSumB) * signExtend(positionHor-3);
			Bit#(17) tempProductC = signExtend(intraSumC) * signExtend(positionVer-3);
			Bit#(17) tempTotal = tempProductB + tempProductC + zeroExtend(intraSumA) + 16;
			if(tempTotal[16]==1)
			   predictedfifo.enq(0);
			else if(tempTotal[15:5] > 255)
			   predictedfifo.enq(255);
			else
			   predictedfifo.enq(tempTotal[12:5]);
			outFlag = 1;
		     end
		  else
		     $display( "ERROR Prediction: intraProcessStep intraChroma plane unknown intraStepCount");
	       end
	    endcase
	 end
      else
	 $display( "ERROR Prediction: intraProcessStep unknown intrastate");

      if(outFlag==1)
	 begin
	    pixelNum <= pixelNum+1;
	    if(pixelNum == 15)
	       begin
		  if(intraChromaFlag==0)
		     begin
			blockNum <= blockNum+1;
			if(blockNum == 15)
			   begin
			      intraChromaFlag <= 1;
			      intraStepCount <= 2;
			   end
			else if(intrastate==Intra4x4)
			   intraStepCount <= 1;
		     end
		  else
		     begin
			if(blockNum == 7)
			   begin
			      blockNum <= 0;
			      intraChromaFlag <= 0;
			      intraStepCount <= 0;
			   end
			else
			   begin
			      blockNum <= blockNum+1;
			      if(intra_chroma_pred_mode==0)
				 intraStepCount <= 2;
			      else if(blockNum==3)
				 intraStepCount <= 2;
			   end
		     end
	       end
	 end
      else
	 intraStepCount <= nextIntraStepCount;
   endrule

   
   
   interface Client mem_client_intra;
      interface Get request  = fifoToGet(intraMemReqQ);
      interface Put response = fifoToPut(intraMemRespQ);
   endinterface
	 
   interface Put ioin  = fifoToPut(infifo);
   interface Get ioout = fifoToGet(outfifo);

      
endmodule

endpackage
