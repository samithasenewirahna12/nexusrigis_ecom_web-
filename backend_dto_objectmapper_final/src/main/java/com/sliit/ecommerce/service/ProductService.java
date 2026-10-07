package com.sliit.ecommerce.service;


import com.sliit.ecommerce.Entitys.Category;
import com.sliit.ecommerce.Entitys.Product;

import com.sliit.ecommerce.dto.ProductCreateRequest;
import com.sliit.ecommerce.dto.ProductDTO;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.AdministratorRepository;
import com.sliit.ecommerce.repository.CategoryRepository;
import com.sliit.ecommerce.repository.ProductRepository;
import com.sliit.ecommerce.repository.WarehouseStaffRepository;
import com.sliit.ecommerce.util.IdGenerator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AdministratorRepository administratorRepository;
    private final WarehouseStaffRepository warehouseStaffRepository;
    private final ModelMapper modelMapper;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository, AdministratorRepository administratorRepository, WarehouseStaffRepository warehouseStaffRepository, ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.administratorRepository = administratorRepository;
        this.warehouseStaffRepository = warehouseStaffRepository;
    }

    // create a new product
    public ProductDTO createProduct(ProductCreateRequest request) {

        // find category
        Optional<Category> categoryResult = categoryRepository.findById(request.getCategoryId());

        if (categoryResult.isEmpty()) {
            throw ResourceNotFoundException.of("Category", request.getCategoryId());
        }

        Category category = categoryResult.get();

        // create product
        Product product = new Product();
        product.setProductId(IdGenerator.nextId("P", productRepository.findAll().stream().map(Product::getProductId).toList()));

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setBrand(request.getBrand());
        product.setDiscription(request.getDiscription());
        product.setStockQty(request.getStockQty());
        product.setImages(request.getImages());
        product.setCategory(category);

        // assign administrator if provided
        if (request.getAdminId() != null && !request.getAdminId().isBlank()) {
            administratorRepository.findById(request.getAdminId()).ifPresent(product::setManagedByAdmin);
        }

        // assign warehouse staff if provided
        if (request.getWarehouseStaffId() != null && !request.getWarehouseStaffId().isBlank()) {
            warehouseStaffRepository.findById(request.getWarehouseStaffId()).ifPresent(product::setStockUpdatedBy);
        }

        // save product
        Product savedProduct = productRepository.save(product);

        // convert to DTO
        ProductDTO productDTO = toProductDTO(savedProduct);

        return productDTO;
    }

    // get product by ID
    @Transactional(readOnly = true)
    public ProductDTO getProduct(String id) {

        Optional<Product> productResult = productRepository.findById(id);

        if (productResult.isEmpty()) {
            throw ResourceNotFoundException.of("Product", id);
        }

        Product product = productResult.get();

        ProductDTO productDTO = toProductDTO(product);

        return productDTO;
    }

    // get all products
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {

        List<Product> products = productRepository.findAll();

        List<ProductDTO> productDTOList = new ArrayList<>();

        for (Product product : products) {

            ProductDTO productDTO = toProductDTO(product);
            productDTOList.add(productDTO);
        }

        return productDTOList;
    }

    // get products by category
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(String categoryId) {

        List<Product> products = productRepository.findByCategory_CategoryId(categoryId);

        List<ProductDTO> productDTOList = new ArrayList<>();

        for (Product product : products) {

            ProductDTO productDTO = toProductDTO(product);
            productDTOList.add(productDTO);
        }

        return productDTOList;
    }

    // update product
    public ProductDTO updateProduct(String id, ProductCreateRequest request) {

        // find product
        Optional<Product> productResult = productRepository.findById(id);

        if (productResult.isEmpty()) {
            throw ResourceNotFoundException.of("Product", id);
        }

        Product product = productResult.get();

        // find category
        Optional<Category> categoryResult = categoryRepository.findById(request.getCategoryId());

        if (categoryResult.isEmpty()) {
            throw ResourceNotFoundException.of("Category", request.getCategoryId());
        }

        Category category = categoryResult.get();

        // update product details
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setBrand(request.getBrand());
        product.setDiscription(request.getDiscription());
        product.setStockQty(request.getStockQty());
        product.setImages(request.getImages());
        product.setCategory(category);

        // update administrator if provided
        if (request.getAdminId() != null && !request.getAdminId().isBlank()) {
            administratorRepository.findById(request.getAdminId()).ifPresent(product::setManagedByAdmin);
        }

        // update warehouse staff if provided
        if (request.getWarehouseStaffId() != null && !request.getWarehouseStaffId().isBlank()) {
            warehouseStaffRepository.findById(request.getWarehouseStaffId()).ifPresent(product::setStockUpdatedBy);
        }

        // save updated product
        Product updatedProduct = productRepository.save(product);

        // convert to DTO
        ProductDTO productDTO = toProductDTO(updatedProduct);

        return productDTO;
    }

    private ProductDTO toProductDTO(Product product) {
        ProductDTO dto = modelMapper.map(product, ProductDTO.class);
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getCategoryId());
            dto.setCategoryName(product.getCategory().getCategoryName());
        }
        if (product.getManagedByAdmin() != null) {
            dto.setAdminId(product.getManagedByAdmin().getUserId());
        }
        if (product.getStockUpdatedBy() != null) {
            dto.setWarehouseStaffId(product.getStockUpdatedBy().getUserId());
        }
        return dto;
    }

    // delete product
    public void deleteProduct(String id) {

        Optional<Product> productResult = productRepository.findById(id);

        if (productResult.isEmpty()) {
            throw ResourceNotFoundException.of("Product", id);
        }

        Product product = productResult.get();
        productRepository.delete(product);
    }
}