package org.fruct.oss.audioguide.test;


import android.content.Context;

import org.fruct.oss.audioguide.parsers.AuthRedirectResponse;
import org.fruct.oss.audioguide.parsers.FileContent;
import org.fruct.oss.audioguide.parsers.FilesContent;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;
import org.fruct.oss.audioguide.parsers.Kml;
import org.fruct.oss.audioguide.parsers.TokenContent;
import org.fruct.oss.audioguide.parsers.TracksContent;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.util.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.util.List;
import static org.junit.Assert.*;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class ParserTest {
	private Context testContext;
	private InputStream stream;

	@Before
	public void setUp() throws Exception {
        if (testContext == null) {
			testContext = Robolectric.application;
        }
	}

	@After
	public void tearDown() throws Exception {
		if (stream != null) {
			stream.close();
			stream = null;
		}
	}

	@Test
	public void testErrorResponse() throws Exception {
		InputStream stream = testContext.getAssets().open("test/error-response.xml");

		GetsResponse resp = GetsResponse.parse(Utils.inputStreamToString(stream), IContent.class);
		assertEquals(1, resp.getCode());
		assertEquals("Wrong token error", resp.getMessage());
		assertNull(resp.getContent());
	}

	@Test
	public void testLoadTracksResponse() throws Exception {
		InputStream stream = testContext.getAssets().open("test/load-tracks-response.xml");

		GetsResponse resp = GetsResponse.parse(Utils.inputStreamToString(stream), TracksContent.class);
		assertEquals(0, resp.getCode());
		assertEquals("success", resp.getMessage());
		assertTrue(resp.getContent() instanceof TracksContent);

		List<Track> tracks = ((TracksContent) resp.getContent()).getTracks();
		assertEquals(2, tracks.size());
		assertEquals("tr_private", tracks.get(0).getName());
		assertEquals("Private track", tracks.get(0).getDescription());

		assertEquals("tr_private2", tracks.get(1).getName());
		assertEquals("Private track 2", tracks.get(1).getDescription());
	}

	@Test
	public void testLoadTrackResponse() throws Exception {
		InputStream stream = testContext.getAssets().open("test/load-track-response.xml");
		GetsResponse resp = GetsResponse.parse(Utils.inputStreamToString(stream), Kml.class);

		assertEquals(0, resp.getCode());
		assertEquals("success", resp.getMessage());
		assertTrue(resp.getContent() instanceof Kml);

		List<Point> tracks = ((Kml) resp.getContent()).getPoints();
		assertEquals(2, tracks.size());
		assertEquals("Test point", tracks.get(0).getName());
		assertEquals("Description text", tracks.get(0).getDescription());
		assertEquals("http://example.com/DescenteInfinie.ogg", tracks.get(0).getAudioUrl());

		assertEquals("qwe", tracks.get(1).getName());
		assertEquals("asd", tracks.get(1).getDescription());
	}

	@Test
	public void testAuth1() throws Exception {
		InputStream stream = testContext.getAssets().open("test/google-auth-stage1.xml");

		GetsResponse resp = GetsResponse.parse(Utils.inputStreamToString(stream), AuthRedirectResponse.class);
		assertEquals(2, resp.getCode());
		assertTrue(resp.getContent() instanceof AuthRedirectResponse);
		
		AuthRedirectResponse content = ((AuthRedirectResponse) resp.getContent());
		assertEquals("somelongid", content.getSessionId());
		assertEquals("http://example.com/authentication.php", content.getRedirectUrl());
	}

	@Test
	public void testAuth2() throws Exception {
		InputStream stream = testContext.getAssets().open("test/auth-token.xml");

		GetsResponse resp = GetsResponse.parse(Utils.inputStreamToString(stream), TokenContent.class);
		assertEquals(0, resp.getCode());
		assertTrue(resp.getContent() instanceof TokenContent);

		TokenContent content = ((TokenContent) resp.getContent());
		assertEquals("74a89174426b40307102e165374ab8ab", content.getAccessToken());
	}

	@Test
	public void testFiles() throws Exception {
		InputStream stream = testContext.getAssets().open("test/files.xml");

		GetsResponse resp = GetsResponse.parse(Utils.inputStreamToString(stream), FilesContent.class);
		assertEquals(0, resp.getCode());
		assertEquals("successs", resp.getMessage());
		assertTrue(resp.getContent() instanceof FilesContent);

		FilesContent content = ((FilesContent) resp.getContent());
		assertEquals(3, content.getFiles().size());

		FileContent file1 = content.getFiles().get(0);
		assertEquals(file1.getTitle(), "qwe asd");
		assertEquals(file1.getMimeType(), "image/png");
		assertEquals(file1.getUrl(), "https://docs.google.com/uc?id=0B99FJDhx6L84ck53cXEwbWdGY28&export=download");

		FileContent file2 = content.getFiles().get(1);
		assertEquals(file2.getTitle(), "qwe asd");
		assertEquals(file2.getMimeType(), "image/png");
		assertEquals(file2.getUrl(), "https://docs.google.com/uc?id=0B99FJDhx6L84Z0lXYXVxRjY0LWc&export=download");

		FileContent file3 = content.getFiles().get(2);
		assertEquals(file3.getTitle(), "<qwe asd>");
		assertEquals(file3.getMimeType(), "image/png");
		assertEquals(file3.getUrl(), "https://docs.google.com/uc?id=0B99FJDhx6L84bEZVVDZpR1duQTQ&export=download");
	}

	@Test
	public void testFile() throws Exception {
		InputStream stream = testContext.getAssets().open("test/file.xml");

		GetsResponse resp = GetsResponse.parse(Utils.inputStreamToString(stream), FileContent.class);
		assertEquals(0, resp.getCode());
		assertEquals("success", resp.getMessage());
		assertTrue(resp.getContent() instanceof FileContent);

		FileContent content = ((FileContent) resp.getContent());
		assertEquals(content.getTitle(), "qwerty");
		assertEquals(content.getMimeType(), "image/png");
		assertEquals(content.getUrl(), "https://docs.google.com/uc?id=0B99FJDhx6L84dmNnMlNOLU9JcXM&export=download");
	}
}
