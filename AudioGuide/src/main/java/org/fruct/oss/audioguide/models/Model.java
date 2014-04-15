package org.fruct.oss.audioguide.models;

public interface Model<T> extends Iterable<T> {
	int getCount();
	T getItem(int position);
	void addListener(ModelListener listener);
	void removeListener(ModelListener listener);
}
