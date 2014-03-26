package org.fruct.oss.audioguide.parsers;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementUnion;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

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
		@Element(name = "kml", type = Kml.class)
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
}
