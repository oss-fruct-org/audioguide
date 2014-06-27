package org.fruct.oss.audioguide.track;

import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.util.Utils;

import java.util.List;

public interface CategoriesBackend {
	void loadCategories(Utils.Callback<List<Category>> callback);
}
