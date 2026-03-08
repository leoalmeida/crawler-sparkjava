package space.lasf.sparkjava.route;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import space.lasf.sparkjava.controller.ControllerInterface;
import space.lasf.sparkjava.dto.CrawlerDto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static spark.Spark.awaitInitialization;
import static spark.Spark.awaitStop;
import static spark.Spark.port;
import static spark.Spark.stop;

class ApiRoutesIntegrationTest {

    private static final Gson GSON = new Gson();
    private static final String BASE_URL_KEY = "BASE_URL";

    private ExecutorService executor;
    private int testPort;
    private FakeController fakeController;

    @BeforeEach
    void setUp() throws IOException {
        executor = Executors.newFixedThreadPool(2);
        testPort = findFreePort();
        port(testPort);

        fakeController = new FakeController();
        ApiRoutes.defineRoutes(fakeController, executor);
        awaitInitialization();
    }

    @AfterEach
    void tearDown() {
        stop();
        awaitStop();
        executor.shutdownNow();
        System.clearProperty(BASE_URL_KEY);
    }

    @Test
    void getCrawlByIdShouldReturnCrawlerJson() throws IOException {
        HttpResponse response = sendRequest("GET", "/crawl/ABCD1234", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"id\":\"ABCD1234\""));
        assertTrue(response.body().contains("\"status\":\"done\""));
    }

    @Test
    void getAllCrawlsShouldReturnListJson() throws IOException {
        HttpResponse response = sendRequest("GET", "/crawl", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().startsWith("["));
        assertTrue(response.body().contains("\"id\":\"ABCD1234\""));
    }

    @Test
    void postCrawlShouldReturn500WhenBaseUrlIsMissing() throws IOException {
        System.clearProperty(BASE_URL_KEY);
        String payload = GSON.toJson(Map.of("keyword", "spring"));

        HttpResponse response = sendRequest("POST", "/crawl", payload);

        assertEquals(500, response.statusCode());
        assertTrue(response.body().contains("BASE_URL environment variable not set"));
    }

    @Test
    void postCrawlShouldReturnIdAndTriggerBackgroundProcessWhenBaseUrlIsConfigured() throws Exception {
        String configuredBaseUrl = "http://localhost:9999/base";
        System.setProperty(BASE_URL_KEY, configuredBaseUrl);
        String payload = GSON.toJson(Map.of("keyword", "spring"));

        HttpResponse response = sendRequest("POST", "/crawl", payload);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"id\":\"CREATED01\""));
        assertTrue(fakeController.awaitProcess(2, TimeUnit.SECONDS));
        assertEquals(configuredBaseUrl, fakeController.getProcessedBase());
        assertEquals("CREATED01", fakeController.getProcessedId());
    }

    private HttpResponse sendRequest(String method, String path, String body) throws IOException {
        URL url = new URL("http://localhost:" + testPort + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");

        if (body != null) {
            connection.setDoOutput(true);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        int statusCode = connection.getResponseCode();
        InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String responseBody = readAll(stream);
        connection.disconnect();

        return new HttpResponse(statusCode, responseBody);
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    private record HttpResponse(int statusCode, String body) {
    }

    private static final class FakeController implements ControllerInterface<CrawlerDto> {

        private final CountDownLatch processLatch = new CountDownLatch(1);
        private volatile String processedBase;
        private volatile String processedId;

        @Override
        public void process(String base, String id) {
            this.processedBase = base;
            this.processedId = id;
            processLatch.countDown();
        }

        @Override
        public CrawlerDto create(String keyword) {
            CrawlerDto dto = new CrawlerDto();
            dto.setId("CREATED01");
            dto.setStatus("active");
            dto.setUrls(List.of());
            return dto;
        }

        @Override
        public CrawlerDto findById(String id) {
            CrawlerDto dto = new CrawlerDto();
            dto.setId(id);
            dto.setStatus("done");
            dto.setUrls(List.of("https://example.com"));
            return dto;
        }

        @Override
        public List<CrawlerDto> findAll() {
            CrawlerDto dto = new CrawlerDto();
            dto.setId("ABCD1234");
            dto.setStatus("active");
            dto.setUrls(List.of("https://example.com"));
            return List.of(dto);
        }

        boolean awaitProcess(long timeout, TimeUnit unit) throws InterruptedException {
            return processLatch.await(timeout, unit);
        }

        String getProcessedBase() {
            return processedBase;
        }

        String getProcessedId() {
            return processedId;
        }
    }
}
