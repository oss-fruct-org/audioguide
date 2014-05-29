package org.fruct.oss.audioguide.files;

public class IconParams {
	private int width, height;
	private String url;
	private FileManager.ScaleMode mode;

	public IconParams(int width, int height, String url, FileManager.ScaleMode mode) {
		this.width = width;
		this.height = height;
		this.url = url;
		this.mode = mode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		IconParams that = (IconParams) o;

		if (height != that.height) return false;
		if (width != that.width) return false;
		if (mode != that.mode) return false;
		if (!url.equals(that.url)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = width;
		result = 31 * result + height;
		result = 31 * result + url.hashCode();
		result = 31 * result + mode.hashCode();
		return result;
	}
}
