<%-- 
    Document   : index
    Created on : 30-Nov-2015, 15:38:59
    Author     : Debasis
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Document Miner</title>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <!--link href="http://fonts.googleapis.com/css?family=Open+Sans&subset=latin,cyrillic" rel="stylesheet"
              type="text/css"-->
        <!--link href="http://netdna.bootstrapcdn.com/bootstrap/3.0.3/css/bootstrap.min.css" rel="stylesheet">
        <link href='https://fonts.googleapis.com/css?family=Architects+Daughter' rel='stylesheet' type='text/css'-->
        <!--link rel="stylesheet" type="text/css" href="css/stylesheet.css" media="screen"/>
        <link rel="stylesheet" type="text/css" href="css/pygment_trac.css" media="screen"/>
        <link rel="stylesheet" type="text/css" href="css/print.css" media="print"-->
        <link rel="stylesheet" href="css/uilayout.css" />
        <link rel="stylesheet" href="css/displayres.css" />
        <link href="jquery/themes/ui-lightness/jquery-ui.css" rel="stylesheet">

        <script src="jquery/jquery-2.1.4.js"></script>
        <script src="jquery/jquery-ui.js"></script>
	<script type="text/javascript" src="jquery/jquery.layout.js"></script>
        
        <script>

            //+++Start: Layout code
            var myLayout; // a var is required because this page utilizes: myLayout.allowOverflow() method

            $(document).ready(function () {

                myLayout = $('body').layout({
                        west__size:			500 /* use this to re-size the west pane */
                ,	west__spacing_closed:		20
                ,	west__togglerLength_closed:	100
                ,	west__togglerAlign_closed:	"top"
                ,	west__togglerContent_closed:"M<BR>E<BR>N<BR>U"
                ,	west__togglerTip_closed:	"Open & Pin Menu"
                ,	west__sliderTip:			"Slide Open Menu"
                ,	west__slideTrigger_open:	"mouseover"
                ,	center__maskContents:		true // IMPORTANT - enable iframe masking
                });

                $("#newgameBttn").button().click(newGame);
                $("#helpBttn").button();
                $("#accordion").accordion({
                        heightStyle: "content"
                    }
                );

                newGame();
            });
            //---End: Layout code
            
            function newGame() {
                $.ajax({ url: "GameManagerServlet?docguessed=none&guessid=-1&score=0",
                        success: function(jsonResponse) {                            
                            var jsonObj = jQuery.parseJSON(jsonResponse);
                            var score = jsonObj.score;
                            var words = jsonObj.words;
                            var msg = jsonObj.msg;
                            var terminate = jsonObj.terminate;
                            
                            $("#gamepanel").html(words);
                            $("#scoreboard").html(score);
                            $("#msgboard").html(msg);
                
                            $("#accordion").accordion("option", "active", 2);                
                        }
                    }
                );                
            }            
        </script>
        
    </head>
    <body>    
<iframe id="mainFrame" name="mainFrame" class="ui-layout-center"
	width="100%" height="600" frameborder="0" scrolling="auto"
        src="search.jsp">            
</iframe>

<div class="ui-layout-west">
    <center>
        <table>
        <tr>
        <td>
        <img src="images/detective.png" alt="ADAPT Centre, DCU"
             border="0" style="max-width: 100px; max-height:100px;">
        </td>
        <td>
        <h2> Document Miner </h2>
        </td>
        </tr>
        </table>
    <!--table>
    <tr><td-->
    <!--input type="button" value="New Game" id="newgame" name="newgame" onclick="newGame()"-->
    <button id="newgameBttn">New Game</button> 
    <button id="helpBttn">Help</button> 
    <!--/td><td>
    <div id="scoreboard"></div>
    </td></tr>
    </table-->
    </center>
    <br><br>
    <br>
    
    <div id="accordion">
        <h5>Score: </h5>
        <div id="scoreboard">
        </div>
        <h5> Status Message: </h5>
        <div id="msgboard">
        </div>
        <h5> Terms Shared: </h5>
        <div id="gamepanel">
        </div>    
    </div>
</div>
<!-- Document Viewer dialog box -->
<div id="gameTerminationDlg" title="Game Over...">
</div>        

    </body>
    
</html>
