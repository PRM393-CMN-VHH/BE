package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.entity.OrderDetail;

import java.util.List;

public interface OrderDetailService {
    OrderDetail saveOrderDetail(OrderDetail orderDetail);
    List<OrderDetail> getOrderDetailsByOrderId(int orderId);
}


