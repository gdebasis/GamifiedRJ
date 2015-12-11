/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import game.GameState;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.http.HttpRequest;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import retriever.TextAnnotator;
import retriever.TrecDocRetriever;
import static servlets.GameManagerServlet.GAME_INFO_PARAM_NAME;

/**
 *
 * @author Debasis
 */
public class DocumentViewer extends HttpServlet {

    Properties prop;
    IndexReader reader;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        String propFileName = config.getInitParameter("configFile");
        this.prop = new Properties();
        try {
            prop.load(new FileReader(propFileName));
            String indexDir = prop.getProperty("index");
            reader = DirectoryReader.open(FSDirectory.open(new File(indexDir)));
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }

    String[] extractAnalyzedQueryTerms(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String analyzedQry = (String)session.getAttribute("analyzedqry");
        String[] analyzedQryTerms = analyzedQry.split("\\s+");
        for (int i = 0; i < analyzedQryTerms.length; i++) {
            analyzedQryTerms[i] += "*"; // for regular expression
        }
        return analyzedQryTerms;    
    }
        
    String highlightDoc(String[] queryTerms, String content) {
        TextAnnotator ta = new TextAnnotator(content);
        String highlighted = ta.annotate(queryTerms);
        return highlighted;
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
            
            out.println("<p>");
            String docId = request.getParameter("id");
            Document doc = reader.document(Integer.parseInt(docId));
            
            String[] queryTerms = extractAnalyzedQueryTerms(request);
            String highlightedHTML = highlightDoc(queryTerms,
                                doc.get(TrecDocRetriever.FIELD_ANALYZED_CONTENT));
            out.println(highlightedHTML);
            out.println("</p>");
        }
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
