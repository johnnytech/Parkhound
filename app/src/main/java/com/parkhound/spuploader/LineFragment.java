package com.parkhound.spuploader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class LineFragment extends Fragment implements OnMapReadyCallback {
    static final String LineFragment = "PSUploaderLineFragment";
    static final LatLng melbourne = new LatLng(-37.815, 144.966);
    private SupportMapFragment fragment;
    private static View rootView;
    private GoogleMap mMap;

    private ImageView imgLarge;
    private ImageView mImageViewStart;
    private ImageView mImageViewEnd;
    private final int imageTargetW = 100;
    private final int imageTargetH = 100;

    // Photo album for this application
    private static final String albumName = "CameraSample";
    private File mAlbumDir = null;
    private String mCurrentPhotoPath;

    private Button btnSat;
    private Button btnMap;
    private Button btnPosStart;
    private Button btnPosEnd;
    private Button btnPicStart;
    private Button btnPicEnd;
    static final int REQUEST_TAKE_PHOTO = 1;


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

        return rootView;
    }

    @Override
    public void onMapReady(final GoogleMap map) {
        Log.e(LineFragment, "GoogleMap is ready.");
        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(melbourne, 19));

        map.addMarker(new MarkerOptions()
                .title("Melbourne")
                .snippet("The most beautiful city in Australia.")
                .position(melbourne));

        mMap = map;

        btnSat = (Button) rootView.findViewById(R.id.satView);
        btnSat.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v){
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            }
        });

        btnMap = (Button) rootView.findViewById(R.id.mapView);
        btnMap.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v){
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mImageViewStart = (ImageView) this.getActivity().findViewById(R.id.imageViewStart);
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

                Log.e(LineFragment, "Decode the JPEG file into a Bitmap: file="+mCurrentPhotoPath);
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

        btnPicStart = (Button) this.getActivity().findViewById(R.id.btnStartPic);
        btnPicStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(LineFragment, "Click btnPicStart to take a pic.");
                dispatchTakePictureIntent();
            }
        });

        mImageViewEnd = (ImageView) this.getActivity().findViewById(R.id.imageViewEnd);
        btnPicEnd = (Button) this.getActivity().findViewById(R.id.btnEndPic);
        btnPicEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(LineFragment, "Click btnPicEnd to take a pic.");
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

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            storageDir = new File (Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), albumName);

            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        Log.e(LineFragment, "failed to create directory");
                        return null;
                    }
                }
            }
        } else {
            Log.e(LineFragment, "External storage is not mounted READ/WRITE.");
        }

        Log.e(LineFragment, "getAlbumDir():"+storageDir);
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
        Log.e(LineFragment, "Enter dispatchTakePictureIntent()");

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(this.getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
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
                Log.e(LineFragment, "photoFile was successfully created.");
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void handleCameraPhoto() {
        Log.e(LineFragment, "Enter handleCameraPhoto()");
        if (mCurrentPhotoPath != null) {
            setPic();
            galleryAddPic();
            //mCurrentPhotoPath = null;
        }
    }

    private void galleryAddPic() {
        Log.e(LineFragment, "Enter galleryAddPic()");

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.getActivity().sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        Log.e(LineFragment, "Enter setPic()");

        // Get the dimensions of the View
        //int targetW = mImageViewStart.getWidth();
        int targetW = imageTargetW;
        //int targetH = mImageViewStart.getHeight();
        int targetH = imageTargetH;
        Log.e(LineFragment, "imageViewSize: width="+targetW+"/height="+targetH);

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        Log.e(LineFragment, "PicSize: width="+photoW+"/height="+photoH);

        /* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        }
        Log.e(LineFragment, "scale down the image. scaleFactor="+scaleFactor);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Log.e(LineFragment, "Decode the JPEG file into a Bitmap: file="+mCurrentPhotoPath);
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageViewStart.setImageBitmap(bitmap);
        mImageViewStart.setVisibility(View.VISIBLE);
    }
}
