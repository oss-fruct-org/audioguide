package org.fruct.oss.audioguide.overlays;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import org.fruct.oss.audioguide.track.Point;
import android.graphics.Rect;
import android.view.MotionEvent;

import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.models.ModelListener;
import org.fruct.oss.audioguide.util.AUtils;
import org.fruct.oss.audioguide.util.Utils;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EditOverlay extends Overlay implements Closeable, ModelListener {
	public interface Listener {
		void pointMoved(Point t, IGeoPoint geoPoint);
		void pointPressed(Point t);
	}

	private final static Logger log = LoggerFactory.getLogger(EditOverlay.class);

	private List<EditOverlayItem> items = new ArrayList<EditOverlayItem>();
	private int itemSize;

	private Paint itemBackgroundPaint;
	private Paint itemBackgroundDragPaint;

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
	private transient HitResult hitResult = new HitResult();

	private Listener listener;
	private final Model<Point> model;

	public EditOverlay(Context ctx, Model<Point> model, int color) {
		super(ctx);

		itemBackgroundPaint = new Paint();
		itemBackgroundPaint.setColor(color);
		itemBackgroundPaint.setStyle(Paint.Style.FILL);

		itemBackgroundDragPaint = new Paint();
		itemBackgroundDragPaint.setColor(0xff1143fa);
		itemBackgroundDragPaint.setStyle(Paint.Style.FILL);

		linePaint = new Paint();
		linePaint.setColor(color);
		linePaint.setStyle(Paint.Style.STROKE);
		linePaint.setStrokeWidth(2);
		linePaint.setAntiAlias(true);

		itemSize = Utils.getDP(16);

		this.model = model;
		this.model.addListener(this);

		dataSetChanged();
	}

	public void setEditable(boolean isEditable) {
		this.isEditable = isEditable;
	}

	@Override
	public void close() {
		this.model.removeListener(this);
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	@Override
	protected void draw(Canvas canvas, MapView view, boolean shadow) {
		if (shadow)
			return;

		drawPath(canvas, view);
		drawItems(canvas, view);
	}

	private void drawPath(Canvas canvas, MapView view) {
		for (int i = 0; i < items.size() - 1; i++) {
			EditOverlayItem item = items.get(i);
			EditOverlayItem item2 = items.get(i + 1);

			drawLine(canvas, view, item, item2);
		}
	}

	// TODO: projection points can be performed only if map position changes
	private void drawLine(Canvas canvas, MapView view, EditOverlayItem item, EditOverlayItem item2) {
		MapView.Projection proj = view.getProjection();
		proj.toMapPixels(item.geoPoint, point);
		proj.toMapPixels(item2.geoPoint, point2);

		canvas.drawLine(point.x, point.y, point2.x, point2.y, linePaint);
	}

	private void drawItems(Canvas canvas, MapView view) {
		for (int i = 0; i < items.size(); i++) {
			EditOverlayItem item = items.get(i);
			drawItem(canvas, view, item, i);
		}
	}

	private void drawItem(Canvas canvas, MapView view, EditOverlayItem item, int index) {
		MapView.Projection proj = view.getProjection();

		proj.toMapPixels(item.geoPoint, point);

		canvas.drawRect(point.x - itemSize, point.y - itemSize,
				point.x + itemSize, point.y + itemSize,
				item == draggingItem ? itemBackgroundDragPaint : itemBackgroundPaint);
	}

	public void addPoint(GeoPoint geoPoint, org.fruct.oss.audioguide.track.Point t) {
		EditOverlayItem item = new EditOverlayItem(geoPoint, t);
		items.add(item);
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

		return ix >= -itemSize && iy >= -itemSize && ix <= itemSize && iy <= itemSize;
	}

	public HitResult testHit(MotionEvent e, MapView mapView) {
		for (EditOverlayItem item : items) {
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
	public void dataSetChanged() {
		items.clear();

		for (Point point : model) {
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
	}

	class HitResult {
		EditOverlayItem item;
		int relHookX;
		int relHookY;
	}
}
