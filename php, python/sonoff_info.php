<?php
session_start();
$RaspiDatabaseID = $_POST['RaspiDatabaseID'];
$RaspiDatabasePW = $_POST['RaspiDatabasePW']; 
$RaspiDatabase = $_POST['RaspiDatabase'];
$conn = mysqli_connect("127.0.0.1","$RaspiDatabaseID","$RaspiDatabasePW","$RaspiDatabase");
mysqli_set_charset($conn,"utf8");
$command = $_POST['command'];
$Ip = $_POST['Ip'];
$Name = $_POST['Name'];
$Status = $_POST['Status'];
$sql = "";
mysqli_query($conn, "CREATE TABLE IF NOT EXISTS sonoff_list(Ip varchar(20) NOT NULL PRIMARY KEY, Name varchar(20) UNIQUE, Status varchar(5))");

if($command == "add")
	$sql = "INSERT INTO sonoff_list VALUES('$Ip', '$Name', DEFAULT)";
else if($command == "modify")
	$sql = "UPDATE sonoff_list SET Name = '$Name' WHERE Ip='$Ip'";
else if($command == "delete")
	$sql = "DELETE FROM sonoff_list WHERE Ip='$Ip'";
else if($command == "status"){
	$ch = curl_init();
	if($Status=="on"){
		$url = "http://$Ip/cm?cmnd=Power%20On";
		curl_setopt($ch, CURLOPT_URL, $url);
		curl_exec($ch);
	}
	if($Status=="off"){
		$url = "http://$Ip/cm?cmnd=Power%20Off";
		curl_setopt($ch, CURLOPT_URL, $url);
		curl_exec($ch);
	}
	curl_close($ch);
	$sql = "UPDATE sonoff_list SET Status = '$Status' WHERE Ip='$Ip'";
}
$result = mysqli_query($conn, $sql);
if($result)
	echo 'success';
else
	echo 'failure';

mysqli_close($conn);
?>
