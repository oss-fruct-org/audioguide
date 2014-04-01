package org.fruct.oss.audioguide.overlays;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.MotionEvent;

import org.fruct.oss.audioguide.util.AUtils;
import org.fruct.oss.audioguide.util.Utils;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class EditOverlay extends Overlay {
	private final static Logger log = LoggerFactory.getLogger(EditOverlay.class);

	private List<EditOverlayItem> items = new ArrayList<EditOverlayItem>();
	private int itemSize;

	private Paint itemBackgroundPaint;
	private Paint itemBackgroundDragPaint;

	private Paint linePaint;

	private EditOverlayItem draggingItem;
	private int dragRelX;
	private int dragRelY;

	private transient Point point = new Point();
	private transient Point point2 = new Point();
	private transient HitResult hitResult = new HitResult();


	public EditOverlay(Context ctx) {
		super(ctx);

		itemBackgroundPaint = new Paint();
		itemBackgroundPaint.setColor(0xffaa43ba);
		itemBackgroundPaint.setStyle(Paint.Style.FILL);

		itemBackgroundDragPaint = new Paint();
		itemBackgroundDragPaint.setColor(0xff1143fa);
		itemBackgroundDragPaint.setStyle(Paint.Style.FILL);


		linePaint = new Paint();
		linePaint.setColor(0xcc4455ff);
		linePaint.setStyle(Paint.Style.STROKE);
		linePaint.setAntiAlias(true);

		itemSize = Utils.getDP(16);
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

	public void addPoint(GeoPoint geoPoint) {
		EditOverlayItem item = new EditOverlayItem(geoPoint);
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
	public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
		HitResult hitResult = testHit(e, mapView);

		if (hitResult != null) {
			log.debug("Hit");
		}

		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, MapView mapView) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			HitResult hitResult = testHit(event, mapView);

			if (hitResult != null) {
				draggingItem = hitResult.item;
				dragRelX = hitResult.relHookX;
				dragRelY = hitResult.relHookY;

				mapView.invalidate();
				return true;
			} else {
				return false;
			}
		} else if (event.getAction() == MotionEvent.ACTION_UP && draggingItem != null) {
			draggingItem = null;
			mapView.invalidate();
			return true;
		} else if (event.getAction() == MotionEvent.ACTION_MOVE && draggingItem != null) {
			moveItem(draggingItem, event, mapView);

			return true;
		} else {
			return false;
		}
	}

	private void moveItem(EditOverlayItem item, MotionEvent e, MapView mapView) {
		final MapView.Projection proj = mapView.getProjection();
		final Rect screenRect = proj.getIntrinsicScreenRect();

		point.set((int) e.getX() + dragRelX, (int) e.getY() + dragRelY);

		IGeoPoint ret = proj.fromPixels(point.x, point.y);
		item.geoPoint = AUtils.copyGeoPoint(ret);
		mapView.invalidate();
	}

	class EditOverlayItem {
		EditOverlayItem(GeoPoint geoPoint) {
			this.geoPoint = geoPoint;
		}

		GeoPoint geoPoint;
	}

	class HitResult {
		EditOverlayItem item;
		int relHookX;
		int relHookY;
	}
}
