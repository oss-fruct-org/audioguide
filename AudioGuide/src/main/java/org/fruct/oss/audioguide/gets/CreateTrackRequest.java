package org.fruct.oss.audioguide.gets;

import android.util.Xml;

import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.util.Utils;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;

public class CreateTrackRequest extends GetsRequest {
	private Track track;

	public CreateTrackRequest(Gets gets, Track track) {
		super(gets);
		this.track = track;
	}

	@Override
	protected String createRequestString() {
		try {
			XmlSerializer serializer = Xml.newSerializer();
			StringWriter writer = new StringWriter();
			serializer.setOutput(writer);

			serializer.startDocument("UTF-8", true);


			serializer.startTag(null, "request").startTag(null, "params");
			gets.writeTokenTag(serializer);
			serializer.startTag(null, "name").text(track.getName()).endTag(null, "name");
			if (!Utils.isNullOrEmpty(track.getHname()))
				serializer.startTag(null, "hname").text(track.getHname()).endTag(null, "hname");
			serializer.startTag(null, "description").text(track.getDescription()).endTag(null, "description")
					.startTag(null, "url").text(track.getUrl()).endTag(null, "url")
					.endTag(null, "params").endTag(null, "request").endDocument();
			serializer.flush();

			return writer.toString();
		} catch (IOException e) {
			return null;
		}

	}

	@Override
	protected String getRequestUrl() {
		return Gets.GETS_SERVER + "/createTrack.php";
	}

	@Override
	protected int getPriority() {
		return 4;
	}

	@Override
	protected Class<? extends IContent> getContentClass() {
		return IContent.class;
	}

	@Override
	protected void onPostProcess(GetsResponse response) {

	}

	@Override
	protected void onError() {

	}
}
