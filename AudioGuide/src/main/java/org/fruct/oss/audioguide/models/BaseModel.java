package org.fruct.oss.audioguide.models;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class BaseModel<T> implements Model<T> {
	protected ArrayList<T> list = new ArrayList<T>();
	private ArrayList<ModelListener> listeners = new ArrayList<ModelListener>();
	protected Handler handler = new Handler(Looper.getMainLooper());

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public T getItem(int position) {
		return list.get(position);
	}

	@Override
	public synchronized void addListener(ModelListener listener) {
		listeners.add(listener);
	}

	@Override
	public synchronized void removeListener(ModelListener listener) {
		listeners.remove(listener);
	}

	public void setData(Collection<T> coll) {
		list.clear();
		for (T t : coll) {
			list.add(t);
		}

		handler.post(new Runnable() {
			@Override
			public void run() {
				notifyDataSetChanged();
			}
		});
	}

	public void insertElement(T t) {
		list.add(t);

		handler.post(new Runnable() {
			@Override
			public void run() {
				notifyDataSetChanged();
			}
		});
	}

	public void notifyDataSetChanged() {
		for (ModelListener listener : listeners) {
			listener.dataSetChanged();
		}
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private int index;

			@Override
			public boolean hasNext() {
				return index < getCount();
			}

			@Override
			public T next() {
				return getItem(index++);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Can't remove from model");
			}
		};
	}
}
