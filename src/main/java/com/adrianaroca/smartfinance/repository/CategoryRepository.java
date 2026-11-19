package com.adrianaroca.smartfinance.repository;

import com.adrianaroca.smartfinance.model.Category;
import com.adrianaroca.smartfinance.model.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByTypeAndUserIsNullOrTypeAndUser_IdOrderByNameAsc(
            CategoryType globalType,
            CategoryType userType,
            Long userId
    );

    List<Category> findAllByUser_IdOrderByTypeAscNameAsc(Long userId);

    java.util.Optional<Category> findByIdAndUser_Id(Long id, Long userId);

    void deleteAllByUser(com.adrianaroca.smartfinance.model.User user);
}
