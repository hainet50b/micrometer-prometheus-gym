package com.programacho;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MicrometerPrometheusGymApplication {
    public static void main(String[] args) throws IOException {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        Random random = new Random();
        server.createContext("/timer", exchange -> {
            registry.timer("programacho.timer").record(() -> {
                sleep(random.nextInt(1_000));

                try (OutputStream os = exchange.getResponseBody()) {
                    exchange.sendResponseHeaders(200, 0);
                    os.write(new byte[]{});
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });

        server.createContext("/prometheus", exchange -> {
            try (OutputStream os = exchange.getResponseBody()) {
                String responseBody = registry.scrape();
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);

                exchange.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        });

        server.start();
    }

    private static void sleep(int timeout) {
        try {
            TimeUnit.MILLISECONDS.sleep(timeout);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
