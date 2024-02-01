package pl.use.auction.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String slug;
    private String description;

    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    private AuctionUser auctionCreator;

    @ManyToOne(fetch = FetchType.LAZY)
    private AuctionUser highestBidder;

    @ManyToMany(mappedBy = "observedAuctions")
    private List<AuctionUser> observers;

    @ElementCollection
    @CollectionTable(name = "auction_images", joinColumns = @JoinColumn(name = "auction_id"))
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal startingPrice;
    private BigDecimal highestBid;
    private String status;
}