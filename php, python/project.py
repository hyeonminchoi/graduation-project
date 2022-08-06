import pygatt
import time
import binascii
import pymysql
import threading
import urllib.request
import datetime

_TIMER = 60
_DEBUG = True

#데이터베이스 정보 입력
_USER = "project"
_PASSWD = "project"
_DB = "project"

def SonoffStatusCheck():
    prev = time.time()
    curr = time.time()
    while True:
        curr = time.time()
        if(curr-prev>=3):
            prev=curr
            try:
                conn = pymysql.connect(host="localhost", user=_USER, passwd=_PASSWD, db=_DB, charset='utf8')
                cursor = conn.cursor()
                test = cursor.execute("SELECT * FROM sonoff_list")
                sonoff_list = cursor.fetchall()
                for i in range(len(sonoff_list)):
                    try:
                        url = 'http://' + sonoff_list[i][0] + '/cm?cmnd=Power'
                        result = urllib.request.urlopen(url).read().decode()
                        if(_DEBUG):
                            now = datetime.datetime.now()
                            print("%d/%d/%d %02d:%02d:%02d %s %s"%(now.year, now.month, now.day, now.hour, now.minute, now.second, sonoff_list[i][0],result))
                        if("ON" in result):
                            cursor.execute("UPDATE sonoff_list SET Status = 'on' WHERE Ip='%s'"%sonoff_list[i][0])
                        elif("OFF" in result):
                            cursor.execute("UPDATE sonoff_list SET Status = 'off' WHERE Ip='%s'"%sonoff_list[i][0])
                        conn.commit()
                    except:
                        if(_DEBUG):
                            now = datetime.datetime.now()
                            print("%d/%d/%d %02d:%02d:%02d %s %s"%(now.year, now.month, now.day, now.hour, now.minute, now.second, sonoff_list[i][0],"NULL"))
                        cursor.execute("UPDATE sonoff_list SET Status = 'NULL' WHERE Ip='%s'"%sonoff_list[i][0])
                        conn.commit()
                conn.close()
            except:
                pass

def SonoffPowerOff(macAddress):
    try:
        conn = pymysql.connect(host="localhost", user=_USER, passwd=_PASSWD, db=_DB, charset='utf8')
        cursor = conn.cursor()
        test = cursor.execute("SELECT * FROM link WHERE MacAddress = '%s'"% macAddress)
        sonoff_list =  cursor.fetchall()
        for i in range(len(sonoff_list)):
            cursor.execute("SELECT Status FROM sonoff_list WHERE Ip = '%s'"%sonoff_list[i][1])
            result = cursor.fetchall()
            status = result[0][0]
            if(status=='on'):
                try:
                    url = 'http://' + sonoff_list[i][1] + '/cm?cmnd=Power%20Off'
                    urllib.request.urlopen(url)
                    cursor.execute("UPDATE sonoff_list SET Status = 'off' WHERE Ip='%s'"%sonoff_list[i][1])
                    conn.commit()
                    if(_DEBUG):
                        now = datetime.datetime.now()
                        print("%d/%d/%d %02d:%02d:%02d %s %s"%(now.year, now.month, now.day, now.hour, now.minute, now.second, macAddress ,url))
                except:
                    continue
        conn.close()
    except:
        pass
    

adapter = pygatt.GATTToolBackend()
uuid = "0000DFB1-0000-1000-8000-00805F9B34FB"


device = None
temp = []
def handle_data(handle, value):
    global temp
    for i in range(len(value)):
        temp += (chr(value[i]))  # 문자가 끊겨서 읽히는 경우가 있어서 전역변수 사용

arr = [1]
user_list = ()
prev = time.time()
curr = time.time()

threading.Thread(target=SonoffStatusCheck).start()

while True:
    curr = time.time()
    if(curr - prev >= _TIMER):
        conn = pymysql.connect(host="localhost", user=_USER, passwd=_PASSWD, db=_DB, charset='utf8')
        cursor = conn.cursor()
        test = cursor.execute("SELECT * FROM user")
        user_list =  cursor.fetchall()
        prev = curr
        for i in range(len(user_list)):
            try:
                temp.clear()
                adapter.start()
                device = adapter.connect(user_list[i][0])
                device.char_write(uuid,arr)
                device.subscribe(uuid, callback = handle_data,wait_for_response = False)
                time.sleep(1) # 이거 없으면 if문은 실행되는데 문자열이 없는  경우가 있음
                sql = "INSERT INTO %s VALUES(DEFAULT, %d)"
                if(len(temp)>0): # 심박수가 기록되어 있다면 저장
                    heartrate = int(''.join(temp))
                    if(heartrate>=40 and heartrate<=130):
                        if(_DEBUG):
                            now = datetime.datetime.now()
                            print("%d/%d/%d %02d:%02d:%02d %s %d"%(now.year, now.month, now.day, now.hour, now.minute, now.second, user_list[i][1], heartrate))
                        cursor.execute(sql%(user_list[i][1],(heartrate)))
                        conn.commit()
                        temp.clear()
                    try:
                        if(heartrate<user_list[i][2]):
                            if(_DEBUG):
                                now = datetime.datetime.now()
                                print("%d/%d/%d %02d:%02d:%02d %s의 심박수: %d, Threshold: %d / 연결된 전원 차단"%(now.year, now.month, now.day, now.hour, now.minute, now.second, user_list[i][1], heartrate, user_list[i][2]))
                            t1 = threading.Thread(target=SonoffPowerOff, args=(user_list[i][0],))
                            t1.start()
                    except:
                        pass
            except:
                pass
            finally:
                try:
                    adapter.stop()
                except:
                    pass
        conn.close()
