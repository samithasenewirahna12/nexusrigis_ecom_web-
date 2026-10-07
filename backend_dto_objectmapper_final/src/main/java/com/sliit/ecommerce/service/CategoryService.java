package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.Administrator;
import com.sliit.ecommerce.Entitys.Category;
import com.sliit.ecommerce.dto.CategoryCreateRequest;
import com.sliit.ecommerce.dto.CategoryDTO;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.AdministratorRepository;
import com.sliit.ecommerce.repository.CategoryRepository;
import com.sliit.ecommerce.util.IdGenerator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AdministratorRepository administratorRepository;

    private final ModelMapper modelMapper;


    public CategoryService(CategoryRepository categoryRepository, AdministratorRepository administratorRepository, ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.categoryRepository = categoryRepository;
        this.administratorRepository = administratorRepository;
    }

    // create category
    public CategoryDTO createCategory(CategoryCreateRequest request) {

        Category category = new Category();
        category.setCategoryId(IdGenerator.nextId("CAT", categoryRepository.findAll().stream().map(Category::getCategoryId).toList()));
         category.setCategoryName(request.getCategoryName());
         category.setDescription(request.getDescription());
         category.setCategoryImage(request.getCategoryImage());

        // set administrator
        if (request.getAdminId() != null) {

            Optional<Administrator> result = administratorRepository.findById(request.getAdminId());

            if (result.isEmpty()) {
                throw ResourceNotFoundException.of("Administrator", request.getAdminId());
            }

            Administrator admin = result.get();
            category.setManagedByAdmin(admin);
        }


        // save category
        Category savedCategory = categoryRepository.save(category);

        // convert to DTO
        CategoryDTO categoryDTO = modelMapper.map(savedCategory, CategoryDTO.class);

        return categoryDTO;
    }


    // get category by ID
    @Transactional(readOnly = true)
    public CategoryDTO getCategory(String id) {

        Optional<Category> result = categoryRepository.findById(id);

        if (result.isEmpty()) {
            throw ResourceNotFoundException.of("Category", id);
        }

        Category category = result.get();

        CategoryDTO categoryDTO = modelMapper.map(category, CategoryDTO.class);

        return categoryDTO;
    }


    // get all categories
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {

        List<Category> categories = categoryRepository.findAll();

        List<CategoryDTO> categoryDTOList = new ArrayList<>();

        for (Category category : categories) {
            CategoryDTO categoryDTO = modelMapper.map(category, CategoryDTO.class);
            categoryDTOList.add(categoryDTO);
        }

        return categoryDTOList;
    }


    // update category
    public CategoryDTO updateCategory(String id, CategoryCreateRequest request) {

        // find category
        Optional<Category> result = categoryRepository.findById(id);

        if (result.isEmpty()) {
            throw ResourceNotFoundException.of("Category", id);
        }

        Category category = result.get();

        // update category details
         category.setCategoryName(request.getCategoryName());
         category.setDescription(request.getDescription());
         category.setCategoryImage(request.getCategoryImage());

        // update administrator
        if (request.getAdminId() != null) {

            Optional<Administrator> adminResult = administratorRepository.findById(request.getAdminId());

            if (adminResult.isEmpty()) {
                throw ResourceNotFoundException.of("Administrator", request.getAdminId());
            }

            Administrator admin = adminResult.get();

            category.setManagedByAdmin(admin);
        }


        // save updated category
        Category updatedCategory = categoryRepository.save(category);

        // convert to DTO
        CategoryDTO categoryDTO = modelMapper.map(updatedCategory, CategoryDTO.class);

        return categoryDTO;
    }


    // delete category
    public void deleteCategory(String id) {

        Optional<Category> result = categoryRepository.findById(id);

        if (result.isEmpty()) {
            throw ResourceNotFoundException.of("Category", id);
        }

        Category category = result.get();

        categoryRepository.delete(category);
    }
}