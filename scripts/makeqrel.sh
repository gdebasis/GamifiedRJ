#!/bin/bash

if [ $# -lt 3 ]
then
	echo "usage $0 <log file> <found rel docs file> <full rel docs file>"
	exit
fi

logfile=$1
gamerelfile=$2
fullrelfile=$3 # full qrels only for the queries hit in the game

if [ -e $gamerelfile ]
then
	cp /dev/null $gamerelfile
	cp /dev/null $fullrelfile
fi

#cat $logfile| awk -F '\t' '{if ($6!="none") print $4 " 0 " $6 " 1"}'|sort -n -k1|uniq > tmp
cat $logfile| awk -F '\t' '{if ($9=="true" && $6!="none") print $4 " 0 " $6 " 1"}'|sort -n -k1|uniq > tmp

while read line
do
	grep "$line" /mnt/sdb2/trec/qrels/qrels.trec8.adhoc >> $gamerelfile
	qid=`echo $line | awk '{print $1}'`
done < tmp

cat tmp | awk '{print $1}'| sort|uniq > tmp2 

while read line
do
	grep "^$line" /mnt/sdb2/trec/qrels/qrels.trec8.adhoc >> $fullrelfile
done < tmp2

rm tmp
rm tmp2

for f in `find /mnt/sdb2/research/eval/trec_runs/trec8/ -name "input*"`
do
	gamifiedmap=`trec_eval $gamerelfile $f | grep -w map | awk '{print $3}'`
	truemap=`trec_eval $fullrelfile $f | grep -w map | awk '{print $3}'`
	echo "$gamifiedmap $truemap"
done
