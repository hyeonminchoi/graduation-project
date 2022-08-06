<?php
session_start();
$RaspiDatabaseID = $_POST['RaspiDatabaseID'];
$RaspiDatabasePW = $_POST['RaspiDatabasePW']; 
$RaspiDatabase = $_POST['RaspiDatabase'];
$conn = mysqli_connect("127.0.0.1","$RaspiDatabaseID","$RaspiDatabasePW","$RaspiDatabase");
mysqli_set_charset($conn,"utf8");
$MacAddress = $_POST['MacAddress'];
$sql = "select Name from user where MacAddress='$MacAddress'";
$result = mysqli_query($conn, $sql);
$arr = mysqli_fetch_array($result);
$Name = $arr[0];
$sql = "DELETE FROM $Name";
$result = mysqli_query($conn, $sql);
if($result)
	echo 'success';
else
	echo 'failure';
mysqli_close($conn);
?>
