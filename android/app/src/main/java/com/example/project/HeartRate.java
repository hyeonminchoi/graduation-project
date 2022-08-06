package com.example.project;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class HeartRate extends AppCompatActivity {
    private static LineChart lineChart;
    private static ArrayList<Entry> entry_chart;
    private static LineDataSet lineDataSet;
    private static LineData chartData;
    private static String mRaspiDatabaseStr = "";
    private static float mY = 0;
    private static Timer timer;
    private static String mMacAddress;
    private static int mCount = 0;
    private HeartrateCountSettingDialogFragment mHeartrateCountDialogFragment;
    private HeartrateInitDialogFragment mHeartrateInitDialogFragment;
    protected static ArrayList<String> time_heartrate;
    private static String mRaspiIP = "";
    private static String mRaspiDatabaseID = "";
    private static String mRaspiDatabasePW = "";
    private static String mRaspiDatabase = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heartrate);
        setTitle("심박수 그래프");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences sharedPreferences = getSharedPreferences("HeartrateInfo",MODE_PRIVATE);
        mCount = Integer.parseInt(sharedPreferences.getString("HeartrateCount","0"));

        if(mRaspiIP.equals("")) {
            Intent intent = getIntent();
            mRaspiDatabaseID = intent.getStringExtra("RaspiDatabaseID");
            mRaspiDatabasePW = intent.getStringExtra("RaspiDatabasePW");
            mRaspiDatabase = intent.getStringExtra("RaspiDatabase");
            mRaspiIP = intent.getStringExtra("RaspiIP");
        }

        mRaspiDatabaseStr = "RaspiDatabaseID="+mRaspiDatabaseID+"&RaspiDatabasePW="+mRaspiDatabasePW+"&RaspiDatabase="+mRaspiDatabase;
        lineChart = (LineChart) findViewById(R.id.chart);
        entry_chart = new ArrayList<>();
        lineDataSet = new LineDataSet(entry_chart,"BPM");
        chartData = new LineData();
        chartData.addDataSet(lineDataSet);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.getAxisRight().setDrawGridLines(false);

        time_heartrate = new ArrayList<>();
        MyMarkerView marker = new MyMarkerView(this,R.layout.mymarkerview);
        marker.setChartView(lineChart);
        lineChart.setMarker(marker);

        mHeartrateCountDialogFragment = new HeartrateCountSettingDialogFragment();
        mHeartrateCountDialogFragment.setCancelable(false);

        mHeartrateInitDialogFragment = new HeartrateInitDialogFragment();
        mHeartrateInitDialogFragment.setCancelable(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        mMacAddress = intent.getExtras().getString("mMacAddress");
        TimerStart();
    }

    //심박수 그래프를 주기적으로 갱신하는 timertask
    private static void TimerStart(){
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                String param = mRaspiDatabaseStr + "&MacAddress=" + mMacAddress + "&Count=" + mCount;
                PHPReadUserHeartRate writeData = new PHPReadUserHeartRate();
                writeData.execute(param);
            }
        };

        timer = new Timer();
        timer.schedule(timerTask,0,10000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        timer.cancel();
        SharedPreferences sharedPreferences = getSharedPreferences("HeartrateInfo",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("HeartrateCount", String.valueOf(mCount));
        editor.commit();
    }


    //데이터베이스에서 심박수를 읽어옴
    private static class PHPReadUserHeartRate extends AsyncTask<String,Void,Void> {
        @Override
        protected Void doInBackground(String... strings) {
            String param = strings[0];
            URL url = null;
            try {
                //URL연결
                url = new URL(
                        "http://" + mRaspiIP + "/heartrate.php");
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
                    while (true) {
                        //한 라인을 읽어서 null일 경우 종료
                        String line = br.readLine();
                        if (line == null) {
                            break;
                        }
                        line = line.replace("<br>", ""); //php에서 개행인 <br>를 없앰
                        String temp[] = line.split(" ");
                        time_heartrate.add(temp[0]+" "+temp[1]);
                        entry_chart.add(new Entry(mY, Float.parseFloat(temp[2])));
                        mY++;
                    }
                    br.close();
                }
                conn.disconnect();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            entry_chart.clear();
            time_heartrate.clear();
            mY = 0;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            lineDataSet.notifyDataSetChanged();
            chartData.notifyDataChanged();
            lineChart.setData(chartData);
            lineChart.invalidate();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_heartrate,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.menuHeartrateInit:
                mHeartrateInitDialogFragment.show(getSupportFragmentManager(),"HeartrateInitDialogFragment");
                return true;
            case R.id.menuHeartrateCount:
                mHeartrateCountDialogFragment.show(getSupportFragmentManager(),"HeartrateCountDialogFragment");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //사용자의 심박수 정보를 초기화
    private static class PHPUserHeartrateInit extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... strings) {
            String param = strings[0];
            String result= "";
            URL url = null;
            try {
                //URL연결
                url = new URL(
                        "http://" + mRaspiIP + "/heartrate_init.php");
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
                    result = br.readLine();
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

    //심박수 그래프에 보여지는 개수 설정하는 다이얼로그
    public static class HeartrateCountSettingDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            timer.cancel();
            LinearLayout linearLayout = (LinearLayout) View.inflate(getActivity(),R.layout.dialog_heartrate_count,null);
            final EditText etHeartrateCount = (EditText)linearLayout.findViewById(R.id.etHeartrateCount);
            etHeartrateCount.setHint(Integer.toString(mCount));
            final AlertDialog.Builder adBuilder = new AlertDialog.Builder(getActivity());
            adBuilder.setTitle("표시할 심박수 개수 설정")
                    .setView(linearLayout)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(etHeartrateCount.equals("")){
                               Toast.makeText(getActivity(),"값을 입력하세요.",Toast.LENGTH_SHORT).show();
                            } else{
                                mCount = Integer.parseInt(etHeartrateCount.getText().toString());
                                TimerStart();
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            return adBuilder.create();
        }
    }

    public static class HeartrateInitDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            timer.cancel();
            final AlertDialog.Builder adBuilder = new AlertDialog.Builder(getActivity());
            adBuilder.setMessage("심박수를 초기화하겠습니까?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String param = mRaspiDatabaseStr + "&MacAddress=" + mMacAddress + "&Count=" + mCount;
                            PHPUserHeartrateInit heartrateInit = new PHPUserHeartrateInit();
                            try {
                                String result = heartrateInit.execute(param).get();
                                if(result.equals("success")){
                                    Toast.makeText(getActivity(),"심박수를 초기화하였습니다.", Toast.LENGTH_SHORT).show();
                                } else if(result.equals("failure")){
                                    Toast.makeText(getActivity(),"심박수 초기화에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                                }
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
                        }
                    });
            return adBuilder.create();
        }
    }
}
