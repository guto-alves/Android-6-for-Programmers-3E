package com.gutotech.weatherviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.MyViewHolder> {
    private Map<String, Bitmap> bitmaps = new HashMap<>();

    private Context context;
    private List<Weather> forecast;

    public WeatherAdapter(Context context, List<Weather> forecast) {
        this.context = context;
        this.forecast = forecast;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Weather day = forecast.get(position);

        if (bitmaps.containsKey(day.iconURL))
            holder.conditionImageView.setImageBitmap(bitmaps.get(day.iconURL));
        else
            new LoadImageTask(holder.conditionImageView).execute(day.iconURL);


        holder.dayTextView.setText(context.getString(R.string.day_description, day.dayOfWeek, day.description));
        holder.lowTextView.setText(context.getString(R.string.low_temp, day.minTemp));
        holder.highTextView.setText(context.getString(R.string.high_temp, day.maxTemp));
        holder.humidityTextView.setText(context.getString(R.string.humidity, day.humidity));
    }

    @Override
    public int getItemCount() {
        return forecast.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView conditionImageView;
        private TextView dayTextView;
        private TextView lowTextView;
        private TextView highTextView;
        private TextView humidityTextView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            conditionImageView = itemView.findViewById(R.id.conditionImageView);
            dayTextView = itemView.findViewById(R.id.dayTextView);
            lowTextView = itemView.findViewById(R.id.lowTextView);
            highTextView = itemView.findViewById(R.id.highTextView);
            humidityTextView = itemView.findViewById(R.id.humidityTextView);
        }
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
