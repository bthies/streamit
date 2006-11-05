//**********************************************************************
// Interface for NAL unwrapper
//----------------------------------------------------------------------
//
//
//

package INalUnwrap;

import H264Types::*;
import GetPut::*;

interface INalUnwrap;

  // Interface for inter-module io
  interface Put#(InputGenOT) ioin;
  interface Get#(NalUnwrapOT) ioout;

endinterface

endpackage

