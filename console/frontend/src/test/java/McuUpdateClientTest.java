import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.openpnp.smt.McuUpdateClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class McuUpdateClientTest {
    private JsonObject request(BufferedReader reader) throws Exception {
        return new JsonParser().parse(reader.readLine()).getAsJsonObject();
    }

    private void reply(Socket socket, JsonObject request, String type, boolean wrongId) throws Exception {
        JsonObject response = new JsonObject();
        response.addProperty("request_id", wrongId ? "wrong-id" : request.get("request_id").getAsString());
        response.addProperty("type", type);
        response.addProperty("protocol_version", 1);
        JsonObject update = new JsonObject();
        update.addProperty("can_update", false);
        update.addProperty("state", "blocked");
        response.add("update", update);
        byte[] bytes = (response.toString() + "\n").getBytes(StandardCharsets.UTF_8);
        // Deliberately split the frame; TCP is not a message transport.
        socket.getOutputStream().write(bytes, 0, 5);
        socket.getOutputStream().flush();
        socket.getOutputStream().write(bytes, 5, bytes.length - 5);
        socket.getOutputStream().flush();
    }

    @Test public void handshakeThenReadOnlyUpdateQuery() throws Exception {
        try (ServerSocket listener = new ServerSocket(0)) {
            CompletableFuture<Void> server = CompletableFuture.runAsync(() -> {
                try (Socket socket = listener.accept()) {
                    socket.setSoTimeout(3000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    JsonObject hello = request(reader);
                    assertEquals("hello", hello.get("type").getAsString());
                    reply(socket, hello, "hello", false);
                    JsonObject query = request(reader);
                    assertEquals("check_mcu_update", query.get("type").getAsString());
                    reply(socket, query, "mcu_update", false);
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            JsonObject update = new McuUpdateClient("127.0.0.1", listener.getLocalPort()).check();
            assertFalse(update.get("can_update").getAsBoolean());
            server.get(5, TimeUnit.SECONDS);
        }
    }

    @Test public void mismatchedResponseIsRejected() throws Exception {
        try (ServerSocket listener = new ServerSocket(0)) {
            CompletableFuture<Void> server = CompletableFuture.runAsync(() -> {
                try (Socket socket = listener.accept()) {
                    socket.setSoTimeout(3000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    reply(socket, request(reader), "hello", true);
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            assertThrows(java.io.IOException.class, () -> new McuUpdateClient("127.0.0.1", listener.getLocalPort()).check());
            server.get(5, TimeUnit.SECONDS);
        }
    }

    // Used by the local three-process smoke check; invokes the same client as the Swing action.
    public static void main(String[] args) throws Exception {
        JsonObject result = new McuUpdateClient("127.0.0.1", Integer.parseInt(args[0])).check();
        if (!result.has("code") || !"MAINTENANCE_UNAVAILABLE".equals(result.get("code").getAsString())
                || result.get("can_update").getAsBoolean() || !result.has("task")) {
            throw new IllegalStateException("Go/Python readiness chain did not return the expected blocked state: " + result);
        }
        System.out.println(result);
    }
}
