<?php
session_start();
$RaspiDatabaseID = $_POST['RaspiDatabaseID'];
$RaspiDatabasePW = $_POST['RaspiDatabasePW']; 
$RaspiDatabase = $_POST['RaspiDatabase'];
$conn = mysqli_connect("127.0.0.1","$RaspiDatabaseID","$RaspiDatabasePW","$RaspiDatabase");
mysqli_set_charset($conn,"utf8");
$command = $_POST['command'];
$MacAddress = $_POST['MacAddress'];
$Name = $_POST['Name'];
$Threshold = $_POST['Threshold'];
$sql = "";
mysqli_query($conn, "CREATE TABLE IF NOT EXISTS user(MacAddress varchar(20) PRIMARY KEY, Name varchar(20) UNIQUE, Threshold int)");
$temp = mysqli_query($conn, "SELECT Name FROM user WHERE MacAddress='$MacAddress'");
$arr = mysqli_fetch_array($temp);
$temp_Name = $arr[0];

if($command == "add")
	$sql = "INSERT INTO user VALUES('$MacAddress', '$Name', NULL)";
else if($command == "modify")
	$sql = "UPDATE user SET Name = '$Name' WHERE MacAddress='$MacAddress'";
else if($command == "delete")
	$sql = "DELETE FROM user WHERE MacAddress='$MacAddress'";
else if($command == "threshold")
	$sql = "UPDATE user SET Threshold = '$Threshold' WHERE MacAddress='$MacAddress'";
$result = mysqli_query($conn, $sql);
if($result){
	if($command == "add")
		mysqli_query($conn, "CREATE TABLE IF NOT EXISTS $Name(time timestamp DEFAULT current_timestamp(), heartrate int NOT NULL)");
	else if($command == "modify")
		mysqli_query($conn, "RENAME TABLE $temp_Name TO $Name");
	else if($command == "delete")
		mysqli_query($conn, "DROP TABLE $temp_Name");
	echo 'success';
}
else
	echo 'failure';
mysqli_close($conn);
?>
