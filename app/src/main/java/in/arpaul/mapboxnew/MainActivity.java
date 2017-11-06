package in.arpaul.mapboxnew;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.arpaul.utilitieslib.LogUtils;
import com.arpaul.utilitieslib.PermissionUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.lang.reflect.Type;

import in.arpaul.mapboxnew.dataobject.LocCoordDO;

import static in.arpaul.mapboxnew.BuildConfig.DEBUG;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private MapView mvMap;
    private MapboxMap map;
    private boolean mIsValidGlVersion;
    private EditText edtLocation;
    private TextView tvLocate, tvSearch, tvCopy, tvPaste;
    private final static String STYLE_ID = "";
    private GoogleApiClient mGoogleApiClient;

    private static final float MAPBOX_BITMAP_MIN_ZOOM_LEVEL = 13.0F;
    private final float zoom = 18F;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsValidGlVersion = isGlEsVersionSupported(this);

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_main);

        intialiseUIControls();
        mvMap.onCreate(savedInstanceState);

        bindContols();
    }

    void bindContols() {
        if (Build.VERSION.SDK_INT >= 21) {
            if(new PermissionUtils().checkPermission(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}) != PackageManager.PERMISSION_GRANTED){
                new PermissionUtils().verifyPermission(this,new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            } else{
                buildGoogleApiClient();
            }
        } else
            buildGoogleApiClient();

        tvLocate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int mWidth= MainActivity.this.getResources().getDisplayMetrics().widthPixels / 2;
                int mHeight= MainActivity.this.getResources().getDisplayMetrics().heightPixels / 2;
//                int[] screenCoord = new int[]{mWidth, mHeight};

                LatLng latLng = map.getProjection().fromScreenLocation(new PointF(mWidth, mHeight));
                LocCoordDO objLocCoordDO = new LocCoordDO();
                objLocCoordDO.lat = latLng.getLatitude();
                objLocCoordDO.lng = latLng.getLongitude();
                String coord = "";
                coord = new Gson().toJson(objLocCoordDO).toString();
//                coord = "Lat: " + objLocCoordDO.lat + "\nLong: " + objLocCoordDO.lng;
                edtLocation.setText(coord);
            }
        });

        tvSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LocCoordDO objLocCoordDO = new Gson().fromJson(edtLocation.getText().toString(), new TypeToken<LocCoordDO>(){}.getType());
                Log.d("LocCoordDO", objLocCoordDO.lat + " " + objLocCoordDO.lng);
                LatLng latlng = new LatLng(objLocCoordDO.lat, objLocCoordDO.lng);
                map.animateCamera(CameraUpdateFactory.newLatLng(latlng));
            }
        });

        tvCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipBoard();
            }
        });

        tvPaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pasteFromClipBoard();
            }
        });

        mvMap.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
                map.getUiSettings().setRotateGesturesEnabled(false);
                map.setMyLocationEnabled(true);

                setupMap();
            }
        });

    }

    private void copyToClipBoard() {
        ClipboardManager clipMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", edtLocation.getText().toString());
        clipMan.setPrimaryClip(clip);
    }

    private void pasteFromClipBoard() {
        ClipboardManager clipMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData.Item item = clipMan.getPrimaryClip().getItemAt(0);
        edtLocation.setText("");
        edtLocation.setText(item.getText().toString());
    }

    private LatLng getCenter() {
        LatLng latLng = null;
        try {
            int mWidth= this.getResources().getDisplayMetrics().widthPixels;
            int mHeight= this.getResources().getDisplayMetrics().heightPixels;
//            float coordinateX = mZipprMarkerView.getLeft() + (mZipprMarkerView.getWidth() / 2);
//            float coordinateY = mZipprMarkerView.getBottom();
            float[] coords = new float[]{mWidth, mHeight};
            latLng = map.getProjection().fromScreenLocation(new PointF(coords[0], coords[1]));

            Log.i(TAG, "getCenter: " + latLng.getLongitude() + ", " + latLng.getLatitude());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            return latLng;
        }
    }

    private void setupMap() {
        Log.d(TAG, "setupMap:");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if(map != null) {
            map.setMyLocationEnabled(true);
            map.getTrackingSettings().setMyLocationTrackingMode(MyLocationTracking.TRACKING_FOLLOW);
            map.setMinZoomPreference(MAPBOX_BITMAP_MIN_ZOOM_LEVEL);
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if(isGpsEnabled()) {
        } else {
            Toast.makeText(MainActivity.this, "Enable location sevices in your Settings", Toast.LENGTH_SHORT).show();
        }

        if(mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Location location = null;

        if (Build.VERSION.SDK_INT >= 21) {
            if (new PermissionUtils().checkPermission(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}) == PackageManager.PERMISSION_GRANTED) {
                location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            } else {
                new PermissionUtils().verifyPermission(this,new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            }
        } else
            location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        final LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
        mvMap.post(new Runnable() {
            @Override
            public void run() {
                if (map != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, (float) zoom));
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        LogUtils.infoLog(TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {

            int location = 0;
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                        grantResult == PackageManager.PERMISSION_GRANTED) {
                    location++;
                } else if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                        grantResult == PackageManager.PERMISSION_GRANTED) {
                    location++;
                }
            }

            if(location == 2) {
                buildGoogleApiClient();
            } else {
                Toast.makeText(MainActivity.this, "Allow location permission to access your location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mvMap.onStart();

        if (DEBUG) Log.i(TAG, "onStart: ");
        if (mGoogleApiClient != null && (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected())) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mvMap.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mvMap.onStop();
        if (DEBUG) Log.i(TAG, "onStop: ");

        if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mvMap.onDestroy();
//        unregisterReceiver(br_Close);
        if (DEBUG) Log.i(TAG, "onDestroy: ");
    }

    private boolean isGpsEnabled(){
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isGpsProviderEnabled;
    }

    public static boolean isGlEsVersionSupported(Context context) {

        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();

        return Float.valueOf(configurationInfo.getGlEsVersion()) >= 3.0;
    }

    void intialiseUIControls() {
        mvMap           = (MapView) findViewById(R.id.mvMap);

        edtLocation     = (EditText) findViewById(R.id.edtLocation);

        tvLocate        = (TextView) findViewById(R.id.tvLocate);
        tvSearch        = (TextView) findViewById(R.id.tvSearch);
        tvCopy          = (TextView) findViewById(R.id.tvCopy);
        tvPaste         = (TextView) findViewById(R.id.tvPaste);

    }
}
