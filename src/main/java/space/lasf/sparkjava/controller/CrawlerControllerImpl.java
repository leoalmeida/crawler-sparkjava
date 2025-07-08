package space.lasf.sparkjava.controller;

import java.util.List;
import java.util.Optional;

import space.lasf.sparkjava.dao.DaoInterface;
import space.lasf.sparkjava.dto.CrawlerDto;
import space.lasf.sparkjava.entity.Crawler;
import space.lasf.sparkjava.exception.InvalidRequestException;
import space.lasf.sparkjava.exception.ResourceNotFoundException;
import space.lasf.sparkjava.handler.CrawlerHandler;
import space.lasf.sparkjava.helper.CrawlerMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller responsible for handling web requests related to crawling.
 * It orchestrates the creation and retrieval of crawl jobs by interacting with the DAO and Handler layers.
 */
public class CrawlerControllerImpl implements ControllerInterface<CrawlerDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerControllerImpl.class);
    
    private final DaoInterface<Crawler> dao;
	private final CrawlerHandler crawlerHandler;

    /**
     * Constructs a new CrawlerController with its dependencies.
     *
     * @param dao     The data access object for managing crawler instances.
     * @param crawlerHandler The handler responsible for the crawling logic.
     */
    public CrawlerControllerImpl(DaoInterface<Crawler> dao, CrawlerHandler crawlerHandler) {
        this.dao = dao;
        this.crawlerHandler = crawlerHandler;
    }

    /**
     * Initiates the crawling process for a given request in the background.
     *
     * @param crawlerUrl The base URL to start crawling from.
     * @param id The ID of the crawl request used on processing.
     */
    @Override
    public void process(String crawlerUrl, String id) {
        LOGGER.info("Starting crawl for request ID: {}", id);
        crawlerHandler.crawlResource(crawlerUrl, id);
        LOGGER.info("Crawl process submitted for request ID: {}", id);
	}

    /**
     * Validates the keyword and creates a new crawl request.
     *
     * @param keyword The search term for the crawl. Must be between 4 and 32 characters.
     * @return The newly created Crawler instance.
     * @throws InvalidRequestException if the keyword is invalid.
     */
    @Override
    public CrawlerDto create(String keyword) {
        if (keyword == null || keyword.isBlank() || keyword.length() < 4 || keyword.length() > 32) {
            throw new InvalidRequestException("The keyword must be between 4 and 32 characters.");
        }        

        Crawler request = dao.create(keyword);
        return CrawlerMapper.toCrawlerDto(request);
    }

    /**
     * Finds a crawl request by its ID and returns its data transfer object.
     *
     * @param id The ID of the crawl request.
     * @return A {@link CrawlerDto} representing the state of the crawl.
     * @throws ResourceNotFoundException if no crawl with the given ID is found.
     */
    @Override
    public CrawlerDto findById(String id) {
        if (id == null || id.isBlank() || id.length() < 8 || id.length() > 8) {
            throw new InvalidRequestException("The id must be have 8 characters.");
        }   
        LOGGER.info("Finding request by ID: {}", id);
        return Optional.ofNullable(dao.findById(id))
                .map(CrawlerMapper::toCrawlerDto)
                .orElseThrow(() -> new ResourceNotFoundException("Crawl request with ID '" + id + "' not found."));
    }

    /**
     * Retrieves all crawl requests.
     *
     * @return A list of {@link CrawlerDto} objects for all requests.
     */
    @Override
    public List<CrawlerDto> findAll() {
        LOGGER.info("Finding all requests.");
        return CrawlerMapper.toCrawlerDtoList(dao.findAll());
    }

}