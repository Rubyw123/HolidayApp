package com.az.holiday;

import com.az.holiday.models.Holiday;
import com.az.holiday.models.Place;
import com.az.holiday.models.WeatherInfo;
import com.az.holiday.service.HolidayService;
import com.az.holiday.service.WeatherService;
import com.az.holiday.service.ZipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

@Component
public class WeatherApp {
    private final ZipService zipService;
    private final HolidayService holidayService;
    private final WeatherService weatherService;

    @Autowired
    public WeatherApp(ZipService zipService, HolidayService holidayService, WeatherService weatherService) {
        this.zipService = zipService;
        this.holidayService = holidayService;
        this.weatherService = weatherService;
    }

    private static List<String> parseInput(String input){
        List<String> zips = new ArrayList<>();
        String[] splitted = input.split(",");
        for (String s : splitted) {
            s = s.trim();
            if (!s.isEmpty()) {
                zips.add(s);
            }
        }
        return zips;
    }

    public void run() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input zipcodes: ");
        String input = scanner.nextLine();

        List<String> zips = parseInput(input);

        ZipService zipService = new ZipService();
        HolidayService  holidayService = new HolidayService();
        WeatherService weatherService = new WeatherService();

        try{
            List<Place> places = zipService.getPlaces(zips);
            List<Holiday> holidays = holidayService.getHolidays("2024");

            List<WeatherInfo> weatherInfos = weatherService.getWeatherInfos(places,holidays,2024);
            weatherService.saveResultToJson(weatherInfos,"./holiday.json");

        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


}
