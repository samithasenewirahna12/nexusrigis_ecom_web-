package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.CartItem;
import com.sliit.ecommerce.Entitys.Customer;
import com.sliit.ecommerce.Entitys.Product;
import com.sliit.ecommerce.Entitys.ShoppingCart;
import com.sliit.ecommerce.dto.AddToCartRequest;
import com.sliit.ecommerce.dto.CartItemDTO;
import com.sliit.ecommerce.dto.ShoppingCartDTO;
import com.sliit.ecommerce.exception.BusinessRuleException;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.CartItemRepository;
import com.sliit.ecommerce.repository.CustomerRepository;
import com.sliit.ecommerce.repository.ProductRepository;
import com.sliit.ecommerce.repository.ShoppingCartRepository;
import com.sliit.ecommerce.util.IdGenerator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    private final ShoppingCartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    public CartService(ShoppingCartRepository cartRepository, CartItemRepository cartItemRepository, CustomerRepository customerRepository, ProductRepository productRepository, ModelMapper modelMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.modelMapper = modelMapper;
    }

    // Get customer's cart
    public ShoppingCartDTO getCart(String customerId) {

        ShoppingCart cart = getOrCreateCart(customerId);

        return toShoppingCartDTO(cart);
    }

    // Add product to cart
    public ShoppingCartDTO addItem(
            String customerId,
            AddToCartRequest request) {

        // Validate quantity
        if (request.getQuantity() <= 0) {
            throw new BusinessRuleException("Quantity must be greater than 0");
        }

        // Get customer's cart
        ShoppingCart cart = getOrCreateCart(customerId);

        // Find product
        Optional<Product> productResult = productRepository.findById(request.getProductId());

        if (productResult.isEmpty()) {
            throw ResourceNotFoundException.of("Product", request.getProductId());
        }

        Product product = productResult.get();

        // Check product stock
        if (product.getStockQty() <= 0) {
            throw new BusinessRuleException(
                    "Product is out of stock"
            );
        }

        // Check whether product already exists in cart
        Optional<CartItem> itemResult = cartItemRepository.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), product.getProductId());

        if (itemResult.isPresent()) {

            // Product already exists
            CartItem item = itemResult.get();

            int newQuantity = item.getQuantity() + request.getQuantity();

            if (newQuantity > product.getStockQty()) {
                throw new BusinessRuleException("Insufficient stock for product " + product.getProductId());
            }

            item.setQuantity(newQuantity);

            cartItemRepository.save(item);

        } else {

            // New product
            if (request.getQuantity() > product.getStockQty()) {
                throw new BusinessRuleException("Insufficient stock for product " + product.getProductId());
            }

            CartItem newItem =
                    new CartItem(cart, product, request.getQuantity());

            cart.getItems().add(newItem);
        }

        // Flush pending changes (including cascaded new CartItem) before building DTO
        cartRepository.save(cart);

        return toShoppingCartDTO(cart);
    }

    // Update product quantity in cart
    public ShoppingCartDTO updateItemQuantity(String customerId, String productId, int quantity) {

        if (quantity <= 0) {
            return removeItem(customerId, productId);
        }

        ShoppingCart cart = getOrCreateCart(customerId);

        Optional<CartItem> itemResult = cartItemRepository.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), productId);

        if (itemResult.isEmpty()) {
            throw ResourceNotFoundException.of("CartItem for product", productId);
        }

        CartItem item = itemResult.get();

        if (quantity > item.getProduct().getStockQty()) {
            throw new BusinessRuleException("Insufficient stock for product " + productId);
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return toShoppingCartDTO(cart);
    }

    // Remove one product from cart
    public ShoppingCartDTO removeItem(String customerId, String productId) {

        ShoppingCart cart = getOrCreateCart(customerId);

        Optional<CartItem> itemResult = cartItemRepository.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), productId);

        if (itemResult.isEmpty()) {
            throw ResourceNotFoundException.of("CartItem for product", productId);
        }

        CartItem item = itemResult.get();

        cartItemRepository.delete(item);

        cart.getItems().remove(item);

        return toShoppingCartDTO(cart);
    }

    // Remove all products from cart
    public void clearCart(String customerId) {

        ShoppingCart cart = getOrCreateCart(customerId);

        if (cart.getItems() != null && !cart.getItems().isEmpty()) {

            cartItemRepository.deleteAll(cart.getItems());
            cart.getItems().clear();
        }
    }

    // Find existing cart or create new cart
    private ShoppingCart getOrCreateCart(String customerId) {

        // Check whether customer already has a cart
        Optional<ShoppingCart> cartResult = cartRepository.findByCustomer_UserId(customerId);

        if (cartResult.isPresent()) {
            return cartResult.get();
        }

        // Find customer
        Optional<Customer> customerResult = customerRepository.findById(customerId);

        if (customerResult.isEmpty()) {
            throw ResourceNotFoundException.of("Customer", customerId);
        }

        Customer customer = customerResult.get();

        // Create new cart
        ShoppingCart newCart = new ShoppingCart();

        List<String> cartIds = new ArrayList<>();

        List<ShoppingCart> carts = cartRepository.findAll();

        for (ShoppingCart cart : carts) {
            cartIds.add(cart.getCartId());
        }

        newCart.setCartId(
                IdGenerator.nextId("CART", cartIds)
        );

        newCart.setCustomer(customer);
        newCart.setCreatedDate(LocalDate.now());

        return cartRepository.save(newCart);
    }

    // Convert ShoppingCart to DTO
    private ShoppingCartDTO toShoppingCartDTO(ShoppingCart cart) {

        ShoppingCartDTO dto = modelMapper.map(cart, ShoppingCartDTO.class);

        // Customer ID
        if (cart.getCustomer() != null) {
            dto.setCustomerId(cart.getCustomer().getUserId());
        }

        // Cart items
        List<CartItemDTO> itemDTOs = new ArrayList<>();

        BigDecimal totalPrice = BigDecimal.ZERO;

        if (cart.getItems() != null) {

            for (CartItem item : cart.getItems()) {

                CartItemDTO itemDTO = new CartItemDTO();
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                itemDTO.setQuantity(qty);

                if (item.getProduct() != null) {
                    Product product = item.getProduct();

                    itemDTO.setProductId(product.getProductId());
                    itemDTO.setProductName(product.getName());
                    itemDTO.setUnitPrice(product.getPrice());
                    itemDTO.setStockQty(product.getStockQty());

                    if (product.getCategory() != null) {
                        itemDTO.setCategoryName(product.getCategory().getCategoryName());
                    }

                    if (product.getImages() != null && !product.getImages().isEmpty()) {
                        itemDTO.setImageUrl(product.getImages().get(0));
                        itemDTO.setImages(new ArrayList<>(product.getImages()));
                    }

                    if (product.getPrice() != null) {
                        totalPrice = totalPrice.add(
                                product.getPrice().multiply(BigDecimal.valueOf(qty))
                        );
                    }
                }

                itemDTOs.add(itemDTO);
            }
        }

        dto.setItems(itemDTOs);
        dto.setTotalPrice(totalPrice);

        return dto;
    }
}