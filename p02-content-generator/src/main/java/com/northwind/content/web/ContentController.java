package com.northwind.content.web;

import com.northwind.content.copy.ProductBrief;
import com.northwind.content.copy.ProductCopy;
import com.northwind.content.copy.ProductCopyService;
import com.northwind.content.extract.OrderReference;
import com.northwind.content.extract.OrderReferenceExtractor;
import com.northwind.content.review.ReviewAnalysis;
import com.northwind.content.review.ReviewAnalyzer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ContentController {

    private final ProductCopyService copy;
    private final ReviewAnalyzer reviews;
    private final OrderReferenceExtractor orders;

    public ContentController(ProductCopyService copy, ReviewAnalyzer reviews, OrderReferenceExtractor orders) {
        this.copy = copy;
        this.reviews = reviews;
        this.orders = orders;
    }

    @PostMapping("/copy")
    public ProductCopy writeCopy(@Valid @RequestBody ProductBrief brief) {
        return copy.write(brief);
    }

    @PostMapping("/reviews/analyze")
    public ReviewAnalysis analyze(@Valid @RequestBody TextRequest request) {
        return reviews.analyze(request.text());
    }

    @PostMapping("/order-reference")
    public ResponseEntity<OrderReference> orderReference(@Valid @RequestBody TextRequest request) {
        return ResponseEntity.of(orders.extract(request.text()));      // 404 when there is none
    }

    /** Untrusted free text from a client: bounded in size before it reaches a prompt. */
    public record TextRequest(@NotBlank @Size(max = 4000) String text) {
    }
}
