package org.fruct.oss.audioguide.parsers;

import org.fruct.oss.audioguide.track.Point;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.util.List;
import java.util.StringTokenizer;

@Root(name = "kml", strict = false)
public class Kml implements IContent {
	public List<Point> getPoints() {
		return points;
	}

	public String getName() {
		return name;
	}

	public int getOpen() {
		return open;
	}

	public String getDescription() {
		return description;
	}

	@Path("Document")
	@Element(name = "name")
	private String name;

	@Path("Document")
	@Element(name = "open")
	private int open;

	@Path("Document")
	@Element(name = "description", data = true, required = false)
	private String description;

	@Path("Document")
	@ElementList(inline = true, entry = "Placemark")
	private List<Point> points;
}
