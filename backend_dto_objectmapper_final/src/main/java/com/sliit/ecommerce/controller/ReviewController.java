package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.ReviewCreateRequest;
import com.sliit.ecommerce.dto.ReviewDTO;
import com.sliit.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewDTO> create(@Valid @RequestBody ReviewCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(request));
    }

    @GetMapping("/{id}")
    public ReviewDTO get(@PathVariable String id) {
        return reviewService.getReview(id);
    }

    @GetMapping
    public List<ReviewDTO> getAll(@RequestParam(required = false) String productId,
                                   @RequestParam(required = false) String customerId) {
        if (productId != null) {
            return reviewService.getReviewsByProduct(productId);
        }
        if (customerId != null) {
            return reviewService.getReviewsByCustomer(customerId);
        }
        return reviewService.getAllReviews();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
