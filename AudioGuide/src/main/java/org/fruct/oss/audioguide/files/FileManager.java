package org.fruct.oss.audioguide.files;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import org.fruct.oss.audioguide.fragments.PanelFragment;
import org.fruct.oss.audioguide.parsers.GetsException;

import java.io.Closeable;
import java.io.IOException;

public interface FileManager extends Closeable {

	void close();

	public enum ScaleMode {
		NO_SCALE, SCALE_CROP, SCALE_FIT
	}

	Uri insertLocalFile(String title, Uri localUri);
	void insertRemoteFile(String title, Uri remoteUri);

	String getLocalPath(Uri remoteUri);
	Uri uploadLocalFile(Uri localUri) throws IOException, GetsException;

	void requestAudioDownload(String remoteUrl);

	void requestImageBitmap(String remoteUrl, int width, int height, ScaleMode mode, BitmapSetter bitmapSetter, String clientId);

	void recycleAllBitmaps(String clientId);

	boolean isLocal(Uri uri);

	void addWeakListener(FileListener fileListener);
	void removeListener(FileListener fileListener);



	public static interface BitmapSetter {
		void bitmapReady(Bitmap bitmap, Object checkTag);
		void recycle();

		void setTag(Object tag);
		Object getTag();
	}

	public static class ImageViewSetter implements BitmapSetter {
		private final ImageView imageView;
		private Object tag;
		private Handler handler = new Handler(Looper.getMainLooper());
		private Bitmap bitmap;

		public ImageViewSetter(ImageView imageView) {
			this.imageView = imageView;
		}

		@Override
		public void bitmapReady(final Bitmap newBitmap, final Object checkTag) {
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
		public void setTag(Object tag) {
			this.tag = tag;
		}

		@Override
		public Object getTag() {
			return tag;
		}
	}
}
