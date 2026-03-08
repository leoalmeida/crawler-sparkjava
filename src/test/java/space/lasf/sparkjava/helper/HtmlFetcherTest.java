package space.lasf.sparkjava.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HtmlFetcherTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getlinksShouldExtractValidLinksAndIgnoreMailtoJavascriptAndFragments() {
        String html =
                """
            <html>
              <a href=\"/docs\">Docs</a>
              <a href=\"../api\">Api</a>
              <a href=\"mailto:test@example.com\">Mail</a>
              <a href=\"javascript:void(0)\">Js</a>
              <a href=\"/page#section\">Section</a>
            </html>
            """;

        List<String> links = HtmlFetcher.getlinks(html);

        assertEquals(2, links.size());
        assertTrue(links.contains("/docs"));
        assertTrue(links.contains("api"));
    }

    @Test
    void resolveShouldBuildAbsoluteUrlAndRemoveFragment() {
        String resolved = HtmlFetcher.resolve("https://example.com/base/", "../docs/page#part");

        assertEquals("https://example.com/docs/page", resolved);
    }

    @Test
    void getHtmlContentShouldFetchBodyFromHttpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/page", exchange -> {
            byte[] bytes = "<html><body>hello</body></html>".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        String url = "http://localhost:" + server.getAddress().getPort() + "/page";
        String html = HtmlFetcher.getHtmlContent(url);

        assertTrue(html.contains("hello"));
    }
}
