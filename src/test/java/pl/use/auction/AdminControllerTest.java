package pl.use.auction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.use.auction.controller.AdminController;
import pl.use.auction.model.*;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.AdminService;
import pl.use.auction.service.UserService;

import java.util.*;

@ExtendWith(MockitoExtension.class)
public class AdminControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Model model;

    @Mock
    private Authentication authentication;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AdminService adminService;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminController adminController;

    @Test
    public void testViewAdminProfile() {
        String expectedUserName = "test@example.com";
        AuctionUser expectedUser = new AuctionUser();
        expectedUser.setUsername(expectedUserName);

        when(authentication.getName()).thenReturn(expectedUserName);
        when(userService.findByUsernameOrThrow(expectedUserName)).thenReturn(expectedUser);

        String viewName = adminController.viewAdminProfile(model, authentication);

        assertEquals("admin/profile", viewName);
        verify(model).addAttribute("currentUser", expectedUser);
    }

    @Test
    public void testViewAllAuctionsForAdmin() {
        String expectedUserName = "admin@example.com";
        AuctionUser expectedUser = new AuctionUser();
        expectedUser.setEmail(expectedUserName);

        Auction activeAuction = new Auction();
        activeAuction.setStatus(AuctionStatus.ACTIVE);
        Auction soldAuction = new Auction();
        soldAuction.setStatus(AuctionStatus.SOLD);

        List<Auction> ongoingAuctions = Collections.singletonList(activeAuction);
        List<Auction> pastAuctions = Collections.singletonList(soldAuction);

        when(authentication.getName()).thenReturn(expectedUserName);
        when(userService.findByUsernameOrThrow(expectedUserName)).thenReturn(expectedUser);
        when(adminService.getAllOngoingAuctions()).thenReturn(ongoingAuctions);
        when(adminService.getAllPastAuctions()).thenReturn(pastAuctions);

        String viewName = adminController.viewAllAuctionsForAdmin(model, authentication);

        assertEquals("admin/all-auctions", viewName);

        verify(userService).findByUsernameOrThrow(expectedUserName);
        verify(adminService).getAllOngoingAuctions();
        verify(adminService).getAllPastAuctions();
    }

    @Test
    public void testFeatureAuction_AuctionNotFound() {
        Long auctionId = 1L;
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = adminController.featureAuction(auctionId, "PREMIUM");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(Collections.singletonMap("error", "Auction not found."), response.getBody());
    }

    @Test
    public void testFeatureAuction_ValidFeaturedType() {
        Long auctionId = 2L;
        String featuredType = "GOODDEAL";
        Auction auction = new Auction();
        auction.setId(auctionId);

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(adminService.featureAuction(auction, featuredType)).thenReturn(ResponseEntity.ok().body(Collections.singletonMap("message", "Auction featured successfully")));

        ResponseEntity<Map<String,String>> response = adminController.featureAuction(auctionId, featuredType);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Auction featured successfully", response.getBody().get("message"));
    }

    @Test
    public void testViewAllCategoriesForAdmin() {
        String expectedUserName = "admin@example.com";
        AuctionUser expectedUser = new AuctionUser();
        expectedUser.setEmail(expectedUserName);
        List<Category> expectedParentCategories = Arrays.asList(new Category(), new Category()); // Simplified example

        when(authentication.getName()).thenReturn(expectedUserName);
        when(userService.findByUsernameOrThrow(expectedUserName)).thenReturn(expectedUser);
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(expectedParentCategories);

        String viewName = adminController.viewAllCategoriesForAdmin(model, authentication);

        assertEquals("admin/all-categories", viewName);
        verify(model).addAttribute("currentUser", expectedUser);
        verify(model).addAttribute("parentCategories", expectedParentCategories);
    }

    @Test
    void testEditCategorySuccess() {
        Long categoryId = 1L;
        String newName = "Updated Category Name";
        Long parentCategoryId = 2L;

        Category existingCategory = new Category();
        existingCategory.setId(categoryId);
        existingCategory.setName("Old Name");

        Category parentCategory = new Category();
        parentCategory.setId(parentCategoryId);
        parentCategory.setName("Parent Category");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.findById(parentCategoryId)).thenReturn(Optional.of(parentCategory));
        when(adminService.isSubcategoryOf(existingCategory, parentCategory)).thenReturn(false);

        ResponseEntity<?> response = adminController.editCategory(categoryId, newName, parentCategoryId);

        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("Category updated successfully", responseBody.get("message"));

        verify(categoryRepository).save(existingCategory);
        assertEquals(newName, existingCategory.getName());
        assertEquals(parentCategory, existingCategory.getParentCategory());
    }

    @Test
    void testAddCategoryWithoutParent() {
        String categoryName = "New Category";

        ResponseEntity<?> response = adminController.addCategory(categoryName, null);

        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("New category created successfully", responseBody.get("message"));
    }

    @Test
    void testAddCategoryWithParent() {
        String categoryName = "New Child Category";
        Long parentCategoryId = 1L;
        Category parentCategory = new Category();
        parentCategory.setId(parentCategoryId);
        parentCategory.setName("Parent Category");

        when(categoryRepository.findById(parentCategoryId)).thenReturn(Optional.of(parentCategory));

        ResponseEntity<?> response = adminController.addCategory(categoryName, parentCategoryId);

        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("New category created successfully", responseBody.get("message"));

        verify(categoryRepository).save(argThat(category -> categoryName.equals(category.getName()) && parentCategory.equals(category.getParentCategory())));
    }

    @Test
    void testAddCategoryWithNonExistentParent() {
        String categoryName = "New Category";
        Long parentCategoryId = 99L;

        when(categoryRepository.findById(parentCategoryId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = adminController.addCategory(categoryName, parentCategoryId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("Failed to create category", responseBody.get("error"));
    }

    @Test
    void testDeleteCategorySuccessfully() {
        Long categoryId = 1L;
        Map<String, Object> payload = new HashMap<>();
        payload.put("categoryId", String.valueOf(categoryId));

        when(auctionRepository.existsByCategoryId(categoryId)).thenReturn(false);
        when(categoryRepository.existsByParentCategoryId(categoryId)).thenReturn(false);

        ResponseEntity<?> response = adminController.deleteCategory(payload);

        assertEquals("Category deleted successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void testDeleteCategoryInUseByAuctions() {
        Long categoryId = 2L;
        Map<String, Object> payload = new HashMap<>();
        payload.put("categoryId", String.valueOf(categoryId));

        when(auctionRepository.existsByCategoryId(categoryId)).thenReturn(true);

        ResponseEntity<?> response = adminController.deleteCategory(payload);

        assertEquals("Cannot delete category because it has associated auctions", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void testDeleteParentCategory() {
        Long categoryId = 3L;
        Map<String, Object> payload = new HashMap<>();
        payload.put("categoryId", String.valueOf(categoryId));

        when(auctionRepository.existsByCategoryId(categoryId)).thenReturn(false);
        when(categoryRepository.existsByParentCategoryId(categoryId)).thenReturn(true);

        ResponseEntity<?> response = adminController.deleteCategory(payload);

        assertEquals(CONFLICT, response.getStatusCode());
        assertEquals("Cannot delete category because it is a parent to other categories", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void testDeleteCategoryInvalidIdFormat() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("categoryId", "invalidId");

        ResponseEntity<?> response = adminController.deleteCategory(payload);

        assertEquals(BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid category ID format", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void testSuspendUser() {
        Long userId = 1L;
        int suspensionDays = 30;
        String expectedUsername = "testUser";

        when(adminService.suspendUser(userId, suspensionDays)).thenReturn(Optional.of(expectedUsername));

        String viewName = adminController.suspendUser(userId, suspensionDays, redirectAttributes);

        assertEquals("redirect:/user/{username}", viewName);
        verify(redirectAttributes).addAttribute("username", expectedUsername);
        verify(redirectAttributes).addFlashAttribute("successMessage", "User suspended successfully for " + suspensionDays + " days.");
    }

    @Test
    void testSuspendUserWhenUserNotFound() {
        Long userId = 2L;
        int suspensionDays = 15;

        when(adminService.suspendUser(userId, suspensionDays)).thenReturn(Optional.empty());

        String viewName = adminController.suspendUser(userId, suspensionDays, redirectAttributes);

        assertEquals("redirect:/user/{username}", viewName);
        verify(redirectAttributes, never()).addFlashAttribute("successMessage", "User suspended successfully for " + suspensionDays + " days.");
    }

    @Test
    public void testBanUserSuccess() {
        Long userId = 1L;
        String expectedUsername = "testUser";
        when(adminService.banUser(userId)).thenReturn(Optional.of(expectedUsername));

        String viewName = adminController.banUser(userId, redirectAttributes);

        assertEquals("redirect:/user/{username}", viewName);
        verify(redirectAttributes).addAttribute("username", expectedUsername);
        verify(redirectAttributes).addFlashAttribute("successMessage", "User banned successfully");
    }

    @Test
    public void testBanUserFailure() {
        Long userId = 2L;
        when(adminService.banUser(userId)).thenReturn(Optional.empty());

        String viewName = adminController.banUser(userId, redirectAttributes);

        assertEquals("redirect:/user/{username}", viewName);
        verify(redirectAttributes, never()).addFlashAttribute(eq("successMessage"), eq("User banned successfully"));
    }

    @Test
    public void testUnbanUserSuccess() {
        Long userId = 1L;
        String expectedUsername = "testUser";
        when(adminService.unbanUser(userId)).thenReturn(Optional.of(expectedUsername));

        String viewName = adminController.unbanUser(userId, redirectAttributes);

        assertEquals("redirect:/user/{username}", viewName);
        verify(redirectAttributes).addAttribute("username", expectedUsername);
        verify(redirectAttributes).addFlashAttribute("successMessage", "User unbanned successfully.");
    }

    @Test
    public void testUnbanUserFailure() {
        Long userId = 1L;
        when(adminService.unbanUser(userId)).thenReturn(Optional.empty());

        String viewName = adminController.unbanUser(userId, redirectAttributes);

        assertEquals("redirect:/user/{username}", viewName);
        verify(redirectAttributes, never()).addAttribute(eq("username"), anyString()); // Use anyString() instead of any()
        verify(redirectAttributes, never()).addFlashAttribute(eq("successMessage"), anyString()); // Use anyString() for consistency and clarity
    }

    @Test
    public void testUnsuspendUserSuccess() {
        Long userId = 1L;
        String expectedUsername = "testUser";
        when(adminService.unsuspendUser(userId)).thenReturn(Optional.of(expectedUsername));

        String viewName = adminController.unsuspendUser(userId, redirectAttributes);

        assertEquals("redirect:/user/{username}", viewName);
        verify(redirectAttributes).addAttribute("username", expectedUsername);
        verify(redirectAttributes).addFlashAttribute("successMessage", "User suspension lifted successfully.");
    }

    @Test
    public void testUnsuspendUserFailure() {
        Long userId = 1L;
        when(adminService.unsuspendUser(userId)).thenReturn(Optional.empty());

        String viewName = adminController.unsuspendUser(userId, redirectAttributes);

        assertEquals("redirect:/user/{username}", viewName);
        verify(redirectAttributes, never()).addAttribute(eq("username"), anyString());
        verify(redirectAttributes, never()).addFlashAttribute(eq("successMessage"), anyString());
    }

}
