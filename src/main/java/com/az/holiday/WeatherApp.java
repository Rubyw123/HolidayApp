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
import java.util.*;
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

    private List<String> parseInput(String input){
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

    public static List<Holiday> removeDuplicates(List<Holiday> holidays) {
        Set<String> dates = new HashSet<>();
        List<Holiday> result = new ArrayList<>();

        for (Holiday holiday : holidays) {
            if (dates.add(holiday.getDate().toString())) {  // add returns false if the date is already in the set
                result.add(holiday);      // only add holidays with unique dates
            }
        }

        return result;
    }


    public void run() throws Exception {
        Scanner scanner = new Scanner(System.in);
        String input;
        while(true){
            System.out.println("Please input zipcodes (type 'q' to quit): ");
            input = scanner.nextLine().trim();

            if(input.equals("q")){
                break;
            }
                List<String> zips = parseInput(input);
                if(!zips.isEmpty()){
                    try{
                        List<Place> places = zipService.getPlaces(zips);
                        List<Holiday> holidays = holidayService.getHolidays("2024");

                        if(!holidays.isEmpty() && !places.isEmpty()){
                            List<WeatherInfo> weatherInfos = weatherService.getWeatherInfos(places,removeDuplicates(holidays),2024);
                            weatherService.saveResultToJson(weatherInfos,"./holiday.json");
                        }
                    } catch (IOException | InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }



        }


    }


}
