package com.srFoodDelivery.model;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

public class PaymentSession implements Serializable {

	@Serial
	private static final long serialVersionUID = 2L; // Updated to invalidate old sessions after adding new fields

	private final String deliveryAddress;
	private final String specialInstructions;
	private final String deliveryLocation;
	private final BigDecimal discountAmount;
	private final String appliedCoupon;
	private final Long preorderSlotId;

	private String selectedPaymentOption = "UPI";
	private String upiId;
	private String cardHolderName;
	private String cardNumber;
	private String cardExpiry;
	private String cardCvv;

	public PaymentSession(String deliveryAddress, String specialInstructions) {
		this.deliveryAddress = deliveryAddress;
		this.specialInstructions = specialInstructions;
		this.deliveryLocation = null;
		this.discountAmount = BigDecimal.ZERO;
		this.appliedCoupon = null;
		this.preorderSlotId = null;
	}

	public PaymentSession(String deliveryAddress, String specialInstructions, 
	                      String deliveryLocation, BigDecimal discountAmount, String appliedCoupon) {
		this.deliveryAddress = deliveryAddress;
		this.specialInstructions = specialInstructions;
		this.deliveryLocation = deliveryLocation;
		this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
		this.appliedCoupon = appliedCoupon;
		this.preorderSlotId = null;
	}

	public PaymentSession(String deliveryAddress, String specialInstructions, 
	                      String deliveryLocation, BigDecimal discountAmount, String appliedCoupon, Long preorderSlotId) {
		this.deliveryAddress = deliveryAddress;
		this.specialInstructions = specialInstructions;
		this.deliveryLocation = deliveryLocation;
		this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
		this.appliedCoupon = appliedCoupon;
		this.preorderSlotId = preorderSlotId;
	}

	public String getDeliveryAddress() {
		return deliveryAddress;
	}

	public String getSpecialInstructions() {
		return specialInstructions;
	}

	public String getSelectedPaymentOption() {
		return selectedPaymentOption;
	}

	public void setSelectedPaymentOption(String selectedPaymentOption) {
		this.selectedPaymentOption = selectedPaymentOption;
	}

	public String getUpiId() {
		return upiId;
	}

	public void setUpiId(String upiId) {
		this.upiId = upiId;
	}

	public String getCardHolderName() {
		return cardHolderName;
	}

	public void setCardHolderName(String cardHolderName) {
		this.cardHolderName = cardHolderName;
	}

	public String getCardNumber() {
		return cardNumber;
	}

	public void setCardNumber(String cardNumber) {
		this.cardNumber = cardNumber;
	}

	public String getCardExpiry() {
		return cardExpiry;
	}

	public void setCardExpiry(String cardExpiry) {
		this.cardExpiry = cardExpiry;
	}

	public String getCardCvv() {
		return cardCvv;
	}

	public void setCardCvv(String cardCvv) {
		this.cardCvv = cardCvv;
	}

	public String getDeliveryLocation() {
		return deliveryLocation;
	}

	public BigDecimal getDiscountAmount() {
		return discountAmount;
	}

	public String getAppliedCoupon() {
		return appliedCoupon;
	}

	public Long getPreorderSlotId() {
		return preorderSlotId;
	}
}
