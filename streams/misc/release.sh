#!/bin/sh -e
#
# release.sh: assemble a StreamIt release
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: release.sh,v 1.83 2006-11-01 21:00:43 thies Exp $
#

# for script debugging: -v print line in script, -x print expanded line
set -v
#set -x

# Interesting/configurable variables:

# For a version release
VERSION=2.1
#TAG=RELEASE_2_1_01
TAG=HEAD

# For a snapshot release
#VERSION=2.1.`date +%Y%m%d`
#TAG=HEAD

test -z "$TMPDIR" && TMPDIR=/tmp
PRECIOUS=
CVSROOT="-d /projects/raw/cvsroot"

usage() {
  cat >&2 <<EOF
release.sh: assemble a StreamIt release

Usage:
  release.sh [options]

Options:
  --version (-v)   Use a particular exported version ($VERSION)
  --tag (-r)       Build a release from a CVS tag ($TAG)
  --cvsroot (-d)   Specify the CVS root directory ($CVSROOT)
  --tmpdir         Use a different build directory ($TMPDIR)
  --antlr          Location of the ANTLR jar file ($ANTLRJAR)
  --precious (-k)  Keep the working directory
EOF
}

# Command-line options:
while test -n "$1"
do
  OPT="$1"; shift
  case $OPT in
    --version|-v) VERSION="$1"; shift;;
    --tag|-r) TAG="$1"; shift;;
    --tmpdir) TMPDIR="$1"; shift;;
    -d) CVSROOT="$1" shift;;    #trick: CVS needs the "-d"
    --precious|-k) PRECIOUS=yes;;
    *) usage; exit 1;;
  esac
done

# Temporary directory:
WORKING=$TMPDIR/streamit-$USER-$$
mkdir $WORKING
SRCDIR=$WORKING/streams
STREAMIT_HOME=$SRCDIR
SRCTAR=$WORKING/streamit-src-$VERSION.tar.gz
BINDIR=$WORKING/streamit-$VERSION
BINTAR=$WORKING/streamit-$VERSION.tar.gz
export STREAMIT_HOME

# Helper function to add a list of directories to $DIRS
builddirs() {
  PREFIX="$1"; shift
  while test -n "$1"; do DIRS="$DIRS $PREFIX/$1"; shift; done
}

# Get a checked-out copy of the source tree.
mkdir $WORKING/streams
# remove README.source until it can bu updated to reflect the current compiler
#DIRS="streams/strc streams/Makefile streams/README.source"
DIRS="streams/strc streams/Makefile"
builddirs streams 3rdparty src library include misc configure.in
builddirs streams/apps benchmarks examples libraries sorts
builddirs streams/docs cookbook implementation-notes release syntax
builddirs streams/docs index.html
mkdir $WORKING/streams/javadoc

cvs $CVSROOT export -r $TAG -d $WORKING $DIRS

###############################################################################
# autoconf, and any supporting changes to our files (generating .in files)
#
# since our perl is in a non-standard place, every new perl script needs
# to be mentioned below, and in configure.in
###############################################################################

# Run autoconf to get a configure script.
cat $WORKING/streams/misc/ac_java_macros.m4 $WORKING/streams/configure.in > $WORKING/streams/configure.in2
autoconf $WORKING/streams/configure.in2 > $WORKING/streams/configure
chmod 0755 $WORKING/streams/configure
rm $WORKING/streams/misc/ac_java_macros.m4 $WORKING/streams/configure.in2

###
# Generate .in files.  
# See also the tail of configure.in to process the generated .in files.
###
INFILES="strc Makefile misc/Makefile.vars misc/dat2bin.pl misc/scripts/preprocess.perl misc/scripts/streamitdoc misc/scripts/turnOffPrints.pl misc/htmlformat.pl misc/concat_cluster_threads_cpp.pl"
for f in $INFILES; do
  if test -f "$WORKING/streams/$f"; then
    $WORKING/streams/misc/make-dot-in.pl "$WORKING/streams/$f"
  fi
done
rm -fr $WORKING/streams/misc/make-dot-in.pl

###############################################################################
# remove files that we do not want to release
###############################################################################

###
# Don't release CPLEX jar file or anything that depends on it
###
rm -rf $WORKING/streams/3rdparty/cplex/
rm -rf $WORKING/streams/src/at/dms/kjc/linprog/
rm -rf $WORKING/streams/src/at/dms/kjc/sir/lowering/partition/ILPPartitioner.java
# lpsolve is only used by removed code.
rm -fr $WORKING/streams/3rdparty/lpsolve/
# since we're not releasing eclipse, don't need jgraph
rm -fr $WORKING/streams/3rdparty/jgraph/

###
# Don't release JCC or anything that depends on it
###
# remove JCC jar file
rm -rf $WORKING/streams/3rdparty/jcc
# replace the JCC function with an error message
rm -rf $WORKING/streams/src/streamit/library/jcc/*.java
mv $WORKING/streams/src/streamit/library/jcc/StreamItToJcc.dummy $WORKING/streams/src/streamit/library/jcc/StreamItToJcc.java

# Remove .cvsignore files
rm -rf `find $WORKING -name ".cvsignore"`
# remove "calculations" from ASPLOS paper
rm -rf `find $WORKING -name "calculations"`

# remove PBS number gathering scripts, and other scripts not needed outside
# or used later in this release script.
rm -rf $WORKING/streams/misc/scripts/number-gathering
rm -rf $WORKING/streams/misc/scripts/emacs-indent.sh
rm -rf $WORKING/streams/misc/scripts/hwprof.sh
rm -rf $WORKING/streams/misc/scripts/NumericArraySummary.perl
rm -rf $WORKING/streams/misc/streamit-mail-unowned.pl
rm -rf $WORKING/streams/misc/dat2bin.pl
rm -rf $WORKING/streams/misc/check-javadoc-errors
rm -rf $WORKING/streams/misc/c

###
# Some benchmarks we can't (or won't) export; trim those here.
###

### benchmarks

# remove the proprietary codes (???)
# remove the code that would be examples of bad programming practice (tde)
# remove some code that drives too many bugs.
# remove contributed code or other code that we do not want to maintain.
# remove benchmark collections for papers old enough that people are unlikely
#  to try to reproduce results.
rm -rf $WORKING/streams/apps/benchmarks/serpent/{c,streambit,docs}
rm -rf $WORKING/streams/apps/benchmarks/sar/{c,matlab}
rm -rf $WORKING/streams/apps/benchmarks/des/{c,streambit}
rm -rf $WORKING/streams/apps/benchmarks/beamformer/c

# remove manual partition files
rm -rf $WORKING/streams/apps/benchmarks/beamformer/streamit/MyPartition1.java
rm -rf $WORKING/streams/apps/benchmarks/beamformer/streamit/MyPartition2.java

# make the beamformers so that the script building benchmarks.html can find them
cd $WORKING/streams/apps/benchmarks/beamformer/streamit/
make
cd -

# should asplos06 benchmarks be released separately?
#rm -rf $WORKING/streams/apps/benchmarks/asplos06
# rename "mpeg2" to "mpeg2-subset" to make it clear it's subsetting
mv $WORKING/streams/apps/benchmarks/asplos06/mpeg2 $WORKING/streams/apps/benchmarks/asplos06/mpeg2-subset
# do not release vocoder examples
rm $WORKING/streams/apps/benchmarks/asplos06/vocoder/streamit/VocoderExample*.str
rm -rf $WORKING/streams/apps/benchmarks/asplos06-space
# internal testing.
rm -rf $WORKING/streams/apps/benchmarks/asplos06-superset
rm -rf $WORKING/streams/apps/benchmarks/cfar
# remove the 500MB of input and output for DCT
rm -rf $WORKING/streams/apps/benchmarks/dct_ieee/input
rm -rf $WORKING/streams/apps/benchmarks/dct_ieee/output
rm -rf $WORKING/streams/apps/benchmarks/dct_ieee/streamit/iDCTcompare.str
# this prints mismatches, which is disconcerting
rm -rf $WORKING/streams/apps/benchmarks/dct_ieee/streamit/DCTverify.str
# note that this is just a library
echo "DCT.str provides a library of filters for use in other applications (e.g., MPEG)" > $WORKING/streams/apps/benchmarks/dct_ieee/streamit/README
# complex FIR is fine but was a simple benchmarking exercise, 
#   seems redundant with "fir"
rm -rf $WORKING/streams/apps/benchmarks/complex-fir
rm -rf $WORKING/streams/apps/benchmarks/fhr
# too many FFTs
rm -fr $WORKING/streams/apps/benchmarks/fft/streamit/FFT6.*
# actually, fir contains examples of how not to code, so it goes too
rm -rf $WORKING/streams/apps/benchmarks/fir
# FIR bank might be proprietary, and besides it has 5 MB
# of coefficients and we don't compile it well yet
rm -rf $WORKING/streams/apps/benchmarks/firbank
# does not verify
#rm -rf $WORKING/streams/apps/benchmarks/gsm/c
rm -rf $WORKING/streams/apps/benchmarks/gsm
# this is only relevant for spacedynamic backend, so don't release
rm -rf $WORKING/streams/apps/benchmarks/jpeg/streamit/Transcoder_Raw.str
rm -rf $WORKING/streams/apps/benchmarks/micro04

# deal with MPEG inputs and outputs...
# "test1" inputs never used
rm -rf $WORKING/streams/apps/benchmarks/mpeg2/input/test1
# make a directory for the cact_015 frames, which are currently read
# from compiler-static
mkdir $WORKING/streams/apps/benchmarks/mpeg2/input/cact_015_bmp
# the current mpeg encoder example uses 4 frames
cp /home/streamit/compiler-static/mpeg2/cact_015_bmp/00000001.bmp $WORKING/streams/apps/benchmarks/mpeg2/input/cact_015_bmp
cp /home/streamit/compiler-static/mpeg2/cact_015_bmp/00000002.bmp $WORKING/streams/apps/benchmarks/mpeg2/input/cact_015_bmp
cp /home/streamit/compiler-static/mpeg2/cact_015_bmp/00000003.bmp $WORKING/streams/apps/benchmarks/mpeg2/input/cact_015_bmp
cp /home/streamit/compiler-static/mpeg2/cact_015_bmp/00000004.bmp $WORKING/streams/apps/benchmarks/mpeg2/input/cact_015_bmp
# fix the mpeg encoder to reference these files rather than the ones in /home/streamit
perl -pi -e 's/\/home\/streamit\/compiler\-static\/mpeg2/\.\.\/input/g' $WORKING/streams/apps/benchmarks/mpeg2/streamit/BMPtoMPEG.str

rm -rf $WORKING/streams/apps/benchmarks/nokia
rm -rf $WORKING/streams/apps/benchmarks/perftest4
rm -rf $WORKING/streams/apps/benchmarks/pldi-03
# remove purely data-parallel code as something we do not want emulated.
rm -rf $WORKING/streams/apps/benchmarks/tde/streamit/tde.str
rm -rf $WORKING/streams/apps/benchmarks/traces
# these modifications of bitonic and fft for testing unreleased viram backend:
rm -rf $WORKING/streams/apps/benchmarks/viram
rm -rf $WORKING/streams/apps/benchmarks/vocoder

### examples

rm -rf $WORKING/streams/apps/examples/chol-para
# we have a merge sort in the "sorts" directory
rm -rf $WORKING/streams/apps/examples/mergesort
rm -rf $WORKING/streams/apps/examples/median
rm -rf $WORKING/streams/apps/examples/phase
rm -rf $WORKING/streams/apps/examples/sample-trellis
rm -rf $WORKING/streams/apps/examples/toy-trellis
rm -rf $WORKING/streams/apps/examples/updown
rm -rf $WORKING/streams/apps/examples/vectadd/VectAdd1.*

### tests

# return to previous decision to not release tests directory
rm -rf $WORKING/streams/apps/tests

# remove tests that are obsolete, especially StreamIt 1.0 no longer supported
# rm -rf $WORKING/streams/apps/tests/portals
# # remove tests that are StreamIt 1.0 only:
# rm -rf $WORKING/streams/apps/tests/simple-split
# rm -rf $WORKING/streams/apps/tests/script-ratios
# rm -rf $WORKING/streams/apps/tests/peek-pipe
# rm -rf $WORKING/streams/apps/tests/{hello-splits,hello-simple,hello-separate,hello-message}
# rm -rf $WORKING/streams/apps/tests/fuse-test
# rm -rf $WORKING/streams/apps/tests/fuse
# rm -rf $WORKING/streams/apps/tests/flybit
# rm -rf $WORKING/streams/apps/tests/fir-test
# rm -rf $WORKING/streams/apps/tests/field-init

### sorts -- now released under examples directory

# autobatchersort gets the wrong answer
rm -rf $WORKING/streams/apps/sorts/BatcherSort/AutoBatcherSort.*

### applications

# don't release applications directory except GMTI
# -- don't release GMTI either yet...
rm -rf $WORKING/streams/apps/applications
## GMTI: remove internal-only README file, remove internal-only generator of 
## intermediate results for use with GMTI_Fragment testing.
#rm -rf $WORKING/streams/apps/applications/GMTI/README
#rm -rf $WORKING/streams/apps/applications/GMTI/Tester_Intermediate_Results.str
## remove all except GMTI:
#rm -rf $WORKING/streams/apps/applications/802.11a
#rm -rf $WORKING/streams/apps/applications/crc
#rm -rf $WORKING/streams/apps/applications/DCT
#rm -rf $WORKING/streams/apps/applications/FAT
#rm -rf $WORKING/streams/apps/applications/FAT-new
#rm -rf $WORKING/streams/apps/applications/hdtv
#rm -rf $WORKING/streams/apps/applications/nokia
#rm -rf $WORKING/streams/apps/applications/nokia-fine
#rm -rf $WORKING/streams/apps/applications/nokia-new
#rm -rf $WORKING/streams/apps/applications/raytracer
#rm -rf $WORKING/streams/apps/applications/raytracer-new
#rm -rf $WORKING/streams/apps/applications/reed-solomon
#rm -rf $WORKING/streams/apps/applications/video

### libraries
# these might be reasonable to release, but are untested and are not
# used by any apps
rm -rf $WORKING/streams/apps/libraries

#
# restructure 
#

### library_only gets jpeg, mpeg2 (except MPEGdecoder_nomessage)
mkdir $WORKING/streams/apps/library_only
echo "At the current time, these programs execute correctly only under" > $WORKING/streams/apps/library_only/README
echo "the Java library, which is invoked using 'strc -library Filename.str'." >> $WORKING/streams/apps/library_only/README
echo "They will run much faster once they are supported by the compiler." >> $WORKING/streams/apps/library_only/README
mv $WORKING/streams/apps/benchmarks/jpeg $WORKING/streams/apps/library_only/
mv $WORKING/streams/apps/benchmarks/mpeg2 $WORKING/streams/apps/library_only/

mkdir -p $WORKING/streams/apps/benchmarks/mpeg2-subset/streamit
cp $WORKING/streams/apps/library_only/mpeg2/streamit/{Makefile,MPEGdecoder_nomessage.str.pre,MPEGglobal.str.pre,ColorSpace.str,Misc.str,BinaryFile.str.pre} $WORKING/streams/apps/benchmarks/mpeg2-subset/streamit/
# move the nomessage/noparser README and rename it to something reasonable
mv $WORKING/streams/apps/library_only/mpeg2/streamit/README-nomessage-noparser $WORKING/streams/apps/benchmarks/mpeg2-subset/streamit/README
# share the "input" directory between mpeg2-subset and mpeg2 using a symlink
ln -s ../../library_only/mpeg2/input $WORKING/streams/apps/benchmarks/mpeg2-subset/input
# make the default target to be nomessage
perl -pi -e 's/default\:\ .*/default\:\ nomessage/g' $WORKING/streams/apps/benchmarks/mpeg2-subset/streamit/Makefile
# make the default strc options to be a plain compile
perl -pi -e 's/STRC_OPTIONS\ \=\ .*/STRC_OPTIONS\ \= \ /g' $WORKING/streams/apps/benchmarks/mpeg2-subset/streamit/Makefile

### anything under examples other than cookbook becomes examples/misc
mkdir -p $WORKING/streams/apps/examples/misc
for d in $WORKING/streams/apps/examples/*; do
  f=$(echo $d | sed -e 's/^.*\///')   # csh :t does not apply to vars in sh ??
  if [ $f != "cookbook" ]; then
    if [ $f != "misc" ]; then
      mv $d $WORKING/streams/apps/examples/misc/$f
    fi
  fi
done

### now move sorts to examples/sorts
mv $WORKING/streams/apps/sorts $WORKING/streams/apps/examples/

###
# Some parts of the compiler aren't useful to release; trim those here.
###

rm -rf $WORKING/streams/src/at/dms/kjc/slicegraph
rm -rf $WORKING/streams/src/at/dms/kjc/flatgraph2
rm -rf $WORKING/streams/src/at/dms/kjc/raw2
rm -rf $WORKING/streams/src/com
rm -rf $WORKING/streams/src/org
rm -rf $WORKING/streams/src/streamit/eclipse
rm -rf $WORKING/streams/src/streamit/stair
# desupported backends:
rm -rf $WORKING/streams/src/at/dms/kjc/raw
rm -rf $WORKING/streams/library/c
# remove dependencies on raw:
rm -rf $WORKING/streams/src/at/dms/kjc/sir/stats

perl -pi -e's/at\.dms\.kjc\.raw\.RawWorkEstimator/at.dms.kjc.spacedynamic.RawWorkEstimator/'  $WORKING/streams/src/at/dms/kjc/sir/lowering/partition/WorkInfo.java

perl -ni -e'print unless /RawBackend/ || /SpaceTimeBackend/' $WORKING/streams/src/at/dms/kjc/spacedynamic/RawWorkEstimator.java

perl -pi -e's/StatisticsGathering\.doit\(str\);/\/\*StatisticsGathering.doit(str);\*\//' $WORKING/streams/src/at/dms/kjc/cluster/ClusterBackend.java
perl -pi -e's/import at\.dms\.kjc\.sir\.stats\.StatisticsGathering;/\/\*import at.dms.kjc.sir.stats.StatisticsGathering;\*\//' $WORKING/streams/src/at/dms/kjc/cluster/ClusterBackend.java

perl -pi -e's/StatisticsGathering\.doit\(str\);/\/\*StatisticsGathering.doit(str);\*\//' $WORKING/streams/src/at/dms/kjc/sir/lowering/Flattener.java
perl -pi -e's/import at\.dms\.kjc\.sir\.stats\.StatisticsGathering;/\/\*import at.dms.kjc.sir.stats.StatisticsGathering;\*\//' $WORKING/streams/src/at/dms/kjc/sir/lowering/Flattener.java

perl -pi -e's/StatisticsGathering\.doit\(ssg\.getTopLevelSIR\(\)\);/\/\*StatisticsGathering.doit(ssg.getTopLevelSIR());\*\//' $WORKING/streams/src/at/dms/kjc/spacedynamic/SpaceDynamicBackend.java
perl -pi -e's/import at\.dms\.kjc\.sir\.stats\.StatisticsGathering;/\/\*import at.dms.kjc.sir.stats.StatisticsGathering;\*\//' $WORKING/streams/src/at/dms/kjc/spacedynamic/SpaceDynamicBackend.java

# experimental backend: jcc
rm -rf $WORKING/streams/src/streamit/library/jcc
perl -pi -e's/new StreamItToJcc\(\)\.convertAndRun\(this, nIters\);/\/\*new StreamItToJcc().convertAndRun(this, nIters);\*\/ assert false:"jcc library support removed";/' $WORKING/streams/src/streamit/library/Stream.java
perl -pi -e's/import streamit\.library\.jcc\.StreamItToJcc;/\/\*import streamit.library.jcc.StreamItToJcc;\*\//' $WORKING/streams/src/streamit/library/Stream.java
# not yet finished backends. reasonably well modularized, thank goodness.
rm -rf $WORKING/streams/src/at/dms/kjc/spacetime

# A release does not need to build a release
rm -rf $WORKING/streams/misc/release.sh

# remove PCA machine model
rm -rf $WORKING/streams/misc/raw/pca-mm
rm -rf $WORKING/streams/misc/raw/darpa

# Some parts of the language notes we don't want to be visible
rm -f $WORKING/streams/docs/syntax/02-04-24-additions
rm -f $WORKING/streams/docs/syntax/02-08-additions
rm -f $WORKING/streams/docs/syntax/messaging.tex
# the implementation notes are largely obsolete or irrelevant
rm -rf $WORKING/streams/docs/implementation-notes

# Release 2.1 version of language
mv $WORKING/streams/docs/syntax/streamit-lang-2.1.tex $WORKING/streams/docs/syntax/streamit-lang.tex 

###############################################################################
# Put out javadoc for released version.
# All source directories that are going to be removed must be removed before
# this.
##############################################################################

$WORKING/streams/misc/build-javadoc $WORKING/streams/javadoc

###############################################################################
# Build interesting bits of the documentation; they go in both binary
# and source  releases.
###############################################################################

for d in release cookbook syntax; do
  make -C $WORKING/streams/docs/$d
done
find $WORKING/streams/docs \( -name '*.aux' -o -name '*.log' \
  -o -name '*.toc' -o -name '*.[0-9]' \) -print0 | xargs -0 rm
for f in COPYING COPYING.GPL; do
  cp $WORKING/streams/docs/release/$f $WORKING/streams
done
for f in INSTALL NEWS OPTIONS README; do
  mv $WORKING/streams/docs/release/$f $WORKING/streams
done
# combine documentation from benchmark.xml files into benchmarks.html
# then remove intermediate file, script files.
$WORKING/streams/misc/build-bench-doc
rm $WORKING/streams/apps/benchall.xml
rm $WORKING/streams/misc/{build-bench-doc,build-bench-xml.py,benchall.xsl}

###############################################################################
# Make tarballs
##############################################################################

###
# Make a stripped down tree for binary release in $BINDIR:
# no src, no javadoc, no sources for docs
# keep only the .in version of the files that configure will update.
###

cp -R $WORKING/streams $BINDIR
rm -rf $BINDIR/javadoc
rm -rf $BINDIR/src $BINDIR/README.source
rm -rf $BINDIR/include/dot-bashrc
rm -rf $BINDIR/include/dot-cshrc
rm -rf $BINDIR/misc/get-antlr
find $BINDIR/docs \( -name '*.hva' -o -name '*.tex' -o -name Makefile \
  -o -name '*.mp' \) -print0 | xargs -0 rm
rm -f $BINDIR/misc/htmlformat.pl
for f in $INFILES; do
  rm -f "$BINDIR/$f"
done
rm -rf $BIDIR/include/tex
rm -rf $BIDIR/misc/Makefile.docs

###
# Build the source tarball:
# keep only the .in version of the files that configure will update.
# all other files as set up in $WORKING.
###

cp -R $WORKING/streams $WORKING/streamit-src-$VERSION
for f in $INFILES; do
  rm -f "$WORKING/streamit-src-$VERSION/$f"
done
tar czf $SRCTAR -C $WORKING streamit-src-$VERSION

###
# Create a fresh src/3rdparty/antlr.jar for our build, then
# use src/Makefile to build src/streamit.jar and copy it into $BINDIR
# finally, make tarball for binary release from $BINDIR.
###

# Use the build magic to get an ANTLR jar file.
$SRCDIR/misc/get-antlr $SRCDIR/3rdparty/antlr.jar
CLASSPATH=$SRCDIR/3rdparty/antlr.jar

# Now do a reference build.
export CLASSPATH
. $STREAMIT_HOME/include/dot-bashrc
make -C $SRCDIR/src jar CAG_BUILD=0

# Build binary jar file:
cp $SRCDIR/src/streamit.jar $BINDIR
tar czf $BINTAR -C $WORKING streamit-$VERSION

# move the tarball here.
mv $SRCTAR $BINTAR .

# Clean up.
if test -n "$PRECIOUS"
then
  echo Keeping working directory $WORKING
else
  rm -rf $WORKING
fi

