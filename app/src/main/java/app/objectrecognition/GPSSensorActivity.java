package app.objectrecognition;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class GPSSensorActivity extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mGravity;
    private Sensor mMagnetic;
    private float[] accVal;
    private float[] magVal;

    private List<Direction> directions = new ArrayList<Direction>();
    private Location prevLocation = null;
    private LocationManager lm;
    private LocationListener listener;

    private List<City> myCities = new ArrayList<City>();

    private Context context;
    private static final int durationTime = Toast.LENGTH_SHORT;
    private TextView directionInfo;
    private Toast showInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        setContentView(R.layout.gps_sensors_activity);

        if (mSensorManager != null) {
            mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
        }

        initiateDirections();

        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        listener = new MyListener();
        context = getApplicationContext();

        initDemoDataOfCities();
        registerListener();
        directionInfo = (TextView) findViewById(R.id.editTextDirection);
    }

    private void registerListener() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
    }

    protected void onResume() {
        super.onResume();
        if (mSensorManager != null) {
            mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case (Sensor.TYPE_GRAVITY):
                accVal = sensorEvent.values.clone();
                break;
            case (Sensor.TYPE_MAGNETIC_FIELD):
                magVal = sensorEvent.values.clone();
                break;
        }
        if (accVal != null && magVal != null) {
            findDirection();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void initiateDirections() {
        Direction top = new Direction(0, 0, 1, "TOP");
        Direction bottom = new Direction(0, 0, -1, "BOTTOM");
        Direction east = new Direction(1, 0, 0, "EAST");
        Direction west = new Direction(-1, 0, 0, "WEST");
        Direction north = new Direction(0, 1, 0, "NORTH");
        Direction south = new Direction(0, -1, 0, "SOUTH");
        directions.add(top);
        directions.add(bottom);
        directions.add(east);
        directions.add(west);
        directions.add(south);
        directions.add(north);
    }

    private void initDemoDataOfCities() {
        City moscow = new City();
        moscow.setName("Moscow\n");
        moscow.setLatitude(55.755826);
        moscow.setLongitude(37.617299900000035);
        moscow.setDirection("EAST");
        myCities.add(moscow);

        City athens = new City();
        athens.setName("Athens\n");
        athens.setLatitude(37.9752774);
        athens.setLongitude(23.736975700000016);
        athens.setDirection("SOUTH");
        myCities.add(athens);

        City paris = new City();
        paris.setName("Paris\n");
        paris.setLatitude(48.85661400000001);
        paris.setLongitude(2.3522219000000177);
        paris.setDirection("WEST");
        myCities.add(paris);

        City stockholm = new City();
        stockholm.setName("Stockholm\n");
        stockholm.setLatitude(59.32932349999999);
        stockholm.setLongitude(18.068580800000063);
        stockholm.setDirection("NORTH");
        myCities.add(stockholm);
    }

    private void findDirection() {
        float[] objectM = new float[3];
        float[] camM = convertBframeToMframe();
        double angle;
        double minAngle = 90;
        Direction chosenDirection = new Direction();
        for (Direction direction : directions) {
            objectM[0] = direction.getX();
            objectM[1] = direction.getY();
            objectM[2] = direction.getZ();
            angle = getAngle(camM, objectM);
            if (angle < minAngle) {
                minAngle = angle;
                chosenDirection = direction;
            }
        }
        directionInfo.setText(chosenDirection.getDescription());
        if (prevLocation != null) {
            findNearbyCity(chosenDirection.getDescription());
        }
    }

    private float[] convertBframeToMframe() {
        float[] cameraB = {0, 0, -1};
        float[] cameraM = new float[3];
        float[] rotFromBtoM = new float[9];
        boolean isSuccess = SensorManager.getRotationMatrix(rotFromBtoM, null, accVal, magVal);
        // Multiply the vector of the camera by the rotation matrix
        // to obtain the vector representation of the camera in the M-frame
        if (isSuccess) {
            cameraM[0] = cameraB[0] * rotFromBtoM[0] + cameraB[1] * rotFromBtoM[1] + cameraB[2] * rotFromBtoM[2];
            cameraM[1] = cameraB[0] * rotFromBtoM[0 + 3] + cameraB[1] * rotFromBtoM[1 + 3] + cameraB[2] * rotFromBtoM[2 + 3];
            cameraM[2] = cameraB[0] * rotFromBtoM[0 + 6] + cameraB[1] * rotFromBtoM[1 + 6] + cameraB[2] * rotFromBtoM[2 + 6];
        }
        return cameraM;
    }

    private double getAngle(float[] a, float b[]) {
        double temp = (a[0] * b[0] + a[1] * b[1] + a[2] * b[2])
                / Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])
                / Math.sqrt(b[0] * b[0] + b[1] * b[1] + b[2] * b[2]);
        return Math.acos(temp);
    }

    public void findNearbyCity(String direct) {
        List<City> cities;
        switch (direct) {
            case "EAST":
                cities = getCities("EAST");
                calculateDistance(cities);
                break;
            case "WEST":
                cities = getCities("WEST");
                calculateDistance(cities);
                break;
            case "NORTH":
                cities = getCities("NORTH");
                calculateDistance(cities);
                break;
            case "SOUTH":
                cities = getCities("SOUTH");
                calculateDistance(cities);
                break;
        }
    }

    private List<City> getCities(String direction) {
        List<City> list = new ArrayList<>();
        for (City myCity : myCities) {
            if (myCity.getDirection().equals(direction)) {
                list.add(myCity);
            }
        }
        return list;
    }

    private void calculateDistance(List<City> cities) {
        if (!cities.isEmpty()) {
            City resultCity = null;
            boolean first = true;
            float distance = 0;
            for (City city : cities) {
                Location tempLoc = new Location("");
                tempLoc.setLatitude(city.getLatitude());
                tempLoc.setLongitude(city.getLongitude());
                float distanceTmp = prevLocation.distanceTo(tempLoc);
                if (first) {
                    distance = distanceTmp;
                    resultCity = city;
                    first = false;
                }
                if (distanceTmp < distance) {
                    resultCity = city;
                    distance = distanceTmp;
                }
            }
            if (resultCity != null) {
                if (showInfo != null) {
                    showInfo.cancel();
                }
                showInfo = Toast.makeText(context, "\nCity: " + resultCity.getName()
                        + "\nDistance: " + (int) distance / 1000 + " km", durationTime);
                showInfo.setGravity(Gravity.CENTER, 0, 0);
                showInfo.show();
            }
        }
    }

    private class MyListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            prevLocation = location;
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }
}
