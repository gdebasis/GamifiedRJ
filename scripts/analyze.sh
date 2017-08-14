#!/bin/bash

if [ $# -lt 1 ]
then
	echo "usage $0 <log file>"
	exit
fi

logfile=$1

#sort -k 1 log.txt > log.sorted.txt
echo "total number of games:"
totalgames=`cat $logfile | awk -F '\t' '{print $1 " " $4}'|uniq -c| wc -l`
echo $totalgames

echo "avg number of guesses in a game:"
cat $logfile | awk -F '\t' '{print $1 " " $4}'|uniq -c| awk '{s+=$1} END{print s/NR}'

echo "#unique queries over all games"
cat $logfile | awk -F '\t' '{if($8!="null")print $8}'|sort|uniq|wc -l

echo "#number of times human player wins (guesses correctly the target document): "
cat $logfile | awk -F '\t' '{if ($5==$6 && $9=="true") print $0}'|wc -l

echo "#total number of rel docs found"
cat $logfile | awk -F '\t' '{if ($9=="true") print $6}'|sort|uniq|wc -l

echo "#avg number of rel docs found per query"
cat $logfile | awk -F '\t' '{if ($9=="true") print $4 " " $6}'|sort -n -k 1|uniq -c|awk '{nrels[$2]++} END{ for (qid in nrels) print qid " " nrels[qid]}'| awk '{s+=$2} END{print s/NR " " NR}'

echo "#avg no of rel docs found per game"
cat $logfile | awk -F '\t' '{if ($9=="true") print $1 " " $4}'|sort -n -k 2|uniq -c |awk '{s+=$1} END{print s/NR}'

echo "#number of times a document is submitted"
cat $logfile| awk -F '\t' '{print $4}'|sort|uniq -c|awk '{s+=$1} END{print s/NR}'

echo "#number of times a document is submitted as a rel/nrel"
#cat $logfile| awk -F '\t' '{print $4 " " $5 " " $9}'|sort -k2|uniq -c|  awk '{if($4=="true") rels[$2 " " $3]+=$1; else nrels[$2 " " $3]+=$1; } END{for (i in nrels) if (i in rels) print i " " rels[i] " " rels[i]+nrels[i]}'|sort -k1

cat $logfile| awk -F '\t' '{print $4 " " $9}'|sort -k2|uniq -c|  awk '{if($3=="true") rels[$2]+=$1; else nrels[$2]+=$1; } END{for (i in nrels) if (i in rels) print i " " rels[i] " " rels[i]+nrels[i]}'|sort -k1

echo "#Avg. precision of submitted documents across game..."
cat $logfile| awk -F '\t' '{print $4 " " $9}'|sort -k2|uniq -c|  awk '{if($3=="true") rels[$2]+=$1; else nrels[$2]+=$1; } END{for (i in nrels) if (i in rels) print i " " rels[i] " " rels[i]+nrels[i]}'|sort -k1|awk '{t+=$2;a+=$3} END{print t/a}'

# printing out the individual queries during a gaming session
echo "#reformulations"
cat $logfile | awk -F '\t' '{print $1 "\t" $4 "\t" $8}'|sort -k1 -k2 |awk -F "\t" '{q[$1"_"$2]=q[$1"_"$2] ": " $3} END{for (i in q) {nreforms=split(q[i], queries, ":"); print nreforms}}'| sort|uniq -c|sort -n -k2| awk -v totalgames=$totalgames '{printf("%d %d %.4f %.4f\n", $2, $1, log($1), $1/totalgames)}'

echo "#query terms executed during game play:"
cat $logfile | awk -F '\t' '{print $1 "\t" $4 "\t" $8}'|sort -k1 -k2 |awk -F "\t" '{q[$1"_"$2]=q[$1"_"$2] ": " $3} END{for (i in q) {split(q[i], queries, ":"); for (j in queries) print split(queries[j], qterms, " ")}}'|awk '{if ($1>1) print $1}'|sort -n| uniq -c|sort -n -k2
