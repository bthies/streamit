#!/bin/sh -e
#
# release.sh: assemble a StreamIt release
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: release.sh,v 1.3 2003-03-10 17:26:59 dmaze Exp $
#

# Interesting/configurable variables:
VERSION=0.0.20030314
TAG=HEAD
test -f /usr/share/java/antlrall.jar && ANTLRJAR=/usr/share/java/antlrall.jar
test -f /usr/uns/java/antlr.jar && ANTLRJAR=/usr/uns/java/antlr.jar
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
    --antlr) ANTLRJAR="$1"; shift;;
    --cvsroot|-d) CVSROOT="$1"; export CVSROOT; shift;;
    --precious|-k) PRECIOUS=yes;;
    *) usage; exit 1;;
  esac
done

# Make sure we have ANTLR somewhere.
if test -z "$ANTLRJAR"; then
  echo No ANTLR jar file\; use --antlr command-line option >&2
  exit 1
fi

# Temporary directory:
WORKING=$TMPDIR/streamit-$USER-$$
mkdir $WORKING
SRCDIR=$WORKING/streamit-$VERSION
SRCTAR=$SRCDIR.tar

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
mv $WORKING/streams $SRCDIR

# Build the source tarball:
cvs export -r $TAG -d $SRCDIR streams/docs/release
tar cf $SRCTAR -C $WORKING streamit-$VERSION

# Now do a reference build.
CLASSPATH=${ANTLRJAR:?}
STREAMIT_HOME=$SRCDIR
export CLASSPATH STREAMIT_HOME
. $STREAMIT_HOME/include/dot-bashrc
make -C $SRCDIR/compiler jar

# And add the resulting jar files to the tarball.
mv $SRCDIR/compiler/streamit.jar $SRCDIR
mv $SRCDIR/compiler/streamit-lib.jar $SRCDIR
tar rf $SRCTAR -C $WORKING streamit-$VERSION/streamit.jar
tar rf $SRCTAR -C $WORKING streamit-$VERSION/streamit-lib.jar

# gzip the tarball and move it here.
gzip $SRCTAR
mv $SRCTAR.gz .

# Clean up.
if test -n "$PRECIOUS"
then
  echo Keeping working directory $WORKING
else
  rm -rf $WORKING
fi

