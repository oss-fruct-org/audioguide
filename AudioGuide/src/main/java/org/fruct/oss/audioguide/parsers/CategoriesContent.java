package org.fruct.oss.audioguide.parsers;

import org.fruct.oss.audioguide.util.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CategoriesContent implements IContent {
	private List<Category> categories = new ArrayList<Category>();

	public static IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "content");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "categories");

		CategoriesContent content = new CategoriesContent();

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			if (tagName.equals("category")) {
				content.categories.add(parseCategory(parser));
			} else {
				Utils.skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "categories");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, null, "content");

		return content;
	}

	private static Category parseCategory(XmlPullParser parser) throws IOException, XmlPullParserException {
		long id = 0;
		String name = null;
		String description = null;
		String url = null;

		parser.require(XmlPullParser.START_TAG, null, "category");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();

			if (tagName.equals("id")) {
				id = Long.parseLong(GetsResponse.readText(parser));
			} else if (tagName.equals("name")) {
				name = GetsResponse.readText(parser);
			} else if (tagName.equals("description")) {
				description = GetsResponse.readText(parser);
			} else if (tagName.equals("url")) {
				url = GetsResponse.readText(parser);
			} else {
				Utils.skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "category");

		return new Category(id, name, description, url);
	}

	public List<Category> getCategories() {
		return categories;
	}

	public List<Category> filterByPrefix() {
		Pattern pattern = Pattern.compile("^(image|audio)\\..*");
		List<Category> matchedCategories = new ArrayList<Category>();

		for (Category category : categories) {
			Matcher matcher = pattern.matcher(category.getName());
			if (matcher.matches()) {
				matchedCategories.add(category);
			}
		}

		return matchedCategories;
	}

	public static class Category {
		private long id;
		private String name;
		private String description;
		private String url;

		public Category(long id, String name, String description, String url) {
			this.id = id;
			this.name = name;
			this.description = description;
			this.url = url;
		}

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public String getUrl() {
			return url;
		}
	}
}
