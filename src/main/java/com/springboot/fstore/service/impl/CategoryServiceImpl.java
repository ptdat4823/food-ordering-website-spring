package com.springboot.fstore.service.impl;

import com.springboot.fstore.entity.Category;
import com.springboot.fstore.exception.CustomException;
import com.springboot.fstore.mapper.CategoryMapper;
import com.springboot.fstore.payload.CategoryDTO;
import com.springboot.fstore.repository.CategoryRepository;
import com.springboot.fstore.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryDTO> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .map(CategoryMapper::toCategoryDTO)
                .toList();
    }

    @Override
    public CategoryDTO getCategoryById(int id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CustomException("Category not found", HttpStatus.NOT_FOUND));
        return CategoryMapper.toCategoryDTO(category);
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        if (categoryRepository.existsByName(categoryDTO.getName())) {
            throw new CustomException("Category already exists", HttpStatus.BAD_REQUEST);
        }
        Category category = CategoryMapper.toCategory(categoryDTO);

        Category newCategory = categoryRepository.save(category);
        return CategoryMapper.toCategoryDTO(newCategory);
    }

    @Override
    public CategoryDTO updateCategory(int id, CategoryDTO categoryDTO) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CustomException("Category not found", HttpStatus.NOT_FOUND));
        category.setName(categoryDTO.getName());
        category.setImage(categoryDTO.getImage());

        return CategoryMapper.toCategoryDTO(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(int id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CustomException("Category not found", HttpStatus.NOT_FOUND));
        categoryRepository.delete(category);
    }
}
