#!/bin/sh -e
#
# release.sh: assemble a StreamIt release
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: release.sh,v 1.4 2003-03-12 22:28:25 dmaze Exp $
#

# Interesting/configurable variables:
VERSION=0.0.20030314
TAG=HEAD
test -z "$TMPDIR" && TMPDIR=/tmp
PRECIOUS=

usage() {
  cat >&2 <<EOF
release.sh: assemble a StreamIt release

Usage:
  release.sh [options]

Options:
  --version (-v)   Use a particular exported version ($VERSION)
  --tag (-t)       Build a release from a CVS tag ($TAG)
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
    --tag|-t) TAG="$1"; shift;;
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
LIBDIR=$WORKING/streamit-lib-$VERSION
LIBTAR=$WORKING/streamit-lib-$VERSION.tar

# Helper function to add a list of directories to $DIRS
builddirs() {
  PREFIX="$1"; shift
  while test -n "$1"; do DIRS="$DIRS $PREFIX/$1"; shift; done
}

# Get a checked-out copy of the source tree.
mkdir $WORKING/streams
DIRS="streams/knit streams/README.source"
builddirs streams compiler library include misc
builddirs streams/apps examples libraries sorts
builddirs streams/docs cookbook implementation-notes manual runtime-interface
builddirs streams/docs semantics syntax

cvs export -r $TAG -d $WORKING $DIRS
cvs export -r $TAG -d $SRCDIR streams/docs/release

# Make stable copies for all of the trees.
cp -R $WORKING/streams $BINDIR
rm -rf $BINDIR/compiler
cp -R $BINDIR $LIBDIR

# Build the source tarball:
cp -R $WORKING/streams $WORKING/streamit-src-$VERSION
tar cf $SRCTAR -C $WORKING streamit-src-$VERSION

# Use the build magic to get an ANTLR jar file.
$SRCDIR/misc/get-antlr $SRCDIR/compiler/antlr.jar
CLASSPATH=$SRCDIR/compiler/antlr.jar

# Now do a reference build.
STREAMIT_HOME=$SRCDIR
export CLASSPATH STREAMIT_HOME
. $STREAMIT_HOME/include/dot-bashrc
make -C $SRCDIR/compiler jar

# Build binary jar file:
cp $SRCDIR/compiler/streamit.jar $BINDIR
tar cf $BINTAR -C $WORKING streamit-$VERSION

# Build library jar file:
cp $SRCDIR/compiler/streamit-lib.jar $LIBDIR
tar cf $LIBTAR -C $WORKING streamit-lib-$VERSION

# gzip the tarball and move it here.
gzip $SRCTAR $LIBTAR $BINTAR
mv $SRCTAR.gz $LIBTAR.gz $BINTAR.gz .

# Clean up.
if test -n "$PRECIOUS"
then
  echo Keeping working directory $WORKING
else
  rm -rf $WORKING
fi

