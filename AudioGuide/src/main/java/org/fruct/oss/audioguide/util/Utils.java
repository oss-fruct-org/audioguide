package org.fruct.oss.audioguide.util;

import android.util.Pair;
import android.util.TypedValue;

import org.fruct.oss.audioguide.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@SuppressWarnings("unused")
public class Utils {
	private final static Logger log = LoggerFactory.getLogger(Utils.class);

	private Utils() {
	}

	public static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
		int depth = 1;

		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException("Parser must be on start tag");
		}

		while (depth > 0) {
			switch (parser.next()) {
			case XmlPullParser.START_TAG:
				depth++;
				break;
			case XmlPullParser.END_TAG:
				depth--;
				break;
			}
		}
	}

	public static interface Predicate<T> {
		public boolean apply(T t);
	}
	@SuppressWarnings("hiding")
	public static interface Function<R, T> {
		public R apply(T t);
	}

	public static interface Function2<R1, R2, T> {
		public Pair<R1, R2> apply(T t);
	}
	public static interface FunctionDouble {
		public double apply(double x);
	}

	public static interface Callback<T> {
		void call(T t);
	}

	public static float normalizeAngle(float degree) {
		return (float) (StrictMath.IEEEremainder(degree, 360));
	}

	private static final char[] hexDigits = "0123456789abcdef".toCharArray();
	public static String toHex(byte[] arr) {
		final char[] str = new char[arr.length * 2];

		for (int i = 0; i < arr.length; i++) {
			final int v = arr[i] & 0xff;
			str[2 * i] = hexDigits[v >>> 4];
			str[2 * i + 1] = hexDigits[v & 0x0f];
		}

		return new String(str);
	}

	public static String hashString(String input) {
		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return String.valueOf(input.hashCode());
		}
		md5.update(input.getBytes());
		byte[] hash = md5.digest();
		return toHex(hash);
	}
	public static String hashStream(InputStream in) throws IOException {
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		int bsize = 1024;
		byte[] buffer = new byte[bsize];
		int length;
		while ((length = in.read(buffer, 0, bsize)) > 0) {
			md5.update(buffer, 0, length);
		}
		return toHex(md5.digest());
	}

	public static String inputStreamToString(InputStream stream) throws IOException {
		InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
		return readerToString(reader);
	}

	public static String readerToString(Reader reader) throws IOException {
		StringBuilder builder = new StringBuilder();
		int bufferSize = 4096;
		char[] buf = new char[bufferSize];

		int readed;
		while ((readed = reader.read(buf)) > 0) {
			builder.append(buf, 0, readed);
		}

		return builder.toString();
	}

	public static void copyStream(InputStream input, OutputStream output) throws IOException {
		int bufferSize = 4096;


		byte[] buf = new byte[bufferSize];
		int read;
		while ((read = input.read(buf)) > 0) {
			output.write(buf, 0, read);
		}
	}

	public static <T> void select(Collection<T> source, Collection<T> target, Predicate<T> pred) {
		for (T t : source) {
			if (pred.apply(t))
				target.add(t);
		}
	}

	public static <T> ArrayList<T> select(Collection<T> source, Predicate<T> pred) {
		ArrayList<T> output = new ArrayList<T>();
		select(source, output, pred);
		return output;
	}

	public static <T> T find (Collection<T> source, Predicate<T> pred) {
		for (T t : source) {
			if (pred.apply(t))
				return t;
		}
		return null;
	}

	@SuppressWarnings("hiding")
	public static <R,T> List<R> map(List<T> source, Function<R, T> fun) {
		List<R> list = new ArrayList<R>(source.size());
		for (T t : source) {
			list.add(fun.apply(t));
		}
		return list;
	}

	public static <R1, R2, T> Pair<List<R1>, List<R2>> map2(List<T> source, Function2<R1, R2, T> fun) {
		List<R1> ret1 = new ArrayList<R1>(source.size());
		List<R2> ret2 = new ArrayList<R2>(source.size());

		for (T t : source) {
			Pair<R1, R2> ret = fun.apply(t);
			if (ret != null) {
				ret1.add(ret.first);
				ret2.add(ret.second);
			}
		}

		return new Pair<List<R1>, List<R2>>(ret1, ret2);
	}
	public static int getDP(int px) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px,
				App.getContext().getResources().getDisplayMetrics());
	}

	public static float getSP(int px) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, px,
				App.getContext().getResources().getDisplayMetrics());
	}

	// False position method
	public static double solve(double a, double b, double delta, FunctionDouble fun) {
		double fa = fun.apply(a);
		double fb = fun.apply(b);
		double fr, r = 0;
		int side = 0;
		if (fa > 0 && fb > 0 || fa < 0 && fb < 0 || a >= b)
			throw new IllegalArgumentException();
		final int MAX_ITERS = 100;
		for (int i = 0; i < MAX_ITERS; i++) {
			r = (fa * b - fb * a) / (fa - fb);
			if (Math.abs(a - b) < delta * Math.abs(a + b))
				break;

			fr = fun.apply(r);
			if (fr * fb > 0) {
				b = r;
				fb = fr;
				if (side == -1)
					fa /= 2;
				side = -1;
			} else if (fa * fr > 0) {
				a = r;
				fa = fr;
				if (side == 1)
					fb /= 2;
				side = 1;
			} else {
				break;
			}
		}
		return r;
	}

	public static <T, R extends T> R safeCast(T t, Class<? extends R> cls) {
		if (t == null || !cls.isInstance(t))
			return null;
		else
			return cls.cast(t);
	}

	public static boolean isNullOrEmpty(String str) {
		return str == null || str.isEmpty();
	}

	public static void deleteDir(File dir) {
		if (!dir.exists() && !dir.isDirectory())
			return;

		File[] listFiles = dir.listFiles();
		for (File file : listFiles) {
			if (!file.isDirectory()) {
				file.delete();
			}
		}

		dir.delete();
	}

	/**
	 * Sent post request with content of given stream.
	 * This method doesn't close stream
	 * @param urlString Uri to connect
	 * @param stream Stream to upload
	 * @param mimeType Mime type of content
	 * @return server response
	 * @throws IOException
	 */
	public static String postStream(String urlString, InputStream stream, String mimeType) throws IOException {
		HttpURLConnection conn = null;
		InputStream responseStream = null;

		try {
			URL url = new URL(urlString);
			conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(10000);
			conn.setRequestMethod("POST");
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestProperty("User-Agent", "RoadSigns/0.2 (http://oss.fruct.org/projects/roadsigns/)");
			//conn.setRequestProperty("Content-Length", String.valueOf(fileSize));
			if (mimeType != null)
				conn.setRequestProperty("Content-Type", mimeType);

			BufferedOutputStream outputStream = new BufferedOutputStream(conn.getOutputStream());
			copyStream(stream, outputStream);
			outputStream.close();

			conn.connect();

			int responseCode = conn.getResponseCode();
			responseStream = conn.getInputStream();
			String response = Utils.inputStreamToString(responseStream);

			log.trace("Request url {} data", urlString);
			log.trace("Response code {}, response {}", responseCode, response);

			return response;
		} finally {
			if (conn != null)
				conn.disconnect();

			if (responseStream != null)
				responseStream.close();
		}
	}

	public static String postFile(String urlString, String path, String mimeType) throws IOException {
		File localFile = new File(path);

		if (!localFile.exists() || !localFile.canRead() || localFile.isDirectory()) {
			throw new IOException("Invalid file: " + path);
		}

		long fileSize = localFile.length();
		InputStream localStream = new FileInputStream(path);
		try {
			return postStream(urlString, localStream, mimeType);
		} finally {
			try {
				localStream.close();
			} catch (IOException e) {
			}
		}
	}

	public static String downloadUrl(String urlString, String postQuery) throws IOException {
		HttpURLConnection conn = null;
		InputStream responseStream = null;

		try {
			URL url = new URL(urlString);
			conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(10000);
			conn.setRequestMethod(postQuery == null ? "GET" : "POST");
			conn.setDoInput(true);
			conn.setDoOutput(postQuery != null);
			conn.setRequestProperty("User-Agent", "RoadSigns/0.2 (http://oss.fruct.org/projects/roadsigns/)");
			conn.setRequestProperty("Content-Type", "Content-Type: text/xml;charset=utf-8");

			if (postQuery != null) {
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
				writer.write(postQuery);
				writer.flush();
				writer.close();
			}

			log.trace("Request url {} data {}", urlString, postQuery);
			conn.connect();

			int responseCode = conn.getResponseCode();
			responseStream = conn.getInputStream();
			String response = Utils.inputStreamToString(responseStream);

			log.trace("Response code {}, response {}", responseCode, response);

			return response;
		} finally {
			if (conn != null)
				conn.disconnect();

			if (responseStream != null)
				responseStream.close();
		}
	}

	public static long longHash(String str) {
		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			return str.hashCode();
		}

		md5.update(str.getBytes());

		byte[] bytes = md5.digest();

		long l = 0;

		for (int i = 0; i < 8; i++) {
			l = (l << 8) ^ (bytes[i] & 0xff);
		}

		return Math.abs(l);
	}

	public static <T> T createThrowerImplementation(Class<? extends T> iface) {
		Object obj = Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { iface }, new InvocationHandler() {
			@Override
			public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
				throw new UnsupportedOperationException("Method \"" + method.getName() + "\" not implemented yet");
			}
		});

		return iface.cast(obj);
	}

	public static int color(int r, int g, int b) {
		return (r << 16) + (g << 8) + b;
	}

	public static String[] toArray(Object... objects) {
		String[] arr = new String[objects.length];
		int c = 0;
		for (Object obj : objects) {
			arr[c++] = obj.toString();
		}
		return arr;
	}

	public static void sclose(InputStream stream) {
		try {
			if (stream != null)
				stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sclose(OutputStream stream) {
		try {
			if (stream != null)
				stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static <K, V> void putMultiMap(Map<K, List<V>> map, K key, V value) {
		List<V> list = map.get(key);
		if (list == null) {
			map.put(key, list = new ArrayList<V>());
		}
		list.add(value);
	}

	public static <K, V> List<V> getMultiMap(Map<K, List<V>> map, K key) {
		List<V> list = map.get(key);
		if (list == null) {
			map.put(key, list = new ArrayList<V>());
		}
		return list;
	}

	public static boolean isFutureError(Future<?> future) {
		try {
			if (future.isDone())
				future.get();
		} catch (InterruptedException e) {
			return true;
		} catch (ExecutionException e) {
			return true;
		}

		return false;
	}
}