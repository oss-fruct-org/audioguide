package org.fruct.oss.audioguide.files;

import android.graphics.Bitmap;

public interface BitmapSetter {
	void bitmapReady(Bitmap bitmap, BitmapProcessor checkTag);
	void recycle();

	void setTag(BitmapProcessor tag);
	BitmapProcessor getTag();
}
