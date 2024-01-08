package pl.use.auction.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.use.auction.model.Category;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    Optional<Category> findByNameIgnoreCase(String name);

    @Query(value = "SELECT c.*, COUNT(a.category_id) as auctionCount FROM Category c " +
            "LEFT JOIN Auction a ON c.id = a.category_id " +
            "GROUP BY c.id " +
            "ORDER BY auctionCount DESC",
            nativeQuery = true)
    List<Category> findCategoriesOrderedByAuctionCount();
}