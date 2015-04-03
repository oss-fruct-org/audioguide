package org.fruct.oss.audioguide.util;

import com.nostra13.universalimageloader.utils.IoUtils;

public abstract class IntervalCopyListener implements IoUtils.CopyListener {
	private final int interval;

	private int lastInterval;

	public IntervalCopyListener(int interval) {
		this.interval = interval;
	}

	@Override
	public boolean onBytesCopied(int current, int total) {
		int currentInterval = current / interval;
		if (currentInterval > lastInterval) {
			lastInterval = currentInterval;
			onProgress(current, total);
		}

		return true;
	}

	public abstract void onProgress(int current, int total);
}
