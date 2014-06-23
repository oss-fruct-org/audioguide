package org.fruct.oss.audioguide.overlays;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.track.Point;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import org.fruct.oss.audioguide.track.track2.CursorHolder;
import org.fruct.oss.audioguide.track.track2.CursorReceiver;
import org.fruct.oss.audioguide.util.AUtils;
import org.fruct.oss.audioguide.util.Utils;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EditOverlay extends Overlay implements Closeable, CursorReceiver {
	private final Context context;

	public interface Listener {
		void pointMoved(Point t, IGeoPoint geoPoint);
		void pointPressed(Point t);
	}

	private final static Logger log = LoggerFactory.getLogger(EditOverlay.class);
	private final static int[] markers = {R.drawable.marker_1,
			R.drawable.marker_2,
			R.drawable.marker_3};

	private Map<Point, EditOverlayItem> items = new HashMap<Point, EditOverlayItem>();
	private int itemSize;

	private final Paint itemBackgroundDragPaint;
	private final Paint itemBackgroundPaint;

	private final FileManager fileManager;

	private Rect markerPadding;
	private Drawable markerDrawable;

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
	private transient Rect rect = new Rect();
	private transient Rect rect2 = new Rect();

	private transient HitResult hitResult = new HitResult();

	private Listener listener;
	private final CursorHolder cursorHolder;
	private final List<Point> points = new ArrayList<Point>();


	public EditOverlay(Context ctx, CursorHolder cursorHolder, int markerIndex) {
		super(ctx);

		this.context = ctx;
		this.cursorHolder = cursorHolder;
		cursorHolder.attachToReceiver(this);

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
		cursorHolder.close();
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

		//drawPath(canvas, view);
		drawItems(canvas, view);
	}

	/*private void drawPath(Canvas canvas, MapView view) {
		for (Model<Point> track : points) {
			for (int i = 0; i < track.getCount() - 1; i++) {
				Point p1 = track.getItem(i);
				Point p2 = track.getItem(i + 1);

				EditOverlayItem item = items.get(p1);
				EditOverlayItem item2 = items.get(p2);

				drawLine(canvas, view, item, item2);
			}
		}
	}*/

	// TODO: projection points can be performed only if map position changes
	private void drawLine(Canvas canvas, MapView view, EditOverlayItem item, EditOverlayItem item2) {
		MapView.Projection proj = view.getProjection();
		proj.toMapPixels(item.geoPoint, point);
		proj.toMapPixels(item2.geoPoint, point2);

		canvas.drawLine(point.x, point.y, point2.x, point2.y, linePaint);
	}

	private void drawItems(Canvas canvas, MapView view) {
		int i = 0;
		for (EditOverlayItem item : items.values()) {
			drawItem(canvas, view, item, i++);
		}
	}

	private void drawItem(Canvas canvas, MapView view, EditOverlayItem item, int index) {
		if (item == draggingItem)
			return;

		MapView.Projection proj = view.getProjection();

		proj.toMapPixels(item.geoPoint, point);

		markerDrawable.setBounds(point.x - itemSize - markerPadding.left,
				point.y - 2 * itemSize - markerPadding.bottom - markerPadding.top,
				point.x + itemSize + markerPadding.right,
				point.y);
		markerDrawable.draw(canvas);

		Rect bounds = markerDrawable.getBounds();

		if (item.iconBitmap != null) {
			canvas.drawBitmap(item.iconBitmap, bounds.left + markerPadding.left, bounds.top + markerPadding.top, null);
		} else {
			canvas.drawText(String.valueOf(index), point.x, point.y - itemSize + itemSize / 3, itemBackgroundPaint);
		}
	}

	public void addPoint(GeoPoint geoPoint, org.fruct.oss.audioguide.track.Point t) {
		EditOverlayItem item = new EditOverlayItem(geoPoint, t);

		if (item.data.hasPhoto()) {
			item.iconBitmap = fileManager.getImageBitmap(item.data.getPhotoUrl(),
					Utils.getDP(48), Utils.getDP(48), FileManager.ScaleMode.SCALE_CROP);
		}

		items.put(item.data, item);
	}

	public boolean testHit(MotionEvent e, MapView mapView, EditOverlayItem item, HitResult result) {
		final MapView.Projection proj = mapView.getProjection();
		final Rect screenRect = proj.getIntrinsicScreenRect();

		final int x = screenRect.left + (int) e.getX();
		final int y = screenRect.top + (int) e.getY();

		proj.toMapPixels(item.geoPoint, point);

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

				mapView.invalidate();
				return true;
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
			return true;
		} else if (event.getAction() == MotionEvent.ACTION_MOVE && draggingItem != null) {
			if (isEditable) {
				final int dx = dragStartX - (int) event.getX();
				final int dy = dragStartY - (int) event.getY();

				if (dragStarted || dx * dx + dy * dy > 8 * 8) {
					dragStarted = true;
					moveItem(draggingItem, event, mapView);
				}
			}
			return true;
		} else {
			return false;
		}
	}

	private void moveItem(EditOverlayItem item, MotionEvent e, MapView mapView) {
		final MapView.Projection proj = mapView.getProjection();

		point.set((int) e.getX() + dragRelX, (int) e.getY() + dragRelY);

		IGeoPoint ret = proj.fromPixels(point.x, point.y);
		item.geoPoint = AUtils.copyGeoPoint(ret);
		mapView.invalidate();
	}

	@Override
	public void changeCursor(Cursor cursor) {
		points.clear();

		while (cursor.moveToNext()) {
			Point point = new Point(cursor);
			addPoint(new GeoPoint(point.getLatE6(), point.getLonE6()), point);
		}
	}

	class EditOverlayItem {
		EditOverlayItem(GeoPoint geoPoint, Point data) {
			this.geoPoint = geoPoint;
			this.data = data;
		}

		Point data;
		GeoPoint geoPoint;
		Bitmap iconBitmap;


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


}
