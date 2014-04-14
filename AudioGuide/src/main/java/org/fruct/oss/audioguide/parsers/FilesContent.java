package org.fruct.oss.audioguide.parsers;

import org.fruct.oss.audioguide.track.ArrayStorage;
import org.fruct.oss.audioguide.util.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FilesContent implements IContent {
	private ArrayList<FileContent> files;

	public List<FileContent> getFiles() {
		return files;
	}

	public static IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		FilesContent filesContent = new FilesContent();
		filesContent.files = new ArrayList<FileContent>();

		parser.require(XmlPullParser.START_TAG, null, "content");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "files");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			if (tagName.equals("file")) {
				filesContent.files.add((FileContent) FileContent.parse(parser));
			} else {
				Utils.skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "files");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, null, "content");

		return filesContent;
	}
}
