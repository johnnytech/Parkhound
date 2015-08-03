package com.parkhound.spuploader;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class UploadDataTask extends AsyncTask<URL, Integer, String> {
    private static final String TAG = "parking street";

    private JSONObject mParams;
    private Map<String, File> mFiles;
    private Context mContext;
    private ProgressDialog mDialog;

    UploadDataTask(JSONObject params, Map<String, File> files, Context context) {
        mParams = params;
        mFiles = files;
        mContext = context;
    }

    @Override
    protected void onPreExecute() {
        mDialog = new ProgressDialog(mContext);
        mDialog.setMessage("Uploading Parking Data...");
        mDialog.setIndeterminate(false);
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setProgress(0);
        mDialog.show();
    }

    @Override
    protected String doInBackground(URL... urls) {
        // params comes from the execute() call: params[0] is the url.
        Log.d(TAG, "doInBackground(), URL=" + urls[0].toString());
        try {
            String BOUNDARY = java.util.UUID.randomUUID().toString();
            String PREFIX = "--", LINEND = "\r\n";
            String MULTIPART_FROM_DATA = "multipart/form-data";
            String CHARSET = "UTF-8";

            URL uri = urls[0];
            HttpURLConnection conn = (HttpURLConnection) uri.openConnection();
            conn.setChunkedStreamingMode(0);
            conn.setReadTimeout(5 * 1000);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("Charsert", "UTF-8");
            conn.setRequestProperty("Content-Type", MULTIPART_FROM_DATA + ";boundary=" + BOUNDARY);

            conn.setDoOutput(true);

            OutputStream stream = conn.getOutputStream();
            DataOutputStream outStream = new DataOutputStream(stream);

            // construct the txt part
            Log.d(TAG, "construct the txt part, mParams=" + mParams.toString());
            StringBuilder sb = new StringBuilder();
            sb.append(PREFIX);
            sb.append(BOUNDARY);
            sb.append(mParams.toString());
            sb.append(LINEND);
            outStream.write(sb.toString().getBytes());

            // send pictures
            /*
            Log.d(TAG, "sending pictures...");
            if (mFiles != null) {
                int fileNum = mFiles.size();
                for (Map.Entry<String, File> file : mFiles.entrySet()) {
                    StringBuilder sb1 = new StringBuilder();
                    sb1.append(PREFIX);
                    sb1.append(BOUNDARY);
                    sb1.append(LINEND);
                    sb1.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getKey() + "\"" + LINEND);
                    sb1.append("Content-Type: application/octet-stream; charset=" + CHARSET + LINEND);
                    sb1.append(LINEND);
                    outStream.write(sb1.toString().getBytes());

                    Log.d(TAG, "File Key=" + file.getKey() + ", File Path=" +file.getValue().getPath());
                    InputStream is = new FileInputStream(file.getValue());
                    byte[] bytes = new byte[(int)file.getValue().length()];
                    is.read(bytes);
                    is.close();

                    int bufferLen = 1024;
                    for (int i = 0; i < bytes.length; i += bufferLen) {
                        int progress = (int) ((i / (float)bytes.length) * 100);
                        publishProgress(progress);

                        if (bytes.length - i >= bufferLen)
                            outStream.write(bytes, i, bufferLen);
                        else
                            outStream.write(bytes, i, bytes.length - i);
                    }
                    publishProgress(100);

                    is.close();
                    outStream.write(LINEND.getBytes());
                }
            }*/

            // set end mark
            byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINEND).getBytes();
            outStream.write(end_data);
            outStream.flush();
            outStream.close();

            // get response
            int res = conn.getResponseCode();
            InputStream in = conn.getInputStream();
            Log.d(TAG, "Get responseCode=" + res + ", responseMessage:" + in.toString());

            conn.disconnect();
            return in.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            return "Unable to upload data. URL may be invalid.";
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        mDialog.setProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        Log.d(TAG, result);
        try {
            mDialog.dismiss();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}