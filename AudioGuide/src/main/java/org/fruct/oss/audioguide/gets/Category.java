package org.fruct.oss.audioguide.gets;

public class Category {
	private final long id;
	private final String name;
	private final String description;
	private final String url;
	private boolean isActive;

	public Category(long id, String name, String description, String url, boolean isActive) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.url = url;
		this.isActive = isActive;
	}

	public Category(long id, String name, String description, String url) {
		this(id, name, description, url, true);
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

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean active) {
		this.isActive = active;
	}
}
