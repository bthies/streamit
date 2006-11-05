//**********************************************************************
// Interface for Inverse Quantizer and Inverse Transformer
//----------------------------------------------------------------------
//
//
//

package IInverseTrans;

import H264Types::*;
import GetPut::*;
import ClientServer::*;

interface IInverseTrans;

  // Interface for inter-module io
  interface Put#(EntropyDecOT) ioin;
  interface Get#(EntropyDecOT) ioout;

endinterface

endpackage

