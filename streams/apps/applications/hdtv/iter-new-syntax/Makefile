LIB_FILES = TrellisEncoder.str TrellisDecoder.str PreCoder.str PreDecoder.str UngerboeckEncoder.str UngerboeckDecoder.str ReedSolomonEncoder.str ReedSolomonDecoder.str ConvolutionalInterleaver.str ConvolutionalDeinterleaver.str TrellisDecoderPipeline.str TrellisEncoderPipeline.str SymbolMapper.str SymbolUnMapper.str DelayPipeline.str DataReorder.str Bitifier.str UnBitifier.str NoiseSource.str

SOURCES = HDTV.str $(LIB_FILES)

COMPILER = strc --smp 1 -nodatacollapse -stats

all:
	$(COMPILER) $(SOURCES)
