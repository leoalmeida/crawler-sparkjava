package space.lasf.sparkjava.helper;

import space.lasf.sparkjava.exception.InvalidRequestException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import spark.Request;

import java.util.Map;
import java.util.Optional;

/**
 * A utility class for parsing Spark {@link Request} objects.
 * This class is final and cannot be instantiated.
 */
public final class RequestUtil {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private RequestUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Extracts the 'id' parameter from the request path.
     *
     * @param req The Spark request object.
     * @return The 'id' path parameter.
     * @throws InvalidRequestException if the 'id' parameter is missing or blank.
     */
    public static String getParamId(Request req) {
        String id = req.params(":id");
        if (id == null || id.isBlank()) {
            throw new InvalidRequestException("Path parameter 'id' cannot be missing or blank.");
        }
        return id;
    }

    /**
     * Extracts the 'keyword' from the JSON request body.
     * Expects a JSON body in the format: {"keyword": "some_value"}.
     *
     * @param req The Spark request object.
     * @return The value of the 'keyword' field.
     * @throws InvalidRequestException if the request body is not valid JSON,
     *                                  or if the 'keyword' field is missing or blank.
     */
    public static String getBodyKeyword(Request req, Gson gson) {
        try {
            Map<String, String> bodyMap = gson.fromJson(req.body(), Map.class);

            return Optional.ofNullable(bodyMap)
                    .map(body -> body.get("keyword"))
                    .filter(keyword -> !keyword.isBlank())
                    .orElseThrow(() -> new InvalidRequestException("Request body must contain a non-empty 'keyword' field."));
        } catch (JsonSyntaxException e) {
            throw new InvalidRequestException("Invalid JSON format in request body.", e);
        }
    }
}