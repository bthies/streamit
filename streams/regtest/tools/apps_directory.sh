#!/bin/sh

echo
echo "The following directories are NOT being COMPILED in the regtest:"
for dirname in `ls ${STREAMIT_HOME}apps` 
do
  for name in `tree -dif ${STREAMIT_HOME}apps/$dirname | grep "apps/$dirname/" | grep -v CVS | grep -v directories | grep -v VocoderTests | grep -v '/c/' | grep -v '/c$'`
    do
    info=`grep $name $1 | grep Compile`
    if test "$info" == ""; then
	echo " " $name
    fi
  done
done

echo
echo "The following directories are NOT being EXECUTED in the regtest:"
for dirname in `ls ${STREAMIT_HOME}apps` 
do
  for name in `tree -dif ${STREAMIT_HOME}apps/$dirname | grep "apps/$dirname/" | grep -v CVS | grep -v directories | grep -v VocoderTests | grep -v '/c/' | grep -v '/c$'`
    do
    info=`grep $name $1 | grep Exec`
    if test "$info" == ""; then
	echo " " $name
    fi
  done
done

echo
echo "The following directories are NOT being VERIFIED in the regtest:"
for dirname in `ls ${STREAMIT_HOME}apps` 
do
  for name in `tree -dif ${STREAMIT_HOME}apps/$dirname | grep "apps/$dirname/" | grep -v CVS | grep -v directories | grep -v VocoderTests | grep -v '/c/' | grep -v '/c$'`
    do
    info=`grep $name $1 | grep Verify`
    if test "$info" == ""; then
	echo " " $name
    fi
  done
done
