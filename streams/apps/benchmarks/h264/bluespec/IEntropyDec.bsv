//**********************************************************************
// Interface for Entropy Decoder
//----------------------------------------------------------------------
//
//
//

package IEntropyDec;

import H264Types::*;
import GetPut::*;
import ClientServer::*;

interface IEntropyDec;

  // Interface for inter-module io
  interface Put#(NalUnwrapOT) ioin;
  interface Get#(EntropyDecOT) ioout;

  // Interface for module to memory
  interface Client#(MemReq#(TAdd#(PicWidthSz,1),20),MemResp#(20)) mem_client;

endinterface

endpackage

