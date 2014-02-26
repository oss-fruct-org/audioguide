package org.fruct.oss.audioguide.track;

public class Point {
	private String name;
	private String description;

	private String audioUrl;
	private int latE6;
	private int lonE6;

	public Point(String name, String description, String audioUrl, int latE6, int lonE6) {
		this.name = name;
		this.description = description;
		this.audioUrl = audioUrl;
		this.latE6 = latE6;
		this.lonE6 = lonE6;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getAudioUrl() {
		return audioUrl;
	}

	public int getLatE6() {
		return latE6;
	}

	public int getLonE6() {
		return lonE6;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Point point = (Point) o;

		if (name != null ? !name.equals(point.name) : point.name != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}
}
