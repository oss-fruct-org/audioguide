package org.fruct.oss.audioguide.track.track2;

import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.util.Utils;

import java.util.List;

public interface CategoriesBackend {
	void loadCategories(Utils.Callback<List<Category>> callback);
}
