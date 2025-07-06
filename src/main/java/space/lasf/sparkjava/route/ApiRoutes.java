package space.lasf.sparkjava.route;

import space.lasf.sparkjava.controller.ControllerInterface;
import space.lasf.sparkjava.dto.CrawlerDto;
import space.lasf.sparkjava.exception.InvalidRequestException;
import space.lasf.sparkjava.exception.ResourceNotFoundException;
import space.lasf.sparkjava.exception.ServerConfigurationException;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import static space.lasf.sparkjava.helper.RequestUtil.getBodyKeyword;
import static space.lasf.sparkjava.helper.RequestUtil.getParamId;
import static spark.Spark.*;

/**
 * A utility class for defining all the API routes for the application.
 */
public final class ApiRoutes {

    private static final Logger LOG = LoggerFactory.getLogger(ApiRoutes.class);
    private static final String ENV_BASE_URL = "BASE_URL";
    private static final Gson GSON = new Gson();

    private ApiRoutes() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Defines all the Spark routes and exception mappings for the web crawler service.
     *
     * @param controller      The controller that will handle the requests.
     * @param executorService The service for running background tasks.
     */
    public static void defineRoutes(ControllerInterface<CrawlerDto> controller, ExecutorService executorService) {
        setupFilters();
        setupCrawlerEndpoints(controller, executorService);
        setupExceptionHandlers();
    }

    /**
     * Sets up the before and after filters for all routes.
     */
    private static void setupFilters() {
        before("/*", (q, a) -> LOG.info("Received api call"));
        after((request, response) -> LOG.info("Responded to api call [{} {}]", request.requestMethod(), request.uri()));
    }

    /**
     * Sets up the main API endpoints (GET, POST).
     */
    private static void setupCrawlerEndpoints(ControllerInterface<CrawlerDto> controller, ExecutorService executorService) {
        post("/crawl", (req, res) -> {
            String baseUrl = System.getenv(ENV_BASE_URL);
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new ServerConfigurationException("Server configuration error: BASE_URL environment variable not set.");
            }
            res.type("application/json");
            String keyword = getBodyKeyword(req, GSON);
            LOG.info("Received crawl request for keyword: '{}'", keyword);
            CrawlerDto crawler = controller.create(keyword);
            LOG.info("Created crawl request: {}", crawler.getId());
            LOG.info("...Start processing");
            executorService.submit(() -> controller.process(baseUrl, crawler.getId()));
            return Map.of("id", crawler.getId());
        }, GSON::toJson);

        get("/crawl/:id", (req, res) -> {
            LOG.info("Received fetch request for id: {}", req.params(":id"));
            res.type("application/json");
            return controller.findById(getParamId(req));
        }, GSON::toJson);

        get("/crawl", (req, res) -> {
            LOG.info("Received fetch all requests call");
            res.type("application/json");
            return controller.findAll();
        }, GSON::toJson);
    }

    /**
     * Sets up the exception handlers for the application.
     */
    private static void setupExceptionHandlers() {
        notFound((req, res) -> GSON.toJson(Map.of("message", "Custom 404 - Not Found")));
        internalServerError((req, res) -> GSON.toJson(Map.of("message", "Custom 500 - Internal Server Error")));

        exception(ResourceNotFoundException.class, (e, req, res) -> {
            res.status(404);
            res.body(GSON.toJson(Map.of("error", e.getMessage())));
        });
        exception(InvalidRequestException.class, (e, req, res) -> {
            LOG.warn("Invalid request for [{} {}]: {}", req.requestMethod(), req.uri(), e.getMessage());
            res.status(400);
            res.body(GSON.toJson(Map.of("error", e.getMessage())));
        });
        exception(ServerConfigurationException.class, (e, req, res) -> {
            LOG.error("Server configuration error on request [{} {}]: {}", req.requestMethod(), req.uri(), e.getMessage());
            res.status(500);
            res.body(GSON.toJson(Map.of("error", e.getMessage())));
        });
        exception(Exception.class, (e, req, res) -> {
            LOG.error("Unexpected error processing request [{} {}]", req.requestMethod(), req.uri(), e);
            res.status(500);
            res.body(GSON.toJson(Map.of("error", "An unexpected server error occurred.")));
        });
    }
}