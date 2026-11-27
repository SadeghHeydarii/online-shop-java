package org.example.cli.menu.customer;

import java.util.Objects;
import org.example.cli.io.InputReader;
import org.example.cli.io.OutputWriter;
import org.example.exception.ShopException;
import org.example.exception.UnauthorizedActionException;
import org.example.model.entity.User;
import org.example.service.api.ReviewUseCase;

/**
 * Customer CLI handler dedicated to verified product review submission (SRP, ISP, DIP).
 */
public class ReviewMenuHandler {

    private final ReviewUseCase reviewUseCase;
    private final InputReader in;
    private final OutputWriter out;

    /**
     * Constructs a {@code ReviewMenuHandler}.
     *
     * @param reviewUseCase optional review use-case port (may be null)
     * @param in            narrow console input port
     * @param out           narrow console output port
     */
    public ReviewMenuHandler(ReviewUseCase reviewUseCase, InputReader in, OutputWriter out) {
        this.reviewUseCase = reviewUseCase;
        this.in = Objects.requireNonNull(in, "in cannot be null");
        this.out = Objects.requireNonNull(out, "out cannot be null");
    }

    /**
     * Handles collecting and submitting a verified customer review.
     *
     * @param customer authenticated customer
     */
    public void handleSubmitReview(User customer) {
        Objects.requireNonNull(customer, "customer cannot be null");
        out.printHeader("Submit Product Rating & Review");
        if (reviewUseCase == null) {
            out.printWarning("Review service is not currently available.");
            return;
        }

        String productId = in.readString("Enter Product ID");
        int rating = in.readInt("Rating (1 to 5 stars)", 1, 5);
        String comment = in.readString("Written feedback comment");

        try {
            reviewUseCase.addReview(customer.getId(), customer.getFullName(), productId, rating, comment);
            out.printSuccess("Thank you! Your verified review and " + rating + "-star rating have been recorded.");
        } catch (UnauthorizedActionException e) {
            out.printError("Review submission rejected: " + e.getMessage());
            out.printInfo("Note: You must have purchased and paid for this item in a previous order before reviewing it.");
        } catch (ShopException | IllegalArgumentException e) {
            out.printError("Failed to submit review: " + e.getMessage());
        }
    }
}
