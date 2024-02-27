package pl.use.auction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pl.use.auction.model.*;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.repository.AuctionRepository;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Auction> getAllOngoingAuctions() {
        return auctionRepository.findAll().stream()
                .filter(auction -> auction.getStatus() == AuctionStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    public List<Auction> getAllPastAuctions() {
        return auctionRepository.findAll().stream()
                .filter(auction -> auction.getStatus() != AuctionStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    public ResponseEntity<Map<String, String> > featureAuction(Auction auction, String featuredType) {
        if (!featuredType.isEmpty()) {
            auction.setFeaturedType(FeaturedType.valueOf(featuredType));
        } else {
            auction.setFeaturedType(FeaturedType.NONE);
        }

        auctionRepository.save(auction);

        String imagePath = "/auctionSectionImages/" + auction.getFeaturedType().getImagePath();
        Map<String, String> response = new HashMap<>();
        response.put("imagePath", imagePath);

        return ResponseEntity.ok(response);
    }

    public boolean isSubcategoryOf(Category currentCategory, Category potentialParent) {
        Category parent = potentialParent.getParentCategory();
        while (parent != null) {
            if (parent.getId().equals(currentCategory.getId())) {
                return true;
            }
            parent = parent.getParentCategory();
        }
        return false;
    }

    public Optional<String> suspendUser(Long userId, int suspensionDays) {
        return userRepository.findById(userId).map(user -> {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, suspensionDays);
            user.setSuspensionEndDate(calendar.getTime());
            user.setStatus(UserStatus.SUSPENDED);
            userRepository.save(user);
            return user.getUsername();
        });
    }

    public Optional<String> banUser(Long userId) {
        return userRepository.findById(userId).map(user -> {
            user.setStatus(UserStatus.BANNED);
            userRepository.save(user);
            return user.getUsername();
        });
    }

    public Optional<String> unbanUser(Long userId) {
        return userRepository.findById(userId).map(user -> {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            return user.getUsername();
        });
    }

    public Optional<String> unsuspendUser(Long userId) {
        return userRepository.findById(userId).map(user -> {
            user.setSuspensionEndDate(null);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            return user.getUsername();
        });
    }
}
