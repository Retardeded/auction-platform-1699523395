package pl.use.auction.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.use.auction.model.*;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.service.AdminService;
import pl.use.auction.service.AuctionService;
import pl.use.auction.service.UserService;

import java.util.*;

@Controller
public class AdminController {

    @Autowired
    private AuctionRepository auctionRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AuctionService auctionService;
    @Autowired
    private UserService userService;
    @Autowired
    private AdminService adminService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/profile")
    public String viewAdminProfile(Model model, Authentication authentication) {
        AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());
        model.addAttribute("currentUser", currentUser);

        return "admin/profile";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all-auctions")
    public String viewAllAuctionsForAdmin(Model model, Authentication authentication) {
        AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());
        model.addAttribute("currentUser", currentUser);

        List<Auction> ongoingAuctions = adminService.getAllOngoingAuctions();
        List<Auction> pastAuctions = adminService.getAllPastAuctions();

        model.addAttribute("ongoingAuctions", ongoingAuctions);
        model.addAttribute("pastAuctions", pastAuctions);

        return "admin/all-auctions";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/feature-auction")
    public ResponseEntity<Map<String, String> > featureAuction(@RequestParam("auctionId") Long auctionId,
                                            @RequestParam("featuredType") String featuredType) {

        Optional<Auction> optionalAuction = auctionRepository.findById(auctionId);
        if (optionalAuction.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", "Auction not found."));
        }
        Auction auction = optionalAuction.get();

        return adminService.featureAuction(auction, featuredType);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all-categories")
    public String viewAllCategoriesForAdmin(Model model, Authentication authentication) {
        AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());
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

                if (adminService.isSubcategoryOf(category, parentCategory)) {
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

    @PostMapping("/admin/add-category")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addCategory(@RequestParam String name,
                                         @RequestParam(required = false) Long parentCategoryId) {
        try {
            Category newCategory = new Category(name, null);
            if (parentCategoryId != null) {
                Category parentCategory = categoryRepository.findById(parentCategoryId)
                        .orElseThrow(() -> new EntityNotFoundException("Parent category not found with id: " + parentCategoryId));
                newCategory.setParentCategory(parentCategory);
            }
            categoryRepository.save(newCategory);
            return ResponseEntity.ok().body(Map.of("message", "New category created successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to create category"));
        }
    }

    @DeleteMapping("/admin/delete-category")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCategory(@RequestBody Map<String, Object> payload) {
        try {
            Long categoryId = Long.parseLong((String) payload.get("categoryId"));

            boolean isCategoryInUse = auctionRepository.existsByCategoryId(categoryId);
            if (isCategoryInUse) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Cannot delete category because it has associated auctions"));
            }

            boolean isParentCategory = categoryRepository.existsByParentCategoryId(categoryId);
            if (isParentCategory) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Cannot delete category because it is a parent to other categories"));
            }
            categoryRepository.deleteById(categoryId);

            return ResponseEntity.ok().body(Map.of("message", "Category deleted successfully"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid category ID format"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to delete category"));
        }
    }

    @PostMapping("/admin/suspend-user")
    @PreAuthorize("hasRole('ADMIN')")
    public String suspendUser(@RequestParam Long userId, @RequestParam int suspensionDays, RedirectAttributes redirectAttributes) {
        adminService.suspendUser(userId, suspensionDays).ifPresent(username -> {
            redirectAttributes.addAttribute("username", username);
            redirectAttributes.addFlashAttribute("successMessage", "User suspended successfully for " + suspensionDays + " days.");
        });
        return "redirect:/user/{username}";
    }

    @PostMapping("/admin/ban-user")
    @PreAuthorize("hasRole('ADMIN')")
    public String banUser(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        adminService.banUser(userId).ifPresent(username -> {
            redirectAttributes.addAttribute("username", username);
            redirectAttributes.addFlashAttribute("successMessage", "User banned successfully");
        });
        return "redirect:/user/{username}";
    }

    @PostMapping("/admin/unban-user")
    @PreAuthorize("hasRole('ADMIN')")
    public String unbanUser(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        adminService.unbanUser(userId).ifPresent(username -> {
            redirectAttributes.addAttribute("username", username);
            redirectAttributes.addFlashAttribute("successMessage", "User unbanned successfully.");
        });
        return "redirect:/user/{username}";
    }

    @PostMapping("/admin/unsuspend-user")
    @PreAuthorize("hasRole('ADMIN')")
    public String unsuspendUser(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        adminService.unsuspendUser(userId).ifPresent(username -> {
            redirectAttributes.addAttribute("username", username);
            redirectAttributes.addFlashAttribute("successMessage", "User suspension lifted successfully.");
        });
        return "redirect:/user/{username}";
    }
}
