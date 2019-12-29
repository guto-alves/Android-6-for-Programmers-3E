package com.gutotech.weatherviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private LinearLayout weatherLinearLayout;
    private ImageView conditionImageView;
    private TextView dayTextView;
    private TextView tempTextView;
    private TextView lowTextView;
    private TextView highTextView;
    private TextView humidityTextView;

    private Map<String, Bitmap> bitmaps = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextInputEditText locationEditText = findViewById(R.id.locationEditText);

                String city = locationEditText.getText().toString().trim();

                if (!city.isEmpty()) {
                    URL url = createURL(city);

                    if (url != null) {
                        dismissKeyboard(locationEditText);
                        GetWeatherTask getLocalWeatherTask = new GetWeatherTask();
                        getLocalWeatherTask.execute(url);
                    } else
                        Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.invalid_url, Snackbar.LENGTH_LONG).show();
                }
            }
        });

        weatherLinearLayout = findViewById(R.id.weatherLinearLayout);
        conditionImageView = findViewById(R.id.conditionImageView);
        dayTextView = findViewById(R.id.dayTextView);
        tempTextView = findViewById(R.id.tempTextView);
        lowTextView = findViewById(R.id.lowTextView);
        highTextView = findViewById(R.id.highTextView);
        humidityTextView = findViewById(R.id.humidityTextView);

        weatherLinearLayout.setVisibility(View.GONE);
    }

    private void dismissKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private URL createURL(String city) {
        String apiKey = getString(R.string.api_key);
        String baseUrl = getString(R.string.web_service_url);

        try {
            if (city.contains(","))
                city = city.replaceAll(", ", ",");

            // create URL for specified city and imperial units (Fahrenheit)
            String urlString = baseUrl + URLEncoder.encode(city, "UTF-8") + "&units=imperial&appid=" + apiKey;
            return new URL(urlString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private class GetWeatherTask extends AsyncTask<URL, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(URL... urls) {
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection) urls[0].openConnection();
                int response = connection.getResponseCode();

                if (response == HttpURLConnection.HTTP_OK) {
                    StringBuilder builder = new StringBuilder();

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;

                        while ((line = reader.readLine()) != null)
                            builder.append(line);

                    } catch (IOException e) {
                        Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.read_error, Snackbar.LENGTH_LONG).show();
                        e.printStackTrace();
                    }

                    return new JSONObject(builder.toString());
                } else {
                    Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.connect_error, Snackbar.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.connect_error, Snackbar.LENGTH_LONG).show();
                e.printStackTrace();
            } finally {
                connection.disconnect();
            }

            return null;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            if (jsonObject != null)
                displayWeather(convertJSONtoWeather(jsonObject));
        }
    }

    private Weather convertJSONtoWeather(JSONObject currentWeather) {
        try {
            JSONObject weather = currentWeather.getJSONArray("weather").getJSONObject(0);

            JSONObject temperatures = currentWeather.getJSONObject("main");

            return new Weather(
                    currentWeather.getLong("dt"), // date/time timestamp
                    temperatures.getDouble("temp"),
                    temperatures.getDouble("temp_min"), // minimum temperature
                    temperatures.getDouble("temp_max"), // maximum temperature
                    temperatures.getDouble("humidity"), // percent humidity
                    weather.getString("description"), // weather conditions
                    weather.getString("icon")); // icon name
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void displayWeather(Weather weather) {
        if (bitmaps.containsKey(weather.iconURL))
            conditionImageView.setImageBitmap(bitmaps.get(weather.iconURL));
        else
            new LoadImageTask(conditionImageView).execute(weather.iconURL);

        dayTextView.setText(getString(R.string.day_description, weather.dayOfWeek, weather.description));
        tempTextView.setText(weather.temp);
        lowTextView.setText(getString(R.string.low_temp, weather.minTemp));
        highTextView.setText(getString(R.string.high_temp, weather.maxTemp));
        humidityTextView.setText(getString(R.string.humidity, weather.humidity));

        weatherLinearLayout.setVisibility(View.VISIBLE);
    }

    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imageView;

        public LoadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;

            try {
                URL url = new URL(strings[0]);

                connection = (HttpURLConnection) url.openConnection();

                try (InputStream inputStream = connection.getInputStream()) {
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    bitmaps.put(strings[0], bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                connection.disconnect();
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            imageView.setImageBitmap(bitmap);
        }
    }
}
