<?php
session_start();
$RaspiDatabaseID = $_POST['RaspiDatabaseID'];
$RaspiDatabasePW = $_POST['RaspiDatabasePW']; 
$RaspiDatabase = $_POST['RaspiDatabase'];
$conn = mysqli_connect("127.0.0.1","$RaspiDatabaseID","$RaspiDatabasePW","$RaspiDatabase");
mysqli_set_charset($conn,"utf8");
$DeviceMacAddress = $_POST['MacAddress'];
$ArrayList = $_POST['ArrayList'];
mysqli_query($conn, "CREATE TABLE user_filter(DeviceMacAddress varchar(20), MacAddress varchar(20), FOREIGN KEY(MacAddress) REFERENCES user(MacAddress) ON DELETE CASCADE)");
mysqli_query($conn, "DELETE FROM user_filter WHERE DeviceMacAddress = '$DeviceMacAddress'");
$temp = substr($ArrayList, 1, count($ArrayList)-2);
$temp = str_replace(" ","",$temp);
$temp = explode(",",$temp);
$result = 1;
foreach($temp as $MacAddress){
	if($MacAddress=="")
		break;
	$result = mysqli_query($conn, "INSERT INTO user_filter VALUES('$DeviceMacAddress', '$MacAddress')");
	if($result==0){
		echo 'failure';
		break;
	}
}
if($result)
	echo 'success';
mysqli_close($conn);
?>
