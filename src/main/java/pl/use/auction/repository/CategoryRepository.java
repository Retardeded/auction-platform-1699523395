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
    Optional<Category> findByNameAndParentCategory(String name, Category parentCategory);
    Optional<Category> findByNameIgnoreCase(String name);
    List<Category> findByParentCategoryIsNull();
    boolean existsByParentCategoryId(Long categoryId);
}