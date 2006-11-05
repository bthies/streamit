//**********************************************************************
// Interface for Prediction
//----------------------------------------------------------------------
//
//
//

package IPrediction;

import H264Types::*;
import GetPut::*;
import ClientServer::*;

interface IPrediction;

  // Interface for inter-module io
  interface Put#(EntropyDecOT) ioin;
  interface Get#(EntropyDecOT) ioout;

  // Interface for module to memory
  interface Client#(MemReq#(TAdd#(PicWidthSz,2),68),MemResp#(68)) mem_client_intra;

endinterface

endpackage

