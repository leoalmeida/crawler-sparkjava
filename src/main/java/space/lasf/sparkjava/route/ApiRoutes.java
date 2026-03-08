package space.lasf.sparkjava.route;

import static space.lasf.sparkjava.helper.RequestUtil.getBodyKeyword;
import static space.lasf.sparkjava.helper.RequestUtil.getParamId;
import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.internalServerError;
import static spark.Spark.notFound;
import static spark.Spark.post;

import com.google.gson.Gson;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.lasf.sparkjava.controller.ControllerInterface;
import space.lasf.sparkjava.dto.CrawlerDto;
import space.lasf.sparkjava.exception.InvalidRequestException;
import space.lasf.sparkjava.exception.ResourceNotFoundException;
import space.lasf.sparkjava.exception.ServerConfigurationException;

/**
 * A utility class for defining all the API routes for the application.
 */
public final class ApiRoutes {

    private static final Logger LOG = LoggerFactory.getLogger(ApiRoutes.class);
    private static final String ENV_BASE_URL = "BASE_URL";
    private static final Gson GSON = new Gson();
    private static final int HTTP_STATUS_NOT_FOUND = 404;
    private static final int HTTP_STATUS_BAD_REQUEST = 400;
    private static final int HTTP_STATUS_INTERNAL_SERVER_ERROR = 500;

    private ApiRoutes() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Defines all the Spark routes and exception mappings for the web crawler service.
     *
     * @param controller      The controller that will handle the requests.
     * @param executorService The service for running background tasks.
     */
    public static void defineRoutes(
            final ControllerInterface<CrawlerDto> controller, final ExecutorService executorService) {
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
    private static void setupCrawlerEndpoints(
            final ControllerInterface<CrawlerDto> controller, final ExecutorService executorService) {
        registerPostCrawl(controller, executorService);
        registerGetCrawlById(controller);
        registerGetCrawls(controller);
    }

    private static void registerPostCrawl(
            final ControllerInterface<CrawlerDto> controller,
            final ExecutorService executorService) {
        post(
                "/crawl",
                (req, res) -> {
                    String baseUrl = resolveBaseUrl();
                    if (baseUrl == null || baseUrl.isBlank()) {
                        throw new ServerConfigurationException(
                                "Server configuration error: BASE_URL environment variable not set.");
                    }

                    res.type("application/json");
                    String keyword = getBodyKeyword(req, GSON);
                    CrawlerDto crawler = controller.create(keyword);
                    executorService.submit(() -> controller.process(baseUrl, crawler.getId()));
                    return Map.of("id", crawler.getId());
                },
                GSON::toJson);
    }

    private static void registerGetCrawlById(final ControllerInterface<CrawlerDto> controller) {
        get(
                "/crawl/:id",
                (req, res) -> {
                    res.type("application/json");
                    return controller.findById(getParamId(req));
                },
                GSON::toJson);
    }

    private static void registerGetCrawls(final ControllerInterface<CrawlerDto> controller) {
        get(
                "/crawl",
                (req, res) -> {
                    res.type("application/json");
                    return controller.findAll();
                },
                GSON::toJson);
    }

    private static String resolveBaseUrl() {
        return Optional.ofNullable(System.getenv(ENV_BASE_URL))
                .filter(value -> !value.isBlank())
                .orElse(System.getProperty(ENV_BASE_URL));
    }

    /**
     * Sets up the exception handlers for the application.
     */
    private static void setupExceptionHandlers() {
        notFound((req, res) -> GSON.toJson(Map.of("message", "Custom 404 - Not Found")));
        internalServerError((req, res) -> GSON.toJson(Map.of("message", "Custom 500 - Internal Server Error")));

        exception(ResourceNotFoundException.class, (e, req, res) -> {
            res.status(HTTP_STATUS_NOT_FOUND);
            res.body(GSON.toJson(Map.of("error", e.getMessage())));
        });
        exception(InvalidRequestException.class, (e, req, res) -> {
            LOG.warn("Invalid request for [{} {}]: {}", req.requestMethod(), req.uri(), e.getMessage());
            res.status(HTTP_STATUS_BAD_REQUEST);
            res.body(GSON.toJson(Map.of("error", e.getMessage())));
        });
        exception(ServerConfigurationException.class, (e, req, res) -> {
            LOG.error(
                    "Server configuration error on request [{} {}]: {}",
                    req.requestMethod(),
                    req.uri(),
                    e.getMessage());
            res.status(HTTP_STATUS_INTERNAL_SERVER_ERROR);
            res.body(GSON.toJson(Map.of("error", e.getMessage())));
        });
        exception(Exception.class, (e, req, res) -> {
            LOG.error("Unexpected error processing request [{} {}]", req.requestMethod(), req.uri(), e);
            res.status(HTTP_STATUS_INTERNAL_SERVER_ERROR);
            res.body(GSON.toJson(Map.of("error", "An unexpected server error occurred.")));
        });
    }
}
