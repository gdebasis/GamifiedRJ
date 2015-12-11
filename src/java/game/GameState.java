/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package game;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.util.BytesRef;
import retriever.TrecDocRetriever;
import static retriever.TrecDocRetriever.FIELD_ANALYZED_CONTENT;
import trec.TRECQuery;

/**
 *
 * @author Debasis
 */
class TermFreq implements Comparable<TermFreq> {
    Term term;
    String termStr;
    float tf;  // document component
    float wt;
    
    public TermFreq(Term term, String termStr, int tf) {
        this.term = term;
        this.tf = tf;
        this.termStr = termStr;
    }

    @Override
    public int compareTo(TermFreq t) {
        return -1 * new Float(wt).compareTo(t.wt); // descending
    }
    
    @Override
    public String toString() {
        return "(" + termStr + ", " + wt + ")";
    }
}

class TermFreqComparator_Freq implements Comparator<TermFreq> {

    @Override
    public int compare(TermFreq a, TermFreq b) {
        Integer aLen = (int)a.tf;
        Integer bLen = (int)b.tf;
        return -1*aLen.compareTo(bLen);  // decreasing
    }    
}

class UserSubmitInfo {
    String wordsShared;
    int    luceneDocId;
    String docSubmitted;
    boolean relGuess;

    public UserSubmitInfo(String wordsShared, int luceneDocId, String docSubmitted, boolean relGuess) {
        this.wordsShared = wordsShared;
        this.docSubmitted = docSubmitted;
        this.luceneDocId = luceneDocId;
        this.relGuess = relGuess;
    }
    
    @Override
    public String toString() {
        String className = relGuess? "rel" : "nrel";
        StringBuffer buff = new StringBuffer();
        buff.append("<td>");
        
        if (luceneDocId >= 0) {
            buff.append("<a id='")
                .append(luceneDocId)
                .append("' name='")
                .append(docSubmitted)
                .append("' class='")
                .append(className)
                .append("'>")
                .append(docSubmitted)
                .append("</a>");
        }
        
        buff.append("</td>")    
            .append("<td>")    
            .append(wordsShared)
            .append("</td>");
                
        return buff.toString();
    }
    
}

public class GameState {
    String sessionId;
    String qid;    // the query id
    TRECQuery query;
    long startingEpochs;
    
    String docIdToGuess;  // the doc id which the user needs to guess
    AllRelRcds rels;
    TrecDocRetriever retriever;
    
    Document docToGuess;
    int luceneDocIdToGuess;
    String contentOfDocToGuess;
    
    int numTermsToShare; // number of terms to share in each round
    
    int score;
    boolean startState;
    int numTermsShared;
    List<UserSubmitInfo> submitInfos;
    
    // Instantaneous state variables
    String lastDocumentSubmitted;
    String lastUserQuery;
    
    String wordsSharedNow; // words just shared
    boolean correctGuess;
    boolean relGuess;
    int terminateCode;

    String logFileName;
    
    List<TermFreq> tfvec;  // term freq vec of the doc to be guessed
    TermFreqComparator_Freq tfcomp_freq;
    
    static final float LAMBDA = 0.6f;
    static final float ONE_MINUS_LAMBDA = 1-LAMBDA;
    
    // Termination Codes
    static final int GAME_TO_CONTINUE = 0;
    static final int CORRECT_GUESS_FOUND = 1;
    static final int SCORE_REACHED_MIN_THRESH = 2;
    
    // SCORE UPDATES...
    static final int INIT_SCORE = 10;
    static final int GAME_TERMINATION_SCORE = 0; // stop the game if this score is reached
    static final int SCORE_INCREMENET_FOR_CORRECT_GUESS = 20;
    static final int SCORE_INCREMENET_FOR_CORRECT_REL = 5;
    static final int SCORE_INCREMENET_FOR_INCORRECT_REL = -2;
        
    public GameState(TrecDocRetriever retriever, AllRelRcds rels, String sessionId) {
        this.sessionId = sessionId;
        this.retriever = retriever;
        this.rels = rels;
        startingEpochs = System.currentTimeMillis();
        logFileName = retriever.getProperties().getProperty("gamelog.file");
        
        // Pick one random query
        qid = rels.selectRandomQuery();
        // Pick one relevant document for this query at random
        docIdToGuess = rels.selectRandomRelDoc(qid);
        
        // Load the Lucene document object for the doc to be guessed...
        // This is going to be used for selecting random terms from this doc...
        try {
            loadDoc();        
            loadTfVec();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        
        Properties prop = retriever.getProperties();
        numTermsToShare = Integer.parseInt(prop.getProperty("game.numterms", "3"));
        this.query = retriever.getQuery(qid);        
        
        numTermsShared = 0;
        startState = true;
        
        tfcomp_freq = new TermFreqComparator_Freq();
        submitInfos = new ArrayList<>();
    }
    
    void loadTfVec() throws Exception {
        
        IndexReader reader = retriever.getReader();
        long sumDf = reader.getSumDocFreq(TrecDocRetriever.FIELD_ANALYZED_CONTENT);
        
        Terms terms = reader.getTermVector(luceneDocIdToGuess, FIELD_ANALYZED_CONTENT);
        if (terms == null || terms.size() == 0)
            return;

        TermsEnum termsEnum;
        BytesRef term;
        tfvec = new ArrayList<>();
        
        // Construct the normalized tf vector
        termsEnum = terms.iterator(null); // access the terms for this field
        int doclen = 0;
        while ((term = termsEnum.next()) != null) { // explore the terms for this field
            String termStr = term.utf8ToString();
            String stem = retriever.analyze(termStr);
            DocsEnum docsEnum = termsEnum.docs(null, null); // enumerate through documents, in this case only one
            while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                //get the term frequency in the document
                int tf = docsEnum.freq();
                TermFreq tfq = new TermFreq(
                        new Term(TrecDocRetriever.FIELD_ANALYZED_CONTENT, term),
                        termStr,
                        tf);
                tfvec.add(tfq);
                
                doclen += tf;
            }
        }
        
        for (TermFreq tf: tfvec) {
            tf.tf = tf.tf/(float)doclen; // normalize by len
            float idf = sumDf/reader.docFreq(tf.term);
            tf.wt = (float)(Math.log(1 + LAMBDA/(ONE_MINUS_LAMBDA)*tf.tf*idf));
        }
        
        Collections.sort(tfvec);
    }
    
    void loadDoc() throws Exception {
        IndexReader reader = retriever.getReader();
        IndexSearcher searcher = retriever.getSearcher();

        Term docIdTerm = new Term(TrecDocRetriever.FIELD_ID, this.docIdToGuess);
        TermQuery tq = new TermQuery(docIdTerm);

        TopScoreDocCollector collector = TopScoreDocCollector.create(1, true);            
        searcher.search(tq, collector);
        this.luceneDocIdToGuess = collector.topDocs().scoreDocs[0].doc;
        this.docToGuess = reader.document(luceneDocIdToGuess);
        this.contentOfDocToGuess = docToGuess.get(FIELD_ANALYZED_CONTENT);
    }
    
    String deStemWord(String word) {
        Pattern p = Pattern.compile(word + "\\S*");
        Matcher matcher = p.matcher(contentOfDocToGuess);
        
        if (matcher.find()) {
            String matched = matcher.group();
            int len = matched.length();
            char lastChar = matched.charAt(len-1);
            if (!Character.isLetter(lastChar))
                matched = matched.substring(0, len-1);
            return matched;
        }
        
        return word;
    }
    
    public void selectWords() {
        
        if (startState) {
            startState = false;
            wordsSharedNow = query.title;
            return;
        }
        
        StringBuffer buff = new StringBuffer();
        int start = numTermsShared;
        int end = Math.min(start + numTermsToShare, tfvec.size());
        
        if (start >= end) {
            wordsSharedNow = "No terms left to share!";
            return;
        }
        
        for (int i = start; i < end; i++) {
            String stemmedWord = tfvec.get(i).termStr;
            String deStemmed = deStemWord(stemmedWord); // take the most frequent destem
            buff.append(deStemmed).append(" ");
        }
        
        numTermsShared = end;
        wordsSharedNow = buff.toString();
    }
    
    public String getDocToGuess() { return this.docIdToGuess; }
    
    public void logGameState() {
        try {
            FileWriter fw = new FileWriter(logFileName, true);
            synchronized (this) {
                // Save the game state
                fw.write(this.toString() + "\n");
            }
            fw.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff
            .append(this.sessionId)
            .append("\t")
            .append(this.startingEpochs)
            .append("\t")
            .append(this.qid)
            .append("\t")
            .append(this.docIdToGuess)
            .append("\t")
            .append(this.lastDocumentSubmitted)
            .append("\t")
            .append(this.wordsSharedNow)
            .append("\t")
            .append(this.lastUserQuery)
            .append("\t")
            .append(this.relGuess)
            ;
                
        return buff.toString();
    }
    
    // Update the game state based on user move... User
    // submits a document as a guess...
    public void update(int guessedDocId, String guessedDocName, String query) {
        lastUserQuery = query;
        lastDocumentSubmitted = guessedDocName;
        terminateCode = GAME_TO_CONTINUE;
        
        if (guessedDocName.equalsIgnoreCase(docIdToGuess)) {
            // user guessed correctly...
            correctGuess = true;
            score += SCORE_INCREMENET_FOR_CORRECT_GUESS;
            terminateCode = CORRECT_GUESS_FOUND;
        }
        else if (this.rels.isRel(qid, guessedDocName)) {            
            // a correct relevant document is guessed
            relGuess = true;
            score += SCORE_INCREMENET_FOR_CORRECT_REL;            
        }
        else if (this.startState) {
            // new game starting
            score = INIT_SCORE;
        }
        else {
            relGuess = false;
            score += SCORE_INCREMENET_FOR_INCORRECT_REL;
        }
        if (score == GAME_TERMINATION_SCORE) {
            terminateCode = SCORE_REACHED_MIN_THRESH;
        }
        else {
            // Got to share words with the player...
            selectWords();
            submitInfos.add(new UserSubmitInfo(wordsSharedNow, guessedDocId, guessedDocName, relGuess));            
        }
        logGameState();
    }
    
    public String buildJSON() {
        StringBuffer buff = new StringBuffer("{");
        buff.append("\"score\": \"").append(score).append("\", ");
        
        /* Return all the words everytime so that the client side
           needn't do anything else but to set the HTML...
        */
        buff.append("\"words\": \"");
        buff.append("<table>");
        for (UserSubmitInfo submitInfo : this.submitInfos) {
            buff
                .append("<tr>")
                .append(submitInfo)
                .append("</tr>");
        }
        buff.append("</table>");
        buff.append("\", ");
        
        buff.append("\"terminate\": ").append(terminateCode).append(", ");
        
        String msg = correctGuess?
                "You WIN!! You have guessed the document " + this.docIdToGuess + " correctly.":
                relGuess?
                "Congratulations!! You have hit a relevant document.":
                wordsSharedNow == query.title?
                "Shared the query to start with.":
                "Oops!! Wrong guess and no hit on a relevant document."
                ;
        buff.append("\"msg\": \"").append(msg).append("\"");
        buff.append("}");
        return buff.toString();
    }
}

