package org.fruct.oss.audioguide.files;

public interface PostProcessor<T> {
	T postProcess(String localUrl);
}
