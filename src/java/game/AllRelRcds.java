/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package game;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Debasis
 */

class PerQueryRelDocs {
    String qid;
    HashMap<String, Integer> relMap; // keyed by docid, entry stores the rel value
    int numRel;
    
    public PerQueryRelDocs(String qid) {
        this.qid = qid;
        numRel = 0;
        relMap = new HashMap<>();
    }
    
    void addTuple(String docId, int rel) {
        if (relMap.get(docId) != null)
            return;
        if (rel > 0) {
            numRel++;
            relMap.put(docId, rel);
        }
    }    
}

public class AllRelRcds {
    String qrelsFile;
    HashMap<String, PerQueryRelDocs> perQueryRels;
    int totalNumRel;

    public AllRelRcds(String qrelsFile) {
        this.qrelsFile = qrelsFile;
        perQueryRels = new HashMap<>();
        totalNumRel = 0;
        try {
            load();
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }

    public String selectRandomQuery() {
        String qid = null;
        int randIndex = (int)(Math.random() * perQueryRels.size());
        int i = -1;
        for (Map.Entry<String, PerQueryRelDocs> e : perQueryRels.entrySet()) {
            i++;
            qid = e.getKey();
            if (i >= randIndex)
                break;
        }
        return qid;
    }
    
    public String selectRandomRelDoc(String qid) {
        String docId = null;
        
        PerQueryRelDocs reldocs = perQueryRels.get(qid);
        int randIndex = (int)(Math.random() * reldocs.numRel);
        int i = -1;
        for (Map.Entry<String, Integer> rel : reldocs.relMap.entrySet()) {
            i++;
            docId = rel.getKey();
            if (i >= randIndex)
                break;
        }
        return docId;
    }
    
    int getTotalNumRel() {
        if (totalNumRel > 0)
            return totalNumRel;
        
        for (Map.Entry<String, PerQueryRelDocs> e : perQueryRels.entrySet()) {
            PerQueryRelDocs perQryRelDocs = e.getValue();
            totalNumRel += perQryRelDocs.numRel;
        }
        return totalNumRel;
    }
    
    private void load() throws Exception {
        FileReader fr = new FileReader(qrelsFile);
        BufferedReader br = new BufferedReader(fr);
        String line;
        
        while ((line = br.readLine()) != null) {
            storeRelRcd(line);
        }
        br.close();
        fr.close();
    }
    
    private void storeRelRcd(String line) {
        String[] tokens = line.split("\\s+");
        String qid = tokens[0];
        PerQueryRelDocs relTuple = perQueryRels.get(qid);
        if (relTuple == null) {
            relTuple = new PerQueryRelDocs(qid);
            perQueryRels.put(qid, relTuple);
        }
        relTuple.addTuple(tokens[2], Integer.parseInt(tokens[3]));
    }
    
    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (Map.Entry<String, PerQueryRelDocs> e : perQueryRels.entrySet()) {
            PerQueryRelDocs perQryRelDocs = e.getValue();
            buff.append(e.getKey()).append("\n");
            for (Map.Entry<String, Integer> rel : perQryRelDocs.relMap.entrySet()) {
                String docName = rel.getKey();
                int relVal = rel.getValue();
                buff.append(docName).append(",").append(relVal).append("\t");
            }
            buff.append("\n");
        }
        return buff.toString();
    }
    
    PerQueryRelDocs getRelInfo(String qid) {
        return perQueryRels.get(qid);
    }
    
    boolean isRel(String qid, String docName) {
        PerQueryRelDocs relDocs = perQueryRels.get(qid);
        Integer rel = relDocs.relMap.get(docName);
        return rel==null? false: true;
    }
}
