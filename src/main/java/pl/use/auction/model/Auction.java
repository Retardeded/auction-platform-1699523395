package pl.use.auction.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    private AuctionUser auctionCreator;

    @ManyToOne(fetch = FetchType.LAZY)
    private AuctionUser highestBidder;

    @ManyToMany(mappedBy = "observedAuctions")
    private List<AuctionUser> observers;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal startingPrice;
    private BigDecimal highestBid;
    private String status;
}