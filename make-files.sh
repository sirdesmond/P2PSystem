#!/bin/bash
DIR=$1
[[ -z $DIR ]] && DIR=shared

[[ ! -d $DIR ]] && mkdir $DIR
cd $DIR

counter=1;

for i in {1..10}
do
  echo Creating file no $counter
#head -c 1024 * $i /dev/urandom > file$counter
  dd bs=1024 count=$i if=/dev/urandom of=file$counter;
  let "counter+=1";
done
