package pl.use.auction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import pl.use.auction.model.Rating;
import pl.use.auction.model.TransactionFeedback;
import pl.use.auction.repository.TransactionFeedbackRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.ProfileService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionFeedbackRepository transactionFeedbackRepository;
    @InjectMocks
    private ProfileService profileService;

    @Test
    void testCalculateCumulativeRatingWithAllPositiveFeedback() {
        List<TransactionFeedback> feedbackList = new ArrayList<>();
        TransactionFeedback feedback1 = new TransactionFeedback();
        feedback1.setRatingByBuyer(Rating.POSITIVE);
        feedbackList.add(feedback1);

        TransactionFeedback feedback2 = new TransactionFeedback();
        feedback2.setRatingByBuyer(Rating.POSITIVE);
        feedbackList.add(feedback2);

        String rating = profileService.calculateCumulativeRating(feedbackList, feedbackList.size());
        assertEquals("100% Positive", rating);
    }

    @Test
    void testCalculateCumulativeRatingWithMixedFeedback() {
        List<TransactionFeedback> feedbackList = new ArrayList<>();

        TransactionFeedback feedback1 = new TransactionFeedback();
        feedback1.setRatingByBuyer(Rating.POSITIVE);
        feedbackList.add(feedback1);

        TransactionFeedback feedback2 = new TransactionFeedback();
        feedback2.setRatingByBuyer(Rating.NEGATIVE);
        feedbackList.add(feedback2);

        TransactionFeedback feedback3 = new TransactionFeedback();
        feedback3.setRatingByBuyer(Rating.NEUTRAL);
        feedbackList.add(feedback3);

        String rating = profileService.calculateCumulativeRating(feedbackList, feedbackList.size());

        assertEquals("33% Positive", rating);
    }
}
