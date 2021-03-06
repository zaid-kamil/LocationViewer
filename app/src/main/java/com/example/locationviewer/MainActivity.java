package com.example.locationviewer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.View;

import com.example.locationviewer.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private String[] locationPermission;
    private View textLocation;
    private ActivityMainBinding bind;
    private boolean hasLocPermission = false;
    private FusedLocationProviderClient providerClient;
    private AddressReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bind = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(bind.getRoot());

        locationPermission = new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        // if permission are not given by user
        if (!EasyPermissions.hasPermissions(this, locationPermission)) {
            showPermission();
        } else {
            // everything is awesome
            hasLocPermission = true;
            getLocationData();
        }
    }

    @SuppressLint("MissingPermission")
    private void getLocationData() {
        providerClient = LocationServices.getFusedLocationProviderClient(this);
        providerClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    try {
                        bind.textLocation.setText(
                                String.format("lat:%.3f,\nlong:%.3f", location.getLatitude(), location.getLongitude())
                        );
                        // service that get the data from background so we can have a readable address
                        Intent i = new Intent(this, AddressService.class);
                        i.putExtra("latitude", location.getLatitude());
                        i.putExtra("longitude", location.getLongitude());
                        i.putExtra("receiver", receiver);
                        startService(i);
                    } catch (Exception e) {
                        bind.textLocation.setText("could not get location " + e.getMessage());
                    }

                })
                .addOnFailureListener(e -> {
                    bind.textLocation.setText("could not get location " + e.getMessage());
                });
    }

    private void showPermission() {
        EasyPermissions.requestPermissions(this,
                "provide location permission",
                10,
                locationPermission
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        hasLocPermission = true;
        getLocationData();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        hasLocPermission = false;
    }

    @SuppressLint("RestrictedApi")
    class AddressReceiver extends ResultReceiver {

        public AddressReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultCode == RESULT_OK) {
                Address addr = resultData.getParcelable("address");

                if (addr != null) {
                    bind.textLocation.append(
                            String.format("\n%s\n", addr.getAddressLine(0))
                    );
                }
            }
        }
    }
}