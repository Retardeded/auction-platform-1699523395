package pl.use.auction.controller;


import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.use.auction.model.*;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.AuctionService;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    @Autowired
    private AuctionRepository auctionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AuctionService auctionService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/profile")
    public String viewAdminProfile(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();

        AuctionUser currentUser = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        model.addAttribute("currentUser", currentUser);

        return "admin/profile";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all-auctions")
    public String viewAllAuctionsForAdmin(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();

        AuctionUser currentUser = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        model.addAttribute("currentUser", currentUser);

        List<Auction> allAuctions = auctionRepository.findAll();

        List<Auction> ongoingAuctions = allAuctions.stream()
                .filter(auction -> auction.getStatus() == AuctionStatus.ACTIVE)
                .collect(Collectors.toList());
        List<Auction> pastAuctions = allAuctions.stream()
                .filter(auction -> auction.getStatus() != AuctionStatus.ACTIVE)
                .collect(Collectors.toList());

        model.addAttribute("ongoingAuctions", ongoingAuctions);
        model.addAttribute("pastAuctions", pastAuctions);

        return "admin/all-auctions";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/feature-auction")
    public ResponseEntity<?> featureAuction(@RequestParam("auctionId") Long auctionId,
                                            @RequestParam("featuredType") String featuredType) {
        Optional<Auction> optionalAuction = auctionRepository.findById(auctionId);
        if (!optionalAuction.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", "Auction not found."));
        }

        Auction auction = optionalAuction.get();
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

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all-categories")
    public String viewAllCategoriesForAdmin(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();

        AuctionUser currentUser = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        model.addAttribute("currentUser", currentUser);

        List<Category> parentCategories = categoryRepository.findByParentCategoryIsNull();
        model.addAttribute("parentCategories", parentCategories);

        return "admin/all-categories";
    }

    @PostMapping("/admin/edit-category")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> editCategory(@RequestParam Long categoryId,
                                          @RequestParam String name,
                                          @RequestParam(required = false) Long parentCategoryId) {
        try {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));

            if (parentCategoryId != null && parentCategoryId.equals(categoryId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "A category cannot be its own parent."));
            }

            if (parentCategoryId != null) {
                Category parentCategory = categoryRepository.findById(parentCategoryId)
                        .orElseThrow(() -> new EntityNotFoundException("Parent category not found with id: " + parentCategoryId));

                if (isSubcategoryOf(category, parentCategory)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid parent category."));
                }

                category.setParentCategory(parentCategory);
            } else {
                category.setParentCategory(null);
            }

            category.setName(name);
            categoryRepository.save(category);

            return ResponseEntity.ok().body(Map.of("message", "Category updated successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to update category"));
        }
    }

    private boolean isSubcategoryOf(Category currentCategory, Category potentialParent) {
        Category parent = potentialParent.getParentCategory();
        while (parent != null) {
            if
            (parent.getId().equals(currentCategory.getId())) {
                return true;
            }
            parent = parent.getParentCategory();
        }
        return false;
    }

    @PostMapping("/admin/suspend-user")
    @PreAuthorize("hasRole('ADMIN')")
    public String suspendUser(@RequestParam Long userId, @RequestParam int suspensionDays, RedirectAttributes redirectAttributes) {
        userRepository.findById(userId).ifPresent(user -> {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, suspensionDays);
            user.setSuspensionEndDate(calendar.getTime());
            user.setStatus(UserStatus.SUSPENDED);
            userRepository.save(user);
            redirectAttributes.addAttribute("username", user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "User suspended successfully for " + suspensionDays + " days.");
        });
        return "redirect:/user/{username}";
    }

    @PostMapping("/admin/ban-user")
    @PreAuthorize("hasRole('ADMIN')")
    public String banUser(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(UserStatus.BANNED);
            userRepository.save(user);
            redirectAttributes.addAttribute("username", user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "User banned successfully");
        });
        return "redirect:/user/{username}";
    }

    @PostMapping("/admin/unban-user")
    @PreAuthorize("hasRole('ADMIN')")
    public String unbanUser(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            redirectAttributes.addAttribute("username", user.getUsername());
        });
        redirectAttributes.addFlashAttribute("successMessage", "User unbanned successfully.");
        return "redirect:/user/{username}";
    }

    @PostMapping("/admin/unsuspend-user")
    @PreAuthorize("hasRole('ADMIN')")
    public String unsuspendUser(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setSuspensionEndDate(null);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            redirectAttributes.addAttribute("username", user.getUsername());
        });
        redirectAttributes.addFlashAttribute("successMessage", "User suspension lifted successfully.");
        return "redirect:/user/{username}";
    }
}
