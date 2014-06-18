package org.fruct.oss.audioguide.models;

import android.os.Handler;
import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


public abstract class FilterModel<T> extends BaseModel<T> implements ModelListener, Closeable {
	private static Logger log = LoggerFactory.getLogger(FilterModel.class);

	public abstract boolean check(T t);

	private Model<T> baseModel;

	private boolean isClosed;

	public FilterModel(Model<T> baseModel) {
		this.baseModel = baseModel;
		baseModel.addListener(this);
		dataSetChanged();
	}

	@Override
	public void close() {
		isClosed = true;
		baseModel.removeListener(this);
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();

		if (!isClosed) {
			close();
			log.warn("FilterModel haven't closed correctly!");
		}
	}

	@Override
	public void dataSetChanged() {
		list.clear();

		for (T t : baseModel) {
			if (check(t)) {
				list.add(t);
			}
		}

		handler.post(new Runnable() {
			@Override
			public void run() {
				notifyDataSetChanged();
			}
		});
	}
}
