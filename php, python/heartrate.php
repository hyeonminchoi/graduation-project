<?php
session_start();
$RaspiDatabaseID = $_POST['RaspiDatabaseID'];
$RaspiDatabasePW = $_POST['RaspiDatabasePW']; 
$RaspiDatabase = $_POST['RaspiDatabase'];
$conn = mysqli_connect("127.0.0.1","$RaspiDatabaseID","$RaspiDatabasePW","$RaspiDatabase");
mysqli_set_charset($conn,"utf8");
$MacAddress = $_POST['MacAddress'];
$Count = $_POST['Count'];
$sql = "select Name from user where MacAddress='$MacAddress'";
$result = mysqli_query($conn, $sql);
$arr = mysqli_fetch_array($result);
$Name = $arr[0];
$sql = "";
if($Count==0)
	$sql = "select * from $Name";
else
	$sql = "select * from $Name ORDER BY time DESC LIMIT $Count";
$result = mysqli_query($conn, $sql);
$total_record = mysqli_num_rows($result);

if($Count==0){
	for($i=0;$i<$total_record;$i++){
		mysqli_data_seek($result,$i);

		$row = mysqli_fetch_array($result);
		echo $row[time]. " ".$row[heartrate]."<br>\n";
	}
} else{
	for($i=$total_record-1;$i>=0;$i--){
		mysqli_data_seek($result,$i);

		$row = mysqli_fetch_array($result);
		echo $row[time]. " ".$row[heartrate]."<br>\n";
	}
}
mysqli_close($conn);
?>
