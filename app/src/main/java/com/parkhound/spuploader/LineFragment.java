package com.parkhound.spuploader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class LineFragment extends Fragment implements
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    static final String TAG = "parking street";

    static final LatLng melbourne = new LatLng(-37.815, 144.966);
    private SupportMapFragment fragment;
    private static View rootView;
    private GoogleMap mMap;

    private Button btnSat;
    private Button btnMap;
    private Button btnPosStart;
    private Button btnPosEnd;
    private TextView startAddressTextView;
    private TextView endAddressTextView;

    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Represents a geographical location.
     */
    protected Location mLastLocation;

    /**
     * Tracks whether the user has requested an address. Becomes true when the user requests an
     * address and false when the address (or an error message) is delivered.
     * The user requests an address by pressing the Fetch Address button. This may happen
     * before GoogleApiClient connects. This activity uses this boolean to keep track of the
     * user's intent. If the value is true, the activity tries to fetch the address as soon as
     * GoogleApiClient connects.
     */
    protected boolean mAddressRequested;

    /**
     * The formatted location address.
     */
    protected String mAddressOutput;

    /**
     * Receiver registered with this activity to get the response from FetchAddressIntentService.
     */
    private AddressResultReceiver mResultReceiver;

    // Photo album for this application
    private static final String albumName = "CameraSample";
    private File mAlbumDir = null;
    private String mCurrentPhotoPath;

    private boolean bIsButtonPressed = false;
    private Button btnPicStart;
    private Button btnPicEnd;
    static final int REQUEST_TAKE_PHOTO = 1;
    private ImageView imgLarge;
    private ImageView mImageViewStart;
    private ImageView mImageViewEnd;
    private final int imageTargetW = 100;
    private final int imageTargetH = 100;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        if(savedInstanceState == null) {
            // To avoid tab crash after second entering, remove it before creating it every time.
            if (rootView != null) {
                ViewGroup parent = (ViewGroup) rootView.getParent();
                if (parent != null)
                    parent.removeView(rootView);
            }

            try{
                if(rootView == null) {
                    rootView = inflater.inflate(R.layout.frg_line, container, false);
                }

                fragment = (SupportMapFragment) getChildFragmentManager()
                        .findFragmentById(R.id.map);
                fragment.getMapAsync(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mResultReceiver = new AddressResultReceiver(new Handler());

        startAddressTextView = (TextView) rootView.findViewById(R.id.startAddressView);
        btnPosStart = (Button) rootView.findViewById(R.id.startLocation);
        btnPosStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchAddressButtonHandler();
            }
        });

        endAddressTextView = (TextView) rootView.findViewById(R.id.endAddressView);
        btnPosEnd = (Button) rootView.findViewById(R.id.endLocation);
        btnPosEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchAddressButtonHandler();
            }
        });

        // Set defaults, then update using values stored in the Bundle.
        mAddressRequested = false;
        mAddressOutput = "";
        updateValuesFromBundle(savedInstanceState);

        updateUIWidgets();
        buildGoogleApiClient();

        return rootView;
    }

    @Override
    public void onMapReady(final GoogleMap map) {
        Log.e(TAG, "GoogleMap is ready.");
        map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        mMap = map;
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                Log.e(TAG, "Put a new marker on POS:" + point.toString());
                mLastLocation.setLatitude(point.latitude);
                mLastLocation.setLongitude(point.longitude);
                getCurrentAddress();
            }
        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                marker.hideInfoWindow();
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                mLastLocation.setLatitude(marker.getPosition().latitude);
                mLastLocation.setLongitude(marker.getPosition().longitude);
                getCurrentAddress();
            }

            @Override
            public void onMarkerDrag(Marker marker) {
            }

        });

        btnSat = (Button) rootView.findViewById(R.id.satView);
        btnSat.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v){
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            }
        });

        btnMap = (Button) rootView.findViewById(R.id.mapView);
        btnMap.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        btnPicStart = (Button) rootView.findViewById(R.id.btnStartPic);
        btnPicStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        mImageViewStart = (ImageView) rootView.findViewById(R.id.imageViewStart);
        mImageViewStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View paramView) {
                LayoutInflater inflater = LayoutInflater.from(rootView.getContext());
                View imgEntryView = inflater.inflate(R.layout.dialog_photo_entry, null);
                final AlertDialog dialog = new AlertDialog.Builder(rootView.getContext()).create();
                imgLarge = (ImageView)imgEntryView.findViewById(R.id.largeImage);
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();

                // Decode the image file into a Bitmap sized to fill the View
                bmOptions.inJustDecodeBounds = false;
                bmOptions.inSampleSize = 1;

                Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
                imgLarge.setImageBitmap(bitmap);
                imgLarge.setVisibility(View.VISIBLE);

                dialog.setView(imgEntryView);
                dialog.show();
                imgEntryView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View paramView) {
                        dialog.cancel();
                    }
                });
            }
        });

        btnPicStart = (Button) rootView.findViewById(R.id.btnStartPic);
        btnPicStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        mImageViewEnd = (ImageView) rootView.findViewById(R.id.imageViewEnd);
        btnPicEnd = (Button) rootView.findViewById(R.id.btnEndPic);
        btnPicEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO: {
                if (resultCode == android.support.v4.app.FragmentActivity.RESULT_OK) {
                    handleCameraPhoto();
                }
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            // Determine whether a Geocoder is available.
            if (!Geocoder.isPresent()) {
                Toast.makeText(rootView.getContext(), R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
                return;
            }
            // It is possible that the user presses the button to get the address before the
            // GoogleApiClient object successfully connects. In such a case, mAddressRequested
            // is set to true, but no attempt is made to fetch the address (see
            // fetchAddressButtonHandler()) . Instead, we start the intent service here if the
            // user has requested an address, since we now have a connection to GoogleApiClient.
            startIntentService();
            bIsButtonPressed = true;
        }
    }

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     */
    protected void startIntentService() {
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(this.getActivity(), FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        Log.e(TAG, "startIntentService()");
        this.getActivity().startService(intent);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.e(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.e(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    /**
     * Updates fields based on data stored in the bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Check savedInstanceState to see if the address was previously requested.
            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }
            // Check savedInstanceState to see if the location address string was previously found
            // and stored in the Bundle. If it was found, display the address string in the UI.
            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
                displayAddressOutput();
            }
        }
    }

    /**
     * Builds a GoogleApiClient. Uses {@code #addApi} to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(rootView.getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Runs when user clicks the Fetch Address button. Starts the service to fetch the address if
     * GoogleApiClient is connected.
     */
    public void fetchAddressButtonHandler() {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        bIsButtonPressed = true;
        getCurrentAddress();
    }

    public void getCurrentAddress() {
        // We only start the service to fetch the address if GoogleApiClient is connected.
        if (mGoogleApiClient.isConnected() && mLastLocation != null) {
            startIntentService();
        }
        // If GoogleApiClient isn't connected, we process the user's request by setting
        // mAddressRequested to true. Later, when GoogleApiClient connects, we launch the service to
        // fetch the address. As far as the user is concerned, pressing the Fetch Address button
        // immediately kicks off the process of getting the address.
        mAddressRequested = true;
        updateUIWidgets();
    }

    /**
     * Updates the address in the UI.
     */
    protected void displayAddressOutput() {
        startAddressTextView.setText(mAddressOutput);

        LatLng pos = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        DecimalFormat df = new DecimalFormat("#.0000");

        mMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(19));
        mMap.clear();
        Marker marker = mMap.addMarker(new MarkerOptions()
                .snippet("Lat(" + df.format(pos.latitude) + ")" +
                        ", Lng("+ df.format(pos.longitude) + ")")
                .position(pos)
                .draggable(true));

        if (bIsButtonPressed) {
            marker.setTitle("Current Location");
            bIsButtonPressed = false;
        } else {
            marker.setTitle("Geo-coordinator");
        }
        marker.showInfoWindow();
    }

    /**
     * Toggles the visibility of the progress bar. Enables or disables the Fetch Address button.
     */
    private void updateUIWidgets() {
        if (mAddressRequested) {
            btnPosStart.setEnabled(false);
        } else {
            btnPosStart.setEnabled(true);
        }
    }

    /**
     * Shows a toast with the given text.
     */
    protected void showToast(String text) {
        Toast.makeText(rootView.getContext(), text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save whether the address has been requested.
        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);

        // Save the address string.
        savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         *  Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            displayAddressOutput();

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                showToast(getString(R.string.address_found));
            }

            // Reset. Enable the Fetch Address button and stop showing the progress bar.
            mAddressRequested = false;
            updateUIWidgets();
        }
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            storageDir = new File (Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), albumName);

            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        Log.e(TAG, "failed to create directory");
                        return null;
                    }
                }
            }
        }

        return storageDir;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File albumF = getAlbumDir();
        File image = File.createTempFile(imageFileName, ".jpg", albumF);
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(this.getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile;
            try {
                photoFile = createImageFile();
                mCurrentPhotoPath = photoFile.getAbsolutePath();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
                photoFile = null;
                mCurrentPhotoPath = null;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void handleCameraPhoto() {
        if (mCurrentPhotoPath != null) {
            setPic();
            galleryAddPic();
            //mCurrentPhotoPath = null;
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.getActivity().sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Get the dimensions of the View
        //int targetW = mImageViewStart.getWidth();
        int targetW = imageTargetW;
        //int targetH = mImageViewStart.getHeight();
        int targetH = imageTargetH;

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        /* Figure out which way needs to be reduced less */
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageViewStart.setImageBitmap(bitmap);
        mImageViewStart.setVisibility(View.VISIBLE);
    }
}