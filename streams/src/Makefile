###############################################################################
# Revised Makefile for the java code in the compiler: packages that are 
# sub-directories of src.
#
# Using Makefile.  In directory src:
#   make           To make sure your .class files are up to date,
#                  and the .java files that are built from other files
#                  (lexers, parsers, KjcOptions.java)
#   make all       Same as "make"
#   make clean     To remove all .class files in src and its subdirectories.
#   make veryclean To remove all .class files and those .java files that can
#                  be rebuilt.
#   make jar       Make jar files for StreamIt compiler and library.
#                  Not tested recently.
#
# Maintaining Makefile
#   Makefile now agressively compiles all .java files in at, streamit/frontend
#   streamit/library streamit/misc streamit/scheduler1 streamit/scheduler2.
#   If some package under at becomes obsolete and you no longer want it 
#   compiled then you should CVS remove it.  If you add a package anywhere in
#   the at tree, the java files in that package will be compile bt Makefile.
#
# What a Make all does:
#   First recreates any .java file that is made from other files an is
#   out of date.  Then agressively feeds all .java files -- in directories
#   neded for the compiler -- to javac.
#
# Credits:
#   Original Makefile / Makefile.std by Mikal Karczmarek with many updates
#    by Andrew, Dave , and Bill.
###############################################################################

include $(STREAMIT_HOME)/misc/Makefile.vars

# Under Cygwin, translate the CLASSPATH before sending it to java(c)
ifeq ($(STRC_HOST_TYPE), x86-windows)
  TRANSLATED_PATH = $(shell cygpath -pw "$(CLASSPATH)")
  JAVAC           = javac -cp "$(TRANSLATED_PATH)"
  JAVA            = java -cp "$(TRANSLATED_PATH)"
else
  JAVAC           = javac
  JAVA            = java
endif

DEBUG_OPT       = -g
JAVA_VERSION    = -source 1.5 -target 1.5
JAVA_OPT        = -nowarn $(JAVA_VERSION) 
JAVAC_FLAGS     = $(JAVA_OPT) $(DEBUG_OPT)
RM              = rm -f

###############################################################################
# The usual targets...
###############################################################################

#.PHONY: all clean jar

all: $(JAVA_SRCS)
	$(MAKE) $(JAVA_SRCS)
	$(MAKE) moreall

moreall:
	(cd streamit/frontend; `find . -name "*.java" | xargs -tn1 $(JAVAC)`)
	(cd streamit/library; `find . -name "*.java" | xargs -tn1 $(JAVAC)`)
	(cd streamit/misc; `find . -name "*.java" | xargs -tn1 $(JAVAC)`)
	(cd streamit/scheduler1; `find . -name "*.java" | xargs -tn1 $(JAVAC)`)
	(cd streamit/scheduler2; `find . -name "*.java" | xargs -tn1 $(JAVAC)`)
	(cd at; `find . -name "*.java" | xargs -tn1 $(JAVAC)`)

%.class: %.java
	$(JAVAC) $(JAVAC_FLAGS) $<

%.java : %.opt
	@$(RM) $*.java
	cd $(dir $<) && \
	$(JAVA) at.dms.compiler.tools.optgen.Main $(notdir $<)

%.java %TokenTypes.txt: %.g
	@$(RM) $*.java
	cd $(dir $<) && \
	$(JAVA) antlr.Tool $(notdir $<)


###############################################################################
# Top level parts of the compiler: initially attempted to make from top down
# based on fact the Java compilers will recompile used code as needed.
#
# However, going to Java 5 we had problems: jikes could build dependencies
# but could not handle -source 1.5.  Javamake (unsupported from SUN) was buggy.
# so only easy option was to javac everything.
#
# Still need some dependencies for building .java source files
###############################################################################

KOPI_SRCS = \
  at/dms/kjc/Main.java 

KOPI_TARGET = \
  at/dms/kjc/Main.class

KOPI_GENERATED = \
  at/dms/kjc/KjcOptions.java \
  at/dms/kjc/KjcTokenTypes.java \
  at/dms/kjc/KjcScanner.java \
  at/dms/kjc/KjcParser.java

KOPI_ANTLR_TARGET = \
  at/dms/compiler/tools/antlr/compiler/Main.class

KOPI_OPTGEN_TARGET = \
  at/dms/compiler/tools/optgen/Main.class \
  at/dms/compiler/tools/antlr/runtime/CommonToken.class

KOPI_LEXGEN_TARGET = \
  at/dms/compiler/tools/lexgen/Main.class \
  at/dms/compiler/tools/antlr/runtime/CommonToken.class

KOPI_TOOL_TARGET = \
  $(KOPI_ANTLR_TARGET) \
  $(KOPI_OPTGEN_TARGET) \
  $(KOPI_LEXGEN_TARGET)

KOPI_ANTLR_SRCS = \
  at/dms/compiler/tools/antlr/compiler/Main.java

KOPI_OPTGEN_SRCS = \
  at/dms/compiler/tools/optgen/Main.java \
  at/dms/compiler/tools/antlr/runtime/CommonToken.java

KOPI_LEXGEN_SRCS = \
  at/dms/compiler/tools/lexgen/Main.java \
  at/dms/compiler/tools/antlr/runtime/CommonToken.java

KOPI_TOOL_SRCS = \
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
  streamit/library/Portal.class \
  streamit/library/Global.class \
  streamit/library/Helper.class \
  streamit/library/NativeHelper.class \
  streamit/library/float2.class \
  streamit/library/float3.class \
  streamit/library/float4.class

# these are invoked by reflection from StreaMITMain
STREAMIT_BACKENDS = \
  at/dms/kjc/raw/RawBackend.class \
  at/dms/kjc/rstream/StrToRStream.class \
  at/dms/kjc/sir/lowering/Flattener.class
ifeq ($(CAG_BUILD),1)
STREAMIT_BACKENDS += \
  at/dms/kjc/spacetime/SpaceTimeBackend.class \
  at/dms/kjc/cluster/ClusterBackend.class \
  at/dms/kjc/sir/SIRToStreamIt.class \
  at/dms/kjc/spacedynamic/SpaceDynamicBackend.class
endif

JAVA_SRCS = \
  $(KOPI_SRCS) \
  $(KOPI_GENERATED) \
  $(KOPI_TOOL_SRCS) \
  $(STREAMIT_GENERATED)



###############################################################################
# Created java files: 
###############################################################################

at/dms/kjc/KjcScanner.java : at/dms/compiler/resource/skeleton.shared \
	at/dms/kjc/Kjc.flex  \
	../3rdparty/java_cup/runtime/Scanner.class \
	../3rdparty/java_cup/runtime/lr_parser.class
	@# may need to remove old version before rebuilding
	@rm -f at/dms/kjc/KjcScanner.java
	$(JAVA) JFlex.Main -d at/dms/kjc/ -skel at/dms/compiler/resource/skeleton.shared at/dms/kjc/Kjc.flex

at/dms/kjc/KjcTokenTypes.java : at/dms/kjc/Kjc.t $(KOPI_LEXGEN_TARGET)
	@# may need to remove old version before rebuilding
	@rm -f at/dms/kjc/KjcTokenTypes.java
	cd $(dir $<) && \
	$(JAVA) at.dms.compiler.tools.lexgen.Main -ditf $(notdir $<)

at/dms/kjc/KjcParser.java : at/dms/kjc/Kjc.g at/dms/kjc/KjcTokenTypes.java $(KOPI_ANTLR_TARGET)
	@# may need to remove old version before rebuilding
	@rm -f at/dms/kjc/KjcParser.java
	cd $(dir $<) && \
	$(JAVA) -mx64m at.dms.compiler.tools.antlr.compiler.Main $(notdir $<)


###############################################################################
# Additional dependencies
###############################################################################

at/dms/kjc/KjcOptions.java : at/dms/kjc/KjcOptions.opt $(KOPI_OPTGEN_TARGET)

streamit/frontend/StreamItParserFE.java : streamit/frontend/StreamItLexTokenTypes.txt

streamit/frontend/ToJava.java : streamit/frontend/StreamItLex.java \
	streamit/frontend/StreamItParserFE.java


###############################################################################
# Additional targets, not attempted for a while. 
###############################################################################

clean:
	$(FIND) . -name '*.class' -print | $(XARGS) $(RM) 

veryclean: clean
	$(RM) $(KOPI_GENERATED)
	$(RM) $(STREAMIT_GENERATED)

jar:
	$(RM) streamit.jar streamit-lib.jar
	$(MAKE) streamit.jar streamit-lib.jar

streamit.jar: all
	$(RM) -r classes
	mkdir classes
	find . -name '*.class' | grep -v './classes/' | grep -v './streamit/eclipse/' | cpio -p -d -V classes
	$(STREAMIT_HOME)/misc/get-antlr antlr.jar
	cd classes && jar xf ../antlr.jar
	find . -name '*.properties' | cpio -d -p -V classes
	echo "Main-Class: at.dms.kjc.Main" > manifest.streamit
	jar cf $@ -C classes .
	jar umf manifest.streamit $@
	$(RM) -r classes
	$(RM) -r manifest.streamit

# same as streamit.jar, but also includes eclipse classes
streamit-eclipse.jar: all
	$(RM) -r classes
	mkdir classes
	find . -name '*.class' | grep -v './classes/' | cpio -p -d -V classes
	$(STREAMIT_HOME)/misc/get-antlr antlr.jar
	cd classes && jar xf ../antlr.jar
	find . -name '*.properties' | cpio -d -p -V classes
	echo "Main-Class: at.dms.kjc.Main" > manifest.streamit
	jar cf $@ -C classes .
	jar umf manifest.streamit $@
	$(RM) -r classes
	$(RM) -r manifest.streamit

# only includes streamit frontend, library, scheduler classes
streamit-lib.jar: all
	$(RM) -r classes
	mkdir classes
	find streamit -name '*.class' | grep -v './streamit/eclipse/' | cpio -d -p -V classes
	$(STREAMIT_HOME)/misc/get-antlr antlr.jar
	cd classes && jar xf ../antlr.jar
	jar cf $@ -C classes .
	$(RM) -r classes

