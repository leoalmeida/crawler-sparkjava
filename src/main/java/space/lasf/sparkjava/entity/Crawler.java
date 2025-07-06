package space.lasf.sparkjava.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the state of a single crawl job.
 * This class is designed to be thread-safe, allowing one thread to perform the crawl
 * (adding URLs) while other threads can safely read its status.
 */
public class Crawler {

    private final String id;
    private final String keyword;

    // Volatile ensures that changes to status are visible across threads.
    private volatile Status status;

    // Use a thread-safe Set implementation.
    private final Set<String> urls = ConcurrentHashMap.newKeySet();

    private final LocalDateTime startDate;
    private volatile LocalDateTime lastUpdate;

    /**
     * Constructs a new Crawler instance, initializing it to an ACTIVE state.
     *
     * @param id      The unique identifier for this crawl.
     * @param keyword The keyword to search for.
     */
    public Crawler(String id, String keyword) {
        this.id = id;
        this.keyword = keyword;
        this.status = Status.ACTIVE;
        this.startDate = LocalDateTime.now();
        this.lastUpdate = this.startDate;
    }

    /**
     * Marks the crawl as finished (DONE), but only if it's currently ACTIVE.
     * This prevents overwriting an ERROR state. This method is synchronized.
     */
    public synchronized void endProcess() {
        if (this.status == Status.ACTIVE) {
            this.status = Status.DONE;
        }
        this.lastUpdate = LocalDateTime.now();
    }

    /**
     * Marks the crawl as having encountered an error. This method is synchronized.
     */
    public synchronized void errorProcess() {
        this.status = Status.ERROR;
        this.lastUpdate = LocalDateTime.now();
    }

    /**
     * Adds a found URL to the result set if the crawl is still active.
     * This method is synchronized to ensure atomic check-and-add behavior.
     *
     * @param link A list of URLs that was found.
     */
    public synchronized void addLinks(List<String> link) {
        if (this.status == Status.ACTIVE) {
            this.urls.addAll(link);
            this.lastUpdate = LocalDateTime.now();
        }
    }

    public String getId() {
        return id;
    }

    public String getKeyword() {
        return keyword;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * Returns a defensive, immutable copy of the URLs to ensure the internal set is not modified.
     *
     * @return An immutable Set containing the found URLs.
     */
    public Set<String> getUrls() {
        // Return an immutable copy to prevent external modification of the internal set
        return Set.copyOf(urls);
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    // No setter for lastUpdate as it's managed internally
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public String toString() {
        return "Crawler{" +
                "id='" + id + '\'' +
                ", keyword='" + keyword + '\'' +
                ", status=" + status +
                ", urls.size=" + urls.size() +
                ", startDate=" + startDate +
                ", lastUpdate=" + lastUpdate +
                '}';
    }
}
