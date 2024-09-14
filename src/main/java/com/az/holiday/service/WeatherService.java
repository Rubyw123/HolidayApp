package com.az.holiday.service;

import com.az.holiday.models.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@Service
public class WeatherService {

    private static final int THREAD_POOL_SIZE = 10;
    private static final int NUMBER_OF_YEARS = 5;
    private static final String WEATHER_URL = "https://archive-api.open-meteo.com/v1/archive";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void saveResultToJson(List<WeatherInfo> weathers,String filePath) throws IOException {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        //create json string
        String json = gson.toJson(weathers);

        //write to file
        try(FileWriter fileWriter = new FileWriter(filePath)){
            fileWriter.write(json);
            System.out.println("Successfully wrote to the file"+filePath);
        }catch(IOException e){
            System.out.println("Error writing to the file"+filePath);
        }
    }

    public List<WeatherInfo> getWeatherInfos(List<Place> places,List<Holiday> holidays,int curYear) throws InterruptedException, ExecutionException {
        //init thread pool
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Callable<WeatherInfo>> tasks = new ArrayList<>();

        //Adding tasks for different places in list
        for(Place place : places){
            tasks.add(()->{
                WeatherInfo weatherInfo = new WeatherInfo(
                        place.getName(),
                        place.getZipcode(),
                        new String[holidays.size()],
                        new double[holidays.size()][3]
                );

                for(int i=0; i < holidays.size(); i++){
                    Holiday holiday = holidays.get(i);
                    double[] weather = getPlaceWeatherAsync(place,holiday,curYear).get();
                    weatherInfo.getHolidays()[i] = holiday.getName();
                    weatherInfo.getTemperatures()[i] = weather;
                }
                return weatherInfo;

            });
        }
        //Waiting for all tasks finished
        List<Future<WeatherInfo>> futures = executor.invokeAll(tasks);

        List<WeatherInfo> weatherInfos = new ArrayList<>();
        for(Future<WeatherInfo> future : futures){
            weatherInfos.add(future.get());
        }
        executor.shutdown();
        return weatherInfos;
    }

    public CompletableFuture<double[]> getPlaceWeatherAsync(Place place, Holiday holiday, int currentYear) {
        //init completableFuture task list
        List<CompletableFuture<double[]>> futures = new ArrayList<>();

        for(int i=0; i < NUMBER_OF_YEARS; i++){
            int year = currentYear-i;
            //adding task
            futures.add(getWeatherOfYearAsync(place,holiday,year));
        }

        //wait for all tasks completed
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(w->{
                    double[]totals = new double[]{0.0,0.0,0.0};
                    for(CompletableFuture<double[]> future : futures){
                        //return result when completed
                        double[] weatherOfYear = future.join();
                        if(weatherOfYear != null){
                            totals[0] += weatherOfYear[0];
                            totals[1] += weatherOfYear[1];
                            totals[2] += weatherOfYear[2];
                        }
                    }
                    return new double[]{totals[0] / NUMBER_OF_YEARS, totals[1] / NUMBER_OF_YEARS, totals[2] / NUMBER_OF_YEARS};
                });
    }

    private CompletableFuture<double[]> getWeatherOfYearAsync(Place place, Holiday holiday, int year){
        //setting retrieve url for that year
        String url = WEATHER_URL + "?latitude=" + place.getLatitude()
                + "&longitude=" + place.getLongitude()
                + "&start_date=" + holiday.getDate().withYear(year)
                + "&end_date=" + holiday.getDate().withYear(year)
                + "&hourly=temperature_2m";

        //Calling apis
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        //asynchronous fetching data from api
        System.out.println("Fetching weather data for: "+place.getName()+",holiday: "+holiday.getName()+", year: "+year);
        return httpClient.sendAsync(request,HttpResponse.BodyHandlers.ofString())
                .thenApply(response->{
                    if(response.statusCode() == 200){
                        var weatherJson = new Gson().fromJson(response.body(), JsonObject.class);
                        var timestamp = weatherJson.get("hourly").getAsJsonObject().get("time").getAsJsonArray();
                        var temps = weatherJson.get("hourly").getAsJsonObject().get("temperature_2m").getAsJsonArray();

                        LocalDateTime[] weather_time = IntStream.range(0,timestamp.size())
                                .parallel()
                                .mapToObj(i->LocalDateTime.parse(timestamp.get(i).getAsString()))
                                .toArray(LocalDateTime[]::new);

                        double[] weather_temperature = IntStream.range(0,temps.size())
                                .parallel()
                                .mapToDouble(i->temps.get(i).getAsDouble())
                                .toArray();

                        double morning = calculateTempAvg(weather_temperature, weather_time, LocalTime.of(6, 0), LocalTime.of(12, 0));
                        double afternoon = calculateTempAvg(weather_temperature, weather_time, LocalTime.of(12, 1), LocalTime.of(18, 0));
                        double evening = calculateTempAvg(weather_temperature, weather_time, LocalTime.of(18, 1), LocalTime.of(23, 59));
                        return new double[]{morning, afternoon, evening};
                    }else {
                        System.out.println("Weather Error: " + response.statusCode());
                        return null;
                    }
                });
    }

    private double calculateTempAvg(double[] temps, LocalDateTime[] times, LocalTime start,LocalTime end) {

        return IntStream.range(0,times.length)
                //Using parallel to parallel execute the filtering and average
                .parallel()
                //filtering the temps indices that are within this period of time(start < temp_time< end)
                .filter(i->{
                    LocalTime time = times[i].toLocalTime();
                    return !time.isBefore(start) && !time.isAfter(end);
                })
                .mapToDouble(i->temps[i])
                .average()
                .orElse(Double.NaN);
    }
}
