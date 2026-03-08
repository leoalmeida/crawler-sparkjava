package space.lasf.sparkjava.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import space.lasf.sparkjava.dao.DaoInterface;
import space.lasf.sparkjava.dto.CrawlerDto;
import space.lasf.sparkjava.entity.Crawler;
import space.lasf.sparkjava.exception.InvalidRequestException;
import space.lasf.sparkjava.exception.ResourceNotFoundException;
import space.lasf.sparkjava.handler.CrawlerHandler;

class CrawlerControllerImplTest {

    @Mock
    private DaoInterface<Crawler> dao;

    @Mock
    private CrawlerHandler crawlerHandler;

    private CrawlerControllerImpl controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new CrawlerControllerImpl(dao, crawlerHandler);
    }

    @Test
    void createShouldThrowWhenKeywordIsInvalid() {
        assertThrows(InvalidRequestException.class, () -> controller.create(null));
        assertThrows(InvalidRequestException.class, () -> controller.create(""));
        assertThrows(InvalidRequestException.class, () -> controller.create("abc"));
        assertThrows(InvalidRequestException.class, () -> controller.create("a".repeat(33)));
        verify(dao, never()).create(anyString());
    }

    @Test
    void createShouldReturnMappedDtoWhenKeywordIsValid() {
        Crawler crawler = new Crawler("ABCD1234", "keyword");
        crawler.addLinks(List.of("https://example.com/page"));
        when(dao.create("keyword")).thenReturn(crawler);

        CrawlerDto dto = controller.create("keyword");

        assertNotNull(dto);
        assertEquals("ABCD1234", dto.getId());
        assertEquals("active", dto.getStatus());
        assertEquals(1, dto.getUrls().size());
    }

    @Test
    void processShouldDelegateToCrawlerHandler() {
        doNothing().when(crawlerHandler).crawlResource("https://base", "ABCD1234");

        controller.process("https://base", "ABCD1234");

        verify(crawlerHandler).crawlResource("https://base", "ABCD1234");
    }

    @Test
    void findByIdShouldValidateIdFormat() {
        assertThrows(InvalidRequestException.class, () -> controller.findById(null));
        assertThrows(InvalidRequestException.class, () -> controller.findById(""));
        assertThrows(InvalidRequestException.class, () -> controller.findById("short"));
        assertThrows(InvalidRequestException.class, () -> controller.findById("toolongid"));
        verify(dao, never()).findById(anyString());
    }

    @Test
    void findByIdShouldThrowWhenResourceNotFound() {
        when(dao.findById("ABCD1234")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> controller.findById("ABCD1234"));
    }

    @Test
    void findByIdShouldReturnMappedDtoWhenExists() {
        Crawler crawler = new Crawler("ABCD1234", "keyword");
        crawler.endProcess();
        when(dao.findById("ABCD1234")).thenReturn(crawler);

        CrawlerDto dto = controller.findById("ABCD1234");

        assertEquals("ABCD1234", dto.getId());
        assertEquals("done", dto.getStatus());
    }

    @Test
    void findAllShouldReturnMappedList() {
        Crawler one = new Crawler("AAAA1111", "one");
        Crawler two = new Crawler("BBBB2222", "two");
        two.errorProcess();
        when(dao.findAll()).thenReturn(List.of(one, two));

        List<CrawlerDto> all = controller.findAll();

        assertEquals(2, all.size());
        assertEquals("active", all.get(0).getStatus());
        assertEquals("error", all.get(1).getStatus());
    }
}
