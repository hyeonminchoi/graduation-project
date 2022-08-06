<?php
session_start();
$RaspiDatabaseID = $_POST['RaspiDatabaseID'];
$RaspiDatabasePW = $_POST['RaspiDatabasePW']; 
$RaspiDatabase = $_POST['RaspiDatabase'];
$conn = mysqli_connect("127.0.0.1","$RaspiDatabaseID","$RaspiDatabasePW","$RaspiDatabase");
mysqli_set_charset($conn,"utf8");
$MacAddress = $_POST['MacAddress'];
$ArrayList = $_POST['ArrayList'];
mysqli_query($conn, "CREATE TABLE sonoff_filter(MacAddress varchar(20), Ip varchar(20), FOREIGN KEY(Ip) REFERENCES sonoff_list(Ip) ON DELETE CASCADE)");
mysqli_query($conn, "DELETE FROM sonoff_filter WHERE MacAddress = '$MacAddress'");
$temp = substr($ArrayList, 1, count($ArrayList)-2);
$temp = str_replace(" ","",$temp);
$temp = explode(",",$temp);
$result = 1;
foreach($temp as $Ip){
	if($Ip=="")
		break;
	$result = mysqli_query($conn, "INSERT INTO sonoff_filter VALUES('$MacAddress', '$Ip')");
	if($result==0){
		echo 'failure';
		break;
	}
}
if($result)
	echo 'success';
mysqli_close($conn);
?>
