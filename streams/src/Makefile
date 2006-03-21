###############################################################################
# Revised Makefile for the java code in the compiler: packages that are 
# sub-directories of src.
#
# Using Makefile.  In directory src:
#   make           To make sure your .class files are up to date,
#                  and the .java files that are built from other files
#                  (lexers, parsers, KjcOptions.java)
#   make all       Same as "make"
#   make clean     To remove all .class and .u (dependence) files in
#                   src and its subdirectories.
#   make depend    To remove and rebuild makefile.deps.  Shouldn't need to
#                  do, but something to try if a build seems screwed up.
#   make jar       Make jar files for StreamIt compiler and library.
#                  Not tested recently.
#
#   make -jN generally does not work for N more that about 4 and may not work
#   for N > 1 since the multiple copies of jikes dependence analysis running
#   simultaneously may try to access the same output files.  Using
#   make J=N allows the part of the make process that is driven off the
#   pre-computed dependencies to be run in parallel.  Even there, I see
#   occasional clashes, but it may be worth running make J=20 a few times
#   rather than waiting for make to complete: jikes seeps to spend a lot of
#   time doing I/O so running multiple copies speeds up the build.
#
#   The jikes options set up to target a Java 5 virtual machine.  If you are
#   going to target a Java 1.4 VM (our code does not run on older because of 
#   use of asserts) then:
#   make JAVA_VERSION='-source 1.4'
#   However, now that we are targeting Java 5, we will probably start to
#   introduce code that will not compile under '-source 1.4'
#
# Maintaining Makefile
#   If you have a new .java file that is built by a tool, put it into 
#   STREAMIT_GENERATED and add commands to built it in the "created java files"
#   section.
#
#   If you have a new backend, add the top-level .class file name to 
#   STREAMIT_BACKENDS  (Similarly for any other file that is included in the
#   compiler by reflection rather than as a direct dependence.)
#
#   If you add a new package that contains code used to build the compiler,
#   put the package name in PACKAGES.
#
# What a Make all does:
#   (1) First updates makefile.deps if necessary by
#       (a) making sure that all .java files in JAVA_SRCS are up-to-date.
#       (b) having jikes perform a dependency check for all .class files
#           in JAVA_TARGETS
#       (c) combining all .u files in PACKAGES, presumably created in sub-step 
#           (b) into makefile.deps.
#   (2) Has jikes re-build out-of-date .class files in JAVA_TARGETS
#   (3) Has jikes re-build out-of-date .class files with dependencies
#       on .java files specified in makefile.deps. (Is this redundant
#	with using the +F switch?) 
#
# Known Bugs:
#  (o) For some reason, after a make clean, at/dms/kjc/KjcTokenTypes.java,
#  at/dms/kjc/KjcOptions.java and at/KjcParser.java are built on both the
#  first make after the clean and the second make after the clean (but not
#  on subsequent makes unless a prerequisite changes).  This is annoying
#  not so much for the time taken for the second build of the .java files
#  but for the time needed to recreate makefile.deps because of the 
#  rebuilt .java files.
#  (o) As of latest version, changing a .java file seems to result in too
#  much recompilation.  Problem seems to be dependences through inheritance
#  hierarchy...
#
# Credits:
#   (1) Original Makefile / Makefile.std by Mikal Karczmarek with many updates
#        by Andrew, Dave , and Bill.
#   (2) The author of mmake (Jan-Henrik Haukeland) for his simple handling of 
#        dependencies.
###############################################################################

JAVAC           = jikes
DEBUG_OPT       = -g
JAVA_VERSION    = -target 1.5
JAVA_OPT        = -nowarn $(JAVA_VERSION) 
JAVAC_FLAGS     = $(JAVA_OPT) $(DEBUG_OPT)
JIKES_DEP_FLAGS	= +M +F $(JAVAC_FLAGS)
MAKEFILE_DEPEND	= makefile.deps
CAT		= cat
RM              = rm -f
SED		= sed
FIND            = find
XARGS		= xargs
J               = 1

###############################################################################
# The usual targets...
###############################################################################

#.PHONY: all clean makedeps depend jar

all: $(MAKEFILE_DEPEND)
	@# I don't know why I have to use the following form.
	@# If I don't then I get errors on making a fine in $(MAKEFILE_DEPEND)"
	@# make[1]: execvp: FOO.java: Permission denied
	@# I do not get these errors when running makefile.deps directly
	@# from the command line.
	@sh -c "/usr/uns/bin/make -j$(J) -f $(MAKEFILE_DEPEND)"

# for compiling a .class

%.class: %.java
	$(JAVAC) $(JAVAC_FLAGS) $<

# for creating a .u
# There is not a -nowrite option so .class files should be created
# There is a +F option so all .class files needed by the .class file
# corresponding to the .u target shoulf ve created, also subsidiary
# .u files should be created.

%.u: %.java
	$(JAVAC) $(JIKES_DEP_FLAGS) $<


%.java : %.opt
	@rm -f $*.java
	cd $(dir $<) && \
	java at.dms.compiler.tools.optgen.Main $(notdir $<)

%.java %TokenTypes.txt: %.g
	@rm -f $*.java
	cd $(dir $<) && \
	java antlr.Tool $(notdir $<)


###############################################################################
# Top level parts of the compiler: initially attempted to make from top down
# based on fact the Java compilers will recompile used code as needed.
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

# for tool targets, make prerequisite be .u file rathe than .class file
# the .class will be built as a side effect of the .u rule
# but .u is not build as a side effect of the .class rule
# Why do this?  If making a makefile.deps file we do not want to 
# build the tools both before and after the makefile.deps file.

KOPI_ANTLR_TARGET = \
  at/dms/compiler/tools/antlr/compiler/Main.u

KOPI_OPTGEN_TARGET = \
  at/dms/compiler/tools/optgen/Main.u \
  at/dms/compiler/tools/antlr/runtime/CommonToken.u

KOPI_LEXGEN_TARGET = \
  at/dms/compiler/tools/lexgen/Main.u \
  at/dms/compiler/tools/antlr/runtime/CommonToken.u

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

###############################################################################
# Attempt at a simple dependencies file
###############################################################################

# Packages we will compile because they contribute to compiler code
# and contain .java files.
# These are listed explicitly because they are searched for .u files
# to generate $(MAKEFILE_DEPEND)

PACKAGES = \
	streamit.scheduler2.base \
	streamit.library.iriter \
	at.dms.kjc.sir.linear.transform \
	at.dms.kjc.lir \
	streamit.scheduler2.iriter \
	at.dms.kjc.sir.lowering.fission \
	at.dms.compiler.getopt \
	streamit.library \
	streamit.scheduler2.minlatency \
	streamit.frontend.controlflow \
	at.dms.compiler.tools.optgen \
	streamit.frontend.passes \
	at.dms.kjc.sir \
	at.dms.compiler.tools.msggen \
	streamit.frontend.nodes \
	at.dms.compiler \
	at.dms.kjc.rstream \
	streamit.scheduler2.singleappearance \
	at.dms.compiler.antlr.extra \
	streamit.scheduler1.simple \
	streamit.misc \
	at.dms.compiler.antlr.runtime \
	streamit.library.jcc \
	at.dms.compiler.tools.antlr.extra \
	streamit.library.io \
	at.dms.compiler.tools.antlr.runtime \
	at.dms.kjc.linprog \
	at.dms.kjc.sir.lowering.partition \
	streamit.frontend \
	at.dms.kjc.spacetime.switchIR \
	at.dms.kjc.sir.lowering.fusion \
	at.dms.kjc.sir.stats \
	at.dms.compiler.tools.common \
	streamit.scheduler2.print \
	at.dms.kjc \
	at.dms.kjc.cluster \
	streamit.scheduler2 \
	at.dms.optimize \
	at.dms.kjc.sir.lowering.partition.dynamicprog \
	at.dms.kjc.raw \
	at.dms.kjc.sir.statespace \
	at.dms.kjc.sir.lowering.partition.linear \
	streamit.frontend.tojava \
	streamit.scheduler1 \
	at.dms.classfile \
	at.dms.kjc.iterator \
	at.dms.kjc.sir.linear \
	at.dms.kjc.sir.lowering \
	at.dms.kjc.sir.statespace.transform \
	at.dms.compiler.tools.jperf \
	at.dms.backend \
	at.dms.kjc.common \
	at.dms.util \
	streamit.scheduler2.hierarchical \
	at.dms.compiler.tools.include \
	at.dms.kjc.flatgraph \
	at.dms.kjc.sir.lowering.reordering \
	at.dms.kjc.spacetime \
	at.dms.compiler.tools.lexgen \
	at.dms.kjc.sir.linear.frequency \
	at.dms.kjc.spacedynamic \
	streamit.scheduler2.constrained \
	at.dms.kjc.flatgraph2 \
	at.dms.compiler.tools.antlr.compiler \
	at.dms.kjc.sir.lowering.partition.cache


# A marker variable for the top level directory
TOPLEVEL	= .

# Subdirectories with java files:
PACKAGE_DIRS	= $(subst .,/,$(PACKAGES)) $(TOPLEVEL)

# needed by ../misc/makefile.deps.perl
export PACKAGE_DIRS JAVAC JAVAC_FLAGS MAKEFILE_DEPEND

$(MAKEFILE_DEPEND): $(JAVA_SRCS) $(JAVA_TARGET:%.class=%.u)
	@#echo (re)making $(MAKEFILE_DEPEND)
	../misc/makefile.deps.perl

#
# target to specicfically re-make dependencies file
#
depend:
	rm -f $(MAKEFILE_DEPEND)
	$(FIND) -name "*.u" | $(XARGS) $(RM)
	$(MAKE) $(MAKEFILE_DEPEND)



###############################################################################
# Additional targets, not attempted for a while. 
###############################################################################

clean:
	$(FIND) . -name '*.class' -print | $(XARGS) $(RM) 
	$(FIND) . -name '*.u' -print | $(XARGS) $(RM)

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

###############################################################################
# Created java files: 
###############################################################################

at/dms/kjc/KjcScanner.java : at/dms/compiler/resource/skeleton.shared \
	at/dms/kjc/Kjc.flex  \
	../3rdparty/java_cup/runtime/Scanner.class \
	../3rdparty/java_cup/runtime/lr_parser.class
	@# may need to remove old version before rebuilding
	@rm -f at/dms/kjc/KjcScanner.java
	java JFlex.Main -d at/dms/kjc/ -skel at/dms/compiler/resource/skeleton.shared at/dms/kjc/Kjc.flex

at/dms/kjc/KjcTokenTypes.java : at/dms/kjc/Kjc.t $(KOPI_LEXGEN_TARGET)
	@# may need to remove old version before rebuilding
	@rm -f at/dms/kjc/KjcTokenTypes.java
	cd $(dir $<) && \
	java at.dms.compiler.tools.lexgen.Main -ditf $(notdir $<)

at/dms/kjc/KjcParser.java : at/dms/kjc/Kjc.g at/dms/kjc/KjcTokenTypes.java $(KOPI_ANTLR_TARGET)
	@# may need to remove old version before rebuilding
	@rm -f at/dms/kjc/KjcParser.java
	cd $(dir $<) && \
	java -mx64m at.dms.compiler.tools.antlr.compiler.Main $(notdir $<)


###############################################################################
# Additional dependencies
###############################################################################

at/dms/kjc/KjcOptions.java : at/dms/kjc/KjcOptions.opt $(KOPI_OPTGEN_TARGET)

streamit/frontend/StreamItParserFE.java : streamit/frontend/StreamItLexTokenTypes.txt

streamit/frontend/ToJava.java : streamit/frontend/StreamItLex.java \
	streamit/frontend/StreamItParserFE.java

