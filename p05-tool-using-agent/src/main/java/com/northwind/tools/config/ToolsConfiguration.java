package com.northwind.tools.config;

import com.northwind.tools.catalog.CalculatorTools;
import com.northwind.tools.catalog.WeatherTools;
import com.northwind.tools.customer.CustomerTools;
import com.northwind.tools.customer.OrderTools;
import com.northwind.tools.data.ProductCatalogService;
import com.northwind.tools.data.InventoryReportTools;
import com.northwind.tools.data.ProductTools;
import com.northwind.tools.data.ReservationTools;
import com.northwind.tools.data.StockHoldService;
import com.northwind.tools.docs.FileSearchTools;
import com.northwind.tools.integration.CarrierClient;
import com.northwind.tools.integration.EmailTools;
import com.northwind.tools.integration.TrackingTools;
import com.northwind.tools.rules.PromotionRules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.nio.file.Path;
import java.util.Map;

/** Every tool object is an ordinary Spring bean, created with the collaborators it needs. */
@Configuration
public class ToolsConfiguration {

    @Bean CalculatorTools calculatorTools() { return new CalculatorTools(); }

    @Bean WeatherTools weatherTools() { return new WeatherTools(); }

    @Bean CustomerTools customerTools() { return new CustomerTools(); }

    @Bean OrderTools orderTools() { return new OrderTools(); }

    @Bean ProductTools productTools(ProductCatalogService catalog) { return new ProductTools(catalog); }

    @Bean TrackingTools trackingTools(CarrierClient carrier) { return new TrackingTools(carrier); }

    @Bean EmailTools emailTools() {
        return new EmailTools(Map.of("C-1001", "asha@example.com", "C-1002", "mateo@example.com"));
    }

    @Bean FileSearchTools policyLibraryTools(@Value("${northwind.policies.dir}") Path dir) {
        return new FileSearchTools(dir);
    }

    @Bean PromotionRules promotionRules() { return new PromotionRules(); }

    @Bean ReservationTools reservationTools(StockHoldService holds) { return new ReservationTools(holds); }

    @Bean InventoryReportTools inventoryReportTools(JdbcClient jdbc) { return new InventoryReportTools(jdbc); }
}
