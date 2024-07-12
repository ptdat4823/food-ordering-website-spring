package com.springboot.fstore.service;

import com.springboot.fstore.payload.FoodDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FoodService {
    FoodDTO createFood(FoodDTO foodDTO);

    FoodDTO updateFood(int foodId, FoodDTO foodDTO);

    FoodDTO getFood(int id);

    void deleteFood(int id);

    List<FoodDTO> getFoods();

}
