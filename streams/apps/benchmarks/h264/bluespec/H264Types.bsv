//**********************************************************************
// H264 Types
//----------------------------------------------------------------------
// 
//
//


package H264Types;


typedef 7   PicWidthSz;//number of bits to represent the horizontal position of a MB
typedef 7   PicHeightSz;//number of bits to represent the vertical position of a MB
typedef 14  PicAreaSz;//number of bits to represent the 2D position of a MB (max 16)
Bit#(PicWidthSz) maxPicWidthInMB=127;//(2^PicWidthSz)-1


typedef union tagged                
{
 void P_L0_16x16;
 void P_L0_L0_16x8;
 void P_L0_L0_8x16;
 void P_8x8;
 void P_8x8ref0;
 void I_NxN;
 struct{
 Bit#(2) intra16x16PredMode;
 Bit#(2) codedBlockPatternChroma;
 Bit#(1) codedBlockPatternLuma;
 }I_16x16;	  
 void I_PCM;
 void P_Skip;
} MbType deriving(Eq,Bits);


typedef enum
{
 Pred_L0,
 Intra_4x4,
 Intra_16x16,
 NA
} MbPartPredModeType deriving(Eq,Bits);


typedef Bit#(53) Buffer;//not sure size
typedef Bit#(7) Bufcount;
Nat buffersize = 53;//not sure size



function MbPartPredModeType mbPartPredMode( MbType mbtype, Bit#(1) mbPartIdx );
   if(mbPartIdx == 1)
      begin
	 if(mbtype == P_L0_L0_16x8 || mbtype == P_L0_L0_8x16)
	    return Pred_L0;
	 else
	    return NA;
      end
   else
      begin
	 if(mbtype==P_L0_16x16 || mbtype==P_L0_L0_16x8 || mbtype==P_L0_L0_8x16 || mbtype==P_Skip)
	    return Pred_L0;
	 else if(mbtype == I_NxN)
	    return Intra_4x4;
	 else if(mbtype == P_8x8 || mbtype == P_8x8ref0 || mbtype == I_PCM )
	    return NA;
	 else
	    return Intra_16x16;
      end
endfunction


function Bit#(3) numMbPart( MbType mbtype );
   if(mbtype == P_L0_16x16  || mbtype == P_Skip)
      return 1;
   else if(mbtype == P_L0_L0_16x8 || mbtype == P_L0_L0_8x16)
      return 2;
   else if(mbtype == P_8x8 || mbtype == P_8x8ref0)
      return 4;
   else
      return 0;//should never happen
endfunction


function Bit#(3) numSubMbPart( Bit#(2) submbtype );
   if(submbtype == 0)
      return 1;
   else if(submbtype == 1 || submbtype == 2)
      return 2;
   else
      return 4;
endfunction


//----------------------------------------------------------------------
// Inter-module FIFO types
//----------------------------------------------------------------------


typedef union tagged                
{
 Bit#(8) DataByte;
 void    EndOfFile;
}
InputGenOT deriving(Eq,Bits);


typedef union tagged                
{
 void    NewUnit;
 Bit#(8) RbspByte;
 void    EndOfFile;
}
NalUnwrapOT deriving(Eq,Bits);


typedef union tagged                
{
 Bit#(8)  NewUnit;

 ////Sequence Parameter Set
 Bit#(5)  SPSseq_parameter_set_id;//ue 0 to 31
 Bit#(5)  SPSlog2_max_frame_num;//ue+4 4 to 16
 Bit#(2)  SPSpic_order_cnt_type;//ue 0 to 2
 Bit#(5)  SPSlog2_max_pic_order_cnt_lsb;//ue+4 4 to 16
 Bit#(1)  SPSdelta_pic_order_always_zero_flag;//u(1)
 Bit#(32) SPSoffset_for_non_ref_pic;//se -2^31 to 2^31-1
 Bit#(32) SPSoffset_for_top_to_bottom_field;//se -2^31 to 2^31-1
 Bit#(8)  SPSnum_ref_frames_in_pic_order_cnt_cycle;//ue 0 to 255
 Bit#(32) SPSoffset_for_ref_frame;//se -2^31 to 2^31-1
 Bit#(16) SPSnum_ref_frames;//ue 0 to MaxDpbSize (depends on Level)
 Bit#(1)  SPSgaps_in_frame_num_allowed_flag;//u(1)
 Bit#(PicWidthSz) SPSpic_width_in_mbs;//ue+1 1 to ?
 Bit#(PicHeightSz) SPSpic_height_in_map_units;//ue+1 1 to ?
//// Bit#(1)  SPSframe_mbs_only_flag//u(1) (=1 for baseline)
 Bit#(1)  SPSdirect_8x8_inference_flag;//u(1)
 Bit#(1)  SPSframe_cropping_flag;//u(1)
 Bit#(16) SPSframe_crop_left_offset;//ue 0 to ?
 Bit#(16) SPSframe_crop_right_offset;//ue 0 to ?
 Bit#(16) SPSframe_crop_top_offset;//ue 0 to ?
 Bit#(16) SPSframe_crop_bottom_offset;//ue 0 to ?

 ////Picture Parameter Set
 Bit#(8)  PPSpic_parameter_set_id;//ue 0 to 255
 Bit#(5)  PPSseq_parameter_set_id;//ue 0 to 31
//// Bit#(1)  PPSentropy_coding_mode_flag//u(1) (=0 for baseline)
 Bit#(1)  PPSpic_order_present_flag;//u(1)
//// Bit#(4)  PPSnum_slice_groups;//ue+1 1 to 8 (=1 for main)
////some info if PPSnum_slice_groups>1 (not in main)
 Bit#(6)  PPSnum_ref_idx_l0_active;//ue+1 1 to 32
 Bit#(6)  PPSnum_ref_idx_l1_active;//ue+1 1 to 32
//// Bit#(1)  PPSweighted_pred_flag;//u(1) (=0 for baseline)
//// Bit#(2)  PPSweighted_bipred_flag;//u(2) (=0 for baseline)
 Bit#(7)  PPSpic_init_qp;//se+26 0 to 51
 Bit#(7)  PPSpic_init_qs;//se+26 0 to 51
 Bit#(5)  PPSchroma_qp_index_offset;//se -12 to 12
 Bit#(1)  PPSdeblocking_filter_control_present_flag;//u(1)
 Bit#(1)  PPSconstrained_intra_pred_flag;//u(1)
//// Bit#(1)  PPSredundant_pic_cnt_present_flag;//u(1) (=0 for main)

 ////Slice Header
 Bit#(PicAreaSz) SHfirst_mb_in_slice;//ue 0 to PicSizeInMbs-1
 Bit#(4)  SHslice_type;//ue 0 to 9
 Bit#(8)  SHpic_parameter_set_id;//ue 0 to 255
 Bit#(16) SHframe_num;//u(log2_max_frame_num)
 Bit#(16) SHidr_pic_id;//ue 0 to 65535
 Bit#(16) SHpic_order_cnt_lsb;//u(log2_max_pic_order_cnt_lsb)
 Bit#(32) SHdelta_pic_order_cnt_bottom;//se -2^31 to 2^31-1
 Bit#(32) SHdelta_pic_order_cnt0;//se -2^31 to 2^31-1
 Bit#(32) SHdelta_pic_order_cnt1;//se -2^31 to 2^31-1
 Bit#(1)  SHnum_ref_idx_active_override_flag;//u(1)
 Bit#(6)  SHnum_ref_idx_l0_active;//ue+1 1 to 32 (16 for frame mb)
 ////reference picture list reordering
 Bit#(1)  SHRref_pic_list_reordering_flag_l0;//u(1)
 Bit#(2)  SHRreordering_of_pic_nums_idc;//ue 0 to 3
 Bit#(17) SHRabs_diff_pic_num;//ue+1 1 to MaxPicNum
 Bit#(32) SHRlong_term_pic_num;//ue 0 to ?
 ////decoded reference picture marking
 Bit#(1)  SHDno_output_of_prior_pics_flag;//u(1)
 Bit#(1)  SHDlong_term_reference_flag;//u(1)
 Bit#(1)  SHDadaptive_ref_pic_marking_mode_flag;//u(1)
 Bit#(3)  SHDmemory_management_control_operation;//ue 0 to 6
 Bit#(32) SHDdifference_of_pic_nums;//ue+1 1 to ?
 Bit#(32) SHDlong_term_pic_num;//ue 0 to ?
 Bit#(32) SHDlong_term_frame_idx;//ue 0 to MaxLongTermFrameIdx
 Bit#(32) SHDmax_long_term_frame_idx_plus1;//ue 0 to num_ref_frames
 ////Slice Header (continued)
 Bit#(7)  SHslice_qp_delta;//se -51 to 51
 Bit#(2)  SHdisable_deblocking_filter_idc;//ue 0 to 2
 Bit#(5)  SHslice_alpha_c0_offset;//se*2 -12 to 12
 Bit#(5)  SHslice_beta_offset;//se*2 -12 to 12

 ////Slice Data
 Bit#(PicAreaSz) SDmb_skip_run;//ue 0 to PicSizeInMbs
 Bit#(PicAreaSz) SDcurrMbAddr;//ue ->process-> 0 to PicSizeInMbs
 ////macroblock layer
 MbType   SDMmbtype;//ue ->process-> MbType
 Bit#(8)  SDMpcm_sample_luma;//ue 0 to 255
 Bit#(8)  SDMpcm_sample_chroma;//ue 0 to 255
 Bit#(6)  SDMcoded_block_pattern;//me
 Bit#(7)  SDMmb_qp_delta;//se -26 to 25
 ////macroblock prediction
// Bit#(1)  SDMMprev_intra4x4_pred_mode_flag;//u(1)
 Bit#(4)  SDMMrem_intra4x4_pred_mode;//(SDMMprev_intra4x4_pred_mode_flag ? 4'b1000 : {1'b0,u(3)})
 Bit#(2)  SDMMintra_chroma_pred_mode;//ue 0 to 3
 Bit#(5)  SDMMref_idx_l0;//te 0 to num_ref_idx_active_minus1
 Bit#(16) SDMMmvd_l0;//se ? to ? (see Annex A)
 ////sub-macroblock prediction
 Bit#(2)  SDMSsub_mb_type;//ue 0 to 3
 Bit#(5)  SDMSref_idx_l0;//te 0 to num_ref_idx_active_minus1
 Bit#(16) SDMSmvd_l0;//se ? to ? (see Annex A)
 ////residual data
 Bit#(13) SDMRcoeffLevel;//cavlc output in reverse order (high frequency first)
 Bit#(5)  SDMRcoeffLevelZeros;//# of consecutive zeros (also used for ITBresidual)
 
 ////Inverse Transform Block output
 Bit#(10) ITBresidual;//residual data in regular h.264 order
 struct {Bit#(6) qpy; Bit#(6) qpc;} IBTmb_qp;//qp for luma and chroma for the current MB
 
 ////Prediction Block output
 Bit#(8)  PBoutput;//prediction+residual in regular h.264 order

 //// various delimiters
 Bit#(3)  AUDPrimaryPicType;
 void     EndOfSequence;
 void     EndOfStream;
 void     EndOfFile;
}
EntropyDecOT deriving(Eq,Bits);


typedef union tagged                
{
 Bit#(PicWidthSz) PicWidth;
 Bit#(PicHeightSz) PicHeight;
 struct {Bit#(PicAreaSz) mb; Bit#(6) pixel; Bit#(32) data;} Luma;
 struct {Bit#(PicAreaSz) mb; Bit#(1) uv; Bit#(4) pixel; Bit#(32) data;} Chroma;
 void    EndOfFrame;
 void    EndOfFile;
}
DeblockFilterOT deriving(Eq,Bits);


typedef union tagged                
{
 Bit#(128) Luma;
 Bit#(64)  Chroma;
 void      EndOfFile;
}
BufferControlOT deriving(Eq,Bits);



typedef union tagged
{
  Bit#(addrSz) LoadReq;
  struct { Bit#(addrSz) addr; Bit#(dataSz) data; } StoreReq;  
}
MemReq#( type addrSz, type dataSz ) 
deriving(Eq,Bits);

typedef union tagged
{
  Bit#(dataSz) LoadResp;
}
MemResp#( type dataSz )
deriving(Eq,Bits);



endpackage
