package pl.use.auction.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.use.auction.model.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    List<Auction> findByAuctionCreator(AuctionUser user);

    List<Auction> findByBuyer(AuctionUser buyer);

    List<Auction> findByEndTimeAfter(LocalDateTime time);

    List<Auction> findByHighestBidder(AuctionUser user);

    List<Auction> findByCategory(Category category);

    Optional<Auction> findBySlug(String slug);

    List<Auction> findByCategoryAndEndTimeAfter(Category category, LocalDateTime endTime);

    List<Auction> findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndCategoryIdInAndStatus(
            String title,
            String location,
            List<Long> categoryIds,
            AuctionStatus status);

    List<Auction> findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndStatus(
            String title,
            String location,
            AuctionStatus status);

    List<Auction> findByEndTimeAfterAndStatusNot(LocalDateTime now, AuctionStatus sold);

    List<Auction> findByEndTimeBeforeAndStatus(LocalDateTime endTime, AuctionStatus status);

    List<Auction> findByHighestBidderAndStatus(AuctionUser currentUser, AuctionStatus active);

    List<Auction> findByHighestBidderAndStatusIn(AuctionUser highestBidder, List<AuctionStatus> statuses);

    List<Auction> findByEndTimeBetweenAndStatus(LocalDateTime now, LocalDateTime plusHours, AuctionStatus active);

    List<Auction> findByFeaturedType(FeaturedType cheap, PageRequest of);
    boolean existsByCategoryId(Long categoryId);
}