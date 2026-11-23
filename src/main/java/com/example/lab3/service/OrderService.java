package com.example.lab3.service;

import com.example.lab3.dto.OrderCreateRequest;
import com.example.lab3.dto.OrderItemResponse;
import com.example.lab3.dto.OrderResponse;
import com.example.lab3.entity.CustomerOrder;
import com.example.lab3.entity.OrderItem;
import com.example.lab3.entity.Product;
import com.example.lab3.exception.ResourceNotFoundException;
import com.example.lab3.repository.CustomerOrderRepository;
import com.example.lab3.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(CustomerOrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public OrderResponse create(OrderCreateRequest request) {
        CustomerOrder order = new CustomerOrder(LocalDateTime.now());
        for (var itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produit introuvable avec l'id : " + itemRequest.productId()
                    ));
            order.addItem(new OrderItem(product, itemRequest.quantity(), product.getPrice()));
        }
        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable avec l'id : " + id));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    private OrderResponse toResponse(CustomerOrder order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();
        BigDecimal total = items.stream()
                .map(OrderItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderResponse(order.getId(), order.getOrderDate(), items, total);
    }
}
