package org.fruct.oss.audioguide.test;


import android.content.Context;
import android.test.AndroidTestCase;

import org.fruct.oss.audioguide.parsers.AuthRedirectResponse;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;
import org.fruct.oss.audioguide.parsers.Kml;
import org.fruct.oss.audioguide.parsers.TracksContent;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.util.Utils;

import java.io.InputStream;
import java.util.List;

public class ParserTest extends AndroidTestCase{
	private Context testContext;
	private InputStream stream;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

        if (testContext == null) {
			assert getContext() != null;
            testContext = getContext().createPackageContext(
                    "org.fruct.oss.audioguide.test", Context.CONTEXT_IGNORE_SECURITY);
        }
	}

	@Override
	protected void tearDown() throws Exception {
		if (stream != null) {
			stream.close();
			stream = null;
		}

		super.tearDown();
	}

	public void testErrorResponse() throws Exception {
		InputStream stream = testContext.getAssets().open("error-response.xml");

		GetsResponse resp = GetsResponse.parse(Utils.inputStreamToString(stream), IContent.class);
		assertEquals(1, resp.getCode());
		assertEquals("Wrong token error", resp.getMessage());
		assertNull(resp.getContent());
	}

	public void testLoadTracksResponse() throws Exception {
		InputStream stream = testContext.getAssets().open("load-tracks-response.xml");

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

	public void testLoadTrackResponse() throws Exception {
		InputStream stream = testContext.getAssets().open("load-track-response.xml");
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

	public void testAuth1() throws Exception {
		InputStream stream = testContext.getAssets().open("google-auth-stage1.xml");

		GetsResponse resp = GetsResponse.parse(Utils.inputStreamToString(stream), AuthRedirectResponse.class);
		assertEquals(2, resp.getCode());
		assertTrue(resp.getContent() instanceof AuthRedirectResponse);
		
		AuthRedirectResponse content = ((AuthRedirectResponse) resp.getContent());
		assertEquals("somelongid", content.getSessionId());
		assertEquals("http://example.com/authentication.php", content.getRedirectUrl());
	}
}
