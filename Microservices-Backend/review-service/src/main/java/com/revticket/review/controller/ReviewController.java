package com.revticket.review.controller;

import com.revticket.review.dto.ReviewRequest;
import com.revticket.review.dto.ReviewResponse;
import com.revticket.review.service.ReviewService;
import com.revticket.review.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ReviewController - Matches the monolithic backend's pattern exactly.
 * Uses Authentication parameter only, no @RequestHeader for token.
 */
@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = { "http://localhost:4200", "*" })
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(@RequestBody ReviewRequest request,
            Authentication authentication) {
        String userId = securityUtil.getCurrentUserId(authentication);
        ReviewResponse response = reviewService.addReview(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ReviewResponse>> getMovieReviews(@PathVariable String movieId) {
        List<ReviewResponse> reviews = reviewService.getMovieReviews(movieId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/movie/{movieId}/average")
    public ResponseEntity<Map<String, Double>> getAverageRating(@PathVariable String movieId) {
        Double avgRating = reviewService.getAverageRating(movieId);
        return ResponseEntity.ok(Map.of("averageRating", avgRating != null ? avgRating : 0.0));
    }

    @GetMapping("/can-review/{movieId}")
    public ResponseEntity<Map<String, Boolean>> canReviewMovie(@PathVariable String movieId,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(Map.of("canReview", false));
        }
        try {
            String userId = securityUtil.getCurrentUserId(authentication);
            boolean canReview = reviewService.canUserReviewMovie(userId, movieId);
            return ResponseEntity.ok(Map.of("canReview", canReview));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("canReview", false));
        }
    }
}
