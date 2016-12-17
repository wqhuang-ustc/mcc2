package com.mcc2016.ananasocr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.mcc2016.ananasocr.LoginActivity.username;
import static com.mcc2016.ananasocr.MainActivity.SERVER_ADDRESS;
import static com.mcc2016.ananasocr.MainActivity.SERVER_PORT;
import static com.mcc2016.ananasocr.MainActivity.readJsonData;

public class DetailedActivity extends AppCompatActivity {

  public ImageView mImage;
  public TextView mText;

  private String mRecognizedText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_detailed);

    mImage = (ImageView) findViewById(R.id.detailedImage);
    mText = (TextView) findViewById(R.id.detailedText);

    final Bundle extras = getIntent().getExtras();
    final String encodedThumbnailImg = extras.getString("img");
    final String text = extras.getString("text");
    mRecognizedText = text;

    String url = SERVER_ADDRESS + ":" + SERVER_PORT + "/details";
    final GetDetailsTask task = new GetDetailsTask(url, encodedThumbnailImg);
    task.execute();
  }

  public class GetDetailsTask extends AsyncTask<Void, Void, Bitmap> {

    private final String mUrl;
    private final String mEncodedImg;

    public GetDetailsTask(String url, String encoded) {
      this.mEncodedImg = encoded;
      this.mUrl = url;
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
      Bitmap res = null;
      try {
        URL url = new URL(mUrl);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.connect();

        JSONObject jsonParam = new JSONObject();
        jsonParam.put("imgThumbnailEncoded", mEncodedImg);
        jsonParam.put("username", username);

        final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
        outputStreamWriter.write(jsonParam.toString());
        outputStreamWriter.close();

        int httpResult = connection.getResponseCode();
        if (httpResult == HttpURLConnection.HTTP_OK) {
          String jsonData = readJsonData(connection);
          JSONObject obj = new JSONObject(jsonData);
          final String encodedBigImg = obj.getString("imgEncoded");
          res = decodeImage(encodedBigImg);
        }
      } catch (IOException | JSONException e) {
//        showProgress(false);
        e.printStackTrace();
      }

      return res;
    }

    private Bitmap decodeImage(String encodedImg) {
      byte[] decodedString = Base64.decode(encodedImg, Base64.DEFAULT);
      return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
//      showProgress(false);
      mImage.setImageBitmap(bitmap);
      mText.setText(mRecognizedText);
      Toast.makeText(getApplicationContext(), "Finished downloading detailed image",
              Toast.LENGTH_SHORT).show();
    }
  }

}
