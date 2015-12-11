/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package retriever;

/**
 *
 * @author Debasis
 */
/* Inserts HTML highlighting tags at matched places. */

import java.util.regex.*;
import java.util.*;

public class TextAnnotator {
    String content;
    String startAnnotationMarker;
    String endAnnotationMarker;
    BitSet matched;

    public TextAnnotator(String content) {
        this.content = content;
        startAnnotationMarker = "<b>";
        endAnnotationMarker = "</b>";
        matched = new BitSet(content.length());
    }

    public String annotate(String[] regexList) {
        StringBuffer modifiedContent = new StringBuffer();
        for (String regex: regexList) {
                annotate(regex);
        }

        // Union of match intervals
        boolean prevBit = false, thisBit = false;
        for (int i = 0; i < content.length(); i++) {
                thisBit = matched.get(i);
                if (!prevBit && thisBit) {      // transition from 0-1
                        modifiedContent.append(startAnnotationMarker);
                        modifiedContent.append(content.charAt(i));
                }
                else if (prevBit && !thisBit) { // transition from 1-0
                        modifiedContent.append(endAnnotationMarker);
                        modifiedContent.append(content.charAt(i));
                }
                else { // 0-0 or 1-1
                        modifiedContent.append(content.charAt(i));
                }
                prevBit = thisBit;
        }
        if (thisBit)
                modifiedContent.append(endAnnotationMarker);

        return modifiedContent.toString();
    }

    void annotate(String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(content);
        int start, end;

        while (matcher.find()) {
            start = matcher.start();
            end = matcher.end();
            for (int i = start; i < end; i++)
                matched.set(i); // set the bits
        }
    }

    public static void main(String[] args) {
        //String content = "This is a test string where the word string Occurs Twice And the word word thrice";
        //String[] regxps = {"This is a", "is a test", "([A-Z][A-z]+\\s+)+"};
        String content = "This is a test string where the word string Occurs Twice And the word word thrice";
        String[] regxps = {"This*", "test*"};
        TextAnnotator ta = new TextAnnotator(content);
        String newContent = ta.annotate(regxps);
        System.out.println(newContent);
    }
}
