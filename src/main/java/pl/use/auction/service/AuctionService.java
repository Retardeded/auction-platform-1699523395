package pl.use.auction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Category;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    public boolean placeBid(Auction auction, AuctionUser bidder, BigDecimal bidAmount) {
        boolean bidPlaced = false;
        if (bidAmount.compareTo(auction.getHighestBid()) > 0) {
            auction.setHighestBid(bidAmount);
            auction.setHighestBidder(bidder);
            addToWatchlist(bidder, auction);

            auctionRepository.save(auction);
            bidPlaced = true;
        }
        return bidPlaced;
    }

    public void addToWatchlist(AuctionUser user, Auction auction) {
        user.getObservedAuctions().add(auction);
        userRepository.save(user);
    }

    public void removeFromWatchlist(AuctionUser user, Auction auction) {
        user.getObservedAuctions().remove(auction);
        userRepository.save(user);
    }

    public List<Auction> getAggregatedAuctionsForCategory(Category category, AuctionUser currentUser) {
        List<Auction> aggregatedAuctions = auctionRepository.findByCategoryAndEndTimeAfter(category, LocalDateTime.now())
                .stream()
                .filter(auction -> !auction.getAuctionCreator().equals(currentUser))
                .collect(Collectors.toList());

        for (Category childCategory : category.getChildCategories()) {
            aggregatedAuctions.addAll(auctionRepository.findByCategoryAndEndTimeAfter(childCategory, LocalDateTime.now())
                    .stream()
                    .filter(auction -> !auction.getAuctionCreator().equals(currentUser))
                    .toList());
        }

        return aggregatedAuctions;
    }

    public List<Auction> findCheapestAuctions(AuctionUser currentUser, int limit) {
        return auctionRepository.findByEndTimeAfter(LocalDateTime.now()).stream()
                .filter(auction -> !auction.getAuctionCreator().equals(currentUser))
                .sorted(Comparator.comparing(Auction::getHighestBid))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Auction> findExpensiveAuctions(AuctionUser currentUser, int limit) {
        return auctionRepository.findByEndTimeAfter(LocalDateTime.now()).stream()
                .filter(auction -> !auction.getAuctionCreator().equals(currentUser))
                .sorted(Comparator.comparing(Auction::getHighestBid).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
