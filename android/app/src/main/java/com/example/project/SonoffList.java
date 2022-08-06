package com.example.project;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class SonoffList extends AppCompatActivity {
    private SonoffInfoDialogFragment mSonoffInfoDialogFragment;
    private static SonoffInfoListAdapter mSonoffInfoListAdapter;
    private static String mRaspiDatabaseStr = "";
    private static String mIp;
    private static String mName;
    private static String mCommand = "";
    private ListView lvSonoff;
    private static Timer timer;
    private static String mRaspiIP = "";
    private static String mRaspiDatabaseID = "";
    private static String mRaspiDatabasePW = "";
    private static String mRaspiDatabase = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sonoff);
        setTitle("SONOFF 리스트");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(mRaspiIP.equals("")) {
            Intent intent = getIntent();
            mRaspiDatabaseID = intent.getStringExtra("RaspiDatabaseID");
            mRaspiDatabasePW = intent.getStringExtra("RaspiDatabasePW");
            mRaspiDatabase = intent.getStringExtra("RaspiDatabase");
            mRaspiIP = intent.getStringExtra("RaspiIP");
        }

        mRaspiDatabaseStr = "RaspiDatabaseID="+mRaspiDatabaseID+"&RaspiDatabasePW="+mRaspiDatabasePW+"&RaspiDatabase="+mRaspiDatabase;

        mSonoffInfoDialogFragment = new SonoffInfoDialogFragment();
        mSonoffInfoDialogFragment.setCancelable(false);

        mSonoffInfoListAdapter = new SonoffInfoListAdapter();
        lvSonoff = (ListView)findViewById(R.id.lvSonoff);

        //context Menu 등록
        registerForContextMenu(lvSonoff);
        lvSonoff.setAdapter(mSonoffInfoListAdapter);
    }

    //Sonoff 리스트를 주기적으로 갱신하는 timertask
    private static void TimerStart(){
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                String param = mRaspiDatabaseStr + "&command="+ "all";
                PHPReadSonoffInfo mReadSonoffInfo = new PHPReadSonoffInfo();
                mReadSonoffInfo.execute(param);
            }
        };
        timer = new Timer();
        timer.schedule(timerTask,0,10000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //현재 데이터베이스에 있는 사용자 정보를 읽어옴
        TimerStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        timer.cancel();
    }

    //Sonoff 정보 다이얼로그
    public static class SonoffInfoDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            timer.cancel();
            LinearLayout linearLayout = (LinearLayout)View.inflate(getActivity(),R.layout.dialog_sonoff_info,null);
            final EditText etAddIp = linearLayout.findViewById(R.id.etAddSonoffIp);
            etAddIp.setText(mIp);
            final EditText etAddSonoffName = linearLayout.findViewById(R.id.etAddSonoffName);
            if(mCommand.equals("modify"))
                etAddIp.setEnabled(false);
            etAddSonoffName.setText(mName);
            final AlertDialog.Builder adBuilder = new AlertDialog.Builder(getActivity());
            adBuilder.setTitle("SONOFF 정보")
                    .setView(linearLayout)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(!etAddIp.getText().toString().equals("")&&!etAddSonoffName.getText().toString().equals("")) {
                                String param = mRaspiDatabaseStr + "&command=" + mCommand +"&Ip=" + etAddIp.getText().toString() + "&Name=" + etAddSonoffName.getText().toString();
                                PHPWriteSonoffInfo writeData = new PHPWriteSonoffInfo();
                                try {
                                    String result = writeData.execute(param).get();
                                    if(result.equals("success"))
                                        Toast.makeText(getActivity(),"성공하였습니다.",Toast.LENGTH_SHORT).show();
                                    else if(result.equals("failure"))
                                        Toast.makeText(getActivity(),"실패하였습니다.",Toast.LENGTH_SHORT).show();
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
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

    //데이터베이스에 저장된 sonoff 정보를 읽어옴
    private static class PHPReadSonoffInfo extends AsyncTask<String, Void, Void> {
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
                            mSonoffInfoListAdapter.addUser(temp[0], temp[1]);
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
            mSonoffInfoListAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSonoffInfoListAdapter.clear();
        }
    }

    //데이터베이스에 sonoff 정보를 저장
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

    //옵션 메뉴 생성
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_sonoff,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.menuAddSonoff:
                mCommand = "add";
                mIp="";
                mName="";
                mSonoffInfoDialogFragment.show(getSupportFragmentManager(),"SonoffInfoDialogFragment");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //사용자 정보를 저장하는 클래스 (Mac주소, 이름, Threshold 정보)
    public class SonoffInfo {
        private String Ip;
        private String Name;

        public SonoffInfo(String ip, String name) {
            Ip = ip;
            Name = name;
        }
    }

    //데이터베이스에서 읽어온 사용자 정보를 저장할 어댑터
    private class SonoffInfoListAdapter extends BaseAdapter {
        LayoutInflater inflater = null;
        private ArrayList<SonoffInfo> mALSonoff = null;
        private ArrayList<String> mIPList = null;

        public void addUser(String ip, String name){
            if(!mIPList.contains(ip)) {
                mIPList.add(ip);
                mALSonoff.add(new SonoffInfo(ip, name));
            }
        }
        public SonoffInfoListAdapter(){
            super();
            mALSonoff = new ArrayList<>();
            mIPList = new ArrayList<>();
            inflater = getLayoutInflater();
        }
        @Override
        public int getCount() {
            return mALSonoff.size();
        }

        public void clear(){
            mALSonoff.clear();
            mIPList.clear();
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
            if(convertView == null){
                convertView = inflater.inflate(R.layout.database_sonoff,null);
                viewHolder = new ViewHolder();
                viewHolder.name = (TextView)convertView.findViewById(R.id.tvSonoffName);
                viewHolder.ip = (TextView)convertView.findViewById(R.id.tvSonoffIP);
                convertView.setTag(viewHolder);
            } else{
                viewHolder = (ViewHolder) convertView.getTag();

            }
            final SonoffInfo info = mALSonoff.get(position);
            viewHolder.name.setText(info.Name);
            viewHolder.ip.setText(info.Ip);
            convertView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                }
            });
            return convertView;
        }
    }

    static class ViewHolder {
        TextView name;
        TextView ip;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cmenu_sonoff_listview, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        mIp = mSonoffInfoListAdapter.mALSonoff.get(info.position).Ip;
        mName = mSonoffInfoListAdapter.mALSonoff.get(info.position).Name;
        switch(item.getItemId()){
            case R.id.cmenu_sonoff_modify:
                mCommand = "modify";
                mSonoffInfoDialogFragment.show(getSupportFragmentManager(),"SonoffInfoDialogFragment");
                return true;
            case R.id.cmenu_sonoff_delete:
                timer.cancel();
                mCommand = "delete";
                String param = mRaspiDatabaseStr + "&command="+ mCommand + "&Ip=" + mIp;
                PHPWriteSonoffInfo writeSonoffInfo = new PHPWriteSonoffInfo();
                try {
                    String result = writeSonoffInfo.execute(param).get();
                    if(result.equals("success")) {
                        Toast.makeText(getApplicationContext(), "Sonoff 삭제에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                        param = mRaspiDatabaseStr + "&command="+ "all";
                        TimerStart();
                    }
                    else if(result.equals("failure"))
                        Toast.makeText(getApplicationContext(),"Sonoff 삭제에 실패하였습니다.",Toast.LENGTH_SHORT).show();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            default:
                return super.onContextItemSelected(item);
        }

    }

}
