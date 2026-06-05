package prm393.group8.flowermanagement.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * TransactionHistory - Lưu lịch sử giao dịch trong SESSION
 * Không lưu database, chỉ lưu session
 */
public class TransactionHistory implements Serializable {

    private int orderId;
    private double totalPrice;
    private String paymentMethod;
    private String orderStatus;
    private String paymentStatus;
    private LocalDateTime createdAt;

    public TransactionHistory() {
    }

    public TransactionHistory(int orderId, double totalPrice, String paymentMethod, String orderStatus, String paymentStatus, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.paymentMethod = paymentMethod;
        this.orderStatus = orderStatus;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


