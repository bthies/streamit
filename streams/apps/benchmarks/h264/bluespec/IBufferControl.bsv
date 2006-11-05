//**********************************************************************
// Interface for Buffer Controller
//----------------------------------------------------------------------
//
//
//

package IBufferControl;

import H264Types::*;
import GetPut::*;
import ClientServer::*;

interface IBufferControl;

   // Interface for inter-module io
   interface Put#(DeblockFilterOT) ioin;
   interface Get#(BufferControlOT) ioout;
     
endinterface

endpackage

