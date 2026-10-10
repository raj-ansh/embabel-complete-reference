package com.northwind.catalog.web;

import com.northwind.catalog.domain.Product;
import com.northwind.catalog.service.ProductCatalog;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductCatalog catalog;

    public ProductController(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/{sku}")
    public Product bySku(@PathVariable String sku) {
        return catalog.findBySku(sku).orElseThrow(() -> new ProductNotFoundException(sku));
    }

    @PostMapping("/search")
    public List<Product> search(@Valid @RequestBody SearchRequest request) {
        return catalog.search(request.text(), request.inStockOnly());
    }
}
