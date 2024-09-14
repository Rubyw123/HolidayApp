package com.az.holiday.models;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherInfo {
    private String CityName;
    private String zipcode;
    private String[] holidays;
    private double[][] temperatures;
}
