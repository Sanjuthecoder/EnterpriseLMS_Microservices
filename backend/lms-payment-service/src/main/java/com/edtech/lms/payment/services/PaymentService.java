package com.edtech.lms.payment.services;

import com.edtech.lms.payment.dtos.CreateOrderRequestDto;
import com.edtech.lms.payment.dtos.OrderResponseDto;
import com.edtech.lms.payment.dtos.PaymentVerificationRequestDto;
import com.edtech.lms.payment.dtos.PaymentVerificationResponseDto;

public interface PaymentService {

    /**
     * Creates a new Razorpay order for premium subscription.
     * 
     * @param request the order creation details including amount and company ID
     * @return the created order details containing the Razorpay order ID
     */
    OrderResponseDto createOrder(CreateOrderRequestDto request);

    /**
     * Verifies the Razorpay payment signature after successful client-side payment.
     * 
     * @param request the payment verification details including signatures
     * @return a response indicating whether the signature verification was successful
     */
    PaymentVerificationResponseDto verifySignature(PaymentVerificationRequestDto request);

    /**
     * Checks if a specific company has already paid for a premium subscription.
     * 
     * @param companyId the unique identifier of the company
     * @return true if the company is a premium subscriber, false otherwise
     */
    boolean isCompanyPremium(Long companyId);
}
