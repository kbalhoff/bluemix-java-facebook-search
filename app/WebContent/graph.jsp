<!DOCTYPE html>
<html lang="en">
<head>
<title>Facebook Search Analyzer</title>
  <link href="/css/bootstrap.css" rel="stylesheet">
  <style type="text/css">
    #keyword-form{
      margin-top: 10px;
      margin-left: 60px;
    }

    #graph{
      margin-top: 80px;
      float:left;
    }

    #countTable{
      margin-top: 80px;
      padding-right: 30px;
      position:relative;
    }
  </style>
</head>
<meta charset="utf-8">
<body>
  <nav class="navbar navbar-inverse navbar-fixed-top">
    <div class="navbar-inner">
      <a class="brand" href="/">
        <img src="/img/facebook_logo.png" class="img-rounded" height="30px" width="30px"> 
        Facebook Search Analyzer
      </a>
	    <form class="navbar-form pull-right" method="GET" action="/graph" id="search"> 
	      <div class="form-group">
	        <input id="keyword" name="keyword" type="text" class="form-control" placeholder="Keyword">
		      <div class="btn-group" data-toggle="radio">
		        <label class="btn btn-default">
		          <input type="radio" name="option" id="people" value="people"> People
		        </label>
		        <label class="btn btn-default">
		          <input type="radio" name="option" id="companies" value="companies"> Companies
		        </label>
		      </div>
		      <button type="submit" id="update" class="btn btn-default">Submit</button>
		    </div>
	    </form>
    </div>
  </nav>
  <div id="graph">
    <div id="warning"></div>
    <div id="error"></div>
    <div id="loading" style="position:absolute; margin-left:50%; margin-right:50%; margin-top: 20%;">
      <img src="/img/spinnerLarge.gif" style="display:block; margin: 0 auto;">
    </div>
    <canvas id="chart"></canvas>
  </div>
  <div id="countTable" class="pull-right">
    <table class="table table-hover">
      <thead>
        <tr>
          <th>Name</th>
          <th># of Mentions</th>
        </tr>
      </thead>
      <tbody>
      </tbody>
    </table>
  </div>
  <script src="/js/Chart.min.js"></script>
  <script src="/js/jquery-2.0.3.min.js"></script>
  <script src="/js/bootstrap.min.js"></script>
  <script>
    //check if all fields have been filled in properly
    $(document).ready(function () {
      $('#search').submit(function (event) {
        if ($('input[type=radio]:checked').size() == 0 || $('#keyword').val() == '') {
          $('#error').html('<div class="alert alert-danger">Oops, you forgot to fill in a required field.</div>');
          event.preventDefault();
        }
      });
    });

    // set height of graph canvas
    window.onload = function(){
      $("#graph").height((window.innerHeight - 50) * .75);
    };

    // send request to start analyzing facebook data
    $.get('/analyze?keyword=<%= request.getAttribute("keyword") %>&option=<%= request.getAttribute("option") %>', function (data) {
      console.log(JSON.stringify(data));
      
      // remove loading spinner from screen
      $('#loading').html('');
      
      if (data.error != null) {
        return $('#error').html('<div class="alert alert-danger"> Analytics service error: ' + data.error + '</div>');
      }
      
      if (data.labels.length == 0 && data.response.length == 0) {
        $('#warning').width((window.innerWidth - 50) * .75);
        return $('#warning').html('<div class="alert alert-info">No <%= request.getAttribute("keyword") %> mentioned in facebook results.</div>');
      }
      
      // ChartJS will crash the tab if labels are too long, so we truncate them for the chart
      var chartLabels = data.labels.map(function (label) {
        return label.length < 19 ? label : label.substring(0, 15) + '...';
      });
      
      var chartData = {
        labels: chartLabels,
        datasets: [{
          fillColor: 'rgba(220,220,220,0.5)',
          strokeColor: 'rgba(220,220,220,1)',
          data: data.response
        }]
      };

      // generate and draw table based upon analytics results
      var table = '<table class="table table-hover"';
      table += '<thead>';
      table += '<tr>';
      table += '<th>Name</th>';
      table += '<th># of Mentions</th>';
      table += '</thead>';   
      table += '<tbody>';
      for (var i = 0; i < data.labels.length; i++) {
        table += '<tr>';
        table += '<td>' + data.labels[i] + '</td>';
        table += '<td>' + data.response[i] +'</td>';
        table += '</tr>';
      }
      table += '</tbody>';
      table += '</table>';
      $('#countTable').html(table);
      
      var maxValue = data.response.reduce(function (prev, cur) {
        return Math.max(prev, cur);
      });
      
      var scaleStepWidth = maxValue < 10 ? 1 : Math.ceil(maxValue / 10);
      
      var options = { 
        scaleOverlay: false,
        scaleOverride: true,
        scaleStartValue: 0, 
        scaleSteps: (maxValue + 1) / scaleStepWidth,
        scaleStepWidth: scaleStepWidth
      };
      
      // draw graph
      var chart = new Chart(canvas.getContext("2d")).Bar(chartData, options);
      var canvas = $('#chart')[0];
      
      // scale graph and other text areas with window size
      var resize = function () { 
        $('#error').width((window.innerWidth - 50) * .75);
        $('#warning').width((window.innerWidth - 50) * .75);
        $("#graph").height((window.innerHeight - 50) * .90);
        $('#countTable').width((window.innerWidth - 50) * .25);
        canvas.width = (window.innerWidth - 50) * .75;
        canvas.height = (window.innerHeight - 50) * .90;
        chart = new Chart(canvas.getContext("2d")).Bar(chartData, options);
      };

      window.addEventListener('resize', resize, false);
      resize();
    });
</script>
</body>
</html>