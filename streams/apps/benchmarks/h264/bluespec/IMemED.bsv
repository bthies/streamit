//**********************************************************************
// Interface for Memory for Entropy Decoding
//----------------------------------------------------------------------
//
//
//

package IMemED;

import H264Types::*;
import ClientServer::*;
import GetPut::*;

interface IMemED #(type index_size, type data_size);

  // Interface from processor to cache
  interface Server#(MemReq#(index_size,data_size),MemResp#(data_size)) mem_server;

endinterface

endpackage
