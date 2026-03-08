package space.lasf.sparkjava.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import space.lasf.sparkjava.exception.InvalidRequestException;
import spark.Request;

class RequestUtilTest {

    private static final Gson GSON = new Gson();

    @Test
    void getParamIdShouldReturnIdWhenPresent() {
        Request request = mock(Request.class);
        when(request.params(":id")).thenReturn("ABCD1234");

        String id = RequestUtil.getParamId(request);

        assertEquals("ABCD1234", id);
    }

    @Test
    void getParamIdShouldThrowWhenMissing() {
        Request request = mock(Request.class);
        when(request.params(":id")).thenReturn("  ");

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () -> RequestUtil.getParamId(request));

        assertThat(ex.getMessage(), containsString("Path parameter 'id'"));
    }

    @Test
    void getBodyKeywordShouldReturnKeywordWhenBodyIsValid() {
        Request request = mock(Request.class);
        when(request.body()).thenReturn("{\"keyword\":\"spring\"}");

        String keyword = RequestUtil.getBodyKeyword(request, GSON);

        assertEquals("spring", keyword);
    }

    @Test
    void getBodyKeywordShouldThrowWhenKeywordIsBlankOrMissing() {
        Request blankKeywordRequest = mock(Request.class);
        when(blankKeywordRequest.body()).thenReturn("{\"keyword\":\"\"}");

        Request missingKeywordRequest = mock(Request.class);
        when(missingKeywordRequest.body()).thenReturn("{}");

        assertThrows(InvalidRequestException.class, () -> RequestUtil.getBodyKeyword(blankKeywordRequest, GSON));
        assertThrows(InvalidRequestException.class, () -> RequestUtil.getBodyKeyword(missingKeywordRequest, GSON));
    }

    @Test
    void getBodyKeywordShouldThrowWhenJsonIsInvalid() {
        Request request = mock(Request.class);
        when(request.body()).thenReturn("{invalid-json");

        assertThrows(InvalidRequestException.class, () -> RequestUtil.getBodyKeyword(request, GSON));
    }
}
