package pl.use.auction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Category;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static pl.use.auction.util.StringUtils.createSlugFromTitle;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FileSystemStorageService fileSystemStorageService;

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

    public Auction createAndSaveAuction(Auction auction,
                                        Long categoryId,
                                        MultipartFile[] files,
                                        String username) throws IOException {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        AuctionUser user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        auction.setCategory(category);
        auction.setAuctionCreator(user);
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(auction.getEndTime());
        auction.setStartingPrice(auction.getStartingPrice());
        auction.setHighestBid(BigDecimal.valueOf(0));
        auction.setStatus("ONGOING");
        auction.setSlug(createSlugFromTitle(auction.getTitle()));
        auction.setImageUrls(new ArrayList<>()); // Make sure the list is initialized

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String imageUrl = saveImage(file);
                auction.getImageUrls().add(imageUrl); // Add the image URL to the list
            }
        }

        return auctionRepository.save(auction);
    }

    public Auction updateAuction(String slug,
                                 Auction auctionDetails,
                                 MultipartFile[] newImages,
                                 List<String> imagesToDelete) throws IOException {
        Auction existingAuction = auctionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));

        existingAuction.setDescription(auctionDetails.getDescription());
        existingAuction.setCategory(auctionDetails.getCategory());
        existingAuction.setStartingPrice(auctionDetails.getStartingPrice());
        existingAuction.setEndTime(auctionDetails.getEndTime());

        for (MultipartFile file : newImages) {
            if (!file.isEmpty()) {
                String imageUrl = saveImage(file);
                existingAuction.getImageUrls().add(imageUrl);
            }
        }

        if (imagesToDelete != null) {
            existingAuction.getImageUrls().removeAll(imagesToDelete);
        }

        return auctionRepository.save(existingAuction);
    }

    public String saveImage(MultipartFile file) throws IOException {
        return fileSystemStorageService.save(file, "src/main/resources/static/auctionImages/");
    }
}
