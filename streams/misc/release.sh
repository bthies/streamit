#!/bin/sh -e
#
# release.sh: assemble a StreamIt release
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: release.sh,v 1.20 2003-10-05 00:46:47 dmaze Exp $
#

# Interesting/configurable variables:
VERSION=0.0.20030528
TAG=streamit-snapshot-20030528
test -z "$TMPDIR" && TMPDIR=/tmp
PRECIOUS=

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
    --cvsroot|-d) CVSROOT="$1"; export CVSROOT; shift;;
    --precious|-k) PRECIOUS=yes;;
    *) usage; exit 1;;
  esac
done

# Temporary directory:
WORKING=$TMPDIR/streamit-$USER-$$
mkdir $WORKING
SRCDIR=$WORKING/streams
SRCTAR=$WORKING/streamit-src-$VERSION.tar
BINDIR=$WORKING/streamit-$VERSION
BINTAR=$WORKING/streamit-$VERSION.tar

# Helper function to add a list of directories to $DIRS
builddirs() {
  PREFIX="$1"; shift
  while test -n "$1"; do DIRS="$DIRS $PREFIX/$1"; shift; done
}

# Get a checked-out copy of the source tree.
mkdir $WORKING/streams
DIRS="streams/strc streams/Makefile streams/README.source"
builddirs streams 3rdparty src library include misc configure.in Makefile.in
builddirs streams/apps benchmarks examples libraries sorts
builddirs streams/docs cookbook implementation-notes runtime-interface
builddirs streams/docs release syntax

cvs export -r $TAG -d $WORKING $DIRS

# Run autoconf to get a configure script.
autoconf $WORKING/streams/configure.in > $WORKING/streams/configure
chmod 0755 $WORKING/streams/configure

# Generate .in files.  (See also the tail of configure.in.)
INFILES="strc Makefile library/c/Makefile misc/Makefile.vars"
for f in $INFILES; do
  if test -f "$WORKING/streams/$f"; then
    $WORKING/streams/misc/make-dot-in.pl "$WORKING/streams/$f"
  fi
done

# Some benchmarks we can't (or won't) export; trim those here.
rm -rf $WORKING/streams/apps/benchmarks/beamformer/c
rm -rf $WORKING/streams/apps/benchmarks/cfar
rm -rf $WORKING/streams/apps/benchmarks/gsm/c
rm -rf $WORKING/streams/apps/benchmarks/nokia
rm -rf $WORKING/streams/apps/benchmarks/perftest4

# Some parts of the compiler aren't useful to release; trim those here.
rm -rf $WORKING/streams/src/at/dms/kjc/cluster
rm -rf $WORKING/streams/src/at/dms/kjc/flatgraph2
rm -rf $WORKING/streams/src/at/dms/kjc/raw2
rm -rf $WORKING/streams/src/at/dms/kjc/spacetime

# Build interesting bits of the documentation; they go in both releases.
for d in cookbook manual release syntax; do
  make -C $WORKING/streams/docs/$d
done
find $WORKING/streams/docs \( -name '*.aux' -o -name '*.log' \
  -o -name '*.toc' -o -name '*.[0-9]' \) -print0 | xargs -0 rm
for f in COPYING COPYING.GPL INSTALL NEWS OPTIONS README REGTEST; do
  mv $WORKING/streams/docs/release/$f $WORKING/streams
done

# Make stable copies for all of the trees.  Clean the binary tree a little
# in the process.
cp -R $WORKING/streams $BINDIR
rm -rf $BINDIR/src $BINDIR/README.source
rm -rf $BINDIR/include/dot-bashrc
rm -rf $BINDIR/include/dot-cshrc $BINDIR/misc/release.sh
rm -rf $BINDIR/misc/get-antlr
find $BINDIR/docs \( -name '*.hva' -o -name '*.tex' -o -name Makefile \
  -o -name '*.mp' \) -print0 | xargs -0 rm
rm -f $BINDIR/misc/htmlformat.pl
for f in $INFILES; do
  rm -f "$BINDIR/$f"
done

# Build the source tarball:
cp -R $WORKING/streams $WORKING/streamit-src-$VERSION
for f in $INFILES; do
  rm -f "$WORKING/streamit-src-$VERSION/$f"
done
tar cf $SRCTAR -C $WORKING streamit-src-$VERSION

# Use the build magic to get an ANTLR jar file.
$SRCDIR/misc/get-antlr $SRCDIR/3rdparty/antlr.jar
CLASSPATH=$SRCDIR/3rdparty/antlr.jar

# Now do a reference build.
STREAMIT_HOME=$SRCDIR
export CLASSPATH STREAMIT_HOME
. $STREAMIT_HOME/include/dot-bashrc
make -C $SRCDIR/src jar CAG_BUILD=0

# Build binary jar file:
cp $SRCDIR/src/streamit.jar $BINDIR
tar cf $BINTAR -C $WORKING streamit-$VERSION

# gzip the tarball and move it here.
gzip $SRCTAR $BINTAR
mv $SRCTAR.gz $BINTAR.gz .

# Clean up.
if test -n "$PRECIOUS"
then
  echo Keeping working directory $WORKING
else
  rm -rf $WORKING
fi

