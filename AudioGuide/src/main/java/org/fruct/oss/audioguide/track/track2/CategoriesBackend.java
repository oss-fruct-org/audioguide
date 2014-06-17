package org.fruct.oss.audioguide.track.track2;

import org.fruct.oss.audioguide.gets.Category;

import java.util.List;

public interface CategoriesBackend {
	List<Category> loadCategories();
}
