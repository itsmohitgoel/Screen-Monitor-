package com.mohit.sleepmonitor;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.mohit.sleepmonitor.Graph.PieGraph;
import com.mohit.sleepmonitor.Graph.PieSection;
import com.mohit.sleepmonitor.adapter.MovementAdapter;
import com.mohit.sleepmonitor.bean.MovementItem;
import com.mohit.sleepmonitor.data.SleepMonitorContract.MovementEntry;
import com.mohit.sleepmonitor.data.SleepMonitorDbHelper;

import org.achartengine.GraphicalView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity  {

    private ArrayList<PieSection> mDataList;

    private RecyclerView mRecycleView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<MovementItem> mMovementDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPieChartData();
        if (mDataList != null && !mDataList.isEmpty()) {
            PieGraph pieGraph = new PieGraph();
            pieGraph.setData(mDataList);
            GraphicalView graphicalView = (GraphicalView) pieGraph.getView(this);
            LinearLayout linearLayout = (LinearLayout) findViewById(R.id.chart_pie);
            linearLayout.addView(graphicalView);
        }

        mRecycleView = (RecyclerView) findViewById(R.id.recycler_view_movements);
        mRecycleView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecycleView.setLayoutManager(mLayoutManager);


        getTabularData(); // gets the data from db for RecyclerView
        if (mMovementDataList != null && mMovementDataList.size() > 0) {
            mAdapter = new MovementAdapter(this, mMovementDataList);
            mRecycleView.setAdapter(mAdapter);
        } else {
            Toast.makeText(this, "No data available yet", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * calculate effective sleep and disturbance data
     * for pie chart
     */
    private void getPieChartData() {
        SQLiteOpenHelper openHelper = new SleepMonitorDbHelper(this);
        SQLiteDatabase db = openHelper.getReadableDatabase();

        long availaibleDuration = 9 * 60 * 60;

        Cursor c = db.rawQuery("select sum(duration) from movement where end <> 0  ;", null);
        int movementDuration = -1;
        if (c.moveToFirst())
            movementDuration = c.getInt(0);
        else
            movementDuration = -1;
        c.close();

        long disturbanceInterval = (availaibleDuration - movementDuration);

        int disturbancePercent = (int) ((movementDuration * 100) / availaibleDuration);
        int effectivePercent = 100 - disturbancePercent;
        PieSection disturbanceSection = new PieSection(disturbancePercent, "Disturbance (" + disturbancePercent
                + "%)", getResources().getColor(R.color.pie_orange));
        PieSection effectiveSection = new PieSection(effectivePercent, "Effective Sleep (" +
                effectivePercent + "%)", getResources().getColor(R.color.pie_green));

        mDataList = new ArrayList<>();
        mDataList.add(disturbanceSection);
        mDataList.add(effectiveSection);
        Log.i("MainActivity", "total: movement in sec - " + movementDuration);
    }

    private void getTabularData() {
        SQLiteOpenHelper openHelper = new SleepMonitorDbHelper(this);
        SQLiteDatabase db = openHelper.getReadableDatabase();

        mMovementDataList = new ArrayList<>();

        Cursor c = db.query(MovementEntry.TABLE_NAME, null,
                MovementEntry.COLUMN_DURATION + " > 0", null, null, null, null);
        int idxRowID = c.getColumnIndex(MovementEntry._ID);
        int idxStartTime = c.getColumnIndex(MovementEntry.COLUMN_START);
        int idxEndTime = c.getColumnIndex(MovementEntry.COLUMN_END);
        int idxDuration = c.getColumnIndex(MovementEntry.COLUMN_DURATION);

        if (c.getCount() > 0) {
            while (c.moveToNext()) {
                MovementItem item = new MovementItem();
                item.setId(c.getString(idxRowID));
                item.setStartTime(c.getString(idxStartTime));
                item.setEndTime(c.getString(idxEndTime));
                long duration = c.getLong(idxDuration) / 60;
                item.setDuration(Long.toString(duration));

                mMovementDataList.add(item);
            }
        }
    }
}
