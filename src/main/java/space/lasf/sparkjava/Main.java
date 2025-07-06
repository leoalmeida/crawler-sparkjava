package space.lasf.sparkjava;

import space.lasf.sparkjava.dao.CrawlerDao;
import space.lasf.sparkjava.dao.DaoInterface;
import space.lasf.sparkjava.dto.CrawlerDto;
import space.lasf.sparkjava.entity.Crawler;
import space.lasf.sparkjava.handler.CrawlerHandler;
import space.lasf.sparkjava.route.ApiRoutes;
import space.lasf.sparkjava.controller.ControllerInterface;
import space.lasf.sparkjava.controller.CrawlerControllerImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static spark.Spark.port;

/**
 * Main application class for the web crawler service.
 * Initializes dependencies and configures the SparkJava web server.
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final String ENV_PORT = "PORT";
    private static final int DEFAULT_PORT = 4567;

    private final ControllerInterface<CrawlerDto> crawlerController;
    private final ExecutorService executorService;

    public Main() {
        // Using a cached thread pool is more efficient than creating a new thread for each request.
        this.executorService = Executors.newCachedThreadPool();

        // --- Dependency Injection ---
        // Create and wire the application components.
        DaoInterface<Crawler> crawlerDao = new CrawlerDao();
        CrawlerHandler crawlerHandler = new CrawlerHandler(crawlerDao, this.executorService);
        this.crawlerController = new CrawlerControllerImpl(crawlerDao, crawlerHandler);
    }

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        // Configure Spark port from environment variable or use default
        int serverPort = Optional.ofNullable(System.getenv(ENV_PORT))
                .map(Integer::parseInt)
                .orElse(DEFAULT_PORT);
        port(serverPort);
        LOG.info("Server started on port {}", serverPort);

        ApiRoutes.defineRoutes(this.crawlerController, executorService);

        addShutdownHook();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutdown hook initiated. Shutting down ExecutorService...");
            executorService.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOG.warn("Executor did not terminate in the specified time. Forcing shutdown...");
                    executorService.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        LOG.error("Executor did not terminate even after forceful shutdown.");
                    }
                }
            } catch (InterruptedException ie) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            LOG.info("ExecutorService has been shut down.");
        }));
    }
}