package com.example.hellotoast;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback, LocationListener {

    private static final String TAG = "GPS_MAP";
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final long MIN_TIME_MS = 5000;   // 5 seconds
    private static final float MIN_DISTANCE_M = 5f;  // 5 meters

    private GoogleMap mMap;
    private LocationManager locationManager;

    // UI
    private TextView tvInfo, tvLatLng, tvProvider;

    // Counter for markers
    private int markerCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        setTitle("GPS Map");

        // Init UI
        tvInfo = findViewById(R.id.tvInfo);
        tvLatLng = findViewById(R.id.tvLatLng);
        tvProvider = findViewById(R.id.tvProvider);

        // Init location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Init map
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Called when the map is ready to use.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Check GPS & start location updates
        checkGpsAndRequestLocation();
    }

    /**
     * Check if GPS is enabled. If not, show dialog to enable it.
     * Then request location updates from both GPS and Network providers.
     */
    private void checkGpsAndRequestLocation() {
        // 1. Check permissions first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        // 2. Check if GPS provider is enabled
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsEnabled) {
            showGpsDisabledDialog();
        }

        // 3. Request location updates from GPS provider
        if (gpsEnabled) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    this
            );
            Log.d(TAG, "GPS provider: listening for updates");
        }

        // 4. Request location updates from Network provider
        if (networkEnabled) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    this
            );
            Log.d(TAG, "Network provider: listening for updates");
        }

        // 5. Enable my-location layer on the map
        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
        }

        // 6. Try to get last known location as initial position
        Location lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (lastGps != null) {
            onLocationChanged(lastGps);
        } else if (lastNetwork != null) {
            onLocationChanged(lastNetwork);
        } else {
            tvInfo.setText("Recherche de position en cours...");
        }
    }

    /**
     * Show alert dialog when GPS is disabled, offering to go to settings.
     */
    private void showGpsDisabledDialog() {
        new AlertDialog.Builder(this)
                .setTitle("GPS désactivé")
                .setMessage("Le GPS est désactivé sur votre appareil.\n" +
                        "Voulez-vous l'activer dans les paramètres ?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Open location settings
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Non", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Toast.makeText(MapsActivity.this,
                                "Le GPS est nécessaire pour une localisation précise",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .setCancelable(false)
                .show();
    }

    // ========================
    // LocationListener callbacks
    // ========================

    /**
     * Called when a new location is received from GPS or Network provider.
     * Adds a marker on the map and zooms to the position.
     */
    @Override
    public void onLocationChanged(@NonNull Location location) {
        markerCount++;

        double lat = location.getLatitude();
        double lng = location.getLongitude();
        String provider = location.getProvider() != null ? location.getProvider() : "unknown";
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        LatLng position = new LatLng(lat, lng);

        Log.d(TAG, "Position #" + markerCount + " : lat=" + lat + " lng=" + lng
                + " provider=" + provider);

        // Add marker on the map
        if (mMap != null) {
            // Choose marker color based on provider
            float color;
            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                color = BitmapDescriptorFactory.HUE_RED;
            } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
                color = BitmapDescriptorFactory.HUE_BLUE;
            } else {
                color = BitmapDescriptorFactory.HUE_GREEN;
            }

            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title("Position #" + markerCount)
                    .snippet("Provider: " + provider + " | " + time)
                    .icon(BitmapDescriptorFactory.defaultMarker(color)));

            // Zoom to position with animation
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16f));
        }

        // Update info panel
        tvInfo.setText("📍 Position #" + markerCount + " reçue à " + time);
        tvLatLng.setText(String.format(Locale.getDefault(),
                "Lat: %.6f / Lng: %.6f", lat, lng));

        String providerLabel = LocationManager.GPS_PROVIDER.equals(provider) ?
                "🛰️ GPS" : "📶 NETWORK";
        tvProvider.setText(providerLabel);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "Provider status changed: " + provider + " status=" + status);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.d(TAG, "Provider enabled: " + provider);
        Toast.makeText(this, "Provider activé : " + provider, Toast.LENGTH_SHORT).show();

        // Re-start listening when a provider is enabled
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(provider, MIN_TIME_MS, MIN_DISTANCE_M, this);
        }
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.d(TAG, "Provider disabled: " + provider);

        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            showGpsDisabledDialog();
        }
    }

    // ========================
    // Permissions
    // ========================

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkGpsAndRequestLocation();
            } else {
                tvInfo.setText("❌ Permission de localisation refusée");
                Toast.makeText(this, "Permission refusée. L'application a besoin de la localisation.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ========================
    // Lifecycle
    // ========================

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check GPS when coming back from settings
        if (mMap != null) {
            checkGpsAndRequestLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery
        locationManager.removeUpdates(this);
    }
}
