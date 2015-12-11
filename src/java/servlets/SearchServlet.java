package servlets;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import retriever.TrecDocRetriever;
import static retriever.TrecDocRetriever.FIELD_ANALYZED_CONTENT;
import static retriever.TrecDocRetriever.FIELD_ID;

/**
 *
 * @author Debasis
 */

public class SearchServlet extends HttpServlet {

    String propFileName;
    TrecDocRetriever retriever;
    
    static final int NUMDOCS_TO_RETRIEVE = 100;    
    static final int PAGE_SIZE = 10;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        propFileName = config.getInitParameter("configFile");
        try {
            retriever = new TrecDocRetriever(propFileName);
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }
    
    public String getSearchResultsDisplayString(Query query, TopDocs topDocs, int page) throws Exception {
        
        ScoreDoc[] hits = topDocs.scoreDocs;
        int start, end;
        start = (page-1)*PAGE_SIZE;  // starting from this index
        end = Math.min(start + PAGE_SIZE, hits.length); // ending before this index
        
        SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
        Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
        StringBuffer resultsBuff = new StringBuffer("<ul>");
        IndexSearcher searcher = retriever.getSearcher();
        Analyzer analyzer = retriever.getAnalyzer();
        
        for (int i = start; i < end; i++) {
            ScoreDoc hit = hits[i];
            resultsBuff.append("<li>");
            Document doc = searcher.doc(hit.doc);
            String text = doc.get(FIELD_ANALYZED_CONTENT);
            String docName = doc.get(FIELD_ID);
            resultsBuff.append("<div class=\"ResultURLStyle\">")
                    .append("<a id=\"")
                    .append(hit.doc)
                    .append("\" name=\"")
                    .append(docName)
                    .append("\">")
                    .append(docName)
                    .append("</a>")
                    .append("</div>");
            
            resultsBuff.append("<div class=\"ResultSnippetStyle\">");
            TokenStream tokenStream = TokenSources.getTokenStream(
                    searcher.getIndexReader(), hit.doc, FIELD_ANALYZED_CONTENT,
                    analyzer);
            TextFragment[] frags = highlighter.getBestTextFragments(tokenStream, text, false, 5);
            for (TextFragment frag : frags) {
                if ((frag != null) && (frag.getScore() > 0)) {
                    resultsBuff.append(frag.toString());
                }
            }
            resultsBuff.append("...");
            resultsBuff.append("</div>");
            resultsBuff.append("</li>");                        
        }
        
        resultsBuff.append("</ul>");
        return resultsBuff.toString();
    }
    
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        
        try (PrintWriter out = response.getWriter()) {            
            
            int page = Integer.parseInt(request.getParameter("page"));
            String query = request.getParameter("query");

            HttpSession session = request.getSession();
            TopDocs topDocs = null;
            if (page > 1) {
                // Get the saved topdocs
                topDocs = (TopDocs)session.getAttribute("prevres");
            }
            else {
                // first hit
                topDocs = retriever.retrieve(query, NUMDOCS_TO_RETRIEVE);
                session.setAttribute("prevres", topDocs);
                
                // Save the analyzed query (to be lated used for highlighting
                // each document content in the viewer
                String analyzedQry = retriever.analyze(query);                
                session.setAttribute("analyzedqry", analyzedQry);
            }
        
            String responseStr = getSearchResultsDisplayString(retriever.buildQuery(query), topDocs, page);
            out.println(responseStr);
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
