#!/bin/sh

echo
for name in `ls` 
do
  dummy=`echo 23 >! temp && echo 35 >> temp && pro $name -f $name_s`
done
