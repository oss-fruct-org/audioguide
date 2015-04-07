package org.fruct.oss.audioguide.track.tasks;

import android.os.AsyncTask;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.track.gets.Category;
import org.fruct.oss.audioguide.track.Database;
import org.fruct.oss.audioguide.track.gets.Gets;
import org.fruct.oss.audioguide.track.gets.GetsException;
import org.fruct.oss.audioguide.track.gets.parsers.CategoriesContent;
import org.fruct.oss.audioguide.track.gets.parsers.CategoriesParser;

import java.io.IOException;
import java.util.List;


public class CategoriesTask extends AsyncTask<Void, Void, List<Category>> {
	public List<Category> executeSync() {
		return doInBackground();
	}

	@Override
	protected List<Category> doInBackground(Void... params) {
		String request = "<request><params/></request>";
		Gets gets = new Gets(Gets.GETS_SERVER);

		try {
			CategoriesContent cats = gets.query("getCategories.php", request, new CategoriesParser());
			List<Category> filteredCats = CategoriesContent.filterByPrefix(cats.getCategories());

			storeToDatabase(filteredCats);
			return filteredCats;
		} catch (IOException | GetsException e) {
			return null;
		}
	}

	private void storeToDatabase(List<Category> categories) {
		Database database = App.getInstance().getDatabase();
		database.updateCategories(categories);
	}
}
