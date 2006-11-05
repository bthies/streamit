//**********************************************************************
// Interface for nC Calculator
//----------------------------------------------------------------------
//
//
//

package ICalc_nC;

import H264Types::*;
import GetPut::*;
import ClientServer::*;

interface Calc_nC;
   method Action  initialize_picWidth( Bit#(PicWidthSz) picWidthInMb );
   method Action  initialize( Bit#(PicAreaSz) firstMbAddr );
   method Action  loadMb( Bit#(PicAreaSz) mbAddr );
   method Bit#(5) nCcalc_luma( Bit#(4) microBlockNum );
   method Bit#(5) nCcalc_chroma( Bit#(3) microBlockNum );
   method Action  nNupdate_luma( Bit#(4) microBlockNum, Bit#(5) updataVal );
   method Action  nNupdate_chroma( Bit#(3) microBlockNum, Bit#(5) updataVal );
   method Action  nNupdate_pskip( Bit#(PicAreaSz) mb_skip_run );
   method Action  nNupdate_ipcm();
   interface Client#(MemReq#(TAdd#(PicWidthSz,1),20),MemResp#(20)) mem_client;
endinterface

endpackage

