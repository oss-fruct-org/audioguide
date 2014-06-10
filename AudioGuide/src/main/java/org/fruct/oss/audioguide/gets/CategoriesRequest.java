package org.fruct.oss.audioguide.gets;

import org.fruct.oss.audioguide.parsers.CategoriesContent;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;

public class CategoriesRequest extends GetsRequest {
	public CategoriesRequest(Gets gets) {
		super(gets);
	}

	@Override
	protected String createRequestString() {
		return "<request><params /></request>";
	}

	@Override
	protected String getRequestUrl() {
		return Gets.GETS_SERVER + "/getCategories.php";
	}

	@Override
	protected Class<? extends IContent> getContentClass() {
		return CategoriesContent.class;
	}

	@Override
	protected void onPostProcess(GetsResponse response) {
		if (response.getCode() == 0) {
			gets.setEnv("categories", CategoriesContent.filterByPrefix(((CategoriesContent) response.getContent()).getCategories()));
		}
	}
	@Override
	protected void onError() {

	}
}
