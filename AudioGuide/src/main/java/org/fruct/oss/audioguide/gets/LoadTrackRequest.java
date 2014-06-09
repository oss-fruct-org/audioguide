package org.fruct.oss.audioguide.gets;

import android.util.Xml;

import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;
import org.fruct.oss.audioguide.parsers.Kml;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;

public class LoadTrackRequest extends GetsRequest {
	private final String channelName;

	public LoadTrackRequest(Gets gets, String channelName) {
		super(gets);
		this.channelName = channelName;
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
			serializer.startTag(null, "name").text(channelName).endTag(null, "name");
			serializer.endTag(null, "params").endTag(null, "request");
			serializer.flush();

			return writer.toString();
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	protected String getRequestUrl() {
		return Gets.GETS_SERVER + "/loadTrack.php";
	}

	@Override
	protected Class<? extends IContent> getContentClass() {
		return Kml.class;
	}

	@Override
	protected void onPostProcess(GetsResponse response) {

	}

	@Override
	protected void onError() {

	}
}
