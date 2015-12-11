/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import game.AllRelRcds;
import game.GameState;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.lucene.index.IndexReader;
import retriever.TrecDocRetriever;

/**
 *
 * @author Debasis
 */

public class GameManagerServlet extends HttpServlet {
    String propFileName;
    TrecDocRetriever retriever;
    AllRelRcds rels;
    IndexReader reader;

    static final String GAME_INFO_PARAM_NAME = "gameinfo";
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        propFileName = config.getInitParameter("configFile");
        try {
            retriever = new TrecDocRetriever(propFileName);
            Properties prop = retriever.getProperties();
            rels = new AllRelRcds(prop.getProperty("qrels.file"));
            reader = retriever.getReader();
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }
    
    GameState initiateNewGame(HttpSession session) {
        session.removeAttribute(GAME_INFO_PARAM_NAME); // terms already shared
        
        GameState gameState = new GameState(retriever, rels, session.getId());
        session.setAttribute(GAME_INFO_PARAM_NAME, gameState);
        return gameState;
    }
    
    protected boolean isGameOver(GameState gameState, String submittedDoc) {
        if (submittedDoc.equalsIgnoreCase(gameState.getDocToGuess())) {
            // user guessed correctly
            return true;
        }
        return false;
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
        
        HttpSession session = request.getSession();
        int guessedDocId = Integer.parseInt(request.getParameter("guessid"));
        String submittedDoc = request.getParameter("docguessed");
        String query = request.getParameter("query");
        if (submittedDoc.equals("none")) {
            // start a new game... delete the previous one saved in session
            session.removeAttribute(GAME_INFO_PARAM_NAME);
        }
        
        GameState gameState = (GameState)session.getAttribute(GAME_INFO_PARAM_NAME);
        if (gameState == null) {
            gameState = initiateNewGame(session);            
        }
        gameState.update(guessedDocId, submittedDoc, query);
        
        // Construct a JSON response to send out to the client...
        String json = gameState.buildJSON();
        try (PrintWriter out = response.getWriter()) {
            out.println(json);
            System.out.println("json response: " + json);
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
