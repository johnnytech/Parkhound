package com.parkhound.spuploader;


import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class UploadDataTask extends AsyncTask<URL, Integer, String> {
    private Map<String, String> mParams;
    private Map<String, File> mFiles;

    UploadDataTask(Map<String, String> params, Map<String, File> files) {
        mParams = params;
        mFiles = files;
    }

    @Override
    protected String doInBackground(URL... urls) {
        // params comes from the execute() call: params[0] is the url.
        try {
            post(urls[0], mParams, mFiles);
        } catch (IOException e) {
            return "Unable to retrieve web page. URL may be invalid.";
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        //setProgressPercent(progress[0]);
    }

    // onPostExecute displays the results of the AsyncTask.
    /*@Override
    protected void onPostExecute(String result) {
        //textView.setText(result);
    }*/

    public static String post(URL url, Map<String, String> params, Map<String, File> files) throws IOException {

        String BOUNDARY = java.util.UUID.randomUUID().toString();
        String PREFIX = "--", LINEND = "\r\n";
        String MULTIPART_FROM_DATA = "multipart/form-data";
        String CHARSET = "UTF-8";

        URL uri = url;
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
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(PREFIX);
            sb.append(BOUNDARY);
            sb.append(LINEND);
            sb.append("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + LINEND);
            sb.append("Content-Type: text/plain; charset=" + CHARSET + LINEND);
            sb.append("Content-Transfer-Encoding: 8bit" + LINEND);
            sb.append(LINEND);
            sb.append(entry.getValue());
            sb.append(LINEND);
        }

        outStream.write(sb.toString().getBytes());
        // send pictures
        if (files != null)
            for (Map.Entry<String, File> file : files.entrySet()) {
                StringBuilder sb1 = new StringBuilder();
                sb1.append(PREFIX);
                sb1.append(BOUNDARY);
                sb1.append(LINEND);
                sb1.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getKey() + "\"" + LINEND);
                sb1.append("Content-Type: application/octet-stream; charset=" + CHARSET + LINEND);
                sb1.append(LINEND);
                outStream.write(sb1.toString().getBytes());

                InputStream is = new FileInputStream(file.getValue());
                byte[] buffer = new byte[1024];
                int len = 0;
                while ((len = is.read(buffer)) != -1) {
                    outStream.write(buffer, 0, len);
                }

                is.close();
                outStream.write(LINEND.getBytes());
            }

        // set end mark
        byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINEND).getBytes();
        outStream.write(end_data);
        outStream.flush();

        // get response
        int res = conn.getResponseCode();
        InputStream in = conn.getInputStream();
        if (res == 200) {
            int ch;
            StringBuilder sb2 = new StringBuilder();
            while ((ch = in.read()) != -1) {
                sb2.append((char) ch);
            }
        }

        outStream.close();
        conn.disconnect();
        return in.toString();
    }
}