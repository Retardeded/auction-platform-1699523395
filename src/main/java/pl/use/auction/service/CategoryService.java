package pl.use.auction.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import pl.use.auction.model.Category;
import pl.use.auction.repository.CategoryRepository;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAllCategories() {
        return categoryRepository.findByParentCategoryIsNull();
    }

    public List<Category> findAllMainCategoriesWithSubcategories() {
        List<Category> mainCategories = categoryRepository.findByParentCategoryIsNull();
        mainCategories.forEach(category -> category.getChildCategories().size());
        return mainCategories;
    }

    public Category findCategoryById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }
}
