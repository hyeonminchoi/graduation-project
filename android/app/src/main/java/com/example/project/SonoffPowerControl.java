package com.example.project;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationView;

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

public class SonoffPowerControl extends AppCompatActivity {
    private static SonoffInfoListAdapter mSonoffInfoListAdapter;
    private static String mRaspiDatabaseStr = "";
    private String mIp;
    private String mName;
    private static String mCommand = "";
    private String mStatus = "off";
    private static GridView gvSonoffPower;
    private static Timer timer;
    private static ArrayList<String> mSonoffIPList;
    private static ArrayList<String> mSonoffList;
    private static SonoffFilterListDialogFragment mSonoffFilterListDialogFragment;
    private static ArrayList<String> mFilterList;
    private static String mDeviceMacAddress;
    private static FragmentManager mFragManager;
    private DrawerLayout mDrawerLayout;
    private static String mRaspiIP = "";
    private static String mRaspiDatabaseID = "";
    private static String mRaspiDatabasePW = "";
    private static String mRaspiDatabase = "";
    private RaspiInfoDialogFragment mRaspiInfoDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sonoff_power);
        setTitle("SONOFF 전원");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);

        SharedPreferences sharedPreferences = getSharedPreferences("RaspiInfo",MODE_PRIVATE);
        mRaspiIP = sharedPreferences.getString("mRaspiIP","");
        mRaspiDatabaseID = sharedPreferences.getString("mRaspiDatabaseID","");
        mRaspiDatabasePW = sharedPreferences.getString("mRaspiDatabasePW","");
        mRaspiDatabase = sharedPreferences.getString("mRaspiDatabase","");

        mRaspiInfoDialogFragment = new RaspiInfoDialogFragment();
        mRaspiInfoDialogFragment.setCancelable(false);

        if(mRaspiIP.equals(""))
            mRaspiInfoDialogFragment.show(getSupportFragmentManager(),"raspiIPSettingDialogFragment");


        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawerLayout);

        mFragManager = this.getSupportFragmentManager();
        mRaspiDatabaseStr = "RaspiDatabaseID="+mRaspiDatabaseID+"&RaspiDatabasePW="+mRaspiDatabasePW+"&RaspiDatabase="+mRaspiDatabase;

        mSonoffInfoListAdapter = new SonoffInfoListAdapter();
        gvSonoffPower = (GridView)findViewById(R.id.gvSonoffPower);
        gvSonoffPower.setAdapter(mSonoffInfoListAdapter);
        mSonoffIPList = new ArrayList<>();
        mSonoffList = new ArrayList<>();
        mSonoffFilterListDialogFragment = new SonoffFilterListDialogFragment();
        mSonoffFilterListDialogFragment.setCancelable(false);
        mFilterList = new ArrayList<>();
        mDeviceMacAddress = getWifiMacAddress();

        //네비게이션 뷰를 설정
        NavigationView navigationView = (NavigationView)findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                mDrawerLayout.closeDrawers();
                int id = menuItem.getItemId();
                Intent intent = new Intent();
                intent.putExtra("RaspiDatabaseID",mRaspiDatabaseID);
                intent.putExtra("RaspiDatabasePW",mRaspiDatabasePW);
                intent.putExtra("RaspiDatabase",mRaspiDatabase);
                intent.putExtra("RaspiIP",mRaspiIP);

                switch(id){
                    case R.id.sonoff:
                        intent.setClass(getApplicationContext(), SonoffList.class);
                        startActivity(intent);
                        return true;
                    case R.id.user:
                        intent.setClass(getApplicationContext(), User.class);
                        startActivity(intent);
                        return true;
                    case R.id.setting:
                        mRaspiInfoDialogFragment.show(getSupportFragmentManager(),"raspiIPSettingDialogFragment");
                        return true;
                    case R.id.exit:
                        finish();
                        return true;
                    default:
                        return false;
                }
            }
        });

        Button btSonoffPowerAllOn = (Button)findViewById(R.id.btSonoffPowerAllOn);
        btSonoffPowerAllOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer.cancel();
                for(int i=0;i<mSonoffInfoListAdapter.mALSonoff.size();i++) {
                    if(mSonoffInfoListAdapter.mALSonoff.get(i).Status.equals("off")) {
                        mSonoffInfoListAdapter.mALSonoff.get(i).Status="on";
                        mCommand = "status";
                        String param = mRaspiDatabaseStr + "&command=" + mCommand + "&Ip=" + mSonoffInfoListAdapter.mALSonoff.get(i).Ip + "&Status=" + "on";
                        PHPWriteSonoffInfo writeSonoffInfo = new PHPWriteSonoffInfo();
                        writeSonoffInfo.execute(param);
                    }
                }
                TimerStart();
            }
        });
        Button btSonoffPowerAllOff = (Button)findViewById(R.id.btSonoffPowerAllOff);
        btSonoffPowerAllOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer.cancel();
                for(int i=0;i<mSonoffInfoListAdapter.mALSonoff.size();i++) {
                    if(mSonoffInfoListAdapter.mALSonoff.get(i).Status.equals("on")) {
                        mSonoffInfoListAdapter.mALSonoff.get(i).Status="off";
                        mCommand = "status";
                        String param = mRaspiDatabaseStr + "&command=" + mCommand + "&Ip=" + mSonoffInfoListAdapter.mALSonoff.get(i).Ip + "&Status=" + "off";
                        PHPWriteSonoffInfo writeSonoffInfo = new PHPWriteSonoffInfo();
                        writeSonoffInfo.execute(param);
                    }
                }
                TimerStart();
            }
        });
    }

    //주기적으로 sonoff 필터된 리스트를 읽어옴
    private static void TimerStart(){
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                mCommand= "select";
                String param = mRaspiDatabaseStr + "&command="+ mCommand + "&MacAddress=" + mDeviceMacAddress;
                PHPReadSonoffFilterInfo mReadSonoffFilterInfo = new PHPReadSonoffFilterInfo();
                mReadSonoffFilterInfo.execute(param);
            }
        };
        timer = new Timer();
        timer.schedule(timerTask,0,2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //현재 데이터베이스에 있는 사용자 정보를 읽어옴
        if(!mRaspiIP.equals(""))
            TimerStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        timer.cancel();
        SharedPreferences sharedPreferences = getSharedPreferences("RaspiInfo",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("mRaspiIP",mRaspiIP);
        editor.putString("mRaspiDatabaseID",mRaspiDatabaseID);
        editor.putString("mRaspiDatabasePW",mRaspiDatabasePW);
        editor.putString("mRaspiDatabase",mRaspiDatabase);
        editor.commit();
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

    //데이터베이스에 저장된 sonoff 필터 정보를 읽어옴
    private static class PHPReadSonoffFilterInfo extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                //URL 연결
                URL url = new URL("http://" + mRaspiIP + "/sonoff_filter_list.php");
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
                            if(temp.length==2) {
                                temp = new String[]{temp[0], temp[1], "NULL"};
                            }
                            mSonoffInfoListAdapter.addUser(temp[0], temp[1], temp[2]);
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
            if(mSonoffInfoListAdapter.getCount()==0){
                mCommand = "all";
                String param = mRaspiDatabaseStr + "&command="+ mCommand;
                PHPReadSonoffInfo mReadSonoffInfo = new PHPReadSonoffInfo();
                mReadSonoffInfo.execute(param);
            } else{
                mSonoffInfoListAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSonoffInfoListAdapter.clear();
        }
    }

    //옵션 메뉴 생성
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_sonoff_power,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.menuSonoffPowerFilter:
                mCommand="select";
                String param;
                param = mRaspiDatabaseStr + "&command="+ mCommand + "&MacAddress=" + mDeviceMacAddress;
                PHPReadFilterSonoffList mPHPReadFilterSonoffList = new PHPReadFilterSonoffList();
                mPHPReadFilterSonoffList.execute(param);
                return true;
            case android.R.id.home:
                if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
                    mDrawerLayout.closeDrawers();
                else
                    mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private class SonoffInfo {
        private String Ip;
        private String Name;
        private String Status;

        public SonoffInfo(String ip, String name, String status) {
            Ip = ip;
            Name = name;
            Status = status;
        }
    }

    //데이터베이스에서 읽어온 sonoff 필터 리스트를 저장하는 어댑터
    private class SonoffInfoListAdapter extends BaseAdapter {
        LayoutInflater inflater = null;
        private ArrayList<SonoffInfo> mALSonoff = null;
        private ArrayList<String> mIpList = null;

        public void addUser(String ip, String name, String status){
            if(!mIpList.contains(ip)){
                mIpList.add(ip);
                mALSonoff.add(new SonoffInfo(ip, name, status));
            }
        }
        public SonoffInfoListAdapter(){
            super();
            mALSonoff = new ArrayList<>();
            mIpList = new ArrayList<>();
            inflater = getLayoutInflater();
        }
        @Override
        public int getCount() {
            return mALSonoff.size();
        }

        public void clear(){
            mALSonoff.clear();
            mIpList.clear();
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
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;

            int gridviewH = gvSonoffPower.getHeight()/6;

            if(convertView == null){
                convertView = inflater.inflate(R.layout.database_sonoff_power,null);
                viewHolder = new ViewHolder();
                viewHolder.name = (TextView)convertView.findViewById(R.id.tvSonoffPowerName);
                viewHolder.status = (ToggleButton) convertView.findViewById(R.id.tbSonoffPowerStatus);
                convertView.setTag(viewHolder);
            } else{
                viewHolder = (ViewHolder) convertView.getTag();

            }

            viewHolder.name.setHeight(gridviewH-1);
            viewHolder.status.setHeight(gridviewH-1);

            if(mALSonoff.size()>0) {
                final SonoffInfo info = mALSonoff.get(position);
                viewHolder.name.setText(info.Name);
                if (info.Status.equals("on")) {
                    viewHolder.status.setTextOn("");
                    viewHolder.status.setClickable(true);
                    viewHolder.status.setChecked(true);
                    viewHolder.status.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_power_settings_new_green_24dp));
                }
                else if (info.Status.equals("off")) {
                    viewHolder.status.setTextOff("");
                    viewHolder.status.setClickable(true);
                    viewHolder.status.setChecked(false);
                    viewHolder.status.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_power_settings_new_red_24dp));
                }
                else if (info.Status.equals("NULL")) {
                    viewHolder.status.setText("");
                    viewHolder.status.setClickable(false);
                    viewHolder.status.setBackgroundDrawable(getResources().getDrawable(R.drawable.baseline_power_off_black_18dp));
                }
                viewHolder.status.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mCommand = "status";
                        if (isChecked) {
                            viewHolder.status.setTextOn("");
                            mStatus = "on";
                            viewHolder.status.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_power_settings_new_green_24dp));
                        }
                        else {
                            viewHolder.status.setTextOff("");
                            mStatus = "off";
                            viewHolder.status.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_power_settings_new_red_24dp));
                        }
                        if(mALSonoff.size()>0) {
                            String param = mRaspiDatabaseStr + "&command=" + mCommand + "&Ip=" + mALSonoff.get(position).Ip + "&Status=" + mStatus;
                            PHPWriteSonoffInfo writeSonoffInfo = new PHPWriteSonoffInfo();
                            writeSonoffInfo.execute(param);
                        }
                    }
                });
            }
            return convertView;
        }
    }

    static class ViewHolder {
        TextView name;
        ToggleButton status;
    }

    //sonoff 필터를 하는 다이얼로그
    public static class SonoffFilterListDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            timer.cancel();
            AlertDialog.Builder adBuilder = new AlertDialog.Builder(getActivity());
            final boolean[] mChecked = new boolean[mSonoffIPList.size()];
            if(mFilterList.size()>0){
                for(int i=0;i<mFilterList.size();i++){
                    int index = mSonoffIPList.indexOf(mFilterList.get(i));
                    mChecked[index] = true;
                }
            }
            adBuilder.setTitle("SONOFF 필터")
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

                            String param = mRaspiDatabaseStr + "&command="+ mCommand + "&MacAddress=" + mDeviceMacAddress + "&ArrayList="+arrayList;
                            PHPWriteFilterList writeLink = new PHPWriteFilterList();
                            try {
                                String result = writeLink.execute(param).get();
                                if(result.equals("success"))
                                    Toast.makeText(getActivity(),"SONOFF 필터에 성공하였습니다.",Toast.LENGTH_SHORT).show();
                                else if(result.equals("failure"))
                                    Toast.makeText(getActivity(),"SONOFF 필터에 실패하였습니다.",Toast.LENGTH_SHORT).show();
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

    //데이터베이스에 sonoff 필터 결과를 저장
    private static class PHPWriteFilterList extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... strings) {
            String param = strings[0];
            String result = "";
            URL url = null;
            try {
                //URL연결
                url = new URL(
                        "http://" + mRaspiIP + "/sonoff_filter.php");
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

    //데이터베이스에 저장된 sonoff 필터 리스트를 읽어옴
    private static class PHPReadFilterSonoffList extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                //URL 연결
                URL url = new URL("http://" + mRaspiIP + "/sonoff_filter_list.php");
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
                            if(!mFilterList.contains(temp[0]))
                                mFilterList.add(temp[0]);
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
            mFilterList.clear();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            String param = mRaspiDatabaseStr + "&command="+ "all";
            PHPReadSonoffInfo mReadSonoffInfo = new PHPReadSonoffInfo();
            mReadSonoffInfo.execute(param);
        }
    }

    //전원 상태가 변경된 것을 데이터베이스에 저장
    private static class PHPWriteSonoffInfo extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... strings) {
            String param = strings[0];
            String result = "";
            URL url = null;
            try {
                url = new URL(
                        "http://" + mRaspiIP + "/sonoff_info.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setConnectTimeout(10000);
                conn.setUseCaches(false);

                OutputStream outs = conn.getOutputStream();
                outs.write(param.getBytes("UTF-8"));
                outs.flush();
                outs.close();
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

    //전원 상태가 변경된 것을 읽어옴
    private static class PHPReadSonoffInfo extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                //URL 연결
                URL url = new URL("http://" + mRaspiIP + "/sonoff_filter_list.php");
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
                            if(!mSonoffIPList.contains(temp[0]))
                                mSonoffIPList.add(temp[0]);
                            if(!mSonoffList.contains(temp[1]+"("+temp[0]+")"))
                                mSonoffList.add(temp[1]+"("+temp[0]+")");
                            if(mCommand.equals("all")){
                                if(temp.length==2) {
                                    temp = new String[]{temp[0], temp[1], "NULL"};
                                }
                                mSonoffInfoListAdapter.addUser(temp[0], temp[1], temp[2]);
                            }
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
            if(!mSonoffFilterListDialogFragment.isAdded()&&mCommand=="select")
                mSonoffFilterListDialogFragment.show(mFragManager,"SonoffFilterListDialogFragment");
            if(mCommand.equals("all"))
                mSonoffInfoListAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSonoffIPList.clear();
            mSonoffList.clear();
        }
    }

    //라즈베리파이와 관련된 설정을 하는 다이얼로그
    public static class RaspiInfoDialogFragment extends DialogFragment{
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            LinearLayout result = (LinearLayout)View.inflate(getActivity(),R.layout.dialog_raspi_info,null);
            final EditText etRaspiIP = (EditText)result.findViewById(R.id.etRaspiIP);
            final EditText etRaspiDatabaseID = (EditText)result.findViewById(R.id.etRaspiDataBaseID);
            final EditText etRaspiDatabasePW = (EditText)result.findViewById(R.id.etRaspiDataBasePW);
            final EditText etRaspiDatabase = (EditText)result.findViewById(R.id.etRaspiDataBase);
            if(!mRaspiIP.equals(""))
                etRaspiIP.setHint(mRaspiIP);
            AlertDialog.Builder adBuilder = new AlertDialog.Builder(getActivity());
            adBuilder.setView(result)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if((!etRaspiIP.getText().toString().equals(""))&&(!etRaspiDatabaseID.getText().toString().equals("")&&(!etRaspiDatabasePW.getText().toString().equals(""))&&(!etRaspiDatabase.getText().toString().equals("")))){
                                mRaspiIP = etRaspiIP.getText().toString();
                                mRaspiDatabaseID = etRaspiDatabaseID.getText().toString();
                                mRaspiDatabasePW = etRaspiDatabasePW.getText().toString();
                                mRaspiDatabase = etRaspiDatabase.getText().toString();
                                mRaspiDatabaseStr = "RaspiDatabaseID="+mRaspiDatabaseID+"&RaspiDatabasePW="+mRaspiDatabasePW+"&RaspiDatabase="+mRaspiDatabase;
                                TimerStart();
                            }
                            else{
                                Toast.makeText(getActivity(),"값을 입력하세요.",Toast.LENGTH_SHORT).show();
                            }
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


}
