<?php
session_start();
$RaspiDatabaseID = $_POST['RaspiDatabaseID'];
$RaspiDatabasePW = $_POST['RaspiDatabasePW'];
$RaspiDatabase = $_POST['RaspiDatabase'];
$conn = mysqli_connect("localhost", "$RaspiDatabaseID", "$RaspiDatabasePW", "$RaspiDatabase");
if(!$conn){
	echo "Error";
	return 0;
}

$command = $_POST['command'];
$MacAddress = $_POST['MacAddress'];
if($command =="all")
	$query = "SELECT * FROM sonoff_list";
else if($command =="select")
	$query = "SELECT * FROM link WHERE MacAddress='$MacAddress'";
$result = mysqli_query($conn, $query);
$total_record = mysqli_num_rows($result);
for($i=0;$i<$total_record;$i++)
{
	mysqli_data_seek($result,$i);
	$row = mysqli_fetch_array($result);
	if($command =="all")
		echo $row['Ip']. " ".$row['Name']. " ".$row['Status']."<br>\n";
	else if($command =="select")
		echo $row['Ip']."<br>\n";
}
?>
