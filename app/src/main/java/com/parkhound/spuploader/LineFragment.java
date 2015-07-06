package com.parkhound.spuploader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class LineFragment extends Fragment implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "parking street";
    private static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    private static final String LOCATION_ADDRESS_KEY = "location-address";
    private static final String ALBUM_NAME = "CameraSample";

    private static View rootView;
    private static int lineID = 1;

    // Location
    private Button btnPosStart;
    private Button btnPosEnd;
    private TextView tvStartAddress;
    private TextView tvEndAddress;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private boolean mAddressRequested;
    private String mAddressOutput;
    private AddressResultReceiver mResultReceiver;
    private boolean mIsClickOnMap = false;
    private LatLng mStartPos;
    private LatLng mEndPos;
    private Marker mMarkerStart;
    private Marker mMarkerEnd;
    private final int ID_POS_START = 1;
    private final int ID_POS_END = 2;
    private int btnID = ID_POS_START;

    // Restrictions
    private Spinner spinnerSpaceType;
    private Spinner spinnerResType;
    private Spinner spinnerDur;
    private Spinner spinnerStartDay;
    private Spinner spinnerEndDay;
    private EditText etStartTime;
    private EditText etEndTime;
    private EditText etPrice;
    private Button btnFree;
    private Button btnAddRestrictioins;
    private Button btnSubmit;

    private String mSpaceType;
    private String mResType;
    private String mResDur;
    private String mStartDay;
    private String mEndDay;
    private String mStartTime;
    private String mEndTime;
    private String mPrice;
    private boolean mIsClickOnFree = false;
    private final String[] Day = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    // Pictures
    private final int REQUEST_TAKE_PHOTO = 1;
    private final int imageTargetW = 100;
    private final int imageTargetH = 100;
    private ImageView mImageViewStart;
    private ImageView mImageViewEnd;
    private String mCurrentPhotoPath;
    private final int ID_PIC_START = 1;
    private final int ID_PIC_END = 2;
    private int picID = ID_PIC_START;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
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

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            // To avoid tab crash after second entering, remove it before creating it every time.
            if (rootView != null) {
                ViewGroup parent = (ViewGroup) rootView.getParent();
                if (parent != null)
                    parent.removeView(rootView);
            }

            try {
                rootView = inflater.inflate(R.layout.frg_line, container, false);
                SupportMapFragment fragment = (SupportMapFragment) getChildFragmentManager()
                        .findFragmentById(R.id.map);
                fragment.getMapAsync(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        updateValuesFromBundle(savedInstanceState);
        initLocation(savedInstanceState);
        initRestrictions();
        initPictures();

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);
        savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
        super.onSaveInstanceState(savedInstanceState);
    }

    // Updates fields based on data stored in the bundle.
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }

            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
                displayAddressOutput();
            }
        }
    }

    public void initLocation(Bundle savedInstanceState) {
        mResultReceiver = new AddressResultReceiver(new Handler());

        tvStartAddress = (TextView) rootView.findViewById(R.id.startAddressView);
        btnPosStart = (Button) rootView.findViewById(R.id.startLocation);
        btnPosStart.requestFocus();
        btnPosStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnID = ID_POS_START;
                mIsClickOnMap = false;
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                getCurrentAddress();
            }
        });

        tvEndAddress = (TextView) rootView.findViewById(R.id.endAddressView);
        btnPosEnd = (Button) rootView.findViewById(R.id.endLocation);
        btnPosEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnID = ID_POS_END;
                mIsClickOnMap = false;
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                getCurrentAddress();
            }
        });

        mAddressRequested = false;
        mAddressOutput = "";

        updateUIWidgets();
        buildGoogleApiClient();
    }

    public void initRestrictions() {
        spinnerSpaceType = (Spinner) rootView.findViewById(R.id.spinnerSpaceType);
        spinnerResType = (Spinner) rootView.findViewById(R.id.spinnerRestrictionType);
        spinnerDur = (Spinner) rootView.findViewById(R.id.spinnerRestrictionDuration);
        spinnerStartDay = (Spinner) rootView.findViewById(R.id.spinnerStartDay);
        spinnerEndDay = (Spinner) rootView.findViewById(R.id.spinnerEndDay);

        ArrayAdapter adapter = ArrayAdapter.createFromResource(rootView.getContext(),
                R.array.spaceType, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpaceType.setAdapter(adapter);

        adapter = ArrayAdapter.createFromResource(rootView.getContext(),
                R.array.restrictionType, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerResType.setAdapter(adapter);

        adapter = ArrayAdapter.createFromResource(rootView.getContext(),
                R.array.duration, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDur.setAdapter(adapter);

        adapter = ArrayAdapter.createFromResource(rootView.getContext(),
                R.array.day, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStartDay.setAdapter(adapter);

        adapter = ArrayAdapter.createFromResource(rootView.getContext(),
                R.array.day, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEndDay.setAdapter(adapter);

        etStartTime = (EditText) rootView.findViewById(R.id.editTextStartTime);
        etStartTime.setInputType(InputType.TYPE_NULL);
        etEndTime = (EditText) rootView.findViewById(R.id.editTextEndTime);
        etEndTime.setInputType(InputType.TYPE_NULL);
        etPrice = (EditText) rootView.findViewById(R.id.editTextPrice);

        etStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(rootView.getContext(),
                        new TimePickerDialog.OnTimeSetListener() {
                            public void onTimeSet(TimePicker view, int hour, int minute) {
                                etStartTime.setText(new StringBuilder()
                                        .append((hour < 10) ? "0" + hour : hour).append(":")
                                        .append((minute < 10) ? "0" + minute : minute));
                            }
                        }, 0, 0, false);

                timePickerDialog.show();
            }
        });

        etEndTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(rootView.getContext(),
                        new TimePickerDialog.OnTimeSetListener() {
                            public void onTimeSet(TimePicker view, int hour, int minute) {
                                etEndTime.setText(new StringBuilder()
                                        .append((hour < 10) ? "0" + hour : hour).append(":")
                                        .append((minute < 10) ? "0" + minute : minute));
                            }
                        }, 0, 0, false);
                timePickerDialog.show();
            }
        });

        btnFree = (Button) rootView.findViewById(R.id.btnFree);
        btnFree.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                mIsClickOnFree = true;
                setFreeRestriction();
            }
        });

        btnAddRestrictioins = (Button) rootView.findViewById(R.id.btnAddRestriction);
        btnAddRestrictioins.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                addRestrictionItem();
            }
        });

        btnSubmit = (Button) rootView.findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                uploadParkInfo();
            }
        });
    }

    public void initPictures() {
        Button btnPicStart = (Button) rootView.findViewById(R.id.btnStartPic);
        btnPicStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                picID = ID_PIC_START;
                dispatchTakePictureIntent();
            }
        });

        Button btnPicEnd = (Button) rootView.findViewById(R.id.btnEndPic);
        btnPicEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                picID = ID_PIC_END;
                dispatchTakePictureIntent();
            }
        });

        mImageViewStart = (ImageView) rootView.findViewById(R.id.imageViewStart);
        mImageViewStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View paramView) {
                enlargePic();
            }
        });

        mImageViewEnd = (ImageView) rootView.findViewById(R.id.imageViewEnd);
        mImageViewEnd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View paramView) {
                enlargePic();
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
    public void onMapReady(final GoogleMap map) {
        Log.d(TAG, "GoogleMap is ready.");
        mMap = map;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                mLastLocation.setLatitude(point.latitude);
                mLastLocation.setLongitude(point.longitude);
                mIsClickOnMap = true;
                getCurrentAddress();
            }
        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                //marker.hideInfoWindow();
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if (marker.equals(mMarkerStart)) {
                    btnID = ID_POS_START;
                } else {
                    btnID = ID_POS_END;
                }

                mLastLocation.setLatitude(marker.getPosition().latitude);
                mLastLocation.setLongitude(marker.getPosition().longitude);
                mIsClickOnMap = true;
                getCurrentAddress();
            }

            @Override
            public void onMarkerDrag(Marker marker) {
            }

        });

        Button btnSat = (Button) rootView.findViewById(R.id.satView);
        btnSat.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            }
        });

        Button btnMap = (Button) rootView.findViewById(R.id.mapView);
        btnMap.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        });
    }

    // Builds a GoogleApiClient. Uses {@code #addApi} to request the LocationServices API.
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(rootView.getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    // Runs when a GoogleApiClient object successfully connects.
    @Override
    public void onConnected(Bundle connectionHint) {
        // Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            if (!Geocoder.isPresent()) {
                Toast.makeText(rootView.getContext(), R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
                return;
            }
            startIntentService();
        }
    }

    // Start the intent service for fetching an address.
    protected void startIntentService() {
        Intent intent = new Intent(this.getActivity(), FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
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

    protected void displayAddressOutput() {
        mMap.clear();
        LatLng pos = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

        if (!mIsClickOnMap) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(19));
        }

        DecimalFormat df = new DecimalFormat("#.0000");
        if (btnID == ID_POS_START) {
            tvStartAddress.setText(mAddressOutput);
            mStartPos = pos;

            if (mEndPos != null) {
                mMarkerEnd = mMap.addMarker(new MarkerOptions()
                        .snippet("Lat(" + df.format(mEndPos.latitude) + ")" +
                                ", Lng(" + df.format(mEndPos.longitude) + ")")
                        .position(mEndPos).draggable(true).title("End Point"));
                mMarkerEnd.showInfoWindow();
            }

            mMarkerStart = mMap.addMarker(new MarkerOptions()
                    .snippet("Lat(" + df.format(pos.latitude) + ")" +
                            ", Lng(" + df.format(pos.longitude) + ")")
                    .position(pos).draggable(true).title("Start Point"));
            mMarkerStart.showInfoWindow();
        } else {
            tvEndAddress.setText(mAddressOutput);
            mEndPos = pos;

            mMarkerStart = mMap.addMarker(new MarkerOptions()
                    .snippet("Lat(" + df.format(mStartPos.latitude) + ")" +
                            ", Lng(" + df.format(mStartPos.longitude) + ")")
                    .position(mStartPos).draggable(true).title("Start Point"));
            mMarkerStart.showInfoWindow();

            mMarkerEnd = mMap.addMarker(new MarkerOptions()
                    .snippet("Lat(" + df.format(pos.latitude) + ")" +
                            ", Lng(" + df.format(pos.longitude) + ")")
                    .position(pos).draggable(true).title("End Point"));
            mMarkerEnd.showInfoWindow();
        }

        if ((mStartPos != null) && (mEndPos != null))
            drawLine(mStartPos, mEndPos);
    }

    private void drawLine(LatLng pos1, LatLng pos2) {
        PolylineOptions polylineOpt = new PolylineOptions();
        polylineOpt.add(pos1);
        polylineOpt.add(pos2);
        polylineOpt.color(Color.RED);

        Polyline polyline = mMap.addPolyline(polylineOpt);
        polyline.setWidth(5);
    }

    private void updateUIWidgets() {
        if (mAddressRequested) {
            if (btnID == ID_POS_START)
                btnPosStart.setEnabled(false);
            else
                btnPosEnd.setEnabled(false);
        } else {
            btnPosStart.setEnabled(true);
            btnPosEnd.setEnabled(true);
        }
    }

    protected void showToast(String text) {
        Toast.makeText(rootView.getContext(), text, Toast.LENGTH_SHORT).show();
    }

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            displayAddressOutput();
            if (resultCode == Constants.SUCCESS_RESULT) {
                showToast(getString(R.string.address_found));
            }
            mAddressRequested = false;
            updateUIWidgets();
        }
    }

    public void setFreeRestriction() {
        spinnerResType.setSelection(0);
        spinnerStartDay.setSelection(0);
        spinnerEndDay.setSelection(6);
        etStartTime.setText("00:00");
        etEndTime.setText("00:00");
        etPrice.setText("0.00");
    }

    public String getRestrictionItem() {
        mSpaceType = spinnerSpaceType.getSelectedItem().toString();
        mResType = spinnerResType.getSelectedItem().toString();
        mResDur = spinnerDur.getSelectedItem().toString();
        mStartDay = spinnerStartDay.getSelectedItem().toString();
        mEndDay = spinnerEndDay.getSelectedItem().toString();

        mStartTime = etStartTime.getText().toString();
        if (mStartTime.equals(""))
            mStartTime = "00:00";

        mEndTime = etEndTime.getText().toString();
        if (mEndTime.equals(""))
            mEndTime = "00:00";

        mPrice = etPrice.getText().toString();
        if (mPrice.equals(""))
            mPrice = "0.00";

        return mResType + ", " + mResDur + ", " + mStartDay + "-" + mEndDay + ", " +
                    mStartTime + "-" + mEndTime;
    }

    public String calFreeDuration(String type) {
        String freeDur;
        int pos1 = spinnerStartDay.getSelectedItemPosition();
        int pos2 = spinnerEndDay.getSelectedItemPosition();
        int newPos1 = ((pos1 == 0) ? (Day.length - 1) : (pos1 - 1));
        int newPos2 = ((pos2 == Day.length - 1) ? (0) : (pos2 + 1));

        if (mStartTime.equals("00:00") && mEndTime.equals("00:00")) {
            freeDur = Day[newPos2] + "-" + Day[newPos1] + ", " + "00:00-00:00";
        } else {
            String[] time1 = mStartTime.split(":");
            String[] time2 = mEndTime.split(":");
            String newHour1, newMin1;
            String newHour2, newMin2;

            if (time1[1].equals("00")) {
                newMin1 = "59";
                if (time1[0].equals("00")) {
                    newHour1 = "23";
                } else {
                    int hour = Integer.parseInt(time1[0]) - 1;
                    newHour1 = (hour < 10 ? "0" + hour : hour) + "";
                }
            } else {
                int min = Integer.parseInt(time1[1]) - 1;
                newMin1 = (min < 10 ? "0" + min : min) + "";
                newHour1 = time1[0];
            }

            if (time2[1].equals("59")) {
                newMin2 = "00";
                if (time2[0].equals("23")) {
                    newHour2 = "00";
                } else {
                    int hour = Integer.parseInt(time2[0]) + 1;
                    newHour2 = (hour < 10 ? "0" + hour : hour) + "";
                }
            } else {
                int min = Integer.parseInt(time2[1]) + 1;
                newMin2 = (min < 10 ? "0" + min : min) + "";
                newHour2 = time2[0];
            }

            freeDur = Day[pos1] + "-" + Day[pos2] + ", " + newHour2 + ":" + newMin2 + "-" + newHour1 + ":" + newMin1 +
                    "\n" + Day[newPos2] + "-" + Day[newPos1] + ", " + "00:00-00:00";
        }

        return freeDur;
    }

    public void addRestrictionItem() {
        String resDur = getRestrictionItem();
        String freeDur = calFreeDuration(mResType);

        if (mIsClickOnFree) {
            mIsClickOnFree = false;
            resDur = "None";
            freeDur = "Monday-Sunday, 00:00-00.00";
        }

        LayoutInflater inflater = LayoutInflater.from(rootView.getContext());
        View uploadDialog = inflater.inflate(R.layout.dialog_upload, null);

        /*
        info = (TextView) uploadDialog.findViewById(R.id.textViewStartPointPos);
        info.setText("Start Pos: " + "(" + mLastLocation.getLatitude() + ", " +
                mLastLocation.getLongitude() + ")");
        info = (TextView) uploadDialog.findViewById(R.id.textViewStartPointAddress);
        info.setText("Start Address: ");

        info = (TextView) uploadDialog.findViewById(R.id.textViewEndPointPos);
        info.setText("End Pos: " + "(" + mLastLocation.getLatitude() + ", " +
                mLastLocation.getLongitude() + ")");
        info = (TextView) uploadDialog.findViewById(R.id.textViewEndPointAddress);
        info.setText("Start Address: ");
        */

        TextView info = (TextView) uploadDialog.findViewById(R.id.textViewResInfo);
        info.setText(Html.fromHtml("<b>Restriction: </b>"));
        info.append("\n" + resDur);

        info = (TextView) uploadDialog.findViewById(R.id.textViewFreeInfo);
        info.setText(Html.fromHtml("<b>Free Duration: </b>"));
        info.append("\n" + freeDur);

        info = (TextView) uploadDialog.findViewById(R.id.textViewPrice);
        info.setText(Html.fromHtml("<b>Price: </b>" + mPrice + " AUD/Hour"));

        final AlertDialog dialog = new AlertDialog.Builder(rootView.getContext()).create();
        dialog.setTitle("Add Restriction");
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.setView(uploadDialog);
        dialog.show();
    }

    public void uploadParkInfo() {
        final AlertDialog dialog = new AlertDialog.Builder(rootView.getContext()).create();
        dialog.setTitle("UPLOAD PARK INFO");
        dialog.setMessage("Upload Line ID: " + lineID);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                lineID++;
                dialog.dismiss();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(this.getActivity().getPackageManager()) != null) {
            File photoFile;
            try {
                photoFile = createImageFile();
                mCurrentPhotoPath = photoFile.getAbsolutePath();
            } catch (IOException ex) {
                ex.printStackTrace();
                photoFile = null;
                mCurrentPhotoPath = null;
            }
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File albumF = getAlbumDir();
        File image = File.createTempFile(imageFileName, ".jpg", albumF);
        return image;
    }

    private File getAlbumDir() {
        File storageDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            storageDir = new File (Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), ALBUM_NAME);
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

    private void handleCameraPhoto() {
        if (mCurrentPhotoPath != null) {
            setPic();
            galleryAddPic();
        }
    }

    private void setPic() {
        int targetW = imageTargetW;
        int targetH = imageTargetH;

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        if (picID == ID_PIC_START) {
            mImageViewStart.setImageBitmap(bitmap);
            mImageViewStart.setVisibility(View.VISIBLE);
        } else {
            mImageViewEnd.setImageBitmap(bitmap);
            mImageViewEnd.setVisibility(View.VISIBLE);
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.getActivity().sendBroadcast(mediaScanIntent);
    }

    private void enlargePic() {
        LayoutInflater inflater = LayoutInflater.from(rootView.getContext());
        View imgEntryView = inflater.inflate(R.layout.dialog_photo_entry, null);

        // Decode the image file into a Bitmap sized to fill the View
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = 1;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        ImageView imgLarge = (ImageView) imgEntryView.findViewById(R.id.largeImage);
        imgLarge.setImageBitmap(bitmap);
        imgLarge.setVisibility(View.VISIBLE);

        final AlertDialog dialog = new AlertDialog.Builder(rootView.getContext()).create();
        dialog.setView(imgEntryView);
        dialog.show();
        imgEntryView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View paramView) {
                dialog.cancel();
            }
        });
    }
}