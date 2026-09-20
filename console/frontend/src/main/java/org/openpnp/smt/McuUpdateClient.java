package org.openpnp.smt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Talks only to Python; the panel never connects to the flasher or grants maintenance. */
public final class McuUpdateClient {
    private final String host;
    private final int port;

    public McuUpdateClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public JsonObject check() throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            long deadline = System.nanoTime() + 15_000_000_000L;
            JsonObject hello = new JsonObject();
            hello.addProperty("type", "hello");
            hello.addProperty("protocol_version", 1);
            hello.addProperty("client_name", "smt-swing-console");
            JsonObject greeting = exchange(socket, hello, "hello", deadline);
            if (!greeting.has("protocol_version") || greeting.get("protocol_version").getAsInt() != 1) {
                throw new IOException("Controller protocol mismatch");
            }
            JsonObject request = new JsonObject();
            request.addProperty("type", "check_mcu_update");
            JsonObject result = exchange(socket, request, "mcu_update", deadline);
            if (!result.has("update") || !result.get("update").isJsonObject()) {
                throw new IOException("Missing MCU update status");
            }
            return result.getAsJsonObject("update");
        }
        catch (IllegalStateException | IllegalArgumentException e) {
            throw new IOException("Invalid controller response", e);
        }
    }

    private JsonObject exchange(Socket socket, JsonObject request, String expectedType,
            long deadline) throws IOException {
        String id = UUID.randomUUID().toString();
        request.addProperty("request_id", id);
        socket.getOutputStream().write((request.toString() + "\n").getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
        InputStream input = socket.getInputStream();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        while (bytes.size() < 8192) {
            long remaining = (deadline - System.nanoTime()) / 1_000_000L;
            if (remaining <= 0) {
                throw new IOException("Controller request timed out");
            }
            socket.setSoTimeout((int) Math.max(1, remaining));
            int b = input.read();
            if (b < 0) {
                throw new IOException("Controller disconnected");
            }
            if (b == '\n') {
                try {
                    JsonObject response = new JsonParser().parse(
                            new String(bytes.toByteArray(), StandardCharsets.UTF_8)).getAsJsonObject();
                    if (!response.has("request_id") || !id.equals(response.get("request_id").getAsString())) {
                        throw new IOException("Controller response ID mismatch");
                    }
                    if (response.has("error")) {
                        throw new IOException(response.get("error").toString());
                    }
                    if (!response.has("type") || !expectedType.equals(response.get("type").getAsString())) {
                        throw new IOException("Controller response type mismatch");
                    }
                    return response;
                }
                catch (RuntimeException e) {
                    throw new IOException("Malformed controller response", e);
                }
            }
            bytes.write(b);
        }
        throw new IOException("Controller response exceeds frame limit");
    }
}
