package com.voluum.httpClient;

import com.voluum.logger.ProjectLogger;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Created by bogdan on 31.10.16.
 * Test framework
 */
public class HttpClientHelper {

	private static final String DEFAULT_ENCODING = "UTF-8";
	private static final Logger LOG = ProjectLogger.getLogger(HttpClientHelper.class.getSimpleName());
	private HttpRequestBase rawRequest;
	private HttpClient httpClient;
	private boolean withAuth;
	private String login;
	private String password;
	private boolean logResponseBody = true;

	public HttpClientHelper(final String url, final HttpMethod method) {
		this.httpClient = HttpClientBuilder.create().build();
		if (method.equals(HttpMethod.GET)) {
			this.rawRequest = new HttpGet(url);
		} else if (method.equals(HttpMethod.PUT)) {
			this.rawRequest = new HttpPut(url);
		} else if (method.equals(HttpMethod.POST)) {
			this.rawRequest = new HttpPost(url);
		} else {
			fail(format("%s is unsupported for now!", method.toString()));
		}
	}

	public HttpClientHelper(final String url, final DefaultRedirectStrategy redirectStrategy) {
		this.httpClient = HttpClientBuilder.create().setRedirectStrategy(redirectStrategy).build();
		this.rawRequest = new HttpGet(url);
	}

	public static HttpClientHelper get(final String url, final boolean withRedirectStrategy) {
		return withRedirectStrategy ? new HttpClientHelper(url, new RedirectStrategy()) : new HttpClientHelper(url,
				HttpMethod.GET);
	}

	public static HttpClientHelper put(final String url) {
		return new HttpClientHelper(url, HttpMethod.PUT);
	}

	public static HttpClientHelper post(final String url) {
		return new HttpClientHelper(url, HttpMethod.POST);
	}

	private static boolean isBodyApplicableTo(final HttpRequestBase request) {
		return (request.getClass().equals(HttpPut.class) || request.getClass().equals(HttpPost.class));
	}

	public HttpClientHelper addHeader(final String key, final String value) {
		rawRequest.addHeader(key, value);
		return this;
	}

	public HttpClientHelper addAccept(final String value) {
		return addHeader("Accept", value);
	}

	public HttpClientHelper addContentType(final String value) {
		return addHeader("Content-Type", value);
	}

	public HttpClientHelper addCwauthToken(final String value) {
		return addHeader("cwauth-token", value);
	}

	public HttpClientHelper addBasicAuth(final String login, final String password) {
		this.withAuth = true;
		this.login = login;
		this.password = password;
		String encodedAuthorization = "Basic " + Base64.encodeBase64String((login + ":" + password).getBytes());
		addHeader("Authorization", encodedAuthorization);
		return this;
	}

	public HttpClientHelper addBody(final String body) {
		if (isBodyApplicableTo(rawRequest)) {
			try {
				((HttpEntityEnclosingRequestBase) rawRequest).setEntity(new ByteArrayEntity(body
						.getBytes(DEFAULT_ENCODING)));
			} catch (UnsupportedEncodingException e) {
				fail(format("Exception during assigning body to request occurs \n %s", e));
			}
		} else {
			LOG.error("Cannot assign body to this http method!");
		}
		return this;
	}

	public HttpResponseWrapper sendAndGetResponse(final int expectedResponseCode) {
		HttpResponseWrapper response = sendRequest();
		assertThat(response.getStatusCode()).as("Response code is not the same as expected").isEqualTo
				(expectedResponseCode);
		return response;
	}

	public HttpResponseWrapper sendAndGetResponse() {
		return sendRequest();
	}

	private HttpResponseWrapper sendRequest() {
		HttpResponseWrapper response = null;
		try {
			logRequest(rawRequest);
			HttpResponse httpResponse = httpClient.execute(rawRequest);
			response = new HttpResponseWrapper(httpResponse);
			logResponse(response);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			fail(e.getMessage());
		}
		return response;
	}

	public String getRedirectedLink() {
		HttpContext context = new BasicHttpContext();
		HttpResponse resp = null;
		try {
			resp = httpClient.execute(rawRequest, context);
		} catch (IOException e) {
			LOG.error("Response status is not correct (expecting for status code 200)" + e);
			fail("Response status is not correct (expecting for status code 200)" + e);
		}
		if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			LOG.error("Response status is not correct (expecting for status code 200)");
			fail("Response status is not correct (expecting for status code 200)");
		}
		HttpUriRequest currentReq = (HttpUriRequest) context.getAttribute("http.request");
		HttpHost currentHost = (HttpHost) context.getAttribute("http.target_host");
		return (currentReq.getURI().isAbsolute()) ? currentReq.getURI().toString() : (currentHost.toURI()
				+ currentReq.getURI());
	}

	private void logRequest(final HttpRequestBase rawRequest) throws IOException {
		StringBuilder requestDescription = new StringBuilder("=== REQUEST ===\n");
		requestDescription.append(rawRequest.getRequestLine().toString()).append("\n");
		for (Header header : rawRequest.getAllHeaders()) {
			requestDescription.append(header).append("\n");
		}
		if (withAuth) {
			requestDescription.append("User/password: ").append(login).append("/").append(password).append("\n");
		}
		if (isBodyApplicableTo(rawRequest)) {
			HttpEntity entity = ((HttpEntityEnclosingRequestBase) rawRequest).getEntity();
			if (entity != null) {
				requestDescription.append(EntityUtils.toString(entity));
			}
		}
		requestDescription.append("\n");
		LOG.info(requestDescription.toString());
	}

	public HttpClientHelper doNotLogResponseBody() {
		logResponseBody = false;
		return this;
	}

	private void logResponse(final HttpResponseWrapper response) {
		StringBuilder responseDescription = new StringBuilder("=== RESPONSE ===\n");
		responseDescription.append(response.getRawResponse().getStatusLine().toString()).append("\n");
		for (Header header : response.getRawResponse().getAllHeaders()) {
			responseDescription.append(header).append("\n");
		}
		if (logResponseBody) {
			responseDescription.append(response.getBody()).append("\n");
		} else {
			responseDescription.append("-skip-body-\n");
		}
		LOG.debug(responseDescription.toString());

	}

	public enum HttpMethod {
		GET,
		PUT,
		POST,
		DELETE,
		OPTIONS,
		HEAD,
		CONNECT,
		TRACE;
	}

	private static class RedirectStrategy extends DefaultRedirectStrategy {

		@Override
		public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) {
			boolean isRedirect = false;
			try {
				isRedirect = super.isRedirected(request, response, context);
			} catch (ProtocolException e) {
				e.printStackTrace();
			}
			if (!isRedirect) {
				int responseCode = response.getStatusLine().getStatusCode();
				if (responseCode == 301 || responseCode == 302) {
					return true;
				}
			}
			return false;
		}
	}
}


