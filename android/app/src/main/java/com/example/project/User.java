package com.example.project;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class User extends AppCompatActivity {
    private static LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private static boolean mScanning;
    private static String mMacAddress;
    private static String mName;
    private final long SCAN_PERIOD = 10000;
    private static FragmentManager mFragManager;
    private static UserInfoDialogFragment mUserInfoDialogFragment;
    private static ArrayAdapter<String> mBluetoothListAdapter;
    private static UserInfoListAdapter mUserInfoListAdapter;
    private static String mRaspiDatabaseStr = "";
    private ThresholdSettingDialogFragment mThresholdDialogFragment;
    private static String mCommand = "";
    private static ArrayList<String> mSonoffIPList;
    private static ArrayList<String> mSonoffList;
    private static ArrayList<String> mLinkList;
    private static SonoffListDialogFragment mSonoffListDialogFragment;
    private static Timer timer;
    private static String mRaspiIP = "";
    private static String mRaspiDatabaseID = "";
    private static String mRaspiDatabasePW = "";
    private static String mRaspiDatabase = "";
    private final int REQUEST_ENABLE_BT = 1;
    private static String mDeviceMacAddress;
    private static UserFilterListDialogFragment mUserFilterListDialogFragment;
    private static boolean mFilter = false;
    private static ArrayList<String> mUserList;
    private static ArrayList<String> mFilterList;
    private static boolean mCheck = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blunobeetle);
        setTitle("사용자");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(mRaspiIP.equals("")) {
            Intent intent = getIntent();
            mRaspiDatabaseID = intent.getStringExtra("RaspiDatabaseID");
            mRaspiDatabasePW = intent.getStringExtra("RaspiDatabasePW");
            mRaspiDatabase = intent.getStringExtra("RaspiDatabase");
            mRaspiIP = intent.getStringExtra("RaspiIP");
        }
        mRaspiDatabaseStr = "RaspiDatabaseID="+mRaspiDatabaseID+"&RaspiDatabasePW="+mRaspiDatabasePW+"&RaspiDatabase="+mRaspiDatabase;


        mSonoffIPList = new ArrayList<>();
        mSonoffList = new ArrayList<>();
        mLinkList = new ArrayList<>();
        mUserList = new ArrayList<>();
        mFilterList = new ArrayList<>();

        mUserInfoDialogFragment = new UserInfoDialogFragment();
        mUserInfoDialogFragment.setCancelable(false);
        mThresholdDialogFragment = new ThresholdSettingDialogFragment();
        mThresholdDialogFragment.setCancelable(false);
        mSonoffListDialogFragment = new SonoffListDialogFragment();
        mUserFilterListDialogFragment = new UserFilterListDialogFragment();
        mUserFilterListDialogFragment.setCancelable(false);
        mSonoffListDialogFragment.setCancelable(false);
        mDeviceMacAddress = getWifiMacAddress();

        //블루투스 스캔은 위치권한이 필요해서 권한 확인
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},0);
        } else{

        }

        mFragManager = this.getSupportFragmentManager();
        mLeDeviceListAdapter = new LeDeviceListAdapter();

        //리스트뷰와 어댑터 연결
        mUserInfoListAdapter = new UserInfoListAdapter();
        ListView lvBlunoBeetle = (ListView)findViewById(R.id.lvBlunoBeetle);
        lvBlunoBeetle.setAdapter(mUserInfoListAdapter);

        //context Menu 등록
        registerForContextMenu(lvBlunoBeetle);
        lvBlunoBeetle.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),HeartRate.class);
                intent.putExtra("mMacAddress",mUserInfoListAdapter.mALMacAddress.get(position).MacAddress);
                intent.putExtra("RaspiDatabaseID",mRaspiDatabaseID);
                intent.putExtra("RaspiDatabasePW",mRaspiDatabasePW);
                intent.putExtra("RaspiDatabase",mRaspiDatabase);
                intent.putExtra("RaspiIP",mRaspiIP);
                startActivity(intent);
            }
        });
    }

    //주기적으로 사용자 정보를 읽어오는 timertask
    private static void TimerStart(){
        mCheck = true;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                mCommand = "select";
                String param = mRaspiDatabaseStr + "&command=" + mCommand + "&MacAddress=" + mDeviceMacAddress;
                PHPReadUserFilterInfo mPHPReadUserFilterInfo = new PHPReadUserFilterInfo();
                mPHPReadUserFilterInfo.execute(param);
            }
        };
        timer = new Timer();
        timer.schedule(timerTask,0,10000);
    }
    @Override
    protected void onResume() {
        super.onResume();
        TimerStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        timer.cancel();
    }

    //사용자 정보 다이얼로그
    public static class UserInfoDialogFragment extends DialogFragment{
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            timer.cancel();
            LinearLayout linearLayout = (LinearLayout)View.inflate(getActivity(),R.layout.dialog_user_info,null);
            final EditText etMacAddress = linearLayout.findViewById(R.id.etAddBlunoBeetleMacAddress);
            if(mCommand.equals("modify"))
                etMacAddress.setEnabled(false);
            etMacAddress.setText(mMacAddress);
            etMacAddress.setEnabled(false);
            final EditText etName = linearLayout.findViewById(R.id.etAddBlunoBeetleName);
            etName.setText(mName);
            final AlertDialog.Builder adBuilder = new AlertDialog.Builder(getActivity());
            adBuilder.setTitle("사용자 정보")
                    .setView(linearLayout)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(!etMacAddress.getText().toString().equals("")&&!etName.getText().toString().equals("")) {
                                if(etName.getText().charAt(0)>='0'&&etName.getText().charAt(0)<='9')
                                    Toast.makeText(getActivity(),"Name은 숫자로 시작할 수 없습니다.",Toast.LENGTH_SHORT).show();
                                else {
                                    String param = mRaspiDatabaseStr + "&command=" + mCommand + "&MacAddress=" + etMacAddress.getText().toString() + "&Name=" + etName.getText().toString();
                                    Log.e("gwegew",param);
                                    PHPWriteUserInfo writeData = new PHPWriteUserInfo();
                                    try {
                                        String result = writeData.execute(param).get();
                                        if (result.equals("success")) {
                                            if (mCommand == "add")
                                                Toast.makeText(getActivity(), "사용자 추가를 성공하였습니다.", Toast.LENGTH_SHORT).show();
                                            else if (mCommand == "modify")
                                                Toast.makeText(getActivity(), "사용자 정보 편집을 성공하였습니다.", Toast.LENGTH_SHORT).show();
                                        } else if (result.equals("failure")) {
                                            if (mCommand == "add")
                                                Toast.makeText(getActivity(), "사용자 추가에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                                            else if (mCommand == "modify")
                                                Toast.makeText(getActivity(), "사용자 정보 편집을 실패하였습니다.", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (ExecutionException e) {
                                        e.printStackTrace();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else{
                                Toast.makeText(getActivity(),"값을 입력하세요.",Toast.LENGTH_SHORT).show();
                            }

                            TimerStart();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            TimerStart();
                        }
                    });
            return adBuilder.create();
        }
    }


    //Threshold 설정 다이얼로그
    public static class ThresholdSettingDialogFragment extends DialogFragment{
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            timer.cancel();
            LinearLayout linearLayout = (LinearLayout)View.inflate(getActivity(),R.layout.dialog_threshold,null);
            RadioGroup rgThreshold = linearLayout.findViewById(R.id.rgThreshold);
            final EditText etManual = linearLayout.findViewById(R.id.etManual);
            SeekBar sbThreshold = linearLayout.findViewById(R.id.sbThreshold);
            final LinearLayout llManual = linearLayout.findViewById(R.id.llManual);
            final LinearLayout llAutomatic = linearLayout.findViewById(R.id.llAutomatic);
            final TextView etThreshold = linearLayout.findViewById(R.id.etThreshold);
            final TextView etHeartrateAvg = linearLayout.findViewById(R.id.etHeartrateAvg);
            final int[] checked = new int[1];
            final double[] avg = new double[1];
            sbThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    etThreshold.setText(Integer.toString((int)(avg[0]*(progress+75)*0.01)));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            rgThreshold.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch(checkedId){
                        case R.id.rbManual:
                            checked[0] =0;
                            llManual.setVisibility(View.VISIBLE);
                            llAutomatic.setVisibility(View.GONE);
                            break;
                        case R.id.rbAutomatic:
                            checked[0] =1;
                            llManual.setVisibility(View.GONE);
                            llAutomatic.setVisibility(View.VISIBLE);
                            PHPReadAvgHeartRate readHeartRateAvg = new PHPReadAvgHeartRate();
                            try {
                                String param = mRaspiDatabaseStr + "&MacAddress=" + mMacAddress;
                                avg[0] = readHeartRateAvg.execute(param).get();
                                etHeartrateAvg.setText(Integer.toString((int)avg[0]));
                                etThreshold.setText(Integer.toString((int)(avg[0]*85*0.01)));
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            });
            final AlertDialog.Builder adBuilder = new AlertDialog.Builder(getActivity());
            adBuilder.setView(linearLayout)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mCommand = "threshold";
                            int threshold;
                            String result = "";
                            switch(checked[0]){
                                case 0:
                                    threshold = Integer.parseInt(etManual.getText().toString());
                                    if(threshold<30 || threshold>130){
                                        Toast.makeText(getActivity(),"범위를 초과하였습니다.",Toast.LENGTH_SHORT).show();
                                    } else{
                                        String param = mRaspiDatabaseStr + "&command="+ mCommand + "&MacAddress=" + mMacAddress + "&Threshold=" + threshold;
                                        PHPWriteUserInfo writeData = new PHPWriteUserInfo();
                                        try {
                                            result = writeData.execute(param).get();
                                        } catch (ExecutionException e) {
                                            e.printStackTrace();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    break;
                                case 1:
                                    String param = mRaspiDatabaseStr + "&command="+ mCommand + "&MacAddress=" + mMacAddress + "&Threshold=" + etThreshold.getText().toString();
                                    PHPWriteUserInfo writeData = new PHPWriteUserInfo();
                                    try {
                                        result = writeData.execute(param).get();
                                    } catch (ExecutionException e) {
                                        e.printStackTrace();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                            }
                            if(result.equals("success")) {
                                Toast.makeText(getActivity(), "Threshold를 설정하였습니다.", Toast.LENGTH_SHORT).show();
                            }
                            else if(result.equals("failure"))
                                Toast.makeText(getActivity(),"Threshold 설정을 실패하였습니다.",Toast.LENGTH_SHORT).show();
                            TimerStart();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            TimerStart();
                        }
                    });
            return adBuilder.create();
        }
    }

    //블루투스 리스트 출력 다이얼로그
    public static class BluetoothListDialogFragment extends DialogFragment{
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            mLeDeviceListAdapter.mLeDevices.clear();
            timer.cancel();
            mMacAddress = "";
            mName = "";
            mBluetoothListAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice, mLeDeviceListAdapter.mLeDevices);
            AlertDialog.Builder adBuilder = new AlertDialog.Builder(getActivity());
            adBuilder.setTitle("Bluetooth List")
                    .setSingleChoiceItems(mBluetoothListAdapter, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mMacAddress = mBluetoothListAdapter.getItem(which).split("\n")[1];
                        }
                    })
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mScanning = false;
                            if(!mMacAddress.equals(""))
                                mCommand = "add";
                            mUserInfoDialogFragment.show(mFragManager, "addUserDialogFragment");
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            TimerStart();
                        }
                    });
            return adBuilder.create();
        }
    }

    //블루투스 스캔
    public void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    try {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } catch(Exception e){

                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;
            try {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } catch(Exception e){

            }
        } else {
            mScanning = false;
            try {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } catch(Exception e){

            }
        }
    }

    // 스캔 중 블루투스를 찾으면 정보(디바이스이름, Mac주소)를 저장
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<String> mLeDevices;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<String>();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device.getName()+"\n"+device.getAddress())) {
                mLeDevices.add(device.getName()+"\n"+device.getAddress());
                mBluetoothListAdapter.notifyDataSetChanged();
            }
        }
        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }

    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    //평균 심박수 읽어오기
    private static class PHPReadAvgHeartRate extends AsyncTask<String,Void,Double> {
        @Override
        protected Double doInBackground(String... strings) {
            String param = strings[0];
            double result=0;
            URL url = null;
            try {
                //URL연결
                url = new URL(
                        "http://" + mRaspiIP + "/heartrateAvg.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setUseCaches(false);

                //PHP에 데이터 전송
                OutputStream outs = conn.getOutputStream();
                outs.write(param.getBytes("UTF-8"));
                outs.flush();
                outs.close();

                if(conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    result = Double.parseDouble(br.readLine());
                    br.close();
                }

                conn.disconnect();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    //php를 이용하여 데이터베이스에 저장된 사용자 정보를 읽어옴
    private static class PHPReadUserInfo extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            try {
                //URL 연결
                URL url = new URL("http://" + mRaspiIP + "/user_list.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setConnectTimeout(10000);
                conn.setUseCaches(false);

                if(conn != null){
                    //연결이 되었다면 php를 이용하여 데이터베이스의 사용자 정보를 읽어옴
                    String param = strings[0];
                    OutputStream outs = conn.getOutputStream();
                    outs.write(param.getBytes("UTF-8"));
                    outs.flush();
                    outs.close();

                    if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while(true)
                        {
                            //한 라인을 읽어서 null일 경우 종료
                            String line = br.readLine();
                            if(line == null) {
                                break;
                            }
                            line = line.replace("<br>",""); //php에서 개행인 <br>를 없앰
                            String temp[] = line.split(" ");
                            if(temp.length==2) {
                                temp = new String[]{temp[0], temp[1], "NULL"};
                            }
                            mUserInfoListAdapter.addUser(temp[0],temp[1],temp[2]);
                            if(!mUserList.contains(temp[1]+"("+temp[0]+")"))
                                mUserList.add(temp[1]+"("+temp[0]+")");
                        }
                        br.close();
                    }
                    conn.disconnect();
                }else{
                    System.out.println("실패");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(!mUserFilterListDialogFragment.isAdded()&&mFilter) {
                mUserFilterListDialogFragment.show(mFragManager, "SonoffFilterListDialogFragment");
                mFilter = false;
            } else {
//                if (mUserInfoListAdapter.getCount() == 0 && mCheck) {
//                    mCommand = "all";
//                    String param = mRaspiDatabaseStr + "&command=" + mCommand;
//                    PHPReadUserInfo mPHPReadUserInfo = new PHPReadUserInfo();
//                    mPHPReadUserInfo.execute(param);
//                    mCheck = false;
//                } else {
                mUserInfoListAdapter.notifyDataSetChanged();
//                }
            }
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mUserInfoListAdapter.clear();
            mUserList.clear();
        }
    }

    private static class PHPReadUserFilterInfo extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            try {
                //URL 연결
                URL url = new URL("http://" + mRaspiIP + "/user_list.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setConnectTimeout(10000);
                conn.setUseCaches(false);

                if(conn != null){
                    //연결이 되었다면 php를 이용하여 데이터베이스의 사용자 정보를 읽어옴
                    String param = strings[0];
                    OutputStream outs = conn.getOutputStream();
                    outs.write(param.getBytes("UTF-8"));
                    outs.flush();
                    outs.close();

                    if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while(true)
                        {
                            //한 라인을 읽어서 null일 경우 종료
                            String line = br.readLine();
                            if(line == null) {
                                break;
                            }
                            line = line.replace("<br>",""); //php에서 개행인 <br>를 없앰
                            String temp[] = line.split(" ");
                            if(temp.length==2) {
                                temp = new String[]{temp[0], temp[1], "NULL"};
                            }
                            if(!mFilterList.contains(temp[0]))
                                mFilterList.add(temp[0]);
                            if(temp.length==2) {
                                temp = new String[]{temp[0], temp[1], "NULL"};
                            }
                            mUserInfoListAdapter.addUser(temp[0],temp[1],temp[2]);
                        }
                        br.close();
                    }
                    conn.disconnect();
                }else{
                    System.out.println("실패");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(!mFilter)
                mUserInfoListAdapter.notifyDataSetChanged();
            if(mUserInfoListAdapter.getCount()==0 || mFilter) {
                mCommand = "all";
                String param = mRaspiDatabaseStr + "&command=" + mCommand;
                PHPReadUserInfo phpInsertUser = new PHPReadUserInfo();
                phpInsertUser.execute(param);
            }
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mFilterList.clear();
            mUserInfoListAdapter.clear();
        }
    }

    //php를 이용하여 데이터베이스에 사용자 정보 저장
    private static class PHPWriteUserInfo extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... strings) {
            String param = strings[0];
            String result = "";
            URL url = null;
            try {
                //URL연결
                url = new URL(
                        "http://" + mRaspiIP + "/user_info.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setConnectTimeout(10000);
                conn.setUseCaches(false);

                //PHP에 데이터 전송
                OutputStream outs = conn.getOutputStream();
                outs.write(param.getBytes("UTF-8"));
                outs.flush();
                outs.close();

                //SQL 명령어가 성공하였는지 result에 저장
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                result = br.readLine();

                br.close();
                conn.disconnect();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    //옵션 메뉴 생성
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_blunobeetle,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.menuBlunoBeetleFilter:
                mCommand="select";
                mFilter = true;
                String param;
                param = mRaspiDatabaseStr + "&command="+ mCommand + "&MacAddress=" + mDeviceMacAddress;
                PHPReadUserFilterInfo mPHPReadUserFilterInfo = new PHPReadUserFilterInfo();
                mPHPReadUserFilterInfo.execute(param);
                return true;
            case R.id.menuAddUser:
                mHandler = new Handler();

                //디바이스가 블루투스를 지원하는지 확인
                final BluetoothManager bluetoothManager =
                        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = bluetoothManager.getAdapter();

                if (mBluetoothAdapter == null) {
                    Toast.makeText(getApplicationContext(), R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
                } else if(!mBluetoothAdapter.isEnabled()){
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BT);
                } else {

                    //디바이스가 BLE를 지원하는지 확인
                    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                        Toast.makeText(getApplicationContext(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
                    } else {
                        //블루투스 리스트 다이얼로그 출력
                        scanLeDevice(true); //블루투스 스캔 시작
                        DialogFragment bluetoothDialogFragment = new BluetoothListDialogFragment();
                        bluetoothDialogFragment.setCancelable(false);
                        bluetoothDialogFragment.show(getSupportFragmentManager(), "bluetoothDialogFragment");
                    }
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //블루투스 활성화 결과를 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_ENABLE_BT){
            if(resultCode== Activity.RESULT_OK){
                Toast.makeText(getApplicationContext(),"블루투스가 활성화 되었습니다.",Toast.LENGTH_SHORT).show();
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    Toast.makeText(getApplicationContext(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
                } else {
                    //블루투스 리스트 다이얼로그 출력
                    scanLeDevice(true); //블루투스 스캔 시작
                    DialogFragment bluetoothDialogFragment = new BluetoothListDialogFragment();
                    bluetoothDialogFragment.setCancelable(false);
                    bluetoothDialogFragment.show(getSupportFragmentManager(), "bluetoothDialogFragment");
                }

            } else{
                Toast.makeText(getApplicationContext(),"블루투스 활성화를 취소하였습니다.",Toast.LENGTH_SHORT).show();
            }
        }
    }

    //사용자 정보를 저장하는 클래스 (Mac주소, 이름, Threshold 정보)
    private class UserInfo{
        private String MacAddress;
        private String Name;
        private String Threshold;

        public UserInfo(String macAddress, String name, String threshold) {
            MacAddress = macAddress;
            Name = name;
            Threshold = threshold;
        }
    }

    //데이터베이스에서 읽어온 사용자 정보를 저장할 어댑터
    private class UserInfoListAdapter extends BaseAdapter{
        LayoutInflater inflater = null;
        private ArrayList<UserInfo> mALMacAddress = null;
        private ArrayList<String> mMacAddressList = null;

        public void addUser(String macAddress, String name, String threshold){
            if(!mMacAddressList.contains(macAddress)) {
                mMacAddressList.add(macAddress);
                mALMacAddress.add(new UserInfo(macAddress, name, threshold));
            }
        }
        public UserInfoListAdapter(){
            super();
            mALMacAddress = new ArrayList<>();
            mMacAddressList = new ArrayList<>();
            inflater = getLayoutInflater();
        }
        @Override
        public int getCount() {
            return mALMacAddress.size();
        }

        public void clear(){
            mALMacAddress.clear();
            mMacAddressList.clear();
        }
        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if(convertView == null){
                convertView = inflater.inflate(R.layout.database_blunobeetle,null);
                viewHolder = new ViewHolder();
                viewHolder.macAddress = (TextView)convertView.findViewById(R.id.tvBlunoBeetleMacAddress);
                viewHolder.name = (TextView)convertView.findViewById(R.id.tvBlunoBeetleName);
                viewHolder.threshold = (TextView)convertView.findViewById(R.id.tvBlunoBeetleThreshold);
                convertView.setTag(viewHolder);
            } else{
                viewHolder = (ViewHolder) convertView.getTag();

            }
            UserInfo info = mALMacAddress.get(position);
            viewHolder.macAddress.setText(info.MacAddress);
            viewHolder.name.setText(info.Name);
            viewHolder.threshold.setText(info.Threshold);


            return convertView;
        }
    }

    static class ViewHolder {
        TextView macAddress;
        TextView name;
        TextView threshold;
    }

    //리스트뷰의 컨텍스트 메뉴
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cmenu_blunobeetle_listview, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        mMacAddress = mUserInfoListAdapter.mALMacAddress.get(info.position).MacAddress;
        mName = mUserInfoListAdapter.mALMacAddress.get(info.position).Name;
        String param;
        switch(item.getItemId()){
            case R.id.cmenu_blunobeetle_modify:
                mCommand = "modify";
                mUserInfoDialogFragment.show(getSupportFragmentManager(),"UserInfoDialogFragment");
                return true;
            case R.id.cmenu_blunobeetle_delete:
                timer.cancel();
                mCommand = "delete";
                param = mRaspiDatabaseStr + "&command="+ mCommand + "&MacAddress=" + mMacAddress;
                PHPWriteUserInfo writeData = new PHPWriteUserInfo();
                try {
                    String result = writeData.execute(param).get();
                    if(result.equals("success")) {
                        Toast.makeText(getApplicationContext(), "사용자를 삭제하였습니다.", Toast.LENGTH_SHORT).show();
                    }
                    else if(result.equals("failure"))
                        Toast.makeText(getApplicationContext(),"사용자 삭제에 실패하였습니다.",Toast.LENGTH_SHORT).show();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                TimerStart();
                return true;

            case R.id.cmenu_blunobeetle_threshold_set:
                mCommand = "threshold";
                mThresholdDialogFragment.show(getSupportFragmentManager(),"ThresholdDialogFragment");
                return true;
            case R.id.cmenu_blunobeetle_sonoff_link:
                param = mRaspiDatabaseStr + "&command="+ "select" + "&MacAddress=" + mMacAddress;
                PHPReadLinkSonoffList readSelectedSonoffList = new PHPReadLinkSonoffList();
                readSelectedSonoffList.execute(param);

                param = mRaspiDatabaseStr + "&command="+ "all";
                PHPReadSonoffList readSonoffInfo = new PHPReadSonoffList();
                readSonoffInfo.execute(param);
                return true;
            default:
                return super.onContextItemSelected(item);
        }

    }

    //데이터베이스에서 sonoff 리스트를 읽어옴
    private static class PHPReadSonoffList extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                //URL 연결
                URL url = new URL("http://" + mRaspiIP + "/sonoff_list.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setConnectTimeout(10000);
                conn.setUseCaches(false);

                if (conn != null) {
                    String param = params[0];
                    OutputStream outs = conn.getOutputStream();
                    outs.write(param.getBytes("UTF-8"));
                    outs.flush();
                    outs.close();

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while (true) {
                            String line = br.readLine();
                            if (line == null) {
                                break;
                            }
                            line = line.replace("<br>", "");
                            String temp[] = line.split(" ");
                            if(!mSonoffIPList.contains(temp[0]))
                                mSonoffIPList.add(temp[0]);
                            if(!mSonoffList.contains(temp[1]+"("+temp[0]+")"))
                                mSonoffList.add(temp[1]+"("+temp[0]+")");
                        }
                        br.close();
                    }
                    conn.disconnect();
                } else {
                    System.out.println("실패");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mSonoffListDialogFragment.show(mFragManager,"SonoffListDialogFragment");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSonoffIPList.clear();
            mSonoffList.clear();
        }
    }

    //Sonoff 리스트를 출력하는 다이얼로그(Sonoff 연동)
    public static class SonoffListDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            timer.cancel();
            AlertDialog.Builder adBuilder = new AlertDialog.Builder(getActivity());
            final boolean[] mChecked = new boolean[mSonoffIPList.size()];
            if(mLinkList.size()>0){
                for(int i=0;i<mLinkList.size();i++){
                    int index = mSonoffIPList.indexOf(mLinkList.get(i));
                    mChecked[index] = true;
                }
            }
            adBuilder.setTitle("SONOFF 연동")
                    .setMultiChoiceItems(mSonoffList.toArray(new String[0]), mChecked, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            mChecked[which] = isChecked;
                        }
                    })
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ArrayList<String> arrayList = new ArrayList<>();
                            for(int i=0;i<mChecked.length;i++)
                                if(mChecked[i]==true)
                                    arrayList.add(mSonoffIPList.get(i));

                            String param = mRaspiDatabaseStr + "&command="+ mCommand + "&MacAddress=" + mMacAddress + "&ArrayList="+arrayList;
                            PHPWriteSonoffLink writeLink = new PHPWriteSonoffLink();
                            try {
                                String result = writeLink.execute(param).get();
                                if(result.equals("success"))
                                    Toast.makeText(getActivity(),"SONOFF 연동에 성공하였습니다.",Toast.LENGTH_SHORT).show();
                                else if(result.equals("failure"))
                                    Toast.makeText(getActivity(),"SONOFF 연동에 실패하였습니다.",Toast.LENGTH_SHORT).show();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            TimerStart();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            TimerStart();
                        }
                    });
            return adBuilder.create();
        }
    }

    //사용자가 설정한 Sonoff 연동 결과를 데이터베이스에 저장
    private static class PHPWriteSonoffLink extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... strings) {
            String param = strings[0];
            String result = "";
            URL url = null;
            try {
                //URL연결
                url = new URL(
                        "http://" + mRaspiIP + "/link.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setConnectTimeout(10000);
                conn.setUseCaches(false);

                //PHP에 데이터 전송
                OutputStream outs = conn.getOutputStream();
                outs.write(param.getBytes("UTF-8"));
                outs.flush();
                outs.close();

                //SQL 명령어가 성공하였는지 result에 저장
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                result = br.readLine();

                br.close();
                conn.disconnect();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    //데이터베이스에 저장된 Sonoff 연동을 읽어옴
    private static class PHPReadLinkSonoffList extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                //URL 연결
                URL url = new URL("http://" + mRaspiIP + "/sonoff_list.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setConnectTimeout(10000);
                conn.setUseCaches(false);

                if(conn != null){
                    String param = params[0];
                    OutputStream outs = conn.getOutputStream();
                    outs.write(param.getBytes("UTF-8"));
                    outs.flush();
                    outs.close();

                    if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while(true) {
                            String line = br.readLine();
                            if(line == null) {
                                break;
                            }
                            line = line.replace("<br>","");
                            String temp[] = line.split(" ");
                            if(!mLinkList.contains(temp[0]))
                                mLinkList.add(temp[0]);
                        }
                        br.close();
                    }
                    conn.disconnect();
                }else{
                    System.out.println("실패");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLinkList.clear();
        }
    }

    //데이터베이스에 user 필터 결과를 저장
    private static class PHPWriteUserFilterList extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... strings) {
            String param = strings[0];
            String result = "";
            URL url = null;
            try {
                //URL연결
                url = new URL(
                        "http://" + mRaspiIP + "/user_filter.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setConnectTimeout(10000);
                conn.setUseCaches(false);

                //PHP에 데이터 전송
                OutputStream outs = conn.getOutputStream();
                outs.write(param.getBytes("UTF-8"));
                outs.flush();
                outs.close();

                //SQL 명령어가 성공하였는지 result에 저장
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                result = br.readLine();

                br.close();
                conn.disconnect();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    //user 필터를 하는 다이얼로그
    public static class UserFilterListDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            timer.cancel();
            AlertDialog.Builder adBuilder = new AlertDialog.Builder(getActivity());
            final boolean[] mChecked = new boolean[mUserInfoListAdapter.mMacAddressList.size()];
            if(mFilterList.size()>0){
                for(int i=0;i<mFilterList.size();i++){
                    int index = mUserInfoListAdapter.mMacAddressList.indexOf(mFilterList.get(i));
                    mChecked[index] = true;
                }
            }
            adBuilder.setTitle("사용자 필터")
                    .setMultiChoiceItems(mUserList.toArray(new String[0]), mChecked, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            mChecked[which] = isChecked;
                        }
                    })
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ArrayList<String> arrayList = new ArrayList<>();
                            for(int i=0;i<mChecked.length;i++)
                                if(mChecked[i]==true)
                                    arrayList.add(mUserInfoListAdapter.mMacAddressList.get(i));

                            String param = mRaspiDatabaseStr + "&command="+ mCommand + "&MacAddress=" + mDeviceMacAddress + "&ArrayList="+arrayList;
                            PHPWriteUserFilterList mPHPWriteUserFilterList = new PHPWriteUserFilterList();
                            try {
                                String result = mPHPWriteUserFilterList.execute(param).get();
                                if(result.equals("success"))
                                    Toast.makeText(getActivity(),"사용자 필터에 성공하였습니다.",Toast.LENGTH_SHORT).show();
                                else if(result.equals("failure"))
                                    Toast.makeText(getActivity(),"사용자 필터에 실패하였습니다.",Toast.LENGTH_SHORT).show();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            TimerStart();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            TimerStart();
                        }
                    });
            return adBuilder.create();
        }
    }

    //Wifi MacAddress를 얻어옴
    private String getWifiMacAddress() {
        try {
            String interfaceName = "wlan0";
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.getName().equalsIgnoreCase(interfaceName)){
                    continue;
                }

                byte[] mac = intf.getHardwareAddress();
                if (mac==null){
                    return "";
                }

                StringBuilder buf = new StringBuilder();
                for (byte aMac : mac) {
                    buf.append(String.format("%02X:", aMac));
                }
                if (buf.length()>0) {
                    buf.deleteCharAt(buf.length() - 1);
                }
                return buf.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } // for now eat exceptions

        return "";
    }
}
