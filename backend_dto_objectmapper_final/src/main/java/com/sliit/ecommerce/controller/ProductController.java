package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.ProductCreateRequest;
import com.sliit.ecommerce.dto.ProductDTO;
import com.sliit.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductDTO> create(@Valid @RequestBody ProductCreateRequest request) {
        System.out.println(request.getDiscription());
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));

    }

    @GetMapping("/{id}")
    public ProductDTO get(@PathVariable String id) {
        return productService.getProduct(id);
    }

    @GetMapping
    public List<ProductDTO> getAll(@RequestParam(required = false) String categoryId) {
        if (categoryId != null) {
            return productService.getProductsByCategory(categoryId);
        }
        return productService.getAllProducts();
    }

    @PutMapping("/{id}")
    public ProductDTO update(@PathVariable String id, @Valid @RequestBody ProductCreateRequest request) {
        System.out.println(request.getDiscription());
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
