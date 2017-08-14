#!/bin/bash

if [ $# -lt 1 ]
then
	echo "usage $0 <log file>"
	exit
fi

logfile=$1


cat $logfile| awk -F '\t' '{if($9=="true") b=1; else b=0; if ($6!="none" && b==1) print $4 " 0 " $6 " " b}'|sort -n -k1 > tmp1

tmatched=0
nlines=0

while read line
do
	#grep "$line" /mnt/sdb2/trec/qrels/qrels.trec8.adhoc
	matched=`grep -c "$line" /mnt/sdb2/trec/qrels/qrels.trec8.adhoc`
	if [ $matched -gt 0 ]
	then
		tmatched=`expr $tmatched + 1`
	fi
	#nlines=`expr $nlines + 1`
done < tmp1

nlines=`cat tmp1 | awk '{print $3}'|sort|uniq|wc -l`

echo "#correct (rel): $tmatched"
echo "#outof: $nlines"

tmatched=0
nlines=0

#cat $logfile| awk -F '\t' '{if($9=="true") b=1; else b=0; if ($6!="none" && $5!=$6 && b==0) print $4 " 0 " $6 " " b}'|sort -n -k1 > tmp2
cat $logfile| awk -F '\t' '{if($9=="true") b=1; else b=0; if ($6!="none" && b==0) print $4 " 0 " $6 " " b}'|sort -n -k1 > tmp2

while read line
do
	#grep "$line" /mnt/sdb2/trec/qrels/qrels.trec8.adhoc
	matched=`grep -c "$line" /mnt/sdb2/trec/qrels/qrels.trec8.adhoc`
	if [ $matched -gt 0 ]
	then
		tmatched=`expr $tmatched + 1`
	fi
	#nlines=`expr $nlines + 1`
done < tmp2

nlines=`cat tmp2 | awk '{print $3}'|sort|uniq|wc -l`
echo "#correct (nrel): $tmatched"
echo "#outof: $nlines"

