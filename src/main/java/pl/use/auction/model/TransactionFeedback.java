package pl.use.auction.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class TransactionFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Auction auction;

    @ManyToOne
    private AuctionUser buyer;

    @ManyToOne
    private AuctionUser seller;

    private String commentByBuyer;
    private Rating ratingByBuyer;
    private LocalDateTime dateOfBuyerFeedback;
    private String commentBySeller;
    private Rating ratingBySeller;
    private LocalDateTime dateOfSellerFeedback;
}

