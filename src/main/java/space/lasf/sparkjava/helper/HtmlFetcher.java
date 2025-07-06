package space.lasf.sparkjava.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A utility for fetching HTML content and extracting links without external libraries.
 */
public final class HtmlFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlFetcher.class);
    // A simple regex to find href attributes in <a> tags.
    // This is fragile and a proper HTML parser is always recommended.
    private static final Pattern LINK_PATTERN = Pattern.compile("(?i)<a\\s+(?:[^>]*?\\s+)?href=\"([^\"]*)\"");
    private static final String USER_AGENT = "BackendCrawler/1.0";
    private static final int TIMEOUT_MS = 5000; // 5 seconds

    private HtmlFetcher() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Fetches the HTML content from a given URL string.
     */
    public static String getHtmlContent(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Extracts all link URLs from an HTML string using regex.
     */
    public static List<String> getlinks(String html) {
        List<String> links = new ArrayList<>();
        Matcher matcher = LINK_PATTERN.matcher(html);
        while (matcher.find()) {
            String link = matcher.group(1).replace("../", "");
            if (!link.startsWith("mailto:") && !link.startsWith("javascript:") && !link.contains("#"))
                links.add(link);
        }
        return links;
    }

    /**
     * Resolves a potentially relative link against a base URL.
     */
    public static String resolve(String baseUrl, String link) {
        try {
            URI baseUri = new URI(baseUrl);
            URI resolvedUri = baseUri.resolve(link);
            // Clean up fragment (#) from the URL
            return new URI(resolvedUri.getScheme(), resolvedUri.getAuthority(), resolvedUri.getPath(),
                    resolvedUri.getQuery(), null).toString();
        } catch (URISyntaxException e) {
            LOGGER.warn("Could not resolve link '{}' against base '{}'. Reason: {}", link, baseUrl, e.getMessage());
            // Could not parse the URI, return an invalid string to be filtered out later
            return "";
        }
    }
}