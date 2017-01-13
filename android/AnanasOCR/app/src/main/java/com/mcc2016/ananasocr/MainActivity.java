package com.mcc2016.ananasocr;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_CERT_PASSWORD = "verysecurepassword";
    private static int CAMERA_PIC_REQUEST = 1;
    private static int REQUEST_CAMERA_AND_STORAGE_PERMISSIONS = 2;
    private static int SELECT_PIC_REQUEST = 3;

    private View mProgressView;
    private View mTextView;
    private LinearLayout mLinearLayout;
    private View[] mViewList;
    private OCRTask mOcrTask = null;
    private Uri mPhotoUri = null;
    private long mTimestamp;
    private long mTimestampGlobal;
    private double mTimeTakenLocal, mTimeTakenRemote;
    private long mTransferredBytes;
    private boolean mBenchmarkMode;
    private int mCount;
    private Double[] mLocalTimes, mRemoteTimes;

    // Change this address here and in backend/server.py
    // If you're using Linux -- run 'ifconfig' in cmd and check your ip address
    // If your're using Windows -- run 'ipconfig' in cmd and check your ip address
    public static final String SERVER_ADDRESS = "http://104.199.87.21";
    public static final String SERVER_PORT = "8080";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button mButtonPhoto = (Button) findViewById(R.id.buttonPhoto);
        mButtonPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mayTakeAndStorePhoto()) {
                    takePhoto();
                }
            }
        });

        Button mButtonSelectImage = (Button) findViewById(R.id.buttonSelectImage);
        mButtonSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if (mayTakePhoto()) {
                    selectPhoto();
                //}
            }
        });

        Button mButtonViewHistory = (Button) findViewById(R.id.buttonViewHistory);
        mButtonViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(getApplicationContext(), ViewHistoryActivity.class);
                startActivity(intent);
            }
        });

        mTextView = findViewById(R.id.textView);
        mProgressView = findViewById(R.id.progressBar);
        mLinearLayout = (LinearLayout) findViewById(R.id.itemList);
        showProgress(false);
    }

    private void takePhoto() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String photoFilename = timeStamp + ".jpg";

        File folder = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "AnanasOcr");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success) {
            String photoPath = folder.getAbsolutePath() + "/" + photoFilename;
            File file = new File(photoPath);
            mPhotoUri = Uri.fromFile(file);

            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
            startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
        } else {
            Toast.makeText(this, "Error creating directory for photos.", Toast.LENGTH_LONG).show();
        }
    }

    private void selectPhoto() {
        Intent selectIntent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        selectIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        selectIntent.setType("image/*");
        startActivityForResult(selectIntent, SELECT_PIC_REQUEST);
    }

    private boolean mayTakeAndStorePhoto() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(CAMERA) || shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
            requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_AND_STORAGE_PERMISSIONS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_AND_STORAGE_PERMISSIONS) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN) // This might be bad and need additional workarounds and checks if not, no time now
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // http://gaut.am/making-an-ocr-android-app-using-tesseract/
        // http://imperialsoup.com/2016/04/29/simple-ocr-android-app-using-tesseract-tutorial/
        Log.v("XXX", String.valueOf(1));
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap image = null;
        resetItemList();
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setVisibility(View.GONE);
        mBenchmarkMode = ((RadioButton) findViewById(R.id.radioButtonBenchmark)).isChecked();
        try {
            if(requestCode == CAMERA_PIC_REQUEST) {
                Log.v("XXX", String.valueOf(2));
                Log.v("XXX", String.valueOf(mPhotoUri.toString()));
                    Log.v("XXX", String.valueOf(3));
                    image = getBitmapFromUri(mPhotoUri, 800, 600);
                    if (null != image) {
                        Log.v("XXX", String.valueOf(4));
                        // Update the system that the image was captured
                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mPhotoUri);
                        sendBroadcast(intent);

                        mLocalTimes = new Double[1];
                        mRemoteTimes = new Double[1];

                        if (mBenchmarkMode) {
                            mViewList = new View[3];
                            mCount = 2;
                        } else {
                            Log.v("XXX", String.valueOf(3));
                            mViewList = new View[2];
                            mCount = 1;
                        }
                        Log.v("XXX", String.valueOf(5));
                        photoAcquired(image, 0);
                    }
            } else if (requestCode == SELECT_PIC_REQUEST && resultCode == RESULT_OK) {
                if (null != data) {
                    if (data.getClipData() != null) {
                        ClipData mClipData = data.getClipData();
                        int itemCount = mClipData.getItemCount();
                        mLocalTimes = new Double[itemCount];
                        mRemoteTimes = new Double[itemCount];
                        if (mBenchmarkMode) {
                            mViewList = new View[itemCount * 3];
                            mCount = itemCount * 2;
                        } else {
                            mViewList = new View[itemCount * 2];
                            mCount = itemCount;
                        }
                        for (int i = 0; i < itemCount; i++) {

                            ClipData.Item item = mClipData.getItemAt(i);
                            Uri uri = item.getUri();
                            image = getBitmapFromUri(uri, 800, 600);
                            if (null != image) {
                                photoAcquired(image, i);
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Picture Not taken", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void resetItemList() {
        mLinearLayout.removeAllViews();
    }

    // Credits: http://stackoverflow.com/a/33462547 Alexander Zaldostanov
    private Bitmap getBitmapFromUri(Uri uri, int width, int height) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / width, photoH / height);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;


        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor, null,bmOptions);
        parcelFileDescriptor.close();
        return image;
    }

    private void photoAcquired(Bitmap image, int index) {
        if (mBenchmarkMode) {
            ImageView imageViewTmp = new ImageView(this);
            imageViewTmp.setImageBitmap(image);
            mViewList[index * 3] = imageViewTmp;
            populateViewList();
        }else {
            ImageView imageViewTmp = new ImageView(this);
            imageViewTmp.setImageBitmap(image);
            mViewList[index * 2] = imageViewTmp;
            populateViewList();
        }

        mTimestamp = -1;
        mTimestampGlobal = -1;
        mTimeTakenLocal = -1;
        mTimeTakenRemote = -1;
        ((TextView) mTextView).setText("");

        RadioButton radioButtonLocal = (RadioButton) findViewById(R.id.radioButtonLocal);
        RadioButton radioButtonRemote = (RadioButton) findViewById(R.id.radioButtonRemote);
        RadioButton radioButtonBenchmark = (RadioButton) findViewById(R.id.radioButtonBenchmark);
        if (radioButtonLocal.isChecked()) {
            processLocal(image, index * 2 + 1);
        } else if (radioButtonRemote.isChecked()) {
            processRemote(image, index * 2 + 1);
        } else if (radioButtonBenchmark.isChecked()) {
            benchmark(image, index * 3 + 1);
        }
    }

    private void processLocal(Bitmap image, int index) {
        showProgress(true);
        mOcrTask = new OCRTask(image, index);
        mOcrTask.execute((Void) null);
    }

    private void processRemote(Bitmap image, int index) {
        String address = SERVER_ADDRESS + ":" + SERVER_PORT + "/recognize_text";
        final RemoteOCRTask task = new RemoteOCRTask(address, image, index);
        showProgress(true);
        task.execute();
    }

    private void benchmark(Bitmap image, int index) {
        mTimestamp = new Date().getTime();
        mTimestampGlobal = new Date().getTime();
        mTransferredBytes =TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes() ;
        processLocal(image, index);
        processRemote(image, index + 1);
    }

    private void postBenchmark(boolean local) {
        if (mTimestamp != -1) {
            double timeTaken = (new Date().getTime() - mTimestamp) / 1000.0;
            if (local) {
                mTimeTakenLocal = timeTaken;
            } else {
                mTimeTakenRemote = timeTaken;
            }
            mTimestamp = new Date().getTime();
            if (mTimeTakenLocal < 0 || mTimeTakenRemote < 0) {
                // Continue to show progress if both are not finished yet and show result for the one that is ready
                showProgress(true);
            }
        }
    }


    private String localOCR(Bitmap image) {
        TessBaseAPI baseApi = new TessBaseAPI();
        Matrix mtx = new Matrix();
        mtx.preRotate(0);
        Bitmap tessBitmap = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), mtx, false);
        tessBitmap = tessBitmap.copy(Bitmap.Config.ARGB_8888, true);

        String datapath = getFilesDir()+ "/tesseract/";
        String filepath = datapath + "/tessdata/eng.traineddata";

        AssetManager assetManager = getAssets();
        try {
            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            File datafile = new File(filepath);
            File datafolder = new File(datapath + "tessdata/");
            if (!datafolder.exists())
                datafolder.mkdirs();
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        baseApi.init(datapath, "eng");
        baseApi.setImage(tessBitmap);

        String recognizedText = baseApi.getUTF8Text();
        baseApi.end();
        return recognizedText;
    }

    public class RemoteOCRTask extends AsyncTask<Void, Void, String> {

        private final String mUrl;
        private final Bitmap mImg;
        private final int mIndex;

        public RemoteOCRTask(String url, Bitmap img, int index) {
            this.mUrl = url;
            this.mImg = img;
            this.mIndex = index;
        }

        private String encodeImage() {
            final int IMAGE_QUALITY = 100;

            String encodedImg;
            ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
            mImg.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, bitmapStream);
            byte[] b = bitmapStream.toByteArray();
            encodedImg = Base64.encodeToString(b, Base64.DEFAULT);
            return encodedImg;
        }

        @Override
        protected String doInBackground(Void... params) {
            String recognizedString = "";
            try {
                URL url = new URL(mUrl);

                /*
                // Self signed SSL certificate code
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                FileInputStream fis = new FileInputStream("certificateFile");
                keyStore.load(fis, CLIENT_CERT_PASSWORD.toCharArray());

                KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
                kmf.init(keyStore, CLIENT_CERT_PASSWORD.toCharArray());
                KeyManager[] keyManagers = kmf.getKeyManagers();
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagers, null, null);

                final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                connection.setSSLSocketFactory(sslContext.getSocketFactory());
                */

                final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.connect();

                int width = mImg.getWidth();
                int height = mImg.getHeight();

                JSONObject jsonParam = new JSONObject();
                String encodedImg = encodeImage();

                jsonParam.put("imgWidth", String.valueOf(width));
                jsonParam.put("imgHeight", String.valueOf(height));
                jsonParam.put("imgEncoded", encodedImg);
                jsonParam.put("username", LoginActivity.username);

                final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
                outputStreamWriter.write(jsonParam.toString());
                outputStreamWriter.close();

                int httpResult = connection.getResponseCode();
                if (httpResult == HttpURLConnection.HTTP_OK) {
                    String jsonData = readJsonData(connection);
                    JSONObject obj = new JSONObject(jsonData);
                    recognizedString = obj.getString("recognizedText");
                }
            } catch (IOException | JSONException e) {
                //showProgress(false);
                e.printStackTrace();
                /*
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
                */
            }

            return recognizedString;
        }

        @Override
        protected void onPostExecute(String s) {
            //showProgress(false);
            Toast.makeText(getApplicationContext(), "Finished processing text", Toast.LENGTH_SHORT).show();
            postBenchmark(false);
            setRecognizedText(s, mIndex, false);
        }
    }

    public static String readJsonData(HttpURLConnection connection) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder jsonData = new StringBuilder();
        String curStr;
        while ((curStr = reader.readLine()) != null) {
            jsonData.append(curStr);
            jsonData.append("\n");
        }

        return jsonData.toString();
    }

    /**
     * Shows the progress UI and hides the textView which will hold the result of the OCR
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mProgressView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class OCRTask extends AsyncTask<Void, Void, String> {

        private final Bitmap mImage;
        private final int mIndex;

        OCRTask(Bitmap image, int index) {
            mImage = image;
            mIndex = index;
        }

        @Override
        protected String doInBackground(Void... params) {
            return localOCR(mImage);
        }

        @Override
        protected void onPostExecute(final String recognizedText) {
            mOcrTask = null;
            //showProgress(false);

            postBenchmark(true);
            setRecognizedText(recognizedText, mIndex, true);
        }

        @Override
        protected void onCancelled() {
            mOcrTask = null;
            showProgress(false);
        }
    }

    private void setRecognizedText(String recognizedText, int index, boolean local) {
        String stats = "Number of images: " + mViewList.length / 3 + "\n";
        if (mTimeTakenLocal > 0) {
            stats += String.format("Local time: %.2f seconds\n", getSum(true));
        }
        if (mTimeTakenRemote > 0) {
            String unit = "B";
            double bytes = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes() - mTransferredBytes;
            if (bytes > 1000) {
                bytes /= 1000;
                unit = "kB";
            }
            if (bytes > 1000) {
                bytes /= 1000;
                unit = "MB";
            }
            stats += String.format("Remote time: %.2f seconds\nTransferred bytes: %.2f %s\n", getSum(false) + mTimeTakenRemote, bytes, unit);
        }

        TextView textViewTmp = new TextView(this);
        if (!mBenchmarkMode) {
            textViewTmp.setText("Text:\n" + recognizedText);
        } else {
            if (local) {
                mLocalTimes[(index - 1) / 3] = mTimeTakenLocal;
                textViewTmp.setText("Local, " + mTimeTakenLocal + " s, text:\n" + recognizedText);
            } else {
                mRemoteTimes[(index - 2) / 3] = mTimeTakenRemote;
                textViewTmp.setText("Remote, " + mTimeTakenRemote + " s, text:\n" + recognizedText);
            }
            double minLocal = getMinTime(true);
            double minRemote = getMinTime(false);
            double maxLocal = getMaxTime(true);
            double maxRemote = getMaxTime(false);
            double stdDevLocal = getStdDevTime(true);
            double stdDevRemote = getStdDevTime(false);
            stats += String.format("Local min/max/std: %.2f/%.2f/%.2f\n", minLocal, maxLocal, stdDevLocal);
            stats += String.format("Remote min/max/std: %.2f/%.2f/%.2f\n", minRemote, maxRemote, stdDevRemote);
            TextView textView = (TextView) findViewById(R.id.textView);
            textView.setText(stats);
            textView.setVisibility(View.VISIBLE);
        }
        mViewList[index] = textViewTmp;
        populateViewList();
        if (decrementCount() <= 0)
            showProgress(false);

        saveTextFile(recognizedText, local);
    }

    private void populateViewList() {
        resetItemList();
        for (int i = 0; i < mViewList.length; i++) {
            View view = mViewList[i];
            if (null != view) {
                mLinearLayout.addView(view);
            }
        }
    }

    private synchronized int decrementCount() {
        return --mCount;
    }

    private void saveTextFile(String text, boolean local) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String textFilename = timeStamp;
        if (local) {
            textFilename += "_local.txt";
        } else {
            textFilename += "_remote.txt";
        }

        File folder = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "AnanasOcr");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success) {
            String filePath = folder.getAbsolutePath() + "/" + textFilename;
            File file = new File(filePath);
            try {
                FileOutputStream fOut = new FileOutputStream(file);
                OutputStreamWriter outWriter = new OutputStreamWriter(fOut);
                outWriter.append(text);
                outWriter.close();
                fOut.close();
                System.out.println("Text saved to file: " + filePath);
                // Update the system that the text file was saved
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file));
                sendBroadcast(intent);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private double getSum(boolean local) {
        Double[] tmp;
        Double sum = null;
        if (local)
            tmp = mLocalTimes;
        else
            tmp = mRemoteTimes;

        Double d;
        for (int i = 0; i < tmp.length; i++) {
            d = tmp[i];
            if (null != d) {
                if (sum == null)
                    sum = d;
                else
                    sum += d;
            }
        }
        return nullCheckDouble(sum);
    }

    private double getMinTime(boolean local) {
        Double[] tmp;
        Double min = null;
        if (local)
            tmp = mLocalTimes;
        else
            tmp = mRemoteTimes;

        Double d;
        for (int i = 0; i < tmp.length; i++) {
            d = tmp[i];
            if (null != d) {
                if (min == null)
                    min = d;
                else if (d < min)
                    min = d;
            }
        }
        return nullCheckDouble(min);
    }

    private double getMaxTime(boolean local) {
        Double[] tmp;
        Double max = null;
        if (local)
            tmp = mLocalTimes;
        else
            tmp = mRemoteTimes;

        Double d;
        for (int i = 0; i < tmp.length; i++) {
            d = tmp[i];
            if (null != d) {
                if (max == null)
                    max = d;
                else if (d > max)
                    max = d;
            }
        }
        return nullCheckDouble(max);
    }

    private Double nullCheckDouble(Double d) {
        Double b = null;
        return (d==b) ? 0 : d; // Smileys are fun at 01 in the night xD
    }

    private double getMeanTime(boolean local) {
        Double[] tmp;
        Double sum = 0.0;
        if (local)
            tmp = mLocalTimes;
        else
            tmp = mRemoteTimes;

        Double d;
        int count = 0;
        for (int i = 0; i < tmp.length; i++) {
            d = tmp[i];
            if (null != d) {
                sum += d;
                count++;
            }
        }
        if (count > 0)
            return sum/count;
        else
            return 0.0;
    }

    private double getVarianceTime(boolean local) {
        Double[] tmp;
        double mean = getMeanTime(local);
        Double counter = 0.0;
        if (local)
            tmp = mLocalTimes;
        else
            tmp = mRemoteTimes;

        Double d;
        int count = 0;
        for (int i = 0; i < tmp.length; i++) {
            d = tmp[i];
            if (null != d) {
                counter += (d - mean) * (d - mean);
                count++;
            }
        }
        if (count > 0)
            return counter/count;
        else
            return 0.0;
    }

    private double getStdDevTime(boolean local) {
        return Math.sqrt(getVarianceTime(local));
    }
}
