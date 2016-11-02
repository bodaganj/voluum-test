package com.voluum.httpClient;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.fail;

/**
 * Created by bogdan on 31.10.16.
 * Test framework
 */
public class HttpResponseWrapper {

	private final int statusCode;
	private String body;
	private HttpResponse rawResponse;

	public HttpResponseWrapper(final HttpResponse httpResponse) {
		rawResponse = httpResponse;
		statusCode = httpResponse.getStatusLine().getStatusCode();
		try {
			body = EntityUtils.toString(httpResponse.getEntity());
		} catch (IOException e) {
			fail(format("Exception during body extracting occurs \n %s", e));
		}
	}

	public String getBody() {
		return body;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public HttpResponse getRawResponse() {
		return rawResponse;
	}

	public String getHeader(final String header) {
		return rawResponse.getFirstHeader(header).getValue();
	}

	public String extractRequestTokenFromBody() {
		String regexp = "\"token\"\\s:\\s\"([a-zA-Z0-9_-]+)+\"";
		return extractPatternFromBody(regexp);
	}

	public String extractUrlFromBody() {
		String regexp = "\"url\"\\s:\\s\"(https?:\\/\\/[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b" +
				"([-a-zA-Z0-9@:%_\\+.~#?&//=]*))\"";
		return extractPatternFromBody(regexp);
	}

	public String extractVisitsFromBody() {
		String regexp = "\"visits\"\\s:\\s([0-9]+)";
		return extractPatternFromBody(regexp);
	}

	public String extractConversionsFromBody() {
		String regexp = "\"conversions\"\\s:\\s([0-9]+)";
		return extractPatternFromBody(regexp);
	}

	private String extractPatternFromBody(final String regexp) {
		Pattern pattern = Pattern.compile(regexp);
		Matcher matcher = pattern.matcher(this.body);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return "";
	}
}
