package com.northwind.enterprise;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Test only: a stand-in for NorthWind's billing service, over real HTTP. */
public final class BillingStub {

    private static final Map<String, String> INVOICES = Map.of(
            "INV-7001", "{\"number\":\"INV-7001\",\"customerId\":\"C-1001\",\"amount\":39.99,\"status\":\"PAID\"}",
            "INV-7002", "{\"number\":\"INV-7002\",\"customerId\":\"C-1001\",\"amount\":189.00,\"status\":\"PAID\"}",
            "INV-7003", "{\"number\":\"INV-7003\",\"customerId\":\"C-1002\",\"amount\":59.00,\"status\":\"UNPAID\"}");

    public static final List<String> refundsReceived = new CopyOnWriteArrayList<>();
    private static final AtomicInteger references = new AtomicInteger();
    private static HttpServer server;

    private BillingStub() {
    }

    public static synchronized String start() throws IOException {
        if (server == null) {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/invoices/", exchange -> {
                String number = exchange.getRequestURI().getPath().substring("/invoices/".length());
                String body = INVOICES.get(number);
                respond(exchange, body == null ? 404 : 200, body == null ? "" : body);
            });
            server.createContext("/refunds", exchange -> {
                refundsReceived.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                respond(exchange, 200, "{\"reference\":\"R-" + (1000 + references.incrementAndGet()) + "\"}");
            });
            server.start();
        }
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }
}
