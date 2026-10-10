package com.northwind.catalog.web;

import com.northwind.catalog.domain.Product;
import com.northwind.catalog.service.ProductCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/** A test slice: only the web layer is started; the catalog is a Mockito mock. */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    ProductCatalog catalog;

    @Test
    void returnsProductBySku() {
        given(catalog.findBySku("TENT-2P")).willReturn(Optional.of(
                new Product("TENT-2P", "Ridgeline 2-Person Tent", "tents", new BigDecimal("189.00"), 14)));

        assertThat(mvc.get().uri("/api/products/TENT-2P"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.name").isEqualTo("Ridgeline 2-Person Tent");
    }

    @Test
    void unknownSkuIsAProblemDetail404() {
        given(catalog.findBySku("NOPE")).willReturn(Optional.empty());

        assertThat(mvc.get().uri("/api/products/NOPE"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.title").isEqualTo("Product not found");
    }

    @Test
    void blankSearchTextIsRejected() {
        assertThat(mvc.post().uri("/api/products/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"text": "", "inStockOnly": true}"""))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}
