package space.lasf.sparkjava.dao;

import org.junit.jupiter.api.Test;
import space.lasf.sparkjava.entity.Crawler;
import space.lasf.sparkjava.entity.Status;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrawlerDaoTest {

    @Test
    void createShouldPersistCrawlerWithValidDefaults() {
        CrawlerDao dao = new CrawlerDao();

        Crawler created = dao.create("keyword");

        assertNotNull(created.getId());
        assertEquals(8, created.getId().length());
        assertEquals(Status.ACTIVE, created.getStatus());
        assertEquals("keyword", created.getKeyword());
        assertEquals(created, dao.findById(created.getId()));
    }

    @Test
    void findAllShouldReturnCreatedCrawlers() {
        CrawlerDao dao = new CrawlerDao();
        Crawler first = dao.create("first");
        Crawler second = dao.create("second");

        List<Crawler> all = dao.findAll();

        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(c -> c.getId().equals(first.getId())));
        assertTrue(all.stream().anyMatch(c -> c.getId().equals(second.getId())));
    }

    @Test
    void appendAllShouldAddUrlsOnlyForExistingCrawler() {
        CrawlerDao dao = new CrawlerDao();
        Crawler crawler = dao.create("keyword");

        dao.appendAll(crawler.getId(), List.of("https://example.com", "https://example.com/a"));
        dao.appendAll("UNKNOWN", List.of("https://ignored.com"));

        assertEquals(2, dao.findById(crawler.getId()).getUrls().size());
        assertFalse(dao.findById(crawler.getId()).getUrls().contains("https://ignored.com"));
    }

    @Test
    void changeStatusShouldHandleDoneAndError() {
        CrawlerDao dao = new CrawlerDao();

        Crawler doneCrawler = dao.create("done-keyword");
        dao.changeStatus(doneCrawler.getId(), Status.DONE);
        assertEquals(Status.DONE, dao.findById(doneCrawler.getId()).getStatus());

        Crawler errorCrawler = dao.create("error-keyword");
        dao.changeStatus(errorCrawler.getId(), Status.ERROR);
        assertEquals(Status.ERROR, dao.findById(errorCrawler.getId()).getStatus());
    }

    @Test
    void changeStatusShouldThrowForInvalidStatus() {
        CrawlerDao dao = new CrawlerDao();
        Crawler crawler = dao.create("keyword");

        assertThrows(IllegalArgumentException.class, () -> dao.changeStatus(crawler.getId(), Status.ACTIVE));
    }
}
