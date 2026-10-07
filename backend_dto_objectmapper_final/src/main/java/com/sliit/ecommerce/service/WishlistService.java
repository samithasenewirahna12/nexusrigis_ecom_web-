package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.Customer;
import com.sliit.ecommerce.Entitys.Product;
import com.sliit.ecommerce.Entitys.Wishlist;
import com.sliit.ecommerce.dto.WishlistDTO;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.CustomerRepository;
import com.sliit.ecommerce.repository.ProductRepository;
import com.sliit.ecommerce.repository.WishlistRepository;
import com.sliit.ecommerce.util.IdGenerator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    private final ModelMapper modelMapper;


    public WishlistService(WishlistRepository wishlistRepository, CustomerRepository customerRepository, ProductRepository productRepository, ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.wishlistRepository = wishlistRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    // get customer wishlist
    @Transactional(readOnly = true)
    public WishlistDTO getWishlist(String customerId) {

        Wishlist wishlist = getOrCreateWishlist(customerId);

        WishlistDTO wishlistDTO = toWishlistDTO(wishlist);

        return wishlistDTO;
    }

    // add product to wishlist
    public WishlistDTO addProduct(String customerId, String productId) {

        // get or create wishlist
        Wishlist wishlist = getOrCreateWishlist(customerId);

        // find product
        Optional<Product> productResult = productRepository.findById(productId);

        if (productResult.isEmpty()) {
            throw ResourceNotFoundException.of("Product", productId );
        }

        Product product = productResult.get();

        // add product
        wishlist.getProducts().add(product);

        // save wishlist
        Wishlist savedWishlist = wishlistRepository.save(wishlist);

        // convert to DTO
        WishlistDTO wishlistDTO = toWishlistDTO(savedWishlist);

        return wishlistDTO;
    }

    // remove product from wishlist
    public WishlistDTO removeProduct(String customerId, String productId) {

        // get or create wishlist
        Wishlist wishlist = getOrCreateWishlist(customerId);

        // remove the matching product safely from the Set
        wishlist.getProducts().removeIf(product -> product.getProductId().equals(productId));

        // save wishlist
        Wishlist savedWishlist = wishlistRepository.save(wishlist);

        // convert to DTO
        WishlistDTO wishlistDTO = toWishlistDTO(savedWishlist);

        return wishlistDTO;
    }

    // get existing wishlist or create a new one
    private Wishlist getOrCreateWishlist(String customerId) {

        Optional<Wishlist> wishlistResult = wishlistRepository.findByCustomer_UserId(customerId);

        // wishlist already exists
        if (wishlistResult.isPresent()) {

            Wishlist wishlist = wishlistResult.get();
            return wishlist;
        }

        // find customer
        Optional<Customer> customerResult = customerRepository.findById(customerId);

        if (customerResult.isEmpty()) {
            throw ResourceNotFoundException.of("Customer", customerId);
        }

        Customer customer = customerResult.get();

        // create new wishlist
        Wishlist wishlist = new Wishlist();
        wishlist.setWishlistId(IdGenerator.nextId("WISH", wishlistRepository.findAll().stream().map(Wishlist::getWishlistId).toList()));

        wishlist.setCustomer(customer);
        wishlist.setCreatedDate(LocalDate.now());

        // save new wishlist
        Wishlist savedWishlist = wishlistRepository.save(wishlist);

        return savedWishlist;
    }
    private WishlistDTO toWishlistDTO(Wishlist wishlist) {
        WishlistDTO dto = modelMapper.map(wishlist, WishlistDTO.class);
        if (wishlist.getCustomer() != null) dto.setCustomerId(wishlist.getCustomer().getUserId());
        if (wishlist.getProducts() != null) {
            dto.setProductIds(wishlist.getProducts().stream()
                    .map(Product::getProductId)
                    .collect(java.util.stream.Collectors.toList()));
        } else {
            dto.setProductIds(new java.util.ArrayList<>());
        }
        return dto;
    }

}