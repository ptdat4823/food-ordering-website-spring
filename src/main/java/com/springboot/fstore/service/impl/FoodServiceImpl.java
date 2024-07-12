package com.springboot.fstore.service.impl;

import com.springboot.fstore.entity.*;
import com.springboot.fstore.exception.CustomException;
import com.springboot.fstore.mapper.FoodMapper;
import com.springboot.fstore.mapper.FoodSizeMapper;
import com.springboot.fstore.payload.FoodDTO;
import com.springboot.fstore.payload.FoodSizeDTO;
import com.springboot.fstore.repository.CategoryRepository;
import com.springboot.fstore.repository.FoodRepository;
import com.springboot.fstore.repository.OrderRepository;
import com.springboot.fstore.service.FoodService;
import com.springboot.fstore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodServiceImpl implements FoodService {
    private final UserService userService;
    private final FoodRepository foodRepository;
    private final OrderRepository orderRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public FoodDTO createFood(FoodDTO foodDTO) {
        Food food = FoodMapper.toFood(foodDTO);

        if (foodDTO.getTags() != null) {
            food.setTags(foodDTO.getTags()
                    .stream()
                    .map(tag -> {
                        return Tag.builder()
                                .name(tag)
                                .build();
                    })
                    .toList());
        }

        if (foodDTO.getCategory() != null) {
            Category category = categoryRepository.findById(foodDTO.getCategory().getId()).orElseThrow(() -> new CustomException("Category not found", HttpStatus.NOT_FOUND));
            food.setCategory(category);
        }

        if (foodDTO.getFoodSizes() != null) {
            food.setFoodSizes(foodDTO.getFoodSizes()
                    .stream()
                    .map(foodSizeDTO -> {
                        FoodSize foodSize = FoodSizeMapper.toFoodSize(foodSizeDTO);
                        foodSize.setFood(food);
                        return foodSize;
                    })
                    .toList());
        }

        Food newFood = foodRepository.save(food);

        return FoodMapper.toFoodDTO(newFood);
    }

    @Override
    public FoodDTO updateFood(int foodId, FoodDTO foodDTO) {
        Food food = foodRepository.findById(foodId).orElseThrow(() -> new CustomException("Food not found", HttpStatus.NOT_FOUND));
        food.setName(foodDTO.getName());
        food.setDescription(foodDTO.getDescription());
        food.setStatus(foodDTO.getStatus());

        // Ensure images collection is mutable
        List<Image> newImages = foodDTO.getImages() != null ? foodDTO.getImages().stream().map(url -> Image.builder().url(url).build()).toList() : new ArrayList<>();
        food.getImages().clear();
        food.getImages().addAll(newImages);

        food.getTags().clear();

        if (foodDTO.getTags() != null) {
            food.getTags().addAll(foodDTO.getTags()
                    .stream()
                    .map(tag -> {
                        return Tag.builder()
                                .name(tag)
                                .build();
                    })
                    .toList());
        }

        if (foodDTO.getCategory() == null)
            food.setCategory(null);
        else {
            if (food.getCategory() == null || foodDTO.getCategory().getId() != food.getCategory().getId()) {
                Category category = categoryRepository.findById(foodDTO.getCategory().getId()).orElseThrow(() -> new CustomException("Category not found", HttpStatus.NOT_FOUND));
                food.setCategory(category);
            }
        }

        List<FoodSize> foodSizes = new ArrayList<>(food.getFoodSizes());

        for (FoodSize foodSize : foodSizes) {
            if (foodSize.isDeleted()) continue;
            if (foodDTO.getFoodSizes().stream().noneMatch(foodSizeDTO -> foodSizeDTO.getId() == foodSize.getId())) {
                foodSize.setDeleted(true);
            }
            foodDTO.getFoodSizes().stream()
                    .filter(foodSizeDTO -> foodSizeDTO.getId() == foodSize.getId())
                    .findFirst()
                    .ifPresent(foodSizeDTO -> {
                        if (!foodSize.getName().equals(foodSizeDTO.getName()) ||
                            !foodSize.getPrice().equals(foodSizeDTO.getPrice()) ||
                            !foodSize.getWeight().equals(foodSizeDTO.getWeight()) ||
                            !foodSize.getNote().equals(foodSizeDTO.getNote())) {
                                foodSize.setDeleted(true);
                                FoodSize newFoodSize = FoodSizeMapper.toFoodSize(foodSizeDTO);
                                newFoodSize.setFood(food);
                                food.getFoodSizes().add(newFoodSize);
                            }
                    });
        }
        for (FoodSizeDTO foodSizeDTO : foodDTO.getFoodSizes()) {
            if (foodSizeDTO.getId() == 0) {
                FoodSize foodSize = FoodSizeMapper.toFoodSize(foodSizeDTO);
                foodSize.setFood(food);
                food.getFoodSizes().add(foodSize);
            }
        }

        Food newFood = foodRepository.save(food);
        return FoodMapper.toFoodDTO(newFood);
    }

    @Override
    public FoodDTO getFood(int id) {
        User user = userService.getAuthorizedUser();
        Food food = foodRepository.findById(id).orElseThrow(() -> new CustomException("Food not found", HttpStatus.NOT_FOUND));

        List<Order> orders = orderRepository.findAllByUserId(user.getId());
        boolean isPurchased = orders.stream().anyMatch(order -> order.getItems().stream().anyMatch(cart -> cart.getFood().getId() == food.getId()));
        FoodDTO foodDTO = FoodMapper.toFoodDTO(food);
        foodDTO.setPurchased(isPurchased);

        return foodDTO;
    }

    @Override
    public void deleteFood(int id) {
        Food food = foodRepository.findById(id).orElseThrow(() -> new CustomException("Food not found", HttpStatus.NOT_FOUND));
        food.setIsDeleted(true);
        foodRepository.save(food);
    }

    @Override
    public List<FoodDTO> getFoods() {
        User user = null;

        try {
            user = userService.getAuthorizedUser();
        } catch (Exception e) {
            // do no thing
        }

        List<Food> foods = foodRepository.findAll();
        List<Order> orders = user != null ? orderRepository.findAllByUserId(user.getId()) : new ArrayList<>();
        List<Order> allOrders = orderRepository.findAll();


        return foods.stream()
                .map(food -> {
                    boolean isPurchased = orders.stream().anyMatch(order -> order.getItems().stream().anyMatch(cart -> cart.getFood().getId() == food.getId()));
                    //calculate total sold of food in all delivered orders
                    Integer totalSold =
                        allOrders.stream()
                            .filter(order -> order.getStatus().equals("DELIVERED"))
                            .map(order -> order.getItems().stream().filter(cart -> cart.getFood().getId() == food.getId()).mapToInt(Cart::getQuantity).sum())
                            .reduce(0, Integer::sum);
                    FoodDTO foodDTO = FoodMapper.toFoodDTO(food);
                    foodDTO.setPurchased(isPurchased);
                    foodDTO.setTotalSold(totalSold);
                    return foodDTO;
                })
                .toList();
    }
}
