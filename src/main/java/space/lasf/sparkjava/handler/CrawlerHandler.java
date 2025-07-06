package space.lasf.sparkjava.handler;

import space.lasf.sparkjava.dao.DaoInterface;
import space.lasf.sparkjava.entity.Crawler;
import space.lasf.sparkjava.entity.Status;
import space.lasf.sparkjava.helper.HtmlFetcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.utils.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

/**
 * Handles the logic of crawling a website for a specific keyword.
 * This implementation uses an iterative, breadth-first search (BFS) approach.
 */
public class CrawlerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerHandler.class);
    // REGEX to filter out common non-HTML file extensions.
    private static final Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg|jpeg|png|mp3|mp4|zip|gz|pdf|xls|xlsx|doc|docx))$", Pattern.CASE_INSENSITIVE);
    
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
	public CrawlerHandler(DaoInterface<Crawler> dao, ExecutorService executorService) {
		this.dao = dao;
        this.executorService = executorService;
        this.semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);
	}

    /**
     * Performs a breadth-first search (BFS) crawl starting from a base URL.
     *
     * @param baseUrl The starting URL for the crawl.
     * @param id      The ID of the crawl job to update.
     */
    public void crawlResource(String baseUrl, String id) {
        // Use thread-safe collections for concurrent access
        ConcurrentLinkedQueue<String> frontier = new ConcurrentLinkedQueue<>();
        Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
        Phaser phaser = new Phaser(1); // Register the main dispatching loop

        String keyword = dao.findById(id).getKeyword();

        frontier.add(baseUrl);
        visitedUrls.add(baseUrl);

        try {
            while (!phaser.isTerminated()) {
                String currentUrl = frontier.poll();

                if (currentUrl != null) {
                    try{
                        semaphore.acquire();
                        phaser.register(); // Register a new page processing task
                        
                        executorService.submit(() -> {
                            try {
                                processPage(currentUrl, keyword, id, baseUrl, frontier, visitedUrls);
                            } catch (IOException e) {
                                LOGGER.warn("Could not process URL [ID: {}]: {} - {}", id, currentUrl, e.getMessage());
                            } finally {
                                semaphore.release();
                                phaser.arriveAndDeregister(); // Mark this task as complete
                            }
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOGGER.error("Crawl interrupted for ID: {}", id, e);
                        dao.changeStatus(id, Status.ERROR);
                        return;
                   }
                } else {
                    // The frontier is currently empty, wait for running tasks to potentially add more work.
                    phaser.arriveAndAwaitAdvance();

                    // If the frontier is still empty after waiting, all tasks are done.
                    if (frontier.isEmpty()) {
                        phaser.forceTermination();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("A critical error occurred during crawl for ID: {}", id, e);
            dao.changeStatus(id, Status.ERROR);
        } finally {
            dao.changeStatus(id, Status.DONE);
            LOGGER.info("Crawl finished for ID: {}. Visited {} pages.", id, visitedUrls.size());
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
    private void processPage(String currentUrl, String keyword, String id, String baseUrl, ConcurrentLinkedQueue<String> frontier, Set<String> visitedUrls) throws IOException {
        String html = HtmlFetcher.getHtmlContent(currentUrl);

        if (html.toLowerCase().contains(keyword.toLowerCase())) {
            dao.appendAll(id, List.of(currentUrl));
        }

        List<String> links = HtmlFetcher.getlinks(html);
        //LOGGER.info("{} links found for ID {}: ", links.size(),id);
        for (String link : links) {
            String nextUrl = HtmlFetcher.resolve(baseUrl, link);

            if (isValid(nextUrl) && isInScope(nextUrl, baseUrl)) {
                if (visitedUrls.add(nextUrl)) {
                    frontier.add(nextUrl);
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
    private boolean isInScope(String url, String baseUrl) {
        return url.startsWith(baseUrl);
    }

    /**
     * Checks if a URL is valid for crawling (i.e., not empty and does not match filtered extensions).
     * @param url The URL to validate.
     * @return {@code true} if the URL is valid, {@code false} otherwise.
     */
    private boolean isValid(String url) {
        return StringUtils.hasLength(url) && !FILTERS.matcher(url).matches();
    }
}
