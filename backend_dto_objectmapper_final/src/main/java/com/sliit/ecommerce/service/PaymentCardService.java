package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.PaymentCard;
import com.sliit.ecommerce.dto.PaymentCardRequest;
import com.sliit.ecommerce.dto.PaymentCardResponse;
import com.sliit.ecommerce.repository.PaymentCardRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;

    public PaymentCardService(
            PaymentCardRepository paymentCardRepository
    ) {
        this.paymentCardRepository =
                paymentCardRepository;
    }


    // =====================================================
    // GET CARDS

    public List<PaymentCardResponse> getCards(String customerId) {

        List<PaymentCard> cards = paymentCardRepository.findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customerId);

        List<PaymentCardResponse> responseList = new ArrayList<>();

        for (PaymentCard card : cards) {
            responseList.add(convertToResponse(card));

        }

        return responseList;
    }


    public PaymentCardResponse saveCard(PaymentCardRequest request) {

        validateRequest(request);

        String customerId = request.getCustomerId().trim();

        String cardNumber = cleanCardNumber(request.getCardNumber());


        if (!isValidCardNumber(cardNumber)) {
            throw new RuntimeException("Invalid card number.");

        }

        validateExpiry(request.getExpiryMonth(), request.getExpiryYear());

        String lastFour = cardNumber.substring(cardNumber.length() - 4);

        String cardBrand = detectCardBrand(cardNumber);

        String maskedNumber = createMaskedNumber(lastFour);

        List<PaymentCard> existingCards = paymentCardRepository.findByCustomerId(customerId);

        PaymentCard card = new PaymentCard();

        card.setCardId(generateCardId());

        card.setCustomerId(customerId);

        card.setCardBrand(cardBrand);

        card.setMaskedCardNumber(maskedNumber);

        card.setLastFour(lastFour);

        card.setCardHolderName(request.getCardHolderName().trim());

        card.setExpiryMonth(normalizeMonth(request.getExpiryMonth()));

        card.setExpiryYear(request.getExpiryYear().trim());

        card.setCreatedAt(LocalDateTime.now());


        if (existingCards.isEmpty()) {

            card.setDefault(true);

        } else {

            card.setDefault(false);

        }


        PaymentCard savedCard = paymentCardRepository.save(card);
        return convertToResponse(savedCard);
    }


    public void deleteCard(String cardId) {

        PaymentCard card = paymentCardRepository.findById(cardId).orElseThrow(() -> new RuntimeException("Payment card not found."));

        boolean wasDefault = card.isDefault();

        String customerId = card.getCustomerId();

        paymentCardRepository.delete(card);

        if (wasDefault) {

            List<PaymentCard> remainingCards = paymentCardRepository.findByCustomerId(customerId);

            if (!remainingCards.isEmpty()) {

                PaymentCard nextCard = remainingCards.get(0);

                nextCard.setDefault(true);

                paymentCardRepository.save(nextCard);

            }

        }

    }



    public PaymentCardResponse setDefaultCard(String cardId) {

        PaymentCard selectedCard = paymentCardRepository.findById(cardId).orElseThrow(() -> new RuntimeException("Payment card not found."));

        String customerId = selectedCard.getCustomerId();

        List<PaymentCard> cards = paymentCardRepository.findByCustomerId(customerId);

        for (PaymentCard card : cards) {

            if (
                    card.getCardId().equals(cardId)
            ) {

                card.setDefault(true);

            } else {

                card.setDefault(false);

            }

            paymentCardRepository.save(card);

        }


        return convertToResponse(
                selectedCard
        );
    }



    // VALIDATION
    private void validateRequest(PaymentCardRequest request) {

        if (request == null) {

            throw new RuntimeException("Payment card request cannot be empty.");

        }


        if (
                request.getCustomerId() == null || request.getCustomerId().trim().isEmpty()
        ) {

            throw new RuntimeException("Customer ID is required.");

        }


        if (
                request.getCardHolderName() == null || request.getCardHolderName().trim().isEmpty()
        ) {

            throw new RuntimeException("Card holder name is required.");

        }


        if (
                request.getCardNumber() == null || request.getCardNumber().trim().isEmpty()
        ) {

            throw new RuntimeException("Card number is required.");

        }


        if (
                request.getExpiryMonth() == null || request.getExpiryMonth().trim().isEmpty()
        ) {

            throw new RuntimeException("Expiry month is required.");

        }


        if (
                request.getExpiryYear() == null || request.getExpiryYear().trim().isEmpty()
        ) {

            throw new RuntimeException("Expiry year is required.");

        }

    }



    // CARD NUMBER CLEANING
    private String cleanCardNumber(String cardNumber) {

        return cardNumber.replaceAll("\\D", "");

    }


    // LUHN VALIDATION

    private boolean isValidCardNumber(String cardNumber) {

        if (
                cardNumber.length() < 13 || cardNumber.length() > 19
        ) {

            return false;

        }


        int total = 0;

        boolean doubleDigit = false;


        for (int index = cardNumber.length() - 1; index >= 0; index--) {

            int digit = Character.getNumericValue(cardNumber.charAt(index));

            if (doubleDigit) {

                digit = digit * 2;

                if (digit > 9) {

                    digit = digit - 9;

                }

            }

            total += digit;

            doubleDigit = !doubleDigit;

        }

        return total % 10 == 0;
    }


    // EXPIRY VALIDATION
    private void validateExpiry(String month, String year) {

        String normalizedMonth = normalizeMonth(month);

        String normalizedYear = year.trim();

        int monthNumber;


        try {

            monthNumber = Integer.parseInt(normalizedMonth);

        } catch (Exception e) {

            throw new RuntimeException("Invalid expiry month.");

        }


        if (
                monthNumber < 1 || monthNumber > 12
        ) {

            throw new RuntimeException("Invalid expiry month.");

        }


        int yearNumber;

        try {

            yearNumber = Integer.parseInt(normalizedYear);

        } catch (Exception e) {

            throw new RuntimeException("Invalid expiry year.");

        }


        if (
                normalizedYear.length() == 2
        ) {

            yearNumber += 2000;

        }

        LocalDate currentDate = LocalDate.now();

        int currentMonth = currentDate.getMonthValue();

        int currentYear = currentDate.getYear();

        if (
                yearNumber < currentYear
        ) {

            throw new RuntimeException("Card has expired.");

        }


        if (
                yearNumber == currentYear && monthNumber < currentMonth
        ) {

            throw new RuntimeException("Card has expired.");

        }

    }


    // NORMALIZE MONTH

    private String normalizeMonth(String month) {

        String value = month.trim();

        int number;

        try {

            number = Integer.parseInt(value);

        } catch (Exception e) {
            throw new RuntimeException("Invalid expiry month.");

        }


        if (
                number < 1 || number > 12
        ) {

            throw new RuntimeException("Invalid expiry month.");
        }


        return String.format("%02d", number);

    }


    // CARD BRAND
    private String detectCardBrand(String cardNumber) {

        if (cardNumber.startsWith("4")) {
            return "VISA";
        }

        if (cardNumber.matches("^5[1-5].*")) {
            return "MASTERCARD";
        }

        if (cardNumber.matches("^3[47].*")) {
            return "AMEX";
        }

        if (cardNumber.matches("^6.*")) {
            return "DISCOVER";
        }

        return "CARD";

    }


    // MASK CARD NUMBER
    private String createMaskedNumber(String lastFour) {
        return "**** **** **** " +
                lastFour;
    }


    // CREATE CARD ID
    private String generateCardId() {

        return "CARD-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

    }



    // ENTITY -> RESPONSE
    private PaymentCardResponse convertToResponse(PaymentCard card) {

        PaymentCardResponse response = new PaymentCardResponse();


        response.setCardId(card.getCardId());

        response.setCardBrand(card.getCardBrand());

        response.setMaskedCardNumber(card.getMaskedCardNumber());

        response.setLastFour(card.getLastFour());

        response.setExpiryMonth(card.getExpiryMonth());

        response.setExpiryYear(card.getExpiryYear());

        response.setCardHolderName(card.getCardHolderName());

        response.setDefault(card.isDefault());


        return response;

    }

}