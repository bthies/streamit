#!/bin/sh -e
#
# release.sh: assemble a StreamIt release
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: release.sh,v 1.2 2003-03-10 17:13:35 dmaze Exp $
#

# Interesting/configurable variables:
VERSION=0.0.20030314
TAG=HEAD
test -f /usr/share/java/antlrall.jar && ANTLRJAR=/usr/share/java/antlrall.jar
test -f /usr/uns/java/antlr.jar && ANTLRJAR=/usr/uns/java/antlr.jar

# Temporary directory:
WORKING=${TMPDIR:=/tmp}/streamit-$USER-$$
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
rm -rf $WORKING
