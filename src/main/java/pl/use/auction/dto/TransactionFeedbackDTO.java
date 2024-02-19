package pl.use.auction.dto;

import lombok.Getter;
import lombok.Setter;
import pl.use.auction.model.Rating;

@Getter
@Setter
public class TransactionFeedbackDTO {
    private Long auctionId;
    private Long buyerId;
    private String comment;
    private Rating rating;
}
