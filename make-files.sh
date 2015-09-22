#!/bin/bash
DIR=$1
[[ -z $DIR ]] && DIR=/tmp/shared

[[ ! -d $DIR ]] && mkdir $DIR
cd $DIR

counter=1;

for i in {1..100}
do
 echo Creating file no $counter; 
 for j in {1..10}
 do
  #head -c 1024 * $j /dev/urandom > file$counter
  dd bs=1024 count=$j if=/dev/urandom of=file$counter  
  let "counter+=1";
 done
done
