package space.lasf.sparkjava.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import space.lasf.sparkjava.dao.DaoInterface;
import space.lasf.sparkjava.entity.Crawler;
import space.lasf.sparkjava.entity.Status;

class CrawlerHandlerTest {

    private static final String REQUEST_ID = "ABCD1234";

    private HttpServer server;
    private ExecutorService executor;
    private DaoInterface<Crawler> dao;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(4);
        dao = mock(DaoInterface.class);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void crawlResourceShouldAppendMatchedUrlsAndMarkDone() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            String html =
                    "<html>java <a href=\"/match\">match</a> <a href=\"/file.pdf\">pdf</a> <a href=\"https://other.site/out\">out</a></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.createContext("/match", exchange -> {
            String html = "<html>JAVA keyword here</html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort() + "/";
        when(dao.findById(REQUEST_ID)).thenReturn(new Crawler(REQUEST_ID, "java"));

        CrawlerHandler handler = new CrawlerHandler(dao, executor);
        handler.crawlResource(baseUrl, REQUEST_ID);

        ArgumentCaptor<List<String>> captured = ArgumentCaptor.forClass(List.class);
        verify(dao, atLeastOnce()).appendAll(eq(REQUEST_ID), captured.capture());

        List<String> flattened = new ArrayList<>();
        for (List<String> batch : captured.getAllValues()) {
            flattened.addAll(batch);
        }

        assertTrue(flattened.contains(baseUrl));
        assertTrue(flattened.contains(baseUrl + "match"));
        verify(dao).changeStatus(REQUEST_ID, Status.DONE);
        verify(dao, never()).changeStatus(REQUEST_ID, Status.ERROR);
    }

    @Test
    void crawlResourceShouldStillMarkDoneWhenPageCannotBeFetched() {
        String unreachableBaseUrl = "http://localhost:1/";
        when(dao.findById(REQUEST_ID)).thenReturn(new Crawler(REQUEST_ID, "java"));

        CrawlerHandler handler = new CrawlerHandler(dao, executor);
        handler.crawlResource(unreachableBaseUrl, REQUEST_ID);

        verify(dao).changeStatus(REQUEST_ID, Status.DONE);
    }
}
