# If you ever want to generate a beamformer with the old zero/one
# weights and inputs, you can remove the RANDOM_WEIGHTS and
# RANDOM_INPUTS define tags below.  Also, DISPLAY_OUTPUT causes it to
# print the raw values received by the detector, rather than just
# doing a threshold near the end.
CPP_FLAGS = -P -DRANDOM_WEIGHTS -DRANDOM_INPUTS -DDISPLAY_OUTPUT
FILES = BeamFormer.str SerializedBeamFormer.str CoarseBeamFormer.str CoarseSerializedBeamFormer.str

default: clean ${FILES}

classes: BeamFormer.class SerializedBeamFormer.class CoarseBeamFormer.class CoarseSerializedBeamFormer.class

%.class: ${FILES}
	javac $*.java

BeamFormer.str: TemplateBeamFormer.str
	cpp ${CPP_FLAGS} TemplateBeamFormer.str > $@
	perl -pi -e 's/TemplateBeamFormer/BeamFormer/g' $@

SerializedBeamFormer.str: TemplateBeamFormer.str
	cpp ${CPP_FLAGS} -DSERIALIZED TemplateBeamFormer.str  > $@
	perl -pi -e 's/TemplateBeamFormer/SerializedBeamFormer/g' $@

CoarseBeamFormer.str: TemplateBeamFormer.str
	cpp ${CPP_FLAGS} -DCOARSE TemplateBeamFormer.str  > $@
	perl -pi -e 's/TemplateBeamFormer/CoarseBeamFormer/g' $@

CoarseSerializedBeamFormer.str: TemplateBeamFormer.str
	cpp ${CPP_FLAGS} -DSERIALIZED -DCOARSE TemplateBeamFormer.str  > $@
	perl -pi -e 's/TemplateBeamFormer/CoarseSerializedBeamFormer/g' $@

clean:
# don't remove our output files!
	rm -f BeamFormer.* `find . -name "SerializedBeamFormer.*" | grep -v SerializedBeamFormer.out` `find . -name "Coarse*BeamFormer.*" | grep -v CoarseSerializedBeamFormer.out`
