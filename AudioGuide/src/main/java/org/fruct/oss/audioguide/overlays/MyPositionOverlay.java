package org.fruct.oss.audioguide.overlays;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;

import org.fruct.oss.audioguide.util.AUtils;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

public class MyPositionOverlay extends Overlay {
	interface OnScrollListener {
		void onScroll();
	}
	private OnScrollListener listener;

	private MapView mapView;
	private Location location;

	private Point point = new Point();
	private Point centerPoint = new Point();

	private Path path = new Path();

	private Paint paint;
	private Paint paintRed;
	private Paint paintAccuracy;
	private Paint paintRad;

	private boolean isPaintAccuracy = true;

	private int arrowWidth;
	private int arrowHeight;

	private int range = 50;

	public MyPositionOverlay(Context ctx, MapView mapView) {
		super(ctx);

		this.mapView = mapView;

		paint = new Paint();
		paint.setColor(0xff00ffff);
		paint.setStyle(Style.STROKE);

		paintRed = new Paint();
		paintRed.setColor(0x99ff0000);
		paintRed.setStyle(Style.FILL);
		paintRed.setAntiAlias(true);

		paintAccuracy = new Paint();
		paintAccuracy.setColor(0x2262A4B6);
		paintAccuracy.setStyle(Style.FILL);
		paintAccuracy.setAntiAlias(true);

		paintRad = new Paint();
		paintRad.setColor(0x11B66284);
		paintRad.setStyle(Style.FILL);
		paintRad.setAntiAlias(true);

		arrowWidth = AUtils.getDP(7);
		arrowHeight = AUtils.getDP(32);
	}

	@Override
	protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (location == null) {
			return;
		}

		Projection proj = mapView.getProjection();
		GeoPoint locationPoint = new GeoPoint(location);
		IGeoPoint mapCenter = mapView.getMapCenter();

		proj.toPixels(locationPoint, point);
		proj.toPixels(mapCenter, centerPoint);

		canvas.save();

		canvas.rotate(location.getBearing(), point.x, point.y);
		canvas.translate(point.x, point.y);

		if (isPaintAccuracy) {
			float pixels = 2 * proj.metersToEquatorPixels(range);
			canvas.drawCircle(0, 0, pixels, paintAccuracy);
		}/* else {
float pixels = 2 * proj.metersToEquatorPixels(45);
canvas.drawCircle(0, 0, pixels, paintRad);
}*/

		drawArrow(canvas);

		canvas.restore();
	}

	private void drawArrow(Canvas canvas) {
		path.reset();

		path.moveTo(0, arrowWidth);
		path.lineTo(-arrowWidth, 0);
		path.lineTo(0, -arrowHeight);
		path.lineTo(arrowWidth, 0);
		path.close();

		canvas.drawPath(path, paintRed);
	}

	public void setLocation(@NonNull Location myLocation) {
		this.location = myLocation;
	}

	@Nullable
	public Location getLocation() {
		return location;
	}

	public void setShowAccuracy(boolean isShow) {
		this.isPaintAccuracy = isShow;
	}

	@Override
	public boolean onScroll(MotionEvent pEvent1, MotionEvent pEvent2, float pDistanceX, float pDistanceY, MapView pMapView) {
		if (listener != null)
			listener.onScroll();

		return super.onScroll(pEvent1, pEvent2, pDistanceX, pDistanceY, pMapView);
	}

	public void setListener(OnScrollListener listener) {
		this.listener = listener;
	}

	public void clearListener() {
		this.listener = null;
	}

	public void setRange(int range) {
		this.range = range;
	}
}