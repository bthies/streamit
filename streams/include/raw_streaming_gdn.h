#include <raw.h>

#ifndef _RAW_STREAMING_GDN_H
#define _RAW_STREAMING_GDN_H

#define kStreamingDRAM_CommandRead_GDN         7
#define kStreamingDRAM_CommandWrite_GDN        8
#define kStreamingDRAM_CommandReadStrided_GDN  9
#define kStreamingDRAM_CommandWriteStrided_GDN 10
#define kStreamingDRAM_CommandBypassRead_GDN   11
#define kStreamingDRAM_CommandBypassWrite_GDN  12

/* reads */

#define raw_streaming_dram_gdn_request_read(sampleAddress, numAddresses, transferLines)                                                                                     \
raw_streaming_dram_create_header(sampleAddress, kStreamingDRAM_CommandRead_GDN, kStreamingDRAM_NoInterleave, kStreamingDRAM_NoPresynch, ((numAddresses)+1), (transferLines))

#define raw_streaming_dram_gdn_request_read_presynched(sampleAddress, numAddresses, transferLines)                                                                          \
raw_streaming_dram_create_header(sampleAddress, kStreamingDRAM_CommandRead_GDN, kStreamingDRAM_NoInterleave, kStreamingDRAM_Presynch, ((numAddresses)+1), (transferLines))

#define raw_streaming_dram_gdn_request_read_interleaved(sampleAddress, numAddresses, transferLines, interleave)                                                             \
raw_streaming_dram_create_header(sampleAddress, kStreamingDRAM_CommandRead_GDN, (interleave), kStreamingDRAM_NoPresynch, ((numAddresses)+1), (transferLines))

#define raw_streaming_dram_gdn_request_read_interleaved_presynched(sampleAddress, numAddresses, transferLines, interleave)                                                  \
raw_streaming_dram_create_header(sampleAddress, kStreamingDRAM_CommandRead_GDN, (interleave), kStreamingDRAM_Presynch, ((numAddresses)+1), (transferLines))

#define raw_streaming_dram_gdn_request_bypass_read(sampleAddress, numWords) \
raw_streaming_dram_create_bypass_header(sampleAddress, kStreamingDRAM_CommandBypassRead_GDN, numWords)

/* strided reads */

#define raw_streaming_dram_gdn_request_read_strided(sampleAddress, numAddresses, transferLines, skip, repeat)                                                               \
raw_streaming_dram_strided_create_header(sampleAddress, kStreamingDRAM_CommandReadStrided_GDN, kStreamingDRAM_NoInterleave, kStreamingDRAM_NoPresynch, ((numAddresses)+2), (transferLines), (skip), (repeat))

#define raw_streaming_dram_gdn_request_read_strided_presynched(sampleAddress, numAddresses, transferLines, skip, repeat)                                                    \
raw_streaming_dram_strided_create_header(sampleAddress, kStreamingDRAM_CommandReadStrided_GDN, kStreamingDRAM_NoInterleave, kStreamingDRAM_Presynch, ((numAddresses)+2), (transferLines), (skip), (repeat))

#define raw_streaming_dram_gdn_request_read_strided_interleaved(sampleAddress, numAddresses, transferLines, interleave, skip, repeat)                                       \
raw_streaming_dram_strided_create_header(sampleAddress, kStreamingDRAM_CommandReadStrided_GDN, (interleave), kStreamingDRAM_NoPresynch, ((numAddresses)+2), (transferLines), (skip), (repeat))

#define raw_streaming_dram_gdn_request_read_strided_interleaved_presynched(sampleAddress, numAddresses, transferLines, interleave, skip, repeat)                            \
raw_streaming_dram_strided_create_header(sampleAddress, kStreamingDRAM_CommandReadStrided_GDN, (interleave), kStreamingDRAM_Presynch, ((numAddresses)+2), (transferLines), (skip), (repeat))


/* writes */

#define raw_streaming_dram_gdn_request_write(sampleAddress, numAddresses, transferLines)                                                                                    \
raw_streaming_dram_create_header(sampleAddress, kStreamingDRAM_CommandWrite_GDN, kStreamingDRAM_NoInterleave, kStreamingDRAM_NoPresynch, ((numAddresses)+1), (transferLines))

#define raw_streaming_dram_gdn_request_write_presynched(sampleAddress, numAddresses, transferLines)                                                                         \
raw_streaming_dram_create_header(sampleAddress, kStreamingDRAM_CommandWrite_GDN, kStreamingDRAM_NoInterleave, kStreamingDRAM_Presynch, ((numAddresses)+1), (transferLines))

#define raw_streaming_dram_gdn_request_write_interleaved(sampleAddress, numAddresses, transferLines, interleave)                                                            \
raw_streaming_dram_create_header(sampleAddress, kStreamingDRAM_CommandWrite_GDN, (interleave), kStreamingDRAM_NoPresynch, ((numAddresses)+1), (transferLines))

#define raw_streaming_dram_gdn_request_write_interleaved_presynched(sampleAddress, numAddresses, transferLines, interleave)                                                 \
raw_streaming_dram_create_header(sampleAddress, kStreamingDRAM_CommandWrite_GDN, (interleave), kStreamingDRAM_Presynch, ((numAddresses)+1), (transferLines))

#define raw_streaming_dram_gdn_request_bypass_write(sampleAddress, numWords)                                \
raw_streaming_dram_create_bypass_header(sampleAddress, kStreamingDRAM_CommandBypassWrite_GDN, numWords)

/* strided writes */

#define raw_streaming_dram_gdn_request_write_strided(sampleAddress, numAddresses, transferLines, skip, repeat)                                                              \
raw_streaming_dram_strided_create_header(sampleAddress, kStreamingDRAM_CommandWriteStrided_GDN, kStreamingDRAM_NoInterleave, kStreamingDRAM_NoPresynch, ((numAddresses)+2), (transferLines), (skip), (repeat))

#define raw_streaming_dram_gdn_request_write_strided_presynched(sampleAddress, numAddresses, transferLines, skip, repeat)                                                   \
raw_streaming_dram_strided_create_header(sampleAddress, kStreamingDRAM_CommandWriteStrided_GDN, kStreamingDRAM_NoInterleave, kStreamingDRAM_Presynch, ((numAddresses)+2), (transferLines), (skip), (repeat))

#define raw_streaming_dram_gdn_request_write_strided_interleaved(sampleAddress, numAddresses, transferLines, skip, repeat)                                                  \
raw_streaming_dram_strided_create_header(sampleAddress, kStreamingDRAM_CommandWriteStrided_GDN, (interleave), kStreamingDRAM_NoPresynch, ((numAddresses)+2), (transferLines), (skip), (repeat))

#define raw_streaming_dram_gdn_request_write_strided_interleaved_presynched(sampleAddress, numAddresses, transferLines, skip, repeat)                                       \
raw_streaming_dram_strided_create_header(sampleAddress, kStreamingDRAM_CommandWriteStrided_GDN, (interleave), kStreamingDRAM_Presynch, ((numAddresses)+2), (transferLines), (skip), (repeat))

#endif  //_RAW_STREAMING_GDN_H
