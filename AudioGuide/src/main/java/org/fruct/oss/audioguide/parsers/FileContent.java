package org.fruct.oss.audioguide.parsers;

import org.fruct.oss.audioguide.util.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class FileContent implements IContent {
	private String title;
	private String mimeType;
	private String url;

	public String getTitle() {
		return title;
	}

	public String getMimeType() {
		return mimeType;
	}

	public String getUrl() {
		return url;
	}

	public boolean isImage() {
		return mimeType.startsWith("image/");
	}

	public static IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, null);
		boolean startFromContent = false;
		if (parser.getName().equals("content")) {
			startFromContent = true;
			parser.nextTag();
		}

		FileContent fileContent = new FileContent();
		parser.require(XmlPullParser.START_TAG, null, "file");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();

			if (tagName.equals("title")) {
				fileContent.title = GetsResponse.readText(parser);
			} else if (tagName.equals("mimeType")) {
				fileContent.mimeType = GetsResponse.readText(parser);
			} else if (tagName.equals("downloadUrl")) {
				fileContent.url = GetsResponse.readText(parser);
			} else {
				Utils.skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "file");

		if (startFromContent) {
			parser.nextTag();
			parser.require(XmlPullParser.END_TAG, null, "content");
		}

		return fileContent;
	}
}
