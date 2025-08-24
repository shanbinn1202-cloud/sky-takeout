package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderRankDTO implements Serializable {
    private String name;
    private Integer count;
}
