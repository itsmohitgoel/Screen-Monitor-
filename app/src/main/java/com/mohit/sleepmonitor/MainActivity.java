package com.mohit.sleepmonitor;

import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.facebook.stetho.Stetho;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.mohit.sleepmonitor.Graph.PieGraph;
import com.mohit.sleepmonitor.Graph.PieSection;
import com.mohit.sleepmonitor.data.SleepMonitorDbHelper;

import org.achartengine.GraphicalView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    public static final int REQUEST_ROUTINE_SERVICE = 0;
    public static final int MONITOR_INTERVAL = 1000 * 60;
    private GoogleApiClient mApiClient; // hold reference to api client
    private ArrayList<PieSection> mDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the GoogleApiClient, and
        // register my liseners to the GoogleApiClient member instance
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Connect to Google Play Services
        mApiClient.connect();

        //Implement Stetho library
        Stetho.InitializerBuilder initializerBuilder = Stetho.newInitializerBuilder(this);
        initializerBuilder.enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this));
        initializerBuilder.enableDumpapp(Stetho.defaultDumperPluginsProvider(this));
        Stetho.Initializer initializer = initializerBuilder.build();
        Stetho.initialize(initializer);

        getSleepData();
        if (mDataList != null && !mDataList.isEmpty()) {
            PieGraph pieGraph = new PieGraph();
            pieGraph.setData(mDataList);
            GraphicalView graphicalView = (GraphicalView) pieGraph.getView(this);
            LinearLayout linearLayout = (LinearLayout) findViewById(R.id.chart_pie);
            linearLayout.addView(graphicalView);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Create pending intent to start my IntentService
        Intent regularIntent = new Intent(MainActivity.this, RoutineRecognizeService.class);
        PendingIntent pIntent = PendingIntent.getService(MainActivity.this, REQUEST_ROUTINE_SERVICE, regularIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, MONITOR_INTERVAL, pIntent);

        //  mApiClient.disconnect();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Toast.makeText(getApplicationContext(), "Fail", Toast.LENGTH_SHORT).show();
    }

    /**
     * calculate effective sleep and disturbance data
     * for pie chart
     */
    private void getSleepData() {
        SQLiteOpenHelper openHelper = new SleepMonitorDbHelper(this);
        SQLiteDatabase db = openHelper.getReadableDatabase();

        long availaibleDuration = 9 *60 * 60 ;

        Cursor c = db.rawQuery("select sum(duration) from movement where end <> 0  ;", null);
        int movementDuration = -1;
        if(c.moveToFirst())
            movementDuration = c.getInt(0);
        else
            movementDuration = -1;
        c.close();

        long disturbanceInterval = (availaibleDuration - movementDuration)  ;

        int disturbancePercent = (int) ((movementDuration * 100) / availaibleDuration);
        int effectivePercent = 100 - disturbancePercent;
        PieSection disturbanceSection = new PieSection(disturbancePercent, "Disturbance (" + disturbancePercent
                + "%)", Color.RED);
        PieSection effectiveSection = new PieSection(effectivePercent, "Effective Sleep (" +
                effectivePercent + "%)", Color.BLUE);

        mDataList = new ArrayList<>();
        mDataList.add(disturbanceSection);
        mDataList.add(effectiveSection);
        Log.i("MainActivity", "total: movement in sec - " + movementDuration);
    }

}
