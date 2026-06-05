package prm393.group8.flowermanagement.service;

import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import prm393.group8.flowermanagement.entity.CartItem;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.entity.Product;
import prm393.group8.flowermanagement.repository.CartItemRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;

    public CartServiceImpl(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    @Override
    public List<CartItem> getCartByUser(User user) {
        return cartItemRepository.findByUser(user);
    }

    @Override
    public CartItem addToCart(User user, Product product, int quantity) {
        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserAndProduct(user, product);
        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantity;
            existingItem.setQuantity(newQuantity);
            return cartItemRepository.save(existingItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            return cartItemRepository.save(cartItem);
        }
    }

    @Override
    public CartItem updateCart(User user, Product product, int quantity) {
        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserAndProduct(user, product);
        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(quantity);
            return cartItemRepository.save(existingItem);
        }
        return null;
    }

    @Override
    public void removeFromCart(User user, Product product) {
        cartItemRepository.deleteByUserAndProduct(user, product);
    }

    @Override
    public void clearCart(User user) {
        cartItemRepository.deleteByUser(user);
    }
}
