package pl.use.auction.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @ManyToOne(fetch = FetchType.LAZY)
    private AuctionUser buyer;

    @ManyToMany(mappedBy = "observedAuctions")
    private Set<AuctionUser> observers = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "auction_images", joinColumns = @JoinColumn(name = "auction_id"))
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal startingPrice;
    private CurrencyCode currencyCode = CurrencyCode.PLN;
    private BigDecimal highestBid;
    private BigDecimal buyNowPrice;
    @Enumerated(EnumType.STRING)
    private AuctionStatus status;

    @Enumerated(EnumType.STRING)
    private FeaturedType featuredType = FeaturedType.NONE;
}