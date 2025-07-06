package space.lasf.sparkjava.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.lasf.sparkjava.entity.Crawler;
import space.lasf.sparkjava.entity.Status;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Access Object (DAO) for managing Crawler instances.
 * This class is thread-safe.
 */
public class CrawlerDao implements DaoInterface<Crawler> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerDao.class);
    // Use ConcurrentHashMap for thread-safe, high-performance concurrent access.
    private final Map<String, Crawler> crawlerMap = new ConcurrentHashMap<>();

    private static final int ID_LENGTH = 8;
    private static final String ID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";


    public Crawler findById(String id){
        return crawlerMap.get(id);
    }

    /**
     * Creates a new Crawler instance, initializes its state to ACTIVE, and stores it.
     * This is an atomic operation from the perspective of the caller.
     *
     * @param keyword The keyword for the new crawl request.
     * @return The newly created and initialized Crawler instance.
     */
    public Crawler create(String keyword) {
        String randomCode = generateRandomCode();
        Crawler request = new Crawler(randomCode, keyword);
        crawlerMap.put(request.getId(), request);
        LOGGER.info("Created and started new crawler with ID: {}. Total crawlers: {}", randomCode, crawlerMap.size());
        return request;
    }

    public List<Crawler> findAll(){
        // Return a copy to prevent modification of the underlying values collection
        return new ArrayList<>(crawlerMap.values());
    }

    @Override
    public void changeStatus(String id, Status status) {
        switch (status) {
            case ERROR:
                errorProcessing(id);
                break;
            case DONE:
                endProcessing(id);
                break;
            default:
                throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    @Override
    public void appendAll(String id, List<String> values) {
        Optional.ofNullable(crawlerMap.get(id)).ifPresent(crawler -> crawler.addLinks(values));
    }

    private void endProcessing(String id) {
        Optional.ofNullable(crawlerMap.get(id)).ifPresent(Crawler::endProcess);
    }
    
    private void errorProcessing(String id) {
        Optional.ofNullable(crawlerMap.get(id)).ifPresent(Crawler::errorProcess);
    }

    private static String generateRandomCode() {
        // Define the characters to be used in the random code
        StringBuilder codeBuilder = new StringBuilder();
        Random random;
        try {
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("Could not get SecureRandom instance, falling back to standard Random. This is not recommended for production.");
            random = new Random();
        }

        for (int i = 0; i < ID_LENGTH; i++) {
            // Get a random index within the range of available characters
            int randomIndex = random.nextInt(ID_CHARACTERS.length());
            // Append the character at the random index to the code
            codeBuilder.append(ID_CHARACTERS.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }
}