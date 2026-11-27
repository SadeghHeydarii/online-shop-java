package org.example.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.example.model.vo.Review;
import org.example.service.port.ReviewRepository;

/**
 * Thread-safe in-memory implementation of {@link ReviewRepository}.
 */
public class InMemoryReviewRepository implements ReviewRepository {

    private final Map<String, List<Review>> store;

    /**
     * Constructs an empty {@code InMemoryReviewRepository}.
     */
    public InMemoryReviewRepository() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Constructs an {@code InMemoryReviewRepository} backed by the provided map.
     *
     * @param store non-null backing map keyed by product ID
     */
    public InMemoryReviewRepository(Map<String, List<Review>> store) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
    }

    @Override
    public void addReview(Review review) {
        Objects.requireNonNull(review, "review cannot be null");
        store.computeIfAbsent(review.productId(), k -> new CopyOnWriteArrayList<>())
                .add(review);
    }

    @Override
    public List<Review> findByProductId(String productId) {
        String pid = RepositoryPreconditions.requireNonBlank(productId, "productId");
        List<Review> reviews = store.get(pid);
        if (reviews == null || reviews.isEmpty()) {
            return List.of();
        }
        return List.copyOf(reviews);
    }
}
