package org.fruct.oss.audioguide.track;

public class Track {
	private String name;
	private String description;
	private String url;

	private int localId;
	private boolean isLocal;
	private boolean isActive;

	public Track(String name, String description, String url) {
		this.name = name;
		this.description = description;
		this.url = url;
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

	public boolean isLocal() {
		return isLocal;
	}

	public boolean isActive() {
		return isActive;
	}

	public int getLocalId() {
		return localId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setLocal(boolean isLocal) {
		this.isLocal = isLocal;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Track track = (Track) o;

		if (name != null ? !name.equals(track.name) : track.name != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}
}
