package org.fruct.oss.audioguide.gets;

public class Category {
	private long id;
	private String name;
	private String description;
	private String url;

	public Category(long id, String name, String description, String url) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.url = url;
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
}
