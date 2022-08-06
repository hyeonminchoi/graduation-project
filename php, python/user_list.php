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
$DeviceMacAddress = $_POST['MacAddress'];

if($command =="all"){
	$query = "SELECT * FROM user";
	$result = mysqli_query($conn, $query);
	
	$total_record = mysqli_num_rows($result);
	
	for($i=0;$i<$total_record;$i++)
	{
		mysqli_data_seek($result,$i);
	
		$row = mysqli_fetch_array($result);
		echo $row['MacAddress']. " ".$row['Name']. " ".$row['Threshold']."<br>\n";
	}
}

else if($command =="select"){
	$query = "SELECT MacAddress FROM user_filter WHERE DeviceMacAddress='$DeviceMacAddress'";
	$result = mysqli_query($conn, $query);
	while($row=mysqli_fetch_array($result)){
		$Mac = $row['MacAddress'];
		$query = "SELECT * FROM user WHERE MacAddress = '$Mac'";
		$temp = mysqli_query($conn, $query);
		$arr = mysqli_fetch_array($temp);
		echo $arr['MacAddress']. " ".$arr['Name']. " ".$arr['Threshold']."<br>\n";
	}
}
?>
