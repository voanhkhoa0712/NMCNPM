package com.example.group5;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;

    private Button btnStart = null;
    private Button btnStop = null;
    private Button btnPause = null;
    private TextView textTime = null;
    private TextView textDis = null;
    private TextView textPace = null;
    private Timer mTimer = null;
    private TimerTask mTimerTask = null;
    private Handler mHandler = null;

    private static int count = 0;
    private static double sum = 0;
    private static double pace = 0;
    public Bitmap runRoute;

    private boolean isPause = false;
    private boolean isStop = true;
    private static final int UPDATE_TEXT_VIEW = 0;

    List<Double> latList = new ArrayList<>();
    List<Double> lonList = new ArrayList<>();

    private Location previousLocation = null;
    private ArrayList<Polyline> runningRoute = new ArrayList<>();
    private ArrayList<Location> points = new ArrayList<>();

    // control the drawing status. default is not drawing
    private boolean isDraw = false;
    //
    private boolean showBound = false;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        btnStart = findViewById(R.id.button_start);
        btnPause = findViewById(R.id.button_pause);
        btnStop = findViewById(R.id.button_stop);

        textTime = findViewById(R.id.data_time);
        textDis = findViewById(R.id.data_dis);
        textPace = findViewById(R.id.data_pace);

        btnStart.setOnClickListener(listener);
        btnPause.setOnClickListener(listener);
        btnStop.setOnClickListener(listener);

        btnStart.setEnabled(true);
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UPDATE_TEXT_VIEW:
                        updateTextView();
                        break;
                    default:
                        break;
                }
            }
        };

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        mFusedLocationClient = getFusedLocationProviderClient(this);
        mLocationRequest = new LocationRequest();

        mLocationRequest.setInterval(250);
        mLocationRequest.setFastestInterval(100);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // check for location request permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1340);
        } else {
            requestLocationUpdates();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        // ask for the permission of requesting location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1340);
        } else {
            mMap.setMyLocationEnabled(true);
        }

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        // disable zoom button because the zoom level is fixed.
        mMap.getUiSettings().setZoomControlsEnabled(false);
        //enable positioning button
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        mMap.getUiSettings().setMapToolbarEnabled(false);
    }

    // This function handles the permission result
    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1340) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            } else {
                Toast.makeText(this, "Location cannot be obtained due to " + "missing permission.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void requestLocationUpdates() {
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                onLocationChanged(locationResult.getLastLocation());
            }
        }, null);
    }

    public void onLocationChanged(Location location) {
        if (location != null) {

            if(!showBound) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 17));
            }
            // drawing the route while the user is running
            if (isDraw) {
                // draw the route
                routeDrawing(location);
            }
        }
    }

    // drawing the polyline while running
    private void routeDrawing(Location location) {
        // validity check in case the first point does not have a previous point
        if (previousLocation == null) {
            previousLocation = location;
        }
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        latList.add(lat);
        lonList.add(lon);

        PolylineOptions lineOptions = new PolylineOptions();
        // 0.25 is the location update interval also the drawing interval
        double vdraw = (GetDistance(previousLocation.getLatitude(), previousLocation.getLongitude(), location.getLatitude(), location.getLongitude())) / 0.25;
        System.out.println("vdraw: "+ vdraw);
        if (vdraw <0.01){
            lineOptions.add(new LatLng(previousLocation.getLatitude(), previousLocation.getLongitude()))
                    .add(new LatLng(location.getLatitude(), location.getLongitude()))
                    .color(ResourcesCompat.getColor(getResources(), R.color.slow, null))
                    .width(5);
            System.out.print("I am running slow");
        }
        if (vdraw >=0.01 && vdraw <=0.03){
            lineOptions.add(new LatLng(previousLocation.getLatitude(), previousLocation.getLongitude()))
                    .add(new LatLng(location.getLatitude(), location.getLongitude()))
                    .color(ResourcesCompat.getColor(getResources(), R.color.normal, null))
                    .width(5);
            System.out.print("I am running normally");
        }
        if (vdraw >0.03){
            lineOptions.add(new LatLng(previousLocation.getLatitude(), previousLocation.getLongitude()))
                    .add(new LatLng(location.getLatitude(), location.getLongitude()))
                    .color(ResourcesCompat.getColor(getResources(), R.color.fast, null))
                    .width(5);
            System.out.print("I am running fast");
        }

        // add the polyline to the map
        Polyline partOfRunningRoute = mMap.addPolyline(lineOptions);
        // set the zindex so that the poly line stays on top of my tile overlays
        partOfRunningRoute.setZIndex(1000);
        // add the poly line to the array so they can all be removed if necessary
        runningRoute.add(partOfRunningRoute);
        // add the latlng from this point to the array
        points.add(location);
        // store current location as previous location in the end
        previousLocation = location;
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @SuppressLint("SetTextI18n")
        public void onClick(View v) {
            if (v == btnStart) {//start running

                previousLocation = null;
                // start drawing
                isDraw = true;
                // camera is now showing bounds of points
                showBound = false;

                // clear the running route  array list if it is not empty
                if(!runningRoute.isEmpty()) {
                    // remove all the polylines from the map
                    for (Polyline line : runningRoute) {
                        line.remove();
                    }
                    runningRoute.clear();
                }

                isStop = !isStop;
                btnStart.setEnabled(false);
                btnStart.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.unabled, null));
                btnStop.setEnabled(true);
                btnStop.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimary_Khoa, null));
                btnPause.setEnabled(true);
                startTimer();
                btnPause.setBackgroundResource(R.drawable.pause_button);
                textDis.setText("0.00");
                textPace.setText("0.00");

                //get the current time from device system and parse to String
                String startTime = Calendar.getInstance().getTime().toString();
                System.out.println("Current time is: " + startTime);
            }

            if (v == btnPause) {
                btnStart.setEnabled(false);
                btnStart.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.unabled, null));
                btnStop.setEnabled(true);
                btnPause.setEnabled(true);
                pauseTimer();

                if (isPause) {//pause
                    btnPause.setBackgroundResource(R.drawable.play_button);
                    //stop drawing
                    isDraw = false;
                }else{//resume
                    btnPause.setBackgroundResource(R.drawable.pause_button);
                    previousLocation = null;
                    isDraw = true;
                }
            }

            if (v == btnStop){//stop
                getSnapshot();
                stopButton();
                passToSavedActivity();
            }
        }
    };

    private void stopButton() {
        if (isPause){
            isPause = false;
        }
        stopTimer();

        btnPause.setBackgroundResource(R.drawable.logo_round);
        btnStart.setEnabled(true);
        btnStart.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimary_Khoa, null));
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
        btnStop.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.unabled, null));
        isDraw = false;

        showBound = true;
        showBounds();
        points.clear();
    }

    public void getSnapshot(){
        if (mMap == null) {
            return;
        }

        SnapshotReadyCallback callback = new SnapshotReadyCallback() {
            @Override
            public void onSnapshotReady(Bitmap bitmap) {
                runRoute = bitmap;
            }
        };

        mMap.snapshot(callback, runRoute);
    }

    private void showBounds(){

        LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();


        for (int i = 0; i < points.size(); i++) {
            LatLng b_position = new LatLng(points.get(i).getLatitude(),points.get(i).getLongitude());
            boundBuilder.include(b_position);
        }

        LatLngBounds bounds = boundBuilder.build();

        int deviceWidth = getResources().getDisplayMetrics().widthPixels;
        int deviceHeight = getResources().getDisplayMetrics().heightPixels;
        int devicePadding = (int) (deviceHeight * 0.20);

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, deviceWidth, deviceHeight, devicePadding);
        mMap.animateCamera(cu);
    }

    public static Bitmap getBitmapFromView(View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable =view.getBackground();
        if (bgDrawable!=null)
            bgDrawable.draw(canvas);
        else
            canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        return returnedBitmap;
    }

    private void startTimer() {
        if (mTimer == null) {
            mTimer = new Timer();
        }

        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {

                    sendMessage(UPDATE_TEXT_VIEW);
                    do {
                        try {

                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } while (isPause);
                    count++;
                }
            };
        }

        //1s
        int period = 1000;
        //1s
        int delay = 1000;
        if (mTimer != null)
            mTimer.schedule(mTimerTask, delay, period);
    }

    private void pauseTimer(){
        isPause = !isPause;
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;

        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;

        }
    }

    public void sendMessage(int id) {
        if (mHandler != null) {
            Message message = Message.obtain(mHandler, id);
            mHandler.sendMessage(message);
        }
    }

    public static String getTime(int second) {
        if (second < 10) {
            return "00:00:0" + second;
        }
        if (second < 60) {
            return "00:00:" + second;
        }
        if (second < 3600) {
            int minute = second / 60;
            second = second - minute * 60;
            if (minute < 10) {
                if (second < 10) {
                    return "00:" + "0" + minute + ":0" + second;
                }
                return "00:" + "0" + minute + ":" + second;
            }
            if (second < 10) {
                return "00:" + minute + ":0" + second;
            }
            return "00:" + minute + ":" + second;
        }
        int hour = second / 3600;
        int minute = (second - hour * 3600) / 60;
        second = second - hour * 3600 - minute * 60;
        if (hour < 10) {
            if (minute < 10) {
                if (second < 10) {
                    return "0" + hour + ":0" + minute + ":0" + second;
                }
                return "0" + hour + ":0" + minute + ":" + second;
            }
            if (second < 10) {
                return "0" + hour + ":" + minute + ":0" + second;
            }
            return "0" + hour + ":" + minute + ":" + second;
        }
        if (minute < 10) {
            if (second < 10) {
                return hour + ":0" + minute + ":0" + second;
            }
            return hour + ":0" + minute + ":" + second;
        }
        if (second < 10) {
            return hour + ":" + minute + ":0" + second;
        }
        return hour + ":" + minute + ":" + second;
    }

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    public static double GetDistance(double lat1, double lon1, double lat2, double lon2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lon1) - rad(lon2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        double EARTH_RADIUS = 9000;
        s = s * EARTH_RADIUS;
        return s;
    }

    public double getPace(double length, int t) {
        return (length * 3600 / t);
    }

    /*time display*/
    public void updateTextView() {
        textTime.setText(getTime(count));

        int size = latList.size();
        double s;
        if (size > 1) {
            s = GetDistance(latList.get(size - 2), lonList.get(size - 2), latList.get(size - 1), lonList.get(size - 1));
        }
        else s = 0;

        sum += s;
        @SuppressLint("DefaultLocale") String Sum = String.format("%.2f", sum);
        textDis.setText(Sum);

        double d = 0;
        for (int j = Math.max(1, size - 5); j < size ; j += 1){
            d += GetDistance(latList.get(j - 1),
                    lonList.get(j - 1),
                    latList.get(j),
                    lonList.get(j));
        }

        double curPace = getPace(d, Math.min(size, 5));
        @SuppressLint("DefaultLocale") String CurPace = String.format("%.2f", curPace);
        textPace.setText(CurPace);

        pace = getPace(sum, count);
    }

    @SuppressLint("DefaultLocale")
    public void passToSavedActivity(){
        Intent intent = new Intent(MapsActivity.this, SaveActivity.class);
        intent.putExtra("duration", getTime(count));
        intent.putExtra("pace", String.format("%.2f", pace));
        intent.putExtra("distance", String.format("%.2f", sum));

        startActivity(intent);
    }
}
