<%-- 
    Document   : search
    Created on : 30-Nov-2015, 23:39:51
    Author     : Debasis
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link href="http://fonts.googleapis.com/css?family=Open+Sans&subset=latin,cyrillic" rel="stylesheet"
              type="text/css">
        <link href="css/bootstrap.min.css" rel="stylesheet">
        <link href='https://fonts.googleapis.com/css?family=Architects+Daughter' rel='stylesheet' type='text/css'>
        
        <!--link rel="stylesheet" type="text/css" href="css/stylesheet.css" media="screen"/-->
        <!--link rel="stylesheet" type="text/css" href="css/pygment_trac.css" media="screen"/-->
        <!--link rel="stylesheet" type="text/css" href="css/print.css" media="print"/-->
        
        <link href="jquery/themes/ui-lightness/jquery-ui.css" rel="stylesheet">

        <script src="jquery/jquery-2.1.4.js"></script>
        <script src="jquery/jquery-ui.js"></script>
        <script src="jquery/spin.js"></script>
        <script src="jquery/jquery.twbsPagination.js" type="text/javascript"></script>
        
        <script>
            var opts = {
              lines: 13 // The number of lines to draw
            , length: 28 // The length of each line
            , width: 14 // The line thickness
            , radius: 42 // The radius of the inner circle
            , scale: 1 // Scales overall size of the spinner
            , corners: 1 // Corner roundness (0..1)
            , color: '#000' // #rgb or #rrggbb or array of colors
            , opacity: 0.25 // Opacity of the lines
            , rotate: 0 // The rotation offset
            , direction: 1 // 1: clockwise, -1: counterclockwise
            , speed: 1 // Rounds per second
            , trail: 60 // Afterglow percentage
            , fps: 20 // Frames per second when using setTimeout() as a fallback for CSS
            , zIndex: 2e9 // The z-index (defaults to 2000000000)
            , className: 'spinner' // The CSS class to assign to the spinner
            , top: '50%' // Top position relative to parent
            , left: '50%' // Left position relative to parent
            , shadow: false // Whether to render a shadow
            , hwaccel: false // Whether to use hardware acceleration
            , position: 'absolute' // Element positioning
            };
            var spinner;

            $(document).ready(function() {
                $('#serp').hide();
                $("#srchBttn").button().click(retrieveFirstPage);
            });
           
            function viewDoc() {
                var id = this.id;
                var score = this.rev;
                var docName = this.name;
                var url = "DocumentViewer?id=" + id;
                var target = document.getElementById('containerdiv');                
                spinner = new Spinner(opts).spin(target);
                target.appendChild(spinner.el);                
                $.ajax({ url: url,
                        success: function(result) {
                            // set the content of thid dialog box
                            // by dynamically loading the content
                            // from the servlet
                            spinner.stop();
                            $("#documentSubmitDlg").html(result);
                            
                            var w = 0.8*$(window).width();
                            var h = 0.8*$(window).height();
                            
                            $("#documentSubmitDlg").dialog({
                              resizable: false,
                              width: w,  
                              height: h,
                              modal: true,
                              buttons: {
                                "Submit": function() {
                                    $.ajax({
                                        url: "GameManagerServlet?docguessed=" + docName +
                                                "&query=" + $("#query").val() +
                                                "&guessid=" + id + "&score=" + score,
                                        success: function(jsonResponse) {
                                            var jsonObj = jQuery.parseJSON(jsonResponse);
                                            var score = jsonObj.score;
                                            var words = jsonObj.words;
                                            var msg = jsonObj.msg;
                                            var terminate = jsonObj.terminate;
                                            
                                            window.parent.$('#gamepanel').html(words);
                                            window.parent.$("#scoreboard").html(score);
                                            window.parent.$("#msgboard").html(msg);
                                                                                        
                                            window.parent.$("#gamepanel a").button().click(viewDoc);
                                            
                                            $("#accordion").accordion("option", "active", 2);
                                            
                                            if (terminate == 4) {
                                                window.parent.$("#gameTerminationDlg").html(
                                                        "Doc " + docName + " already submitted. Submit a different one!");
                                                window.parent.$("#gameTerminationDlg").dialog({
                                                    modal: true,
                                                    buttons: {
                                                      Ok: function() {
                                                        window.parent.$("#gameTerminationDlg").dialog("close");
                                                      }
                                                    }
                                                });                                                
                                            }
                                            else if (terminate == 2) {
                                                window.parent.$("#gameTerminationDlg").html(
                                                        "Game Over! Your score reached 0...");
                                                window.parent.$("#gameTerminationDlg").dialog({
                                                    modal: true,
                                                    buttons: {
                                                      Ok: function() {
                                                        window.parent.$("#gameTerminationDlg").dialog("close");
                                                      }
                                                    }
                                                });
                                            }
                                            else if (terminate == 1) {
                                                window.parent.$("#gameTerminationDlg").html(
                                                    "Congratualtions! " +
                                                    "You've WON by guessing the correct document." +
                                                    "Press \"New Game\" to start a new game"                                                        
                                                );
                                                window.parent.$("#gameTerminationDlg").dialog({
                                                    modal: true,
                                                    buttons: {
                                                      Ok: function() {
                                                        window.parent.$("#gameTerminationDlg").dialog("close");
                                                      }
                                                    }
                                                });
                                            }
                                        }
                                    });                
                                    
                                    $(this).dialog("close");
                                },
                                "Cancel": function() {
                                    $(this).dialog("close");
                                }
                              }
                            });                            
                        }});                
            }
            
            function retrieveFirstPage() {
                retrieveAdhoc(1);
            }
            
            function retrieveAdhoc(page) {
                if ($("#query").val().length == 0) {
                    $("#query").focus();
                    return;
                }
                
                var query = $("#query").val().replace(".", " ");
                var url = "SearchServlet?query=" + query + "&page=" + page;
                
                var target = document.getElementById('containerdiv');                
                spinner = new Spinner(opts).spin(target);
                target.appendChild(spinner.el);                
                
                $.ajax({ url: url,
                        success: function(result) {
                            spinner.stop();
                            $("#srchres").html(result);
                            
                            $(".ResultURLStyle a").button().click(viewDoc);
                            
                            $('#serp').twbsPagination({
                                    totalPages: 10,
                                    visiblePages: 5,
                                    onPageClick: function (event, page) {
                                        retrieveAdhoc(page);
                                    }
                            });                            
                            $('#serp').show();
                        }});                
            }
            
        </script>
    </head>
    <body>
        <center>
        <br>    
        <div  id="containerdiv">
            <input type="text" id="query" name="query" size="50">
            <!--input type="button" value="Search" onclick="retrieveAdhoc(1)"/-->
            <button id="srchBttn">Search</button>
        </div>
            
        </center>
    
        <div id="srchres" name="srchres">            
        </div>
    
        <!-- Pagination component -->
        <div align="center">
            <ul id="serp" class="pagination-sm"></ul>
        </div>
    
        <!-- Document Viewer dialog box -->
        <div id="documentSubmitDlg" title="Submit this Document?">
        </div>        
        
    </body>
</html>
