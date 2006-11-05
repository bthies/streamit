//**********************************************************************
// H264 Main Module
//----------------------------------------------------------------------
//
//

package mkH264;

import H264Types::*;
import IH264::*;
import INalUnwrap::*;
import IEntropyDec::*;
import IInverseTrans::*;
import IPrediction::*;
import IDeblockFilter::*;
import mkNalUnwrap::*;
import mkEntropyDec::*;
import mkInverseTrans::*;
import mkPrediction::*;
import mkDeblockFilter::*;

import Connectable::*;
import GetPut::*;
import ClientServer::*;

(* synthesize *)
module mkH264( IH264 );

   // Instantiate the modules

   INalUnwrap     nalunwrap     <- mkNalUnwrap();
   IEntropyDec    entropydec    <- mkEntropyDec();
   IInverseTrans  inversetrans  <- mkInverseTrans();
   IPrediction    prediction    <- mkPrediction();
   IDeblockFilter deblockfilter <- mkDeblockFilter();

   // Internal connections
   
   mkConnection( nalunwrap.ioout, entropydec.ioin );
   mkConnection( entropydec.ioout, inversetrans.ioin );
   mkConnection( inversetrans.ioout, prediction.ioin );
   mkConnection( prediction.ioout, deblockfilter.ioin );

   // Interface to input generator
   interface ioin = nalunwrap.ioin;
   
   // Memory interfaces
   interface mem_clientED          = entropydec.mem_client; 
   interface mem_clientP_intra     = prediction.mem_client_intra;
   interface mem_clientD_data      = deblockfilter.mem_client_data;
   interface mem_clientD_parameter = deblockfilter.mem_client_parameter;

   // Interface for output 
   interface ioout = deblockfilter.ioout;
      
endmodule

endpackage
