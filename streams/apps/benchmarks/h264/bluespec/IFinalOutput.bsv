//**********************************************************************
// Interface for Final Output
//----------------------------------------------------------------------
//
//
//

package IFinalOutput;

import H264Types::*;
import GetPut::*;
import ClientServer::*;

interface IFinalOutput;

  // Interface for inter-module io
  interface Put#(BufferControlOT) ioin;

endinterface

endpackage

