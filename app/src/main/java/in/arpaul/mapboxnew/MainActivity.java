package in.arpaul.mapboxnew;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.RasterLayer;
import com.mapbox.mapboxsdk.style.sources.RasterSource;

import in.arpaul.mapboxnew.dataobject.LocCoordDO;

import static in.arpaul.mapboxnew.BuildConfig.DEBUG;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private MapView mvMap;
    private MapboxMap map;
    private boolean mIsValidGlVersion;
    private ImageView ivMarkerCentre;
    private EditText edtLocation;
    private TextView tvLocate, tvSearch, tvCopy, tvPaste;
    private final static String STYLE_ID = "";
    private GoogleApiClient mGoogleApiClient;

    private static final float MAPBOX_BITMAP_MIN_ZOOM_LEVEL = 13.0F;
    private final float zoom = 18F;
    private double lati = 16.268052, longi = 80.999730;//16.268052, 80.999730 ----- Nimmakuru

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsValidGlVersion = isGlEsVersionSupported(this);

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));//17.447863, 78.391388
//        Mapbox.getInstance(this, getString(R.string.digtalglobe_access_token_sk));
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
                float mWidth = ivMarkerCentre.getX() + ivMarkerCentre.getWidth()  / 2;
                float mHeight = ivMarkerCentre.getY() + ivMarkerCentre.getHeight();

                float wrongX = ivMarkerCentre.getLeft() + (ivMarkerCentre.getWidth() / 2);
                float wrongY = ivMarkerCentre.getBottom();


//                int mWidth= MainActivity.this.getResources().getDisplayMetrics().widthPixels / 2;
//                int mHeight= MainActivity.this.getResources().getDisplayMetrics().heightPixels / 2;
//                mvMap.getpo

                LogUtils.debugLog("markerLoc", "mWidth: " + mWidth + " mHeight: " + mHeight + "\nwrongX: " + wrongX + " wrongY: " + wrongY);

                LatLng latLng = map.getProjection().fromScreenLocation(new PointF(mWidth, mHeight));
                LatLng wronglatLng = map.getProjection().fromScreenLocation(new PointF(wrongX, wrongY));

                setLocateBody(latLng, wronglatLng);
            }
        });

        tvSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                map.clear();

                if(!TextUtils.isEmpty(edtLocation.getText().toString())) {

                    LocCoordDO objLocCoordDO = new Gson().fromJson(edtLocation.getText().toString(), new TypeToken<LocCoordDO>(){}.getType());
                    Log.d("LocCoordDO", objLocCoordDO.lat + " " + objLocCoordDO.lng);
                    LatLng latlng = new LatLng(objLocCoordDO.lat, objLocCoordDO.lng);

                    LatLng wronglatlng = new LatLng(objLocCoordDO.wrongLat, objLocCoordDO.wrongLng);

                    MarkerOptions wrongMarker = new MarkerOptions().position(wronglatlng).title("Wrong");//.icon(iconMax);
                    map.addMarker(wrongMarker);

                    Location correctPoint=new Location("locationCorrect");
                    correctPoint.setLatitude(objLocCoordDO.lat);
                    correctPoint.setLongitude(objLocCoordDO.lng);

                    Location wrongPoint=new Location("locationWrong");
                    wrongPoint.setLatitude(objLocCoordDO.wrongLat);
                    wrongPoint.setLongitude(objLocCoordDO.wrongLng);

                    objLocCoordDO.distance = correctPoint.distanceTo(wrongPoint);
                    objLocCoordDO.bearing = correctPoint.bearingTo(wrongPoint);

//                IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
//                Icon iconMin = iconFactory.fromResource(R.drawable.ic_min);
//                Icon iconMax = iconFactory.fromResource(R.drawable.ic_max);

//                double[] boundingBox = getBoundingBox(wrongPoint.getLatitude(), wrongPoint.getLongitude(), objLocCoordDO.distance, objLocCoordDO.bearing);
//                MarkerOptions minMarker = new MarkerOptions().position(new LatLng(boundingBox[0], boundingBox[1])).title("Min");//.icon(iconMin);
//                map.addMarker(minMarker);
//                MarkerOptions maxMarker = new MarkerOptions().position(new LatLng(boundingBox[2], boundingBox[3])).title("Max");//.icon(iconMax);
//                map.addMarker(maxMarker);

//                System.out.println("boundingBox: " + boundingBox[0] + " " + boundingBox[1] +
//                        "\n" + boundingBox[2] + " " + boundingBox[3] +
//                        "\n" + boundingBox[4] + " " + boundingBox[5]);


                    double[] actualPoint = getActualPOint(wrongPoint.getLatitude(), wrongPoint.getLongitude(), objLocCoordDO.distance);
                    MarkerOptions diffMarker = new MarkerOptions().position(new LatLng(actualPoint[0], actualPoint[1])).title("Diff");//.icon(iconMax);
                    map.addMarker(diffMarker);

                    System.out.println("actualPoint: " + actualPoint[0] + " " + actualPoint[1]);

                    setSearchBody(objLocCoordDO);
                } else {
                    latlng = new LatLng(lati, longi);
                }

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));
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

    private double[] getActualPOint(final double pLatitude, final double pLongitude, final double pDistanceInMeters) {

        final double[] boundingBox = new double[2];

        double lat = pLatitude + (180/Math.PI)*(pDistanceInMeters/6378137);
//        double lon = pLongitude + (180/Math.PI)*(pDistanceInMeters/6378137)/Math.cos(pLatitude);
        double lon = pLongitude;

        boundingBox[0] = lat;
        boundingBox[1] = lon;

        return boundingBox;
    }

    private double[] getBoundingBox(final double pLatitude, final double pLongitude, final double pDistanceInMeters, final double bearing) {

        final double[] boundingBox = new double[6];

        final double latRadian = Math.toRadians(pLatitude);

        double deg = 110.574235;
//        double deg = bearing;
        final double degLatKm = deg;
        final double degLongKm = deg *  Math.cos(latRadian);
        final double deltaLat = pDistanceInMeters / 1000.0 / degLatKm;
        final double deltaLong = pDistanceInMeters / 1000.0 / degLongKm;

        final double minLat = pLatitude - deltaLat;
        final double minLong = pLongitude - deltaLong;
        final double maxLat = pLatitude + deltaLat;
        final double maxLong = pLongitude + deltaLong;

        double lat = pLatitude + (180/Math.PI)*(pDistanceInMeters/6378137);
//        double lon = pLongitude + (180/Math.PI)*(pDistanceInMeters/6378137)/Math.cos(pLatitude);
        double lon = pLongitude;

        boundingBox[0] = minLat;
        boundingBox[1] = minLong;
        boundingBox[2] = maxLat;
        boundingBox[3] = maxLong;

        boundingBox[4] = lat;
        boundingBox[5] = lon;

        return boundingBox;
    }

    private void setLocateBody(LatLng latLng, LatLng wrongLatLng) {
        LocCoordDO objLocCoordDO = new LocCoordDO();
        objLocCoordDO.lat = latLng.getLatitude();
        objLocCoordDO.lng = latLng.getLongitude();

        objLocCoordDO.wrongLat = wrongLatLng.getLatitude();
        objLocCoordDO.wrongLng = wrongLatLng.getLongitude();

        objLocCoordDO.zoom = (int) map.getCameraPosition().zoom;;

        String coord = "";
        coord = new Gson().toJson(objLocCoordDO).toString();
        edtLocation.setText(coord);
    }

    private void setSearchBody(LocCoordDO objLocCoordDO) {
        String coord = "";
        coord = new Gson().toJson(objLocCoordDO).toString();
        LogUtils.debugLog("Search", coord);
        edtLocation.setText(coord);
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
//            map.setMinZoomPreference(MAPBOX_BITMAP_MIN_ZOOM_LEVEL);

//            map.setStyle("cimwagg8700f6ahnpta9fktm4");

//            map.setStyleUrl("https://cloud.pix4d.com/project/embed/86503-16623cc0fa6e41ecba6c0fed5b5954af");

//            RasterSource chicagoSource = new RasterSource("chicago-source", "mapbox://mapbox.u8yyzaor");
            RasterSource nimmakuru = new RasterSource("nimmakuru", "https://cloud.pix4d.com/project/embed/86503-16623cc0fa6e41ecba6c0fed5b5954af");
            map.addSource(nimmakuru);

            RasterLayer nimmakuruLayer = new RasterLayer("nimmakuru", "nimmakuru");
            map.addLayer(nimmakuruLayer);
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

    private LatLng latlng;
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

        if(location != null)
            latlng = new LatLng(location.getLatitude(), location.getLongitude());
        else
            latlng = new LatLng(lati, longi);


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

        ivMarkerCentre  = (ImageView) findViewById(R.id.ivMarkerCentre);

        ivMarkerCentre.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                ivMarkerCentre.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                int h = ivMarkerCentre.getHeight();
                ivMarkerCentre.setTranslationY(-(float) h / 2);
            }
        });
    }
}
