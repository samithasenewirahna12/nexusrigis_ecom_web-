package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.Customer;
import com.sliit.ecommerce.Entitys.Product;
import com.sliit.ecommerce.Entitys.Review;
import com.sliit.ecommerce.dto.ReviewCreateRequest;
import com.sliit.ecommerce.dto.ReviewDTO;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.CustomerRepository;
import com.sliit.ecommerce.repository.ProductRepository;
import com.sliit.ecommerce.repository.ReviewRepository;
import com.sliit.ecommerce.util.IdGenerator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    private final ModelMapper modelMapper;


    public ReviewService(ReviewRepository reviewRepository, CustomerRepository customerRepository, ProductRepository productRepository, ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.reviewRepository = reviewRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    // create a new review
    public ReviewDTO createReview(ReviewCreateRequest request) {

        // find customer
        Optional<Customer> customerResult = customerRepository.findById(request.getCustomerId());

        if (customerResult.isEmpty()) {
            throw ResourceNotFoundException.of("Customer", request.getCustomerId());
        }

        Customer customer = customerResult.get();

        // find product
        Optional<Product> productResult = productRepository.findById(request.getProductId());

        if (productResult.isEmpty()) {
            throw ResourceNotFoundException.of("Product", request.getProductId());
        }

        Product product = productResult.get();

        // create review
        Review review = new Review();
        review.setReviewId(IdGenerator.nextId("REV", reviewRepository.findAll().stream().map(Review::getReviewId).toList()));

        review.setCustomer(customer);
        review.setProduct(product);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setReviewDate(LocalDate.now());

        // save review
        Review savedReview = reviewRepository.save(review);

        // convert to DTO
        ReviewDTO reviewDTO = toReviewDTO(savedReview);

        return reviewDTO;
    }

    // get review by ID
    @Transactional(readOnly = true)
    public ReviewDTO getReview(String id) {

        Optional<Review> reviewResult = reviewRepository.findById(id);

        if (reviewResult.isEmpty()) {
            throw ResourceNotFoundException.of("Review", id);
        }

        Review review = reviewResult.get();

        ReviewDTO reviewDTO = toReviewDTO(review);

        return reviewDTO;
    }

    // get reviews by product
    @Transactional(readOnly = true)
    public List<ReviewDTO> getReviewsByProduct( String productId) {

        List<Review> reviews = reviewRepository.findByProduct_ProductId(productId);

        List<ReviewDTO> reviewDTOList = new ArrayList<>();

        for (Review review : reviews) {

            ReviewDTO reviewDTO = toReviewDTO(review);
            reviewDTOList.add(reviewDTO);
        }

        return reviewDTOList;
    }

    // get reviews by customer
    @Transactional(readOnly = true)
    public List<ReviewDTO> getReviewsByCustomer( String customerId) {

        List<Review> reviews = reviewRepository.findByCustomer_UserId(customerId);

        List<ReviewDTO> reviewDTOList = new ArrayList<>();

        for (Review review : reviews) {

            ReviewDTO reviewDTO = toReviewDTO(review);
            reviewDTOList.add(reviewDTO);
        }

        return reviewDTOList;
    }

    // get all reviews
    @Transactional(readOnly = true)
    public List<ReviewDTO> getAllReviews() {
        List<Review> reviews = reviewRepository.findAll();
        List<ReviewDTO> reviewDTOList = new ArrayList<>();
        for (Review review : reviews) {
            reviewDTOList.add(toReviewDTO(review));
        }
        return reviewDTOList;
    }

    // delete review
    public void deleteReview(String id) {

        Optional<Review> reviewResult = reviewRepository.findById(id);

        if (reviewResult.isEmpty()) {
            throw ResourceNotFoundException.of("Review", id);
        }

        Review review = reviewResult.get();

        reviewRepository.delete(review);
    }
    private ReviewDTO toReviewDTO(Review review) {
        ReviewDTO dto = modelMapper.map(review, ReviewDTO.class);
        if (review.getCustomer() != null) {
            dto.setCustomerId(review.getCustomer().getUserId());
            dto.setCustomerName(review.getCustomer().getName());
            dto.setCustomerImage(review.getCustomer().getUserImage());
        }
        if (review.getProduct() != null) dto.setProductId(review.getProduct().getProductId());
        return dto;
    }

}