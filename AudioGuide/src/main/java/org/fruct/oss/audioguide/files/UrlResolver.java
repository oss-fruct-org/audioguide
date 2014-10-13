package org.fruct.oss.audioguide.files;

public interface UrlResolver {
	String getUrl(String fileUrl, FileSource.Variant variant);
}
