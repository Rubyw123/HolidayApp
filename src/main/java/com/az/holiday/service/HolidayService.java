package com.az.holiday.service;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.az.holiday.models.Holiday;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class HolidayService {
    private final String holiday_url = "https://date.nager.at/api/v2/publicholidays/";

    public List<Holiday> getHolidays(String year) throws IOException, InterruptedException {
        String url = holiday_url +year+"/US";
        List<Holiday> holidays = new ArrayList<>();
        //calling the apis
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            var json = new Gson().fromJson(response.body(), JsonArray.class);
            for (JsonElement jsonElement : json.getAsJsonArray()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                String name = jsonObject.get("name").getAsString();
                LocalDate date = LocalDate.parse(jsonObject.get("date").getAsString());
                holidays.add(Holiday.builder()
                        .name(name)
                        .date(date)
                        .build());

            }

            return holidays;
        }else{
            System.out.println("Holiday Error: " + response.statusCode());
            return null;
        }
    }
}
