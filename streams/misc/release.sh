#!/bin/sh -e
#
# release.sh: assemble a StreamIt release
# David Maze <dmaze@cag.lcs.mit.edu>
# $Id: release.sh,v 1.1 2003-03-10 16:57:33 dmaze Exp $
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

# Get a checked-out copy of the source tree.
mkdir $SRCDIR
mkdir $SRCDIR/apps
mkdir $SRCDIR/docs
for d in compiler library apps/examples apps/libraries apps/sorts \
    docs/cookbook docs/implementation-notes docs/manual \
    docs/runtime-interface docs/semantics docs/syntax include misc
do
    cvs export -r $TAG -d $SRCDIR/$d streams/$d
done

# Build the source tarball:
cvs export -r $TAG -d $SRCDIR streams/docs/release
tar cf $SRCTAR -C $WORKING streamit-$VERSION

# Now do a reference build.
CLASSPATH=${ANTLRJAR:?}
export CLASSPATH
STREAMIT_HOME=$SRCDIR
export STREAMIT_HOME
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
