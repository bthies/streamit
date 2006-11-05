//**********************************************************************
// Interface for Deblocking Filter
//----------------------------------------------------------------------
//
//
//

package IDeblockFilter;

import H264Types::*;
import GetPut::*;
import ClientServer::*;

interface IDeblockFilter;

   // Interface for inter-module io
   interface Put#(EntropyDecOT) ioin;
   interface Get#(DeblockFilterOT) ioout;
	 
   // Interface for module to memory
   interface Client#(MemReq#(TAdd#(PicWidthSz,5),32),MemResp#(32)) mem_client_data;
   interface Client#(MemReq#(PicWidthSz,13),MemResp#(13)) mem_client_parameter;
     
endinterface

endpackage

