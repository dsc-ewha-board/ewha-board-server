package com.february.edsc.service;

import com.february.edsc.domain.category.CategoryPackResponseDto;
import com.february.edsc.domain.category.Category;
import com.february.edsc.domain.category.CategoryRequestDto;
import com.february.edsc.domain.category.CategoryResponseDto;
import com.february.edsc.domain.post.Post;
import com.february.edsc.domain.post.PostListResponseDto;
import com.february.edsc.domain.post.PostResponseDto;
import com.february.edsc.repository.CategoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CategoryService {

	private final CategoryRepository categoryRepository;

	public Optional<Category> findByName(String categoryName) {
		return categoryRepository.findByName(categoryName);
	}

	public Optional<Category> findById(Long parentId) {
		return categoryRepository.findById(parentId);
	}
	
	@Transactional
	public List<CategoryPackResponseDto> getCategories() {
		List<CategoryPackResponseDto> categoryPackResponseDtoList = new ArrayList<>();
		long id = 1;
		for (Category parentCategory : categoryRepository.findAllByLevel(1)) {
			List<CategoryResponseDto> child = getChild(parentCategory);
			long parentNum = child.stream().mapToLong(CategoryResponseDto::getPostNum).sum();
			categoryPackResponseDtoList.add(
				CategoryPackResponseDto.builder()
					.id(id++)
					.parent(parentCategory.toCategoryParentResponseDto(parentNum))
					.childNum((long) child.size())
					.child(child)
					.build());
		}
		return categoryPackResponseDtoList;
	}

	@Transactional
	public List<CategoryResponseDto> getChild(Category category) {
		return categoryRepository.findAllByParentIdAndLevel(category.getId(), 2)
			.stream().map(Category::toCategoryChildResponseDto).collect(Collectors.toList());
	}

	@Transactional
	public PostListResponseDto getPostsByParentCategory(Category category1) {
		List<PostResponseDto> posts = new ArrayList<>();
		categoryRepository.findAllByParentId(category1.getId())
			.stream().map(Category::getPosts)
			.forEach(childPost -> childPost
				.forEach(post -> posts.add(post.toPostResponseDto())));
		return PostListResponseDto.builder()
			.totalNum(posts.size())
			.postList(posts)
			.build();
	}

	@Transactional
	public PostListResponseDto getPostsByChildCategory(Category category2) {
		List<PostResponseDto> posts =
			category2.getPosts().stream()
				.map(Post::toPostResponseDto).collect(Collectors.toList());
		return PostListResponseDto.builder()
			.totalNum(posts.size())
			.postList(posts)
			.build();
	}

	@Transactional
	public String createCategoryChild(CategoryRequestDto categoryRequestDto, Category parentCategory) {
		Category category = categoryRepository.save(Category.builder()
			.name(categoryRequestDto.getName())
			.level(categoryRequestDto.getLevel())
			.parent(parentCategory)
			.build());
		return parentCategory.getName() + "/" + category.getName();
	}

	@Transactional
	public String createCategoryParent(CategoryRequestDto categoryRequestDto) {
		Category category = categoryRepository.save(Category.builder()
			.name(categoryRequestDto.getName())
			.level(categoryRequestDto.getLevel())
			.build());
		category.setParent(category);
		return category.getName();
	}
}
