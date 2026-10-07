package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.Deal;
import com.sliit.ecommerce.Entitys.Product;
import com.sliit.ecommerce.dto.DealCreateRequest;
import com.sliit.ecommerce.dto.DealDTO;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.DealRepository;
import com.sliit.ecommerce.repository.ProductRepository;
import com.sliit.ecommerce.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DealService {

    private final DealRepository dealRepository;
    private final ProductRepository productRepository;

    public DealService(DealRepository dealRepository, ProductRepository productRepository) {
        this.dealRepository = dealRepository;
        this.productRepository = productRepository;
    }

    public List<DealDTO> createDealsBulk(DealCreateRequest request) {
        java.util.Set<String> productIds = new java.util.HashSet<>();
        if (request.getProductId() != null && !request.getProductId().trim().isEmpty()) {
            productIds.add(request.getProductId());
        }
        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            productIds.addAll(request.getProductIds());
        }
        if (request.getCategoryId() != null && !request.getCategoryId().trim().isEmpty()) {
            List<Product> products = productRepository.findByCategory_CategoryId(request.getCategoryId());
            for (Product p : products) {
                productIds.add(p.getProductId());
            }
        }

        List<DealDTO> createdDeals = new java.util.ArrayList<>();
        List<String> existingIds = new java.util.ArrayList<>(dealRepository.findAll().stream().map(Deal::getDealId).toList());

        for (String pid : productIds) {
            Product product = productRepository.findById(pid)
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", pid));

            Deal deal = new Deal();
            deal.setDealId(IdGenerator.nextId("DEAL", existingIds));
            existingIds.add(deal.getDealId());

            deal.setProduct(product);
            deal.setDiscountPercentage(request.getDiscountPercentage());
            deal.setBadgeText(request.getBadgeText());
            deal.setStartDate(request.getStartDate());
            deal.setEndDate(request.getEndDate());

            createdDeals.add(toDTO(dealRepository.save(deal)));
        }
        return createdDeals;
    }

    public DealDTO createDeal(DealCreateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.of("Product", request.getProductId()));

        Deal deal = new Deal();
        deal.setDealId(IdGenerator.nextId("DEAL", dealRepository.findAll().stream().map(Deal::getDealId).toList()));
        deal.setProduct(product);
        deal.setDiscountPercentage(request.getDiscountPercentage());
        deal.setBadgeText(request.getBadgeText());
        deal.setStartDate(request.getStartDate());
        deal.setEndDate(request.getEndDate());

        return toDTO(dealRepository.save(deal));
    }

    public DealDTO updateDeal(String id, DealCreateRequest request) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Deal", id));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.of("Product", request.getProductId()));

        deal.setProduct(product);
        deal.setDiscountPercentage(request.getDiscountPercentage());
        deal.setBadgeText(request.getBadgeText());
        deal.setStartDate(request.getStartDate());
        deal.setEndDate(request.getEndDate());

        return toDTO(dealRepository.save(deal));
    }

    public void deleteDeal(String id) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Deal", id));
        dealRepository.delete(deal);
    }

    @Transactional(readOnly = true)
    public List<DealDTO> getAllDeals() {
        return dealRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    private DealDTO toDTO(Deal deal) {
        DealDTO dto = new DealDTO();
        dto.setDealId(deal.getDealId());
        dto.setDiscountPercentage(deal.getDiscountPercentage());
        dto.setBadgeText(deal.getBadgeText());
        dto.setStartDate(deal.getStartDate());
        dto.setEndDate(deal.getEndDate());
        if (deal.getProduct() != null) {
            dto.setProductId(deal.getProduct().getProductId());
            dto.setProductName(deal.getProduct().getName());
            dto.setProductOriginalPrice(deal.getProduct().getPrice());
            if (!deal.getProduct().getImages().isEmpty()) {
                dto.setProductImage(deal.getProduct().getImages().get(0));
            }
        }
        return dto;
    }
}
