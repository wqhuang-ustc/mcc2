package com.mcc2016.ananasocr;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.mcc2016.ananasocr.MainActivity.SERVER_ADDRESS;
import static com.mcc2016.ananasocr.MainActivity.SERVER_PORT;
import static com.mcc2016.ananasocr.MainActivity.readJsonData;
import static java.lang.Math.min;

public class ViewHistoryActivity extends AppCompatActivity {

  private ListView mViewHistoryList;
  private ArrayAdapter<ViewHistoryData> mViewHistoryAdapter;
  private List<ViewHistoryData> mItems = new ArrayList<>();

  public static int NUMBER_OF_SYMBOLS_LIMIT = 100;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_view_history);

    mViewHistoryList = ((ListView) findViewById(R.id.viewHistoryList));

    // Place for mViewHistoryAdapter
    mViewHistoryAdapter = new ViewHistoryArrayAdapter(getApplicationContext(),
            R.layout.activity_view_history_item, mItems);

    String url = SERVER_ADDRESS + ":" + SERVER_PORT + "/history/" + LoginActivity.username;
    final GetHistoryTask task = new GetHistoryTask(url);
    task.execute();

    mViewHistoryList.setAdapter(mViewHistoryAdapter);
    mViewHistoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ViewHistoryData item = (mViewHistoryAdapter.getItem(position));
        final Intent intent = new Intent(getApplicationContext(), DetailedActivity.class);
        if (item != null) {
          intent.putExtra("img", item.getEncodedImg()).putExtra("text", item.getText());
        }
        startActivity(intent);
      }
    });
  }

  private class ViewHistoryArrayAdapter extends ArrayAdapter<ViewHistoryData> {

    ViewHistoryArrayAdapter(Context context, int resource, List<ViewHistoryData> objects) {
      super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
      final LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
      ViewHistoryData item = getItem(position);

      View view = convertView;

      if (convertView == null) {
        view = inflater.inflate(R.layout.activity_view_history_item, parent, false);
      }

      final ImageView img = (ImageView) view.findViewById(R.id.viewHistoryImage);
      final TextView text = (TextView) view.findViewById(R.id.viewHistoryText);

      if (item != null) {
        img.setImageBitmap(item.getImage());

        boolean isLong = item.getText().length() > NUMBER_OF_SYMBOLS_LIMIT;
        String previewText;
        if (isLong) {
          previewText = item.getText().substring(0, NUMBER_OF_SYMBOLS_LIMIT) + "...";
        } else {
          previewText = item.getText();
        }
        text.setText(previewText);
      }

      return view;
    }
  }

  class ViewHistoryData {
    private Bitmap mImage;
    private String mText;
    private String mEncodedImg;

    public ViewHistoryData(Bitmap image, String text, String encoded) {
      mImage = image;
      mEncodedImg = encoded;
      mText = text;
    }

    Bitmap getImage() {
      return mImage;
    }

    String getText() {
      return mText;
    }

    public String getEncodedImg() {
      return mEncodedImg;
    }
  }

  public class GetHistoryTask extends AsyncTask<Void, Void, List<ViewHistoryData>> {

    private final String mUrl;

    public GetHistoryTask(String url) {
      this.mUrl = url;
    }

    @Override
    protected List<ViewHistoryData> doInBackground(Void... params) {
      List<ViewHistoryData> res = new ArrayList<>();
      try {
        URL url = new URL(mUrl);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();

        int httpResult = connection.getResponseCode();
        if (httpResult == HttpURLConnection.HTTP_OK) {
          String jsonData = readJsonData(connection);
          JSONObject obj = new JSONObject(jsonData);
          final JSONArray data = obj.getJSONArray("data");
          for (int i = 0; i < data.length(); ++i) {
            JSONObject object = (JSONObject) data.get(i);
            final String text = object.getString("recognizedText");
            final String encodedImg = object.getString("imgThumbnailEncoded");
            Bitmap img = decodeImage(encodedImg);
            res.add(new ViewHistoryData(img, text, encodedImg));
          }
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
    protected void onPostExecute(List<ViewHistoryData> l) {
//      showProgress(false);
      Toast.makeText(getApplicationContext(), "Finished downloading history",
              Toast.LENGTH_SHORT).show();

      mItems = l;
      mViewHistoryAdapter.addAll(mItems);
    }
  }
}
