package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.entity.CartItem;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.entity.Product;

import java.util.List;

public interface CartService {
    List<CartItem> getCartByUser(User user);
    CartItem addToCart(User user, Product product, int quantity);
    CartItem updateCart(User user, Product product, int quantity);
    void removeFromCart(User user, Product product);
    void clearCart(User user);
}
