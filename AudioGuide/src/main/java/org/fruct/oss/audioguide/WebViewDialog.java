package org.fruct.oss.audioguide;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.internal.util.Predicate;

public class WebViewDialog extends DialogFragment {
	public static interface Listener {
		void onSuccess();
	}

	private final String url;
	private final Predicate<String> urlCloseChecker;

	private boolean isCompleted = false;
	private Listener listener;

	public WebViewDialog() {
		url = "http://example.com";
		urlCloseChecker = new Predicate<String>() {
			@Override
			public boolean apply(String s) {
				return false;
			}
		};
	}

	public WebViewDialog(String url, Predicate<String> urlCloseChecker) {
		this.url = url;
		this.urlCloseChecker = urlCloseChecker;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		setRetainInstance(true);

		final WebView webView = new FixedWebView(getActivity(), null, android.R.attr.webViewStyle);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				webView.loadUrl(url);
				return true;
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				if (urlCloseChecker.apply(url)) {
					Handler handler = new Handler(Looper.getMainLooper());
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							WebViewDialog.this.dismiss();
							isCompleted = true;
							if (listener != null) {
								listener.onSuccess();
								listener = null;
							}
						}
					}, 2000);
				}
			}
		});

		webView.setFocusable(true);
		webView.setFocusableInTouchMode(true);
		webView.requestFocus(View.FOCUS_DOWN);
		webView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				switch (motionEvent.getAction()) {
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_DOWN:
					if (!view.hasFocus()) {
						view.requestFocus();
					}
					break;
				}
				return false;
			}
		});

		webView.loadUrl(url);

		builder.setView(webView);

		return builder.create();
	}

	public boolean isCompleted() {
		return isCompleted;
	}

	private static class FixedWebView extends WebView {
		public FixedWebView(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
		}

		@Override
		public boolean onCheckIsTextEditor() {
			return true;
		}
	}
}
