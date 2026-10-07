package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.PaymentCardRequest;
import com.sliit.ecommerce.dto.PaymentCardResponse;
import com.sliit.ecommerce.service.PaymentCardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments/cards")
public class PaymentCardController {

    private final PaymentCardService paymentCardService;


    public PaymentCardController(PaymentCardService paymentCardService) {

        this.paymentCardService = paymentCardService;

    }



    @GetMapping
    public ResponseEntity<?> getCards(@RequestParam String customerId) {

        try {

            if (
                    customerId == null || customerId.trim().isEmpty()
            ) {

                return ResponseEntity.badRequest().body("Customer ID is required.");

            }

            List<PaymentCardResponse> cards = paymentCardService.getCards(customerId);

            return ResponseEntity.ok(cards);


        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());

        }

    }



    @PostMapping
    public ResponseEntity<?> saveCard(@RequestBody PaymentCardRequest request) {

        try {

            PaymentCardResponse response = paymentCardService.saveCard(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);


        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        }

    }


    @DeleteMapping("/{cardId}")
    public ResponseEntity<?> deleteCard(@PathVariable String cardId) {

        try {

            paymentCardService.deleteCard(cardId);

            return ResponseEntity.ok("Payment card removed successfully.");

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }


    @PutMapping("/{cardId}/default")
    public ResponseEntity<?> setDefaultCard(@PathVariable String cardId) {

        try {

            PaymentCardResponse response = paymentCardService.setDefaultCard(cardId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        }

    }

}