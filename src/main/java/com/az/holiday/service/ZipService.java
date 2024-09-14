package com.az.holiday.service;


import com.az.holiday.models.Place;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class ZipService {

    private final String zipcode_url = "https://api.zippopotam.us/us/";

    public List<Place> getPlaces(List<String> zips) throws IOException, InterruptedException {
        List<Place> places = new ArrayList<>();
        for(String zip : zips){
            Place place = getPlaceFromZipcode(zip);
            if(place != null){
                places.add(place);
            }
        }

        return places;

    }

    private Place getPlaceFromZipcode(String zipcode) throws IOException, InterruptedException {
        String url = zipcode_url + zipcode;

        //Calling apis
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            //Parser json
            //getting the first place of the array
            var zip = new Gson().fromJson(response.body(), JsonObject.class);
            if(zip.get("places") != null && !zip.get("places").isJsonNull()){
                var place = zip.get("places").getAsJsonArray().get(0).getAsJsonObject();
                String placeName = place.get("place name").getAsString();
                double longitude = place.get("longitude").getAsDouble();
                double latitude = place.get("latitude").getAsDouble();

                return Place.builder()
                        .name(placeName)
                        .zipcode(zipcode)
                        .longitude(longitude)
                        .latitude(latitude)
                        .build();
            }else{
                return null;
            }
        }else{
            System.out.println("Zipcode Error: " + response.statusCode());
            return null;
        }
    }

}
