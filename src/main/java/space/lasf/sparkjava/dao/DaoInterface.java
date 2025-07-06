package space.lasf.sparkjava.dao;

import java.util.List;
import java.util.Set;

import space.lasf.sparkjava.entity.Status;

/**
 * Defines the contract for a Data Access Object (DAO) for managing Crawler instances.
 * This allows for decoupling the business logic from the concrete data storage implementation.
 */
public interface DaoInterface<T> {

    /**
     * Finds an object instance by its ID.
     *
     * @param id The ID of the object Instance.
     * @return The Object instance, or null if not found.
     */
    T findById(String id);

    /**
     * Creates a new object instance, initializes its state to ACTIVE, and stores it.
     *
     * @param keyword The keyword used to start processing request.
     * @return The newly created and initialized Object instance.
     */
    T create(String keyword);

    /**
     * Returns a list of all stored crawler instances.
     *
     * @return A new list containing all crawlers.
     */
    List<T> findAll();

    /**
     * Updates an specific object instance data.
     *
     * @param id  The ID of the object.
     * @param item The object instance to update.
     */
    void appendAll(String id, List<String> value);

    
    /**
     * Updates an specific object instance data.
     *
     * @param id  The ID of the object.
     * @param status The new status of object instance.
     */
    void changeStatus(String id, Status status);


}