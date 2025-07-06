package space.lasf.sparkjava.controller;

import space.lasf.sparkjava.exception.InvalidRequestException;
import space.lasf.sparkjava.exception.ResourceNotFoundException;

import java.util.List;

/**
 * Defines the contract for a generic resource controller.
 * This interface allows for decoupling the API routes from the concrete controller implementation.
 */
public interface ControllerInterface<T> {

    /**
     * Initiates the processing for a given resource in the background.
     *
     * @param base The base value to start processing from.
     * @param id The ID of resource that should be processed.
     */
    void process(String base, String id);

    /**
     * Validates input and creates a new resource.
     *
     * @param keyword The primary identifier or search term for the new resource.
     * @return The newly created object instance.
     * @throws InvalidRequestException if the keyword is invalid.
     */
    T create(String keyword);

    /**
     * Finds a resource by its ID and returns its data transfer object.
     *
     * @param id The ID of the resource.
     * @return A {@link T} representing the state of the resource.
     * @throws ResourceNotFoundException if no resource with the given ID is found.
     */
    T findById(String id);

    /**
     * Retrieves all resources.
     *
     * @return A list of {@link T} objects for all resources.
     */
    List<T> findAll();
}