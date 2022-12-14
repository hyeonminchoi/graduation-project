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
        setTitle("????????? ?????????");
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

    //????????? ???????????? ??????????????? ???????????? timertask
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


    //???????????????????????? ???????????? ?????????
    private static class PHPReadUserHeartRate extends AsyncTask<String,Void,Void> {
        @Override
        protected Void doInBackground(String... strings) {
            String param = strings[0];
            URL url = null;
            try {
                //URL??????
                url = new URL(
                        "http://" + mRaspiIP + "/heartrate.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setUseCaches(false);

                //PHP??? ????????? ??????
                OutputStream outs = conn.getOutputStream();
                outs.write(param.getBytes("UTF-8"));
                outs.flush();
                outs.close();

                if(conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while (true) {
                        //??? ????????? ????????? null??? ?????? ??????
                        String line = br.readLine();
                        if (line == null) {
                            break;
                        }
                        line = line.replace("<br>", ""); //php?????? ????????? <br>??? ??????
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

    //???????????? ????????? ????????? ?????????
    private static class PHPUserHeartrateInit extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... strings) {
            String param = strings[0];
            String result= "";
            URL url = null;
            try {
                //URL??????
                url = new URL(
                        "http://" + mRaspiIP + "/heartrate_init.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setUseCaches(false);

                //PHP??? ????????? ??????
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

    //????????? ???????????? ???????????? ?????? ???????????? ???????????????
    public static class HeartrateCountSettingDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            timer.cancel();
            LinearLayout linearLayout = (LinearLayout) View.inflate(getActivity(),R.layout.dialog_heartrate_count,null);
            final EditText etHeartrateCount = (EditText)linearLayout.findViewById(R.id.etHeartrateCount);
            etHeartrateCount.setHint(Integer.toString(mCount));
            final AlertDialog.Builder adBuilder = new AlertDialog.Builder(getActivity());
            adBuilder.setTitle("????????? ????????? ?????? ??????")
                    .setView(linearLayout)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(etHeartrateCount.equals("")){
                               Toast.makeText(getActivity(),"?????? ???????????????.",Toast.LENGTH_SHORT).show();
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
            adBuilder.setMessage("???????????? ?????????????????????????")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String param = mRaspiDatabaseStr + "&MacAddress=" + mMacAddress + "&Count=" + mCount;
                            PHPUserHeartrateInit heartrateInit = new PHPUserHeartrateInit();
                            try {
                                String result = heartrateInit.execute(param).get();
                                if(result.equals("success")){
                                    Toast.makeText(getActivity(),"???????????? ????????????????????????.", Toast.LENGTH_SHORT).show();
                                } else if(result.equals("failure")){
                                    Toast.makeText(getActivity(),"????????? ???????????? ?????????????????????.", Toast.LENGTH_SHORT).show();
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
