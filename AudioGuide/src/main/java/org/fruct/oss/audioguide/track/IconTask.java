package org.fruct.oss.audioguide.track;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;

import org.fruct.oss.audioguide.util.Downloader;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconTask extends AsyncTask<Uri, Void, Uri> {
	private final static Logger log = LoggerFactory.getLogger(IconTask.class);
	private final Utils.Function<Void, Drawable> callback;
	private final Downloader iconDownloader;

	public IconTask(Utils.Function<Void, Drawable> callback, Downloader iconDownloader) {
		this.callback = callback;
		this.iconDownloader = iconDownloader;
	}

	@Override
	protected Uri doInBackground(Uri... uris) {
		try {
			Uri localUri = iconDownloader.waitUri(uris[0]);

			log.debug("IconTask: {} {}", uris[0], localUri);

			return localUri;
		} catch (InterruptedException e) {
			log.debug("IconTask interrupted");
			return null;
		}
	}

	@Override
	protected void onPostExecute(Uri uri) {
		if (uri != null) {
			Bitmap largeBitmap = BitmapFactory.decodeFile(uri.getPath());
			Bitmap thumbBitmap = ThumbnailUtils.extractThumbnail(largeBitmap,
					Utils.getDP(48), Utils.getDP(48));
			largeBitmap.recycle();

			Drawable drawable = new BitmapDrawable(Resources.getSystem(), thumbBitmap);
			callback.apply(drawable);
		}
	}
}
