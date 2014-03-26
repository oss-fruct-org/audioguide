package org.fruct.oss.audioguide.parsers;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementUnion;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

@Root(name = "response", strict = false)
public class GetsResponse {
	@Element(name = "code", required = true)
	@Path("status")
	private int code;

	@Element(name = "message", required = true)
	@Path("status")
	private String message;

	@Path("content")
	@ElementUnion({
		@Element(name = "tracks", type = TracksContent.class),
		@Element(name = "kml", type = Kml.class),
		@Element(type = AuthRedirectResponse.class)
	})
	private IContent content;

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public IContent getContent() {
		return content;
	}

	public static GetsResponse parse(String responseXml) throws GetsException {
		Serializer serializer = new Persister();
		try {
			return serializer.read(GetsResponse.class, responseXml);
		} catch (Exception e) {
			throw new GetsException(e);
		}
	}
}
