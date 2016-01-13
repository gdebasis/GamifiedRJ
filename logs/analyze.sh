sort -k 1 log.txt > log.sorted.txt

#total number of games
cat log.sorted.txt | awk -F '\t' '{print $1 " " $4}'|uniq -c| wc -l

# avg number of guesses in a game
cat log.sorted.txt | awk -F '\t' '{print $1 " " $4}'|uniq -c| awk '{s+=$1} END{print s/NR}'

# number of times human player wins (guesses correctly the target document)
cat log.sorted.txt | awk -F '\t' '{if ($5==$6 && $9=="true") print $0}'

# total number of rel docs found
cat log.sorted.txt | awk -F '\t' '{if ($9=="true") print $6}'|sort|uniq|wc -l

# avg number of rel docs found per query
cat log.sorted.txt | awk -F '\t' '{if ($9=="true") print $4 " " $6}'|sort -n -k 1|uniq -c|awk '{nrels[$2]++} END{ for (qid in nrels) print qid " " nrels[qid]}'| awk '{s+=$2} END{print s/NR}'

#avg no of rel docs found per game
cat log.sorted.txt | awk -F '\t' '{if ($9=="true") print $1 " " $4}'|sort -n -k 2|uniq -c |awk '{s+=$1} END{print s/NR}'

# number of times a document is submitted as a rel/nrel
cat log.sorted.txt| awk -F '\t' '{print $4 " " $5 " " $9}'|sort -k2|uniq -c|  awk '{if($4=="true") rels[$2 " " $3]+=$1; else nrels[$2 " " $3]+=$1; } END{for (i in nrels) if (i in rels) print i " " rels[i] " " nrels[i]}'|sort -k1

