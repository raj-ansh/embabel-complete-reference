package com.northwind.mcpclient.security;

import java.util.function.Supplier;

/**
 * Chapter 88: the access token of the user this request is for, set at the request boundary. In a web
 * application it comes from Spring Security (the incoming bearer token, or a token obtained for the MCP
 * server through OAuth2AuthorizedClientManager); never from the model.
 */
public final class CurrentUserToken {

    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    private CurrentUserToken() {
    }

    public static <T> T runAs(String token, Supplier<T> work) {
        TOKEN.set(token);
        try {
            return work.get();
        } finally {
            TOKEN.remove();
        }
    }

    public static String get() {
        return TOKEN.get();
    }
}
