package org.fruct.oss.audioguide.overlays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.MotionEvent;

import org.fruct.oss.audioguide.MainActivity;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.util.Utils;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import java.io.Closeable;
import java.util.Timer;
import java.util.TimerTask;

public class DetailsOverlay extends Overlay implements Closeable {
	private static final long ANIMATION_TIME = 500;
	private final Point point;
	private final Rect detailsBounds = new Rect();
	private final int itemSize;
	private final org.fruct.oss.audioguide.files.FileManager fileManager;
	private final Drawable markerDrawable;
	private final Rect markerPadding = new Rect();
	private final MapView view;
	private final TextPaint titlePaint;

	private final TextPaint descriptionPaint;

	private final StaticLayout descriptionTextLayout;
	private final Context context;

	private Bitmap itemFullIcon;
	private boolean isClosed;
	private final StaticLayout titleTextLayout;

	private boolean isAnimationActive = false;
	private boolean isImageHidden = false;
	private long animationStartMs = 0;

	private int imageWidth;
	private int imageHeight;

	public DetailsOverlay(Context context, MapView view, Point point) {
		super(context);

		this.context = context;

		this.point = point;
		this.itemSize = Utils.getDP(24);
		this.fileManager = FileManager.getInstance();
		this.view = view;

		markerDrawable = context.getResources().getDrawable(R.drawable.marker_2);
		markerDrawable.getPadding(markerPadding);

		if (point.hasPhoto()) {
			startImageLoading();
		}

		titlePaint = new TextPaint();
		titlePaint.setColor(0xffffffff);
		titlePaint.setStyle(Paint.Style.FILL);
		titlePaint.setTextSize(Utils.getSP(24));
		titlePaint.setAntiAlias(true);
		titlePaint.setTextAlign(Paint.Align.LEFT);

		descriptionPaint = new TextPaint(titlePaint);
		descriptionPaint.setTextSize(Utils.getSP(12));
		descriptionPaint.setTextAlign(Paint.Align.LEFT);

		MapView.Projection proj = view.getProjection();

		int contentWidth = proj.getIntrinsicScreenRect().width() - itemSize * 2 - markerPadding.left - markerPadding.right;
		int contentHeight = proj.getIntrinsicScreenRect().height() - itemSize * 2 - markerPadding.top - markerPadding.bottom;

		titleTextLayout = new StaticLayout(point.getName(), titlePaint,
				contentWidth, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
		descriptionTextLayout = new StaticLayout(point.getDescription(), descriptionPaint, contentWidth,
				Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
	}

	private void startImageLoading() {
		AsyncTask<Point, Void, Bitmap> loadTask = new AsyncTask<Point, Void, Bitmap>() {
			@Override
			protected Bitmap doInBackground(Point... points) {
				Rect rect = view.getProjection().getIntrinsicScreenRect();
				imageWidth = Math.min(rect.width() - itemSize * 2 - markerPadding.left - markerPadding.right,
						rect.height() - itemSize * 2 - markerPadding.top - markerPadding.bottom);
				imageHeight = imageWidth;
				return fileManager.getImageBitmap(point.getPhotoUrl(), imageWidth,
						imageHeight, FileManager.ScaleMode.SCALE_FIT);
			}

			@Override
			protected void onPostExecute(Bitmap bitmap) {
				if (!isClosed) {
					itemFullIcon = bitmap;
					view.invalidate();
				}
			}
		};
		loadTask.execute(point);
	}

	@Override
	protected void draw(Canvas canvas, MapView mapView, boolean b) {
		if (b)
			return;

		drawDraggingItem(canvas, mapView);
	}

	private void drawDraggingItem(Canvas canvas, MapView view) {
		Rect rect = view.getProjection().getIntrinsicScreenRect();

		detailsBounds.set(rect.left + itemSize + markerPadding.left,
				rect.top + itemSize + markerPadding.top,
				rect.right - itemSize - markerPadding.right,
				rect.bottom - itemSize - markerPadding.bottom);

		markerDrawable.setAlpha(230);
		markerDrawable.setBounds(rect.left + itemSize, rect.top + itemSize,
				rect.right - itemSize, rect.bottom - itemSize);
		markerDrawable.draw(canvas);
		markerDrawable.setAlpha(255);

		canvas.save();
		canvas.translate(detailsBounds.left, detailsBounds.top);
		titleTextLayout.draw(canvas);
		canvas.restore();

		int descriptionOffset = 0;

		if (itemFullIcon != null && !isImageHidden) {
			if (isAnimationActive) {
				descriptionOffset = (int) (imageHeight - imageHeight * (System.currentTimeMillis() - animationStartMs) / ANIMATION_TIME);
			} else {
				descriptionOffset = imageHeight;
			}

			canvas.save();

			if (isAnimationActive) {
				canvas.clipRect(detailsBounds.left, detailsBounds.top, detailsBounds.right,
						detailsBounds.top + titleTextLayout.getHeight() + descriptionOffset);
			}

			canvas.drawBitmap(itemFullIcon,
					detailsBounds.left + detailsBounds.width() / 2 - itemFullIcon.getWidth() / 2,
					titleTextLayout.getHeight() + detailsBounds.top, null);

			canvas.restore();
		}

		canvas.save();
		canvas.translate(detailsBounds.left, detailsBounds.top + descriptionOffset + titleTextLayout.getHeight());
		descriptionTextLayout.draw(canvas);
		canvas.restore();
	}

	private void startAnimation() {
		isAnimationActive = true;
		animationStartMs = System.currentTimeMillis();

		final Timer timer = new Timer();
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				long delay = System.currentTimeMillis() - animationStartMs;

				if (delay > ANIMATION_TIME || isClosed) {
					timer.cancel();
					isAnimationActive = false;
					isImageHidden = true;
				}

				((MainActivity) context).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						view.invalidate();
					}
				});
			}
		};
		timer.schedule(timerTask, 30, 30);
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
		MapView.Projection proj = mapView.getProjection();

		if (!detailsBounds.contains((int) e.getX(), (int) e.getY())) {
			startAnimation();
		} else {
			mapView.getOverlays().remove(this);
			mapView.invalidate();
			close();
		}
		return true;
	}

	@Override
	public void close() {
		this.isClosed = true;
	}
}
