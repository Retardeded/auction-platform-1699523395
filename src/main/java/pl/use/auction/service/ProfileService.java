package pl.use.auction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.use.auction.model.Rating;
import pl.use.auction.model.TransactionFeedback;
import pl.use.auction.repository.TransactionFeedbackRepository;
import pl.use.auction.repository.UserRepository;

import java.util.List;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionFeedbackRepository transactionFeedbackRepository;

    public String calculateCumulativeRating(List<TransactionFeedback> feedbackList, long totalFeedback) {
        if (totalFeedback > 0) {
            long positiveCount = feedbackList.stream().filter(f -> f.getRatingByBuyer() == Rating.POSITIVE || f.getRatingBySeller() == Rating.POSITIVE).count();
            long neutralCount = feedbackList.stream().filter(f -> f.getRatingByBuyer() == Rating.NEUTRAL || f.getRatingBySeller() == Rating.NEUTRAL).count();
            long negativeCount = feedbackList.stream().filter(f -> f.getRatingByBuyer() == Rating.NEGATIVE || f.getRatingBySeller() == Rating.NEGATIVE).count();

            if (positiveCount >= neutralCount && positiveCount >= negativeCount) {
                return String.format("%.0f%% Positive", (positiveCount / (double) totalFeedback) * 100);
            } else if (neutralCount > positiveCount && neutralCount >= negativeCount) {
                return String.format("%.0f%% Neutral", (neutralCount / (double) totalFeedback) * 100);
            } else {
                return String.format("%.0f%% Negative", (negativeCount / (double) totalFeedback) * 100);
            }
        } else {
            return "No feedback so far.";
        }
    }
}
