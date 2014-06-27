package org.fruct.oss.audioguide.models;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CombineModel<T> implements Model<T>, Closeable, ModelListener {
	private final Model<T>[] parents;

	private final ArrayList<ModelListener> listeners = new ArrayList<ModelListener>();

	private transient int count = -1;

	public CombineModel(Model<T>... parents) {
		this.parents = parents;
		for (Model<T> model : parents) {
			model.addListener(this);
		}
	}


	@Override
	public void close() throws IOException {
		for (Model<T> model : parents) {
			model.removeListener(this);
		}
	}

	@Override
	public void dataSetChanged() {
		count = -1;

		for (ModelListener listener : listeners) {
			listener.dataSetChanged();
		}
	}

	@Override
	public int getCount() {
		if (count == -1) {
			count = 0;
			for (Model<T> model : parents) {
				count += model.getCount();
			}
		}

		return count;
	}

	@Override
	public T getItem(int position) {
		int c = 0;
		for (Model<T> model : parents) {
			final int mc = model.getCount();
			if (position < mc + c) {
				return model.getItem(position - c);
			}
			c += mc;
		}

		throw new ArrayIndexOutOfBoundsException(position);
	}

	@Override
	public void addListener(ModelListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ModelListener listener) {
		listeners.remove(listener);
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private int idx = 0;
			private int modelIdx = 0;

			private int localIdx = 0;

			private int currentModelCount = parents[0].getCount();
			private int count = getCount();

			@Override
			public boolean hasNext() {
				return idx < count;
			}

			@Override
			public T next() {
				if (localIdx == currentModelCount) {
					modelIdx++;

					if (modelIdx == parents.length)
						throw new NoSuchElementException();

					localIdx = 0;
					currentModelCount = parents[modelIdx].getCount();
				}
				idx++;
				return parents[modelIdx].getItem(localIdx++);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Can't remove from model");
			}
		};
	}

}
