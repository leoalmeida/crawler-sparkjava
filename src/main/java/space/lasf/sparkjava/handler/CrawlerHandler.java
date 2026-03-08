package space.lasf.sparkjava.handler;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.lasf.sparkjava.dao.DaoInterface;
import space.lasf.sparkjava.entity.Crawler;
import space.lasf.sparkjava.entity.Status;
import space.lasf.sparkjava.helper.HtmlFetcher;
import spark.utils.StringUtils;

/**
 * Handles the logic of crawling a website for a specific keyword.
 * This implementation uses an iterative, breadth-first search (BFS) approach.
 */
public class CrawlerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerHandler.class);
    // REGEX to filter out common non-HTML file extensions.
    private static final Pattern FILTERS = Pattern.compile(
            ".*(\\.(css|js|gif|jpg|jpeg|png|mp3|mp4|zip|gz|pdf|xls|xlsx|doc|docx))$", Pattern.CASE_INSENSITIVE);

    private final DaoInterface<Crawler> dao;
    private final ExecutorService executorService;
    private final Semaphore semaphore;
    private static final int MAX_CONCURRENT_REQUESTS = 5;

    /**
     * Constructs a new CrawlerHandler with its dependencies.
     *
     * @param dao The data access object for managing crawler instances.
     * @param executorService The ExecutorService to used to execute requests in parallel.
     */
    public CrawlerHandler(final DaoInterface<Crawler> dao, final ExecutorService executorService) {
        this.dao = dao;
        this.executorService = executorService;
        this.semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);
    }

    private static final class CrawlContext {
        private String baseUrl;
        private String id;
        private String keyword;
        private Queue<String> frontier;
        private Set<String> visitedUrls;
        private Phaser phaser;
    }

    /**
     * Performs a breadth-first search (BFS) crawl starting from a base URL.
     *
     * @param baseUrl The starting URL for the crawl.
     * @param id      The ID of the crawl job to update.
     */
    public void crawlResource(final String baseUrl, final String id) {
        final Queue<String> frontier = new ConcurrentLinkedQueue<>();
        final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
        final Phaser phaser = new Phaser(1);
        final String keyword = dao.findById(id).getKeyword();

        frontier.add(baseUrl);
        visitedUrls.add(baseUrl);

        final CrawlContext context = new CrawlContext();
        context.baseUrl = baseUrl;
        context.id = id;
        context.keyword = keyword;
        context.frontier = frontier;
        context.visitedUrls = visitedUrls;
        context.phaser = phaser;

        try {
            runDispatchLoop(context);
        } catch (Exception e) {
            LOGGER.error("A critical error occurred during crawl for ID: {}", id, e);
            dao.changeStatus(id, Status.ERROR);
        } finally {
            dao.changeStatus(id, Status.DONE);
            LOGGER.info("Crawl finished for ID: {}. Visited {} pages.", id, visitedUrls.size());
        }
    }

    private void runDispatchLoop(final CrawlContext context) {
        while (!context.phaser.isTerminated()) {
            final String currentUrl = context.frontier.poll();
            if (currentUrl == null) {
                waitForMoreWork(context);
                continue;
            }

            submitPageTask(context, currentUrl);
        }
    }

    private void waitForMoreWork(final CrawlContext context) {
        context.phaser.arriveAndAwaitAdvance();
        if (context.frontier.isEmpty()) {
            context.phaser.forceTermination();
        }
    }

    private void submitPageTask(final CrawlContext context, final String currentUrl) {
        try {
            semaphore.acquire();
            context.phaser.register();
            executorService.submit(() -> runPageTask(context, currentUrl));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Crawl interrupted for ID: {}", context.id, e);
            dao.changeStatus(context.id, Status.ERROR);
            context.phaser.forceTermination();
        }
    }

    private void runPageTask(final CrawlContext context, final String currentUrl) {
        try {
            processPage(currentUrl, context);
        } catch (IOException e) {
            LOGGER.warn("Could not process URL [ID: {}]: {} - {}", context.id, currentUrl, e.getMessage());
        } finally {
            semaphore.release();
            context.phaser.arriveAndDeregister();
        }
    }

    /**
     * Fetches and processes a single page: finds the keyword and discovers new links.
     *
     * @param currentUrl   The URL of the page to process.
     * @param keyword      The keyword to search for.
     * @param id           The ID of the crawl job.
     * @param baseUrl      The base URL of the site to maintain scope.
     * @param frontier     The queue of URLs to visit next.
     * @param visitedUrls  The set of already visited URLs to prevent cycles.
     * @throws IOException if there is an error fetching the HTML content.
     */
    private void processPage(final String currentUrl, final CrawlContext context) throws IOException {
        String html = HtmlFetcher.getHtmlContent(currentUrl);

        if (html.toLowerCase().contains(context.keyword.toLowerCase())) {
            dao.appendAll(context.id, List.of(currentUrl));
        }

        List<String> links = HtmlFetcher.getlinks(html);
        for (String link : links) {
            String nextUrl = HtmlFetcher.resolve(context.baseUrl, link);

            if (isValid(nextUrl) && isInScope(nextUrl, context.baseUrl)) {
                if (context.visitedUrls.add(nextUrl)) {
                    context.frontier.add(nextUrl);
                }
            }
        }
    }

    /**
     * Checks if a given URL is within the scope of the original crawl (i.e., starts with the base URL).
     *
     * @param url     The URL to check.
     * @param baseUrl The base URL that defines the crawl scope.
     * @return {@code true} if the URL is in scope, {@code false} otherwise.
     */
    private boolean isInScope(final String url, final String baseUrl) {
        return url.startsWith(baseUrl);
    }

    /**
     * Checks if a URL is valid for crawling (i.e., not empty and does not match filtered extensions).
     * @param url The URL to validate.
     * @return {@code true} if the URL is valid, {@code false} otherwise.
     */
    private boolean isValid(final String url) {
        return StringUtils.hasLength(url) && !FILTERS.matcher(url).matches();
    }
}
