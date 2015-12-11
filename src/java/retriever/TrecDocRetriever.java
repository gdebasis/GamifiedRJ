/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package retriever;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.Version;
import trec.TRECQuery;
import trec.TRECQueryParser;

/**
 *
 * @author Debasis
 */

public class TrecDocRetriever {
    IndexReader reader;
    IndexSearcher searcher;    
    Analyzer analyzer;
    Properties prop;
    HashMap<String, TRECQuery> trecQriesMap;
    
    static final public String FIELD_ID = "id";
    static final public String FIELD_ANALYZED_CONTENT = "words";  // Standard analyzer w/o stopwords.    
    
    protected List<String> buildStopwordList(String stopwordFileName) {
        List<String> stopwords = new ArrayList<>();
        String stopFile = prop.getProperty(stopwordFileName);        
        String line;

        try {
            FileReader fr = new FileReader(stopFile);
            BufferedReader br = new BufferedReader(fr);
            
            while ( (line = br.readLine()) != null ) {
                stopwords.add(line.trim());
            }
            br.close();
            fr.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return stopwords;
    }
    
    public IndexReader getReader() { return reader; }
    public Analyzer getAnalyzer() { return analyzer; }
    public Properties getProperties() { return prop; }
    
    public TrecDocRetriever(String propFile) throws Exception {
        this.prop = new Properties();
        prop.load(new FileReader(propFile));
        
        String indexDir = prop.getProperty("index");
        
        reader = DirectoryReader.open(FSDirectory.open(new File(indexDir)));
        searcher = new IndexSearcher(reader);
        
        analyzer = new EnglishAnalyzer(
            Version.LUCENE_4_9,
            StopFilter.makeStopSet(
                Version.LUCENE_4_9, buildStopwordList("stopfile"))); // default analyzer
        
        trecQriesMap = new HashMap<>();
        List<TRECQuery> queries = constructQueries();
        for (TRECQuery q : queries) {
            trecQriesMap.put(q.id.trim(), q);
        }
    }
    
    public TRECQuery getQuery(String qid) {
        return trecQriesMap.get(qid);
    }

    public IndexSearcher getSearcher() { return searcher; }
    
    public List<TRECQuery> constructQueries() throws Exception {        
        String queryFilePropName = "query.file";
        String queryFile = prop.getProperty(queryFilePropName);
        TRECQueryParser parser = new TRECQueryParser(queryFile, analyzer);
        parser.parse();
        return parser.getQueries();        
    }    

    public TopDocs retrieve(String query, int nwanted) {
        try {
            searcher.setSimilarity(new BM25Similarity());

            TopScoreDocCollector collector;
            collector = TopScoreDocCollector.create(nwanted, true);

            Query luceneQuery = buildQuery(query);
            searcher.search(luceneQuery, collector);
            return collector.topDocs();        
        }
        catch (Exception ex) { ex.printStackTrace(); return null; }
    }
    
    public String analyze(String query) throws Exception {
        StringBuffer buff = new StringBuffer();
        TokenStream stream = analyzer.tokenStream("dummy", new StringReader(query));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            String term = termAtt.toString();
            term = term.toLowerCase();
            buff.append(term).append(" ");
        }
        stream.end();
        stream.close();
        return buff.toString();
    }
    
    public Query buildQuery(String queryStr) throws Exception {
        BooleanQuery q = new BooleanQuery();
        Term thisTerm = null;
        Query tq = null;
        String[] terms = analyze(queryStr).split("\\s+");

        for (String term : terms) {
            thisTerm = new Term(TrecDocRetriever.FIELD_ANALYZED_CONTENT, term);
            tq = new TermQuery(thisTerm);
            q.add(tq, BooleanClause.Occur.SHOULD);
        }
        return q;
    }
        
}
