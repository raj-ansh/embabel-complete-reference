package com.northwind.springbasics;

import com.northwind.springbasics.catalog.CatalogCache;
import com.northwind.springbasics.config.AppConfig;
import com.northwind.springbasics.domain.Order;
import com.northwind.springbasics.service.OrderPricingService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;

/** Starts a plain Spring container (no Spring Boot) and uses the beans it creates. */
public class SpringBasicsApp {

    public static void main(String[] args) {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            OrderPricingService pricing = context.getBean(OrderPricingService.class);
            CatalogCache catalog = context.getBean(CatalogCache.class);

            Order order = new Order("NW-10442", "TENT-2P", 2, new BigDecimal("189.00"));
            System.out.printf("%s x%d -> %s%n",
                    catalog.nameOf(order.sku()), order.quantity(), pricing.price(order));
        }   // closing the context runs @PreDestroy callbacks
    }
}
