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
  3rdparty/JFlex/Main.class

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
  3rdparty/JFlex/Main.java

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
  streamit/frontend/StreamItParser.java \
  streamit/frontend/StreamItParserFE.java

# these are used by some streamit applications, but not called
# from the library directly
STREAMIT_LIBRARIES = \
  ../library/java/streamit/io/DataPrinter.class \
  ../library/java/streamit/io/FileReader.class \
  ../library/java/streamit/io/FileWriter.class \
  ../library/java/streamit/Structure.class

JAVA_TARGET = \
  $(KOPI_TARGET) \
  $(KOPI_TOOL_TARGET) \
  $(STREAMIT_LIBRARIES) \
  ../library/java/streamit/StreamIt.class \
  streamit/frontend/ToJava.class

JAVA_SRCS = \
  $(KOPI_SRCS) \
  $(KOPI_GENERATED) \
  $(KOPI_TOOL_SRCS) \
  $(STREAMIT_GENERATED) \
  ../library/java/streamit/StreamIt.java

DEBUG_OPT = -g

JAVA_OPT = -nowarn +F

MAKEFILE_DEPS = \
  $(JAVA_SRCS)

include $(STREAMIT_HOME)/misc/Makefile.std

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

