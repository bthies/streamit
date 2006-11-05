//**********************************************************************
// Interface for H264 Main Module
//----------------------------------------------------------------------
//
//
//

package IH264;

import H264Types::*;
import GetPut::*;
import ClientServer::*;

interface IH264;

   // Interface for memory, input generator
   interface Put#(InputGenOT)                    ioin;
   interface Client#(MemReq#(TAdd#(PicWidthSz,1),20),MemResp#(20)) mem_clientED;
   interface Client#(MemReq#(TAdd#(PicWidthSz,2),68),MemResp#(68)) mem_clientP_intra;
   interface Client#(MemReq#(PicWidthSz,13),MemResp#(13)) mem_clientD_parameter;
   interface Client#(MemReq#(TAdd#(PicWidthSz,5),32),MemResp#(32)) mem_clientD_data; 
   interface Get#(DeblockFilterOT) ioout;

endinterface

endpackage

