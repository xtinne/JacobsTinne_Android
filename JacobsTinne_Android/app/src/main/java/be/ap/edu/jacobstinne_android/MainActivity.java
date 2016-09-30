package be.ap.edu.jacobstinne_android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private RequestQueue mRequestQueue;
    private String url = "http://datasets.antwerpen.be/v4/gis/bibliotheekoverzicht.json";
    private JSONObject bibliotheken;
    final ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
    MySQLiteHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissions();
        }

        helper = new MySQLiteHelper(this);

        // https://github.com/osmdroid/osmdroid/wiki/How-to-use-the-osmdroid-library
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(13);
        // default = meistraat
        mapView.getController().setCenter(new GeoPoint(51.2244, 4.38566));

        // http://code.tutsplus.com/tutorials/an-introduction-to-volley--cms-23800
        mRequestQueue = Volley.newRequestQueue(this);

        List<String> allBib = helper.getAll();
        if (allBib.isEmpty()) {
            Log.i("Empty", "Lijst bibliotheken is leeg.");
            // A JSONObject to post with the request. Null is allowed and indicates no parameters will be posted along with request.
            JSONObject obj = null;
            // Alle bibliotheken ophalen
            JsonObjectRequest jr = new JsonObjectRequest(Request.Method.GET, url, obj, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    bibliotheken = response;
                    CreateMarkers(bibliotheken);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("be.ap.edu.mapsaver", error.getMessage());
                }
            });
            mRequestQueue.add(jr);
        } else {
            Log.i("Not empty", "Lijst bibliotheken is niet leeg.");
            CreateMarkersSQLite(allBib);
        }
    }


    private void CreateMarkers(JSONObject bibliotheken) {
        try {
            JSONArray alleBibliotheken = bibliotheken.getJSONArray("data");

            for (int i = 0; i < alleBibliotheken.length(); i++) {
                JSONObject obj = (JSONObject)alleBibliotheken.get(i);

                String naam = obj.getString("naam");
                Double point_lat = obj.getDouble("point_lat");
                Double point_lng = obj.getDouble("point_lng");
                GeoPoint g = new GeoPoint(point_lat, point_lng);

                addMarker(naam, g);

                helper.addBib(naam, point_lat, point_lng);
            }
        }
        catch (Exception e) {
            Log.e("edu.ap.maps", e.getMessage());
        }
    }

    private void CreateMarkersSQLite(List<String> bibliothekenList) {
            try {
                for (String bibliotheek : bibliothekenList) {
                    String[] bib = bibliotheek.split(",");

                    String naam = String.valueOf(bib[0]);
                    Double point_lat = Double.valueOf(bib[1]);
                    Double point_lng = Double.valueOf(bib[2]);
                    GeoPoint g = new GeoPoint(point_lat, point_lng);

                    addMarker(naam, g);
                }
            }
            catch (Exception e) {
                Log.e("edu.ap.maps", e.getMessage());
            }
    }

    private void addMarker(String naam, GeoPoint g) {
        OverlayItem myLocationOverlayItem = new OverlayItem(naam, "Current Position", g);
        Drawable myCurrentLocationMarker = ResourcesCompat.getDrawable(getResources(), R.drawable.marker_default, null);
        myLocationOverlayItem.setMarker(myCurrentLocationMarker);

        items.add(myLocationOverlayItem);
        DefaultResourceProxyImpl resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());

        ItemizedIconOverlay<OverlayItem> currentLocationOverlay = new ItemizedIconOverlay<>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        Toast.makeText(getBaseContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        return true;
                    }
                }, resourceProxy);
        this.mapView.getOverlays().add(currentLocationOverlay);
        this.mapView.invalidate();
    }


    // START PERMISSION CHECK
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        String message = "osmdroid permissions:";
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            message += "\nLocation to show user location.";
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            message += "\nStorage access to store map tiles.";
        }
        if(!permissions.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            String[] params = permissions.toArray(new String[permissions.size()]);
            requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        } // else: We already have permissions, so handle as normal
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION and WRITE_EXTERNAL_STORAGE
                Boolean location = perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                Boolean storage = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                if(location && storage) {
                    // All Permissions Granted
                    Toast.makeText(MainActivity.this, "All permissions granted", Toast.LENGTH_SHORT).show();
                }
                else if (location) {
                    Toast.makeText(this, "Storage permission is required to store map tiles to reduce data usage and for offline usage.", Toast.LENGTH_LONG).show();
                }
                else if (storage) {
                    Toast.makeText(this, "Location permission is required to show the user's location on map.", Toast.LENGTH_LONG).show();
                }
                else { // !location && !storage case
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Storage permission is required to store map tiles to reduce data usage and for offline usage." +
                            "\nLocation permission is required to show the user's location on map.", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

// END PERMISSION CHECK

}
