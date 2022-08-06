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
if($command =="all"){
	$query = "SELECT * FROM sonoff_list";
	$result = mysqli_query($conn, $query);
	$total_record = mysqli_num_rows($result);
	for($i=0;$i<$total_record;$i++)
	{
		mysqli_data_seek($result,$i);
		$row = mysqli_fetch_array($result);
		echo $row['Ip']. " ".$row['Name']. " ".$row['Status']."<br>\n";
	}
}
else if($command =="select"){
	$query = "SELECT Ip FROM sonoff_filter WHERE MacAddress='$MacAddress'";
	$result = mysqli_query($conn, $query);
	while($row=mysqli_fetch_array($result)){
		$Ip = $row['Ip'];
		$query = "SELECT * FROM sonoff_list WHERE Ip = '$Ip'";
		$temp = mysqli_query($conn, $query);
		$arr = mysqli_fetch_array($temp);
		echo $arr['Ip']. " ".$arr['Name']. " ".$arr['Status']."<br>\n";
	}
}
?>
