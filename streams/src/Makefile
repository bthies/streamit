include $(STREAMIT_HOME)/misc/Makefile.vars

JAVAC = jikes

KOPI_SRCS = \
  at/dms/kjc/Main.java 

KOPI_TARGET = \
  at/dms/kjc/Main.class

KOPI_GENERATED = \
  at/dms/kjc/KjcOptions.java \
  at/dms/kjc/KjcTokenTypes.java \
  at/dms/kjc/KjcScanner.java \
  at/dms/kjc/KjcParser.java

KOPI_JFLEX_TARGET = \
  ../3rdparty/JFlex/Main.class

KOPI_ANTLR_TARGET = \
  at/dms/compiler/tools/antlr/compiler/Main.class

KOPI_OPTGEN_TARGET = \
  at/dms/compiler/tools/optgen/Main.class \
  at/dms/compiler/tools/antlr/runtime/CommonToken.class

KOPI_LEXGEN_TARGET = \
  at/dms/compiler/tools/lexgen/Main.class \
  at/dms/compiler/tools/antlr/runtime/CommonToken.class

KOPI_TOOL_TARGET = \
  $(KOPI_JFLEX_TARGET) \
  $(KOPI_ANTLR_TARGET) \
  $(KOPI_OPTGEN_TARGET) \
  $(KOPI_LEXGEN_TARGET)

KOPI_JFLEX_SRCS = \
  ../3rdparty/JFlex/Main.java

KOPI_ANTLR_SRCS = \
  at/dms/compiler/tools/antlr/compiler/Main.java

KOPI_OPTGEN_SRCS = \
  at/dms/compiler/tools/optgen/Main.java \
  at/dms/compiler/tools/antlr/runtime/CommonToken.java

KOPI_LEXGEN_SRCS = \
  at/dms/compiler/tools/lexgen/Main.java \
  at/dms/compiler/tools/antlr/runtime/CommonToken.java

KOPI_TOOL_SRCS = \
  $(KOPI_JFLEX_SRCS) \
  $(KOPI_ANTLR_SRCS) \
  $(KOPI_OPTGEN_SRCS) \
  $(KOPI_LEXGEN_SRCS)

STREAMIT_GENERATED = \
  streamit/frontend/StreamItLex.java \
  streamit/frontend/StreamItParserFE.java

# these are used by some streamit applications, but not called
# from the library directly
STREAMIT_LIBRARIES = \
  streamit/library/io/DataPrinter.class \
  streamit/library/io/FileReader.class \
  streamit/library/io/FileWriter.class \
  streamit/library/Structure.class \
  streamit/library/Portal.class

# these are invoked by reflection from StreaMITMain
STREAMIT_BACKENDS = \
  at/dms/kjc/raw/RawBackend.class \
  at/dms/kjc/rstream/StrToRStream.class \
  at/dms/kjc/sir/lowering/Flattener.class
ifeq ($(CAG_BUILD),1)
STREAMIT_BACKENDS += \
  at/dms/kjc/spacetime/SpaceTimeBackend.class \
  at/dms/kjc/cluster/ClusterBackend.class \
  at/dms/kjc/sir/SIRToStreamIt.class 
endif

JAVA_TARGET = \
  $(KOPI_TARGET) \
  $(KOPI_TOOL_TARGET) \
  $(STREAMIT_LIBRARIES) \
  $(STREAMIT_BACKENDS) \
  streamit/library/StreamIt.class \
  streamit/library/StreamItFilter.class \
  streamit/library/StreamItFeedbackLoop.class \
  streamit/library/StreamItPhasedFilter.class \
  streamit/library/StreamItPipeline.class \
  streamit/library/StreamItSplitJoin.class \
  streamit/frontend/ToJava.class \
  streamit/frontend/ToKopi.class

JAVA_SRCS = \
  $(KOPI_SRCS) \
  $(KOPI_GENERATED) \
  $(KOPI_TOOL_SRCS) \
  $(STREAMIT_GENERATED) \
  streamit/library/StreamIt.java

DEBUG_OPT = -g

JAVA_OPT = -nowarn +F -source 1.4

MAKEFILE_DEPS = \
  $(JAVA_SRCS)

include $(STREAMIT_HOME)/misc/Makefile.std

jar:
	rm -f streamit.jar streamit-lib.jar
	$(MAKE) streamit.jar streamit-lib.jar

streamit.jar: $(JAVA_TARGET)
	rm -rf classes
	mkdir classes
	find . -name '*.class' | grep -v './classes/' | grep -v './streamit/eclipse/' | cpio -p -d -V classes
	$(STREAMIT_HOME)/misc/get-antlr antlr.jar
	cd classes && jar xf ../antlr.jar
	find . -name '*.properties' | cpio -d -p -V classes
	echo "Main-Class: at.dms.kjc.Main" > manifest.streamit
	jar cf $@ -C classes .
	jar umf manifest.streamit $@
	rm -rf classes
	rm -rf manifest.streamit

# same as streamit.jar, but also includes eclipse classes
streamit-eclipse.jar: $(JAVA_TARGET)
	rm -rf classes
	mkdir classes
	find . -name '*.class' | grep -v './classes/' | cpio -p -d -V classes
	$(STREAMIT_HOME)/misc/get-antlr antlr.jar
	cd classes && jar xf ../antlr.jar
	find . -name '*.properties' | cpio -d -p -V classes
	echo "Main-Class: at.dms.kjc.Main" > manifest.streamit
	jar cf $@ -C classes .
	jar umf manifest.streamit $@
	rm -rf classes
	rm -rf manifest.streamit

# only includes streamit frontend, library, scheduler classes
streamit-lib.jar: $(JAVA_TARGET)
	rm -rf classes
	mkdir classes
	find streamit -name '*.class' | grep -v './streamit/eclipse/' | cpio -d -p -V classes
	$(STREAMIT_HOME)/misc/get-antlr antlr.jar
	cd classes && jar xf ../antlr.jar
	jar cf $@ -C classes .
	rm -rf classes

at/dms/kjc/KjcScanner.java : at/dms/compiler/resource/skeleton.shared at/dms/kjc/Kjc.flex $(KOPI_JFLEX_TARGET)
	@rm -f at/dms/kjc/KjcScanner.java
	java JFlex.Main -d at/dms/kjc/ -skel at/dms/compiler/resource/skeleton.shared at/dms/kjc/Kjc.flex

at/dms/kjc/KjcTokenTypes.java : at/dms/kjc/Kjc.t $(KOPI_LEXGEN_TARGET)
	@rm -f at/dms/kjc/KjcTokenTypes.java
	cd $(dir $<) && \
	java at.dms.compiler.tools.lexgen.Main -ditf $(notdir $<)

at/dms/kjc/KjcParser.java : at/dms/kjc/Kjc.g at/dms/kjc/KjcTokenTypes.java $(KOPI_ANTLR_TARGET)
	@rm -f at/dms/kjc/KjcParser.java
	cd $(dir $<) && \
	java -mx64m at.dms.compiler.tools.antlr.compiler.Main $(notdir $<)

at/dms/kjc/KjcOptions.java : at/dms/kjc/KjcOptions.opt $(KOPI_OPTGEN_TARGET)

streamit/frontend/ToJava.java : streamit/frontend/StreamItLex.java \
	streamit/frontend/StreamItParserFE.java
