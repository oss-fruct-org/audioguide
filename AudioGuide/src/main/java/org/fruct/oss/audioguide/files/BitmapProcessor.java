package org.fruct.oss.audioguide.files;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;

import org.fruct.oss.audioguide.util.AUtils;

import java.util.concurrent.Future;

public class BitmapProcessor implements PostProcessor<Bitmap> {
	private Future<Bitmap> future;
	private FileManager.ScaleMode scaleMode;

	private int width;
	private int height;

	private BitmapSetter setter;

	public BitmapProcessor(FileManager.ScaleMode scaleMode,
						   final int width, final int height, BitmapSetter setter) {
		this.scaleMode = scaleMode;
		this.width = width;
		this.height = height;
		this.setter = setter;
	}

	@Override
	public Bitmap postProcess(String localUrl) {
		Bitmap bitmap = AUtils.decodeSampledBitmapFromResource(Resources.getSystem(), localUrl, width, height);

		if (scaleMode != FileManager.ScaleMode.NO_SCALE) {
			if (height < 0) {
				throw new IllegalArgumentException("Negative height can only be used with NO_SCALE");
			}

			float ax = bitmap.getWidth() / (float) width;
			float ay = bitmap.getHeight() / (float) height;
			float ma = scaleMode == FileManager.ScaleMode.SCALE_CROP
					? Math.min(ax, ay)
					: Math.max(ax, ay);

			Matrix matrix = new Matrix();
			matrix.postScale(1 / ma, 1 / ma);

			Bitmap oldBitmap = bitmap;
			if (scaleMode == FileManager.ScaleMode.SCALE_CROP) {
				// TODO: scale_crop can't handle non-square dst
				int minDim = Math.min(bitmap.getWidth(), bitmap.getHeight());
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, minDim, minDim, matrix, true);
			} else {
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
			}
			oldBitmap.recycle();
		}

		synchronized (setter) {
			setter.bitmapReady(bitmap, this);
		}

		return bitmap;
	}

	public static BitmapProcessor requestBitmap(FileManager fileManager, String remoteUrl, FileSource.Variant variant,
									 final int width, final int height, FileManager.ScaleMode scaleMode, BitmapSetter setter) {
		synchronized (setter) {
			BitmapProcessor processor = new BitmapProcessor(scaleMode, width, height, setter);

			BitmapProcessor existingProcessor = setter.getTag();
			if (existingProcessor != null) {
				synchronized (existingProcessor) {
					if (existingProcessor.future != null) {
						existingProcessor.future.cancel(true);
					}
				}
			}

			setter.setTag(processor);

			processor.future = fileManager.postDownload(remoteUrl, variant, FileManager.Storage.CACHE, processor);
			return processor;
		}
	}

	public void recycle() {
		setter.recycle();
	}
}
