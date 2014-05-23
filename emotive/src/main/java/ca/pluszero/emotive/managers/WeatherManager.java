package ca.pluszero.emotive.managers;

import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import ca.pluszero.emotive.ApiKeys;
import ca.pluszero.emotive.fragments.MainFragment;
import ca.pluszero.emotive.models.Forecast;
import ca.pluszero.emotive.models.PlaceDetails;
import ca.pluszero.emotive.models.WeatherIcon;

public class WeatherManager {
    private static final String API_KEY = ApiKeys.OPEN_WEATHER_MAP_APP_ID_KEY;
    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast";

    private static AsyncHttpClient client = new AsyncHttpClient();
    private final OnFinishedListener listener;

    private int kelvinToFahrenheitRounded(double kelvin) {
        return (int) Math.round((kelvin - 273.15) * 1.8) + 32;
    }

    private JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler() {
        @Override
        public void onStart() {
        }

        @Override
        public void onSuccess(JSONObject response) {
            try {
                List<Forecast.FutureWeather> hourlyWeatherList = new ArrayList<Forecast.FutureWeather>();
                List<Forecast.FutureWeather> dailyWeatherList = new ArrayList<Forecast.FutureWeather>();

                JSONArray weatherList = response.getJSONArray("list");
                JSONObject currentWeatherObject = weatherList.getJSONObject(0);
                int temperatureInFahrenheit = kelvinToFahrenheitRounded(currentWeatherObject.getJSONObject("main").getDouble("temp"));
                int humidity = currentWeatherObject.getJSONObject("main").getInt("humidity");
                double windSpeed = currentWeatherObject.getJSONObject("wind").getDouble("speed");
                JSONArray weatherArray = currentWeatherObject.getJSONArray("weather");
                String summary = "";
                String iconName = "";
                if (weatherArray.length() > 0) {
                    summary = weatherArray.getJSONObject(0).getString("main");
                    iconName = weatherArray.getJSONObject(0).getString("icon");

                }

                WeatherIcon weatherIcon = WeatherIcon.getEnumForString(iconName);

                int afternoonIndex = -1; // Index corresponding to 5pm today, so we know where to start for the `for` loop.
                boolean gotAfternoonIndex = false;
                for (int i = 1; i < 10; i++) {
                    JSONObject weatherObject = weatherList.getJSONObject(i);
                    double kelvinTemp = weatherObject.getJSONObject("main").getDouble("temp");
                    int fahrenheitTemp = kelvinToFahrenheitRounded(kelvinTemp);
                    Forecast.Temperature temperature = new Forecast.Temperature(fahrenheitTemp);
                    JSONArray hourlyWeatherArray = weatherObject.getJSONArray("weather");
                    String hourlyIconName = "";
                    if (hourlyWeatherArray.length() > 0) {
                        hourlyIconName = hourlyWeatherArray.getJSONObject(0).getString("icon");
                    }
                    WeatherIcon hourlyWeatherIcon = WeatherIcon.getEnumForString(hourlyIconName);
                    Toast.makeText(((MainFragment) listener).getActivity(), hourlyWeatherIcon.getNames().toString(), Toast.LENGTH_SHORT).show();
                    long timestamp = 1000L * weatherObject.getLong("dt");
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(timestamp);
                    if (cal.get(Calendar.HOUR_OF_DAY) == 17 && !gotAfternoonIndex) {
                        afternoonIndex = i;
                        gotAfternoonIndex = true;
                    }
                    hourlyWeatherList.add(new Forecast.FutureWeather(temperature, hourlyWeatherIcon, timestamp));
                }

                for (int i = afternoonIndex; i < weatherList.length(); i+=8) {
                    JSONObject weatherObject = weatherList.getJSONObject(i);
                    double kelvinTemp = weatherObject.getJSONObject("main").getDouble("temp");
                    int fahrenheitTemp = kelvinToFahrenheitRounded(kelvinTemp);
                    Forecast.Temperature temperature = new Forecast.Temperature(fahrenheitTemp);
                    JSONArray dailyWeatherArray = weatherObject.getJSONArray("weather");
                    String dailyIconName = "";
                    if (dailyWeatherArray.length() > 0) {
                        dailyIconName = dailyWeatherArray.getJSONObject(0).getString("icon");
                    }
                    WeatherIcon dailyWeatherIcon = WeatherIcon.getEnumForString(dailyIconName);
                    long timestamp = 1000L * weatherObject.getLong("dt");
                    dailyWeatherList.add(new Forecast.FutureWeather(temperature, dailyWeatherIcon, timestamp));
                }
//
//                JSONArray dailyData = response.getJSONObject("daily").getJSONArray("data");
//                for (int i = 0; i < 6; i++) {
//                    JSONObject dayData = dailyData.getJSONObject(i);
//                    Forecast.Temperature temperature = new Forecast.Temperature((int) Math.round(dayData.getDouble("temperatureMax")));
//                    String dailyIconName = dayData.getString("icon");
//                    WeatherIcon dailyWeatherIcon = WeatherIcon.getEnumForString(dailyIconName);
//                    long timestamp = 1000L * dayData.getLong("time"); // time in seconds needs to be converted to ms
//                    dailyWeatherList.add(new Forecast.FutureWeather(temperature, dailyWeatherIcon, timestamp));
//                }

                listener.onWeatherQueryFinished(new Forecast(summary, temperatureInFahrenheit, humidity, windSpeed, hourlyWeatherList, dailyWeatherList, weatherIcon));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public WeatherManager(OnFinishedListener listener) {
        this.listener = listener;
    }

    public static void get(String url, RequestParams params,
                           AsyncHttpResponseHandler responseHandler) {

        client.get(url, params, responseHandler);
    }

    public void getWeatherQuery(PlaceDetails placeDetails) {
        RequestParams params = new RequestParams();
        params.put("APP_ID", API_KEY);
        params.put("lat", placeDetails.getLatitude());
        params.put("lon", placeDetails.getLongitude());
        get(BASE_URL, params, responseHandler);
    }

    public interface OnFinishedListener {
        public void onWeatherQueryFinished(Forecast weatherData);
    }
}
