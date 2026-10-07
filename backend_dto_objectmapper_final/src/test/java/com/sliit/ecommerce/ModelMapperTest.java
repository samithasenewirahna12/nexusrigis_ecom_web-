package com.sliit.ecommerce;

import com.sliit.ecommerce.Entitys.Category;
import com.sliit.ecommerce.Entitys.Product;
import com.sliit.ecommerce.dto.AddressDTO;
import com.sliit.ecommerce.dto.ProductDTO;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelMapperTest {

    private final ModelMapper modelMapper = new ModelMapper();

    @Test
    void mapsAddressDtoToAddress() {
        AddressDTO dto = new AddressDTO("Main Street", "Colombo", "00100");

        var address = modelMapper.map(dto, com.sliit.ecommerce.Entitys.Address.class);

        assertEquals("Main Street", address.getStreet());
        assertEquals("Colombo", address.getCity());
        assertEquals("00100", address.getPostalCode());
    }

    @Test
    void mapsProductCommonFieldsToProductDto() {
        Category category = new Category();
        category.setCategoryId("CAT010");
        category.setCategoryName("Laptops");

        Product product = new Product();
        product.setProductId("P001");
        product.setName("Gaming Laptop");
        product.setPrice(new BigDecimal("2999.99"));
        product.setBrand("ASUS");
        product.setStockQty(7);
        product.setImages(List.of("laptop.jpg"));
        product.setCategory(category);

        ProductDTO dto = modelMapper.map(product, ProductDTO.class);

        assertNotNull(dto);
        assertEquals(1L, dto.getProductId());
        assertEquals("Gaming Laptop", dto.getName());
        assertEquals(new BigDecimal("2999.99"), dto.getPrice());
        assertEquals(7, dto.getStockQty());
    }
}
