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
$sql = "select avg(heartrate) from (SELECT * from $Name ORDER BY time DESC LIMIT 100) temp";
$result = mysqli_query($conn, $sql);
$row = mysqli_fetch_array($result);
echo $row[0];
mysqli_close($conn);
?>
