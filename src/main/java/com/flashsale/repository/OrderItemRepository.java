package com.flashsale.repository;

import com.flashsale.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<Product, Long> {
}
