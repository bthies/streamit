FILES = BeamFormer.java SerializedBeamFormer.java CoarseBeamFormer.java CoarseSerializedBeamFormer.java

default: clean ${FILES}

classes: ${FILES} BeamFormer.class SerializedBeamFormer.class CoarseBeamFormer.class CoarseSerializedBeamFormer.class

%.class:
	javac $*.java

BeamFormer.java:
	cpp -P TemplateBeamFormer.java > $@
	perl -pi -e 's/TemplateBeamFormer/BeamFormer/g' $@

SerializedBeamFormer.java:
	cpp -P -DSERIALZED TemplateBeamFormer.java  > $@
	perl -pi -e 's/TemplateBeamFormer/SerializedBeamFormer/g' $@

CoarseBeamFormer.java:
	cpp -P -DCOARSE TemplateBeamFormer.java  > $@
	perl -pi -e 's/TemplateBeamFormer/CoarseBeamFormer/g' $@

CoarseSerializedBeamFormer.java:
	cpp -P -DSERIALZED -DCOARSE TemplateBeamFormer.java  > $@
	perl -pi -e 's/TemplateBeamFormer/CoarseSerializedBeamFormer/g' $@

clean:
	rm -f BeamFormer.* SerializedBeamFormer.* Coarse*BeamFormer.*
