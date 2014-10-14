package org.fruct.oss.audioguide.overlays;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.MotionEvent;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.files.BitmapProcessor;
import org.fruct.oss.audioguide.files.BitmapSetter;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.files.FileSource;
import org.fruct.oss.audioguide.track.CursorHolder;
import org.fruct.oss.audioguide.track.CursorReceiver;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.util.AUtils;
import org.fruct.oss.audioguide.util.Utils;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EditOverlay extends Overlay implements Closeable {
	private final Context context;
	private final MapView mapView;

	public interface Listener {
		void pointMoved(Point p, IGeoPoint geoPoint);
		void pointPressed(Point p);
		void pointLongPressed(Point p);
	}

	private final static Logger log = LoggerFactory.getLogger(EditOverlay.class);
	private final static int[] markers = {R.drawable.marker_1,
			R.drawable.marker_2,
			R.drawable.marker_3};

	private Map<Long, EditOverlayItem> items = new HashMap<Long, EditOverlayItem>();
	private int itemSize;

	private final Paint itemBackgroundDragPaint;
	private final Paint itemBackgroundPaint;

	private final FileManager fileManager;
	private List<BitmapProcessor> processors = new ArrayList<BitmapProcessor>();

	private Rect clipRect = new Rect();

	private Rect markerPadding;
	private Drawable markerDrawable;
	private Drawable markerDrawable2;

	private Paint linePaint;

	private EditOverlayItem draggingItem;
	private int dragRelX;
	private int dragRelY;
	private int dragStartX;
	private int dragStartY;
	private boolean dragStarted;

	private boolean isEditable = false;

	private transient android.graphics.Point point = new android.graphics.Point();
	private transient android.graphics.Point point2 = new android.graphics.Point();
	private transient android.graphics.Point point3 = new android.graphics.Point();

	private transient Rect rect = new Rect();
	private transient Rect rect2 = new Rect();

	private transient HitResult hitResult = new HitResult();

	private Listener listener;

	private final CursorHolder relationsCursorHolder;
	private Cursor currentRelationsCursor;

	private final CursorHolder pointsCursorHolder;
	private Cursor currentPointsCursor;

	private final List<Pair<Long, Long>> relations = new ArrayList<Pair<Long, Long>>();

	public EditOverlay(Context ctx, CursorHolder pointsCursorHolder,
					   CursorHolder relationsCursorHolder, int markerIndex, MapView mapView) {
		super(ctx);

		this.mapView = mapView;
		this.context = ctx;
		this.pointsCursorHolder = pointsCursorHolder;
		this.relationsCursorHolder = relationsCursorHolder;

		pointsCursorHolder.attachToReceiver(pointsCursorReceiver);
		if (relationsCursorHolder != null)
			relationsCursorHolder.attachToReceiver(relationsCursorReceiver);

		itemSize = Utils.getDP(24);

		itemBackgroundDragPaint = new Paint();
		itemBackgroundDragPaint.setColor(0xff1143fa);
		itemBackgroundDragPaint.setStyle(Paint.Style.FILL);

		linePaint = new Paint();
		linePaint.setColor(0xff1143fa);
		linePaint.setStyle(Paint.Style.STROKE);
		linePaint.setStrokeWidth(2);
		linePaint.setAntiAlias(true);

		itemBackgroundPaint = new Paint();
		itemBackgroundPaint.setColor(0xffffffff);
		itemBackgroundPaint.setStyle(Paint.Style.FILL);
		itemBackgroundPaint.setTextSize(itemSize);
		itemBackgroundPaint.setAntiAlias(true);
		itemBackgroundPaint.setTextAlign(Paint.Align.CENTER);

		fileManager = FileManager.getInstance();
	}

	@Override
	public void close() {
		pointsCursorHolder.close();
		if (relationsCursorHolder != null)
			relationsCursorHolder.close();

		for (BitmapProcessor proc : processors) {
			proc.recycle();
		}
	}

	private int getMeanColor(Drawable drawable) {
		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

		drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		Canvas canvas = new Canvas(bitmap);
		drawable.draw(canvas);

		int r = 0, g = 0, b = 0;
		Random rand = new Random();

		int c = 0;
		for (int i = 0; i < 20; i++) {
			int x = rand.nextInt(bitmap.getWidth());
			int y = rand.nextInt(bitmap.getHeight());

			int pix = bitmap.getPixel(x, y);

			int a = (pix >>> 24) & 0xff;

			if (a > 200) {
				c++;
				r += (pix >>> 16) & 0xff;
				g += (pix >>> 8) & 0xff;
				b += (pix) & 0xff;
			}
		}

		if (c > 0) {
			r /= c;
			g /= c;
			b /= c;
		}

		bitmap.recycle();
		return (r << 16) + (g << 8) + b + 0xff000000;
	}

	public void setMarkerIndex(int markerIndex) {
		markerPadding = new Rect();

		markerDrawable = context.getResources().getDrawable(markers[markerIndex % markers.length]);
		markerDrawable2 = context.getResources().getDrawable(markers[0]);

		markerDrawable.getPadding(markerPadding);

		linePaint.setColor(getMeanColor(markerDrawable));
	}

	public void setEditable(boolean isEditable) {
		this.isEditable = isEditable;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	@Override
	protected void draw(Canvas canvas, MapView view, boolean shadow) {
		if (shadow)
			return;

		clipRect.set(view.getProjection().getIntrinsicScreenRect());
		clipRect.inset(-itemSize, -itemSize);

		drawPath(canvas, view);
		drawItems(canvas, view);
	}

	private void drawPath(Canvas canvas, MapView view) {
		for (Pair<Long, Long> line : relations) {
			EditOverlayItem p1 = items.get(line.first);
			EditOverlayItem p2 = items.get(line.second);

			if (p1 != null && p2 != null)
				drawLine(canvas, view, p1, p2);
		}
	}

	// TODO: projection points can be performed only if map position changes
	private void drawLine(Canvas canvas, MapView view, EditOverlayItem item, EditOverlayItem item2) {
		Projection proj = view.getProjection();
		proj.toPixels(item.geoPoint, point);
		proj.toPixels(item2.geoPoint, point2);

		canvas.drawLine(point.x, point.y, point2.x, point2.y, linePaint);
	}

	private void drawItems(Canvas canvas, MapView view) {
		int i = 0;
		for (EditOverlayItem item : items.values()) {
			drawItem(canvas, view, item, i++);
		}
	}

	private void drawItem(Canvas canvas, MapView view, EditOverlayItem item, int index) {
		Projection proj = view.getProjection();

		proj.toPixels(item.geoPoint, point);

		if (!clipRect.contains(point.x, point.y)) {
			return;
		}

		if (!item.iconRequested && item.data.hasPhoto()) {
			log.trace("Requesting icon of point that in screen...");
			item.iconRequested = true;
			BitmapProcessor proc = BitmapProcessor.requestBitmap(fileManager, item.data.getPhotoUrl(), FileSource.Variant.FULL,
					itemSize * 2, itemSize * 2, FileManager.ScaleMode.SCALE_CROP, new EditOverlayBitmapSetter(item));
			processors.add(proc);
		}

		Drawable marker = draggingItem == item ? markerDrawable2 : markerDrawable;

		marker.setBounds(point.x - itemSize - markerPadding.left,
				point.y - 2 * itemSize - markerPadding.bottom - markerPadding.top,
				point.x + itemSize + markerPadding.right,
				point.y);
		marker.draw(canvas);

		Rect bounds = marker.getBounds();

		if (item.iconBitmap != null) {
			canvas.drawBitmap(item.iconBitmap, bounds.left + markerPadding.left, bounds.top + markerPadding.top, null);
		} else {
			canvas.drawText(String.valueOf(index), point.x, point.y - itemSize + itemSize / 3, itemBackgroundPaint);
		}
	}

	public boolean testHit(MotionEvent e, MapView mapView, EditOverlayItem item, HitResult result) {
		final Projection proj = mapView.getProjection();
		final Rect screenRect = proj.getIntrinsicScreenRect();

		final int x = screenRect.left + (int) e.getX();
		final int y = screenRect.top + (int) e.getY();

		proj.toPixels(item.geoPoint, point);

		final int ix = point.x - x;
		final int iy = point.y - y;

		if (result != null) {
			result.item = item;
			result.relHookX = ix;
			result.relHookY = iy;
		}

		return ix >= -itemSize && iy >= 0 && ix <= itemSize && iy <= 2 * itemSize;
	}

	public HitResult testHit(MotionEvent e, MapView mapView) {
		for (EditOverlayItem item : items.values()) {
			if (testHit(e, mapView, item, hitResult))
				return hitResult;
		}

		return null;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, MapView mapView) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			HitResult hitResult = testHit(event, mapView);

			if (hitResult != null) {
				draggingItem = hitResult.item;
				dragRelX = hitResult.relHookX;
				dragRelY = hitResult.relHookY;
				dragStartX = (int) event.getX();
				dragStartY = (int) event.getY();
				dragStarted = false;

				if (draggingItem.data.isEditable())
					setupLongPressHandler(draggingItem);

				mapView.invalidate();
				return false;
			} else {
				return false;
			}
		} else if (event.getAction() == MotionEvent.ACTION_UP && draggingItem != null) {
			if (dragStarted) {
				if (listener != null) {
					listener.pointMoved(draggingItem.data, draggingItem.geoPoint);
				}
			} else {
				if (listener != null) {
					listener.pointPressed(draggingItem.data);
				}
			}

			draggingItem = null;
			mapView.invalidate();
			return false;
		} else if (event.getAction() == MotionEvent.ACTION_MOVE
				&& draggingItem != null
				&& draggingItem.data.isEditable()) {
			final int dx = dragStartX - (int) event.getX();
			final int dy = dragStartY - (int) event.getY();

			if (dragStarted || dx * dx + dy * dy > 32 * 32) {
				dragStarted = true;
				moveItem(draggingItem, event, mapView);
			}
			return true;
		} else {
			return false;
		}
	}

	private void setupLongPressHandler(final EditOverlayItem requestedItem) {
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (draggingItem == requestedItem && !dragStarted) {
					if (listener != null) {
						listener.pointLongPressed(draggingItem.data);
					}
					draggingItem = null;
				}
			}
		}, 500);
	}

/*
private void checkDistance(MapView mapView, android.graphics.Point p) {
		MapView.Projection proj = mapView.getProjection();

		double min = Long.MAX_VALUE;
		for (Pair<Long, Long> line : relations) {
			EditOverlayItem p1 = items.get(line.first);
			EditOverlayItem p2 = items.get(line.second);

			if (p1 != null && p2 != null) {
				proj.toPixels(p1.geoPoint, point);
				proj.toPixels(p2.geoPoint, point2);

				min = Math.min(min, AUtils.distanceToLine(point, point2, p));
			}
		}

		log.debug("Min distance: " + min);
	}
*/

	private void moveItem(EditOverlayItem item, MotionEvent e, MapView mapView) {
		final Projection proj = mapView.getProjection();

		point3.set((int) e.getX() + dragRelX, (int) e.getY() + dragRelY);
		IGeoPoint ret = proj.fromPixels(point3.x, point3.y);
		item.geoPoint = AUtils.copyGeoPoint(ret);

		mapView.invalidate();
	}

	private CursorReceiver pointsCursorReceiver = new CursorReceiver() {
		@Override
		public Cursor swapCursor(Cursor cursor) {
			Cursor oldCursor = currentPointsCursor;
			currentPointsCursor = cursor;

			items.clear();
			Point.CursorFields cf = Point.getCursorFields(cursor);

			while (cursor.moveToNext()) {
				Point point = new Point(cursor);
				long id = cursor.getLong(cf._id);

				EditOverlayItem item = new EditOverlayItem(
						new GeoPoint(point.getLatE6(), point.getLonE6()), point);

				items.put(id, item);
			}

			mapView.invalidate();

			return oldCursor;
		}
	};

	private CursorReceiver relationsCursorReceiver = new CursorReceiver() {
		@Override
		public Cursor swapCursor(Cursor cursor) {
			Cursor oldCursor = currentPointsCursor;
			currentPointsCursor = cursor;

			long currentTrackId = -1;
			long prevPointId = -1;

			while (cursor.moveToNext()) {
				long trackId = cursor.getLong(0);

				if (trackId != currentTrackId) {
					currentTrackId = trackId;
					prevPointId = cursor.getLong(1);
					continue;
				}

				long currentPointId = cursor.getLong(1);

				relations.add(Pair.create(prevPointId, currentPointId));
				prevPointId = currentPointId;
			}

			return oldCursor;
		}
	};

	class EditOverlayItem {
		EditOverlayItem(GeoPoint geoPoint, Point data) {
			this.geoPoint = geoPoint;
			this.data = data;
		}

		Point data;
		GeoPoint geoPoint;
		Bitmap iconBitmap;
		boolean iconRequested;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			EditOverlayItem that = (EditOverlayItem) o;

			if (!data.equals(that.data)) return false;

			return true;
		}

		@Override
		public int hashCode() {
			return data.hashCode();
		}
	}

	class HitResult {
		EditOverlayItem item;
		int relHookX;
		int relHookY;
	}

	class EditOverlayBitmapSetter implements BitmapSetter {
		private final EditOverlayItem item;
		private Bitmap bitmap;
		private Handler handler = new Handler(Looper.getMainLooper());

		public EditOverlayBitmapSetter(EditOverlayItem item) {
			this.item = item;
		}

		private BitmapProcessor tag;

		@Override
		public void bitmapReady(final Bitmap newBitmap, final BitmapProcessor checkTag) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					if (!checkTag.equals(tag)) {
						return;
					}

					item.iconBitmap = newBitmap;
					recycle();
					bitmap = newBitmap;
					mapView.invalidate();
				}
			});
		}

		@Override
		public void recycle() {
			if (bitmap != null && !bitmap.isRecycled()) {
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
}
