package org.fruct.oss.audioguide.files;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

public class ImageViewSetter implements BitmapSetter {
	private final ImageView imageView;
	private BitmapProcessor tag;
	private Handler handler = new Handler(Looper.getMainLooper());
	private Bitmap bitmap;

	public ImageViewSetter(ImageView imageView) {
		this.imageView = imageView;
	}

	@Override
	public void bitmapReady(final Bitmap newBitmap, final BitmapProcessor checkTag) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (!checkTag.equals(tag)) {
					return;
				}

				imageView.setImageDrawable(new BitmapDrawable(Resources.getSystem(), newBitmap));
				if (bitmap != null && !bitmap.isRecycled()) {
					bitmap.recycle();
				}
				ImageViewSetter.this.bitmap = newBitmap;
			}
		});
	}

	@Override
	public void recycle() {
		if (bitmap != null && !bitmap.isRecycled()) {
			imageView.setImageDrawable(null);
			bitmap.recycle();
		}
	}

	@Override
	public void setTag(BitmapProcessor tag) {
		this.tag = tag;
	}

	@Override
	public BitmapProcessor getTag() {
		return tag;
	}
}
