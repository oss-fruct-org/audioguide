package org.fruct.oss.audioguide.preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.util.AUtils;

public class SliderPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {
	private static class Converter {
		int toSliderPosition(int value) {
			return value;
		}

		int toValue(int sliderPosition) {
			return sliderPosition;
		}
	}

	private static class ExponentialConverter extends Converter {
		private double ln10 = Math.log(10);

		@Override
		int toSliderPosition(int value) {
			return (int) (1000 * Math.log(value - 500) / ln10);
		}

		@Override
		int toValue(int sliderPosition) {
			return (int) (Math.exp(sliderPosition* ln10 / 1000) + 500);
		}
	}

	private static final String NS="http://schemas.android.com/apk/res/android";

	private final Converter converter;

	private final int defaultValue;
	private final int maxValue;
	private int value;

	private TextView textView;
	private SeekBar seekBar;

	private String textStr;
	private int textRes;

	private boolean isExponential;

	public SliderPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		defaultValue = attrs.getAttributeIntValue(NS, "defaultValue", 50);
		maxValue = attrs.getAttributeIntValue(NS, "max", 100);

		textRes = attrs.getAttributeResourceValue(NS, "text", -1);
		if (textRes == -1)
			textStr = attrs.getAttributeValue(NS, "text");

		isExponential = attrs.getAttributeBooleanValue(null, "exponential", false);
		if (isExponential)
			converter = new ExponentialConverter();
		else
			converter = new Converter();
	}

	@Override
	protected View onCreateDialogView() {
		Context context = new ContextThemeWrapper(getContext(), AUtils.getDialogTheme());

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

		View view = inflater.inflate(R.layout.preference_slider, null);

		textView = (TextView) view.findViewById(android.R.id.text1);
		seekBar = (SeekBar) view.findViewById(R.id.seek_bar);

		seekBar.setOnSeekBarChangeListener(this);

		if (shouldPersist())
			value = getPersistedInt(defaultValue);

		return view;//super.onCreateDialogView();
	}

	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);

		seekBar.setMax(converter.toSliderPosition(maxValue));
		seekBar.setProgress(converter.toSliderPosition(value));
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		super.onSetInitialValue(restorePersistedValue, defaultValue);

		if (restorePersistedValue) {
			value = shouldPersist() ? getPersistedInt(this.defaultValue) : 0;
		} else {
			value = (Integer) defaultValue;
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progressBarPosition, boolean fromUser) {
		int value = converter.toValue(progressBarPosition);

		textView.setText(getContext().getResources().getQuantityString(textRes, value, value));
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void showDialog(Bundle state) {
		super.showDialog(state);

		Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
		positiveButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		if (shouldPersist()) {
			int progress = seekBar.getProgress();

			value = converter.toValue(progress);
			persistInt(value);
			callChangeListener(value);
		}

		getDialog().dismiss();
	}
}
