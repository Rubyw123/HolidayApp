package com.az.holiday.models;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {
    private String name;
    private String zipcode;
    private double longitude;
    private double latitude;
}
