package org.bsdevelopment.pluginutils.utilities;

import org.bsdevelopment.pluginutils.PluginUtilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Utility for making asynchronous HTTP requests.
 *
 * <p>All methods run the network call off the main thread and deliver the result
 * back on the Bukkit main thread via {@link PluginUtilities#getScheduler()}.
 *
 * <p>Example:
 * <pre>{@code
 * WebConnector.getInputStreamString("https://example.com/data", body -> {
 *     // runs on main thread
 *     Bukkit.broadcastMessage(body);
 * });
 * }</pre>
 */
public final class WebConnector {
    private static final int TIMEOUT_MS = 10_000;

    /**
     * Opens a connection to {@code url} and passes its {@link InputStream} to {@code callback}
     * on the main thread.
     *
     * @param url      the target URL
     * @param callback receives the response input stream
     */
    public static void getInputStream(String url, Consumer<InputStream> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpURLConnection connection = openConnection(url);
                PluginUtilities.getScheduler().runTask(() -> {
                    try {
                        callback.accept(connection.getInputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                PluginUtilities.getScheduler().runTask(() -> callback.accept(null));
            }
        });
    }

    /**
     * Opens a connection to {@code url} and passes its {@link OutputStream} to {@code callback}
     * on the main thread.
     *
     * @param url      the target URL
     * @param callback receives the connection output stream
     */
    public static void getOutputStream(String url, Consumer<OutputStream> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpURLConnection connection = openConnection(url);
                PluginUtilities.getScheduler().runTask(() -> {
                    try {
                        callback.accept(connection.getOutputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                PluginUtilities.getScheduler().runTask(() -> callback.accept(null));
            }
        });
    }

    /**
     * Fetches the full response body of {@code url} as a {@link String} and passes it to
     * {@code callback} on the main thread.
     *
     * @param url      the target URL
     * @param callback receives the response body string
     */
    public static void getInputStreamString(String url, Consumer<String> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpURLConnection connection = openConnection(url);
                String body = readStream(connection.getInputStream());
                PluginUtilities.getScheduler().runTask(() -> callback.accept(body));
            } catch (Exception e) {
                PluginUtilities.getScheduler().runTask(() -> callback.accept(null));
            }
        });
    }

    private static HttpURLConnection openConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/5.0");
        connection.addRequestProperty("Content-Encoding", "gzip");
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setDoInput(true);
        return connection;
    }

    private static String readStream(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
