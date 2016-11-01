package com.voluum;

import com.voluum.httpClient.HttpClientHelper;
import com.voluum.httpClient.HttpResponseWrapper;
import com.voluum.logger.ProjectLogger;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Created by bogdan on 31.10.16.
 * Test framework
 */
public class VoluumTests {

	private static final Logger LOG = ProjectLogger.getLogger(VoluumTests.class.getSimpleName());

	/*private static void curl(final String token) {
		String url = System.getProperty("voluum.campaign.authorisation.url");
		String[] command = {"curl", url, "-X", "PUT", "-H", "Content-Type: application/json", "-H", "cwauth-token: " +
				token, "--data-binary '" + authBody + "'"};
		ProcessBuilder process = new ProcessBuilder(command);
		Process p;
		try {
			p = process.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String result = builder.toString();
			System.out.print(result);

		} catch (IOException e) {
			System.out.print("error");
			e.printStackTrace();
		}
	}*/

	@BeforeClass
	public void beforeClass() {
		try {
			LOG.debug("Initialising properties");
			Class.forName("com.voluum.VoluumProperties");
		} catch (ClassNotFoundException e) {
			LOG.error("Error instantiating VoluumProperties", e);
		}
	}

	@Test
	public void testScenarioOne() throws IOException {
		// User is logged in
		String login = System.getProperty("voluum.login");
		String password = System.getProperty("voluum.password");
		String url = System.getProperty("voluum.login.url");
		HttpClientHelper httpClient = HttpClientHelper.get(url);
		HttpResponseWrapper response = httpClient.addBasicAuth(login, password).sendAndGetResponse(200);
		String token = response.extractRequestTokenFromBody();

		// Authorisation to Core and Reporting services
		url = System.getProperty("voluum.campaign.authorisation.url");
		HttpClientHelper httpClientCore = HttpClientHelper.put(url);
		String authBody = "{\"namePostfix\":\"Google\",\"url\":\"http://google.com/search?q=google\"}";
		httpClientCore.addContentType("application/json").addCwauthToken(token).addBody(authBody).sendAndGetResponse();
		// curl(token);

		// User creates a new Direct linking campaign with Destination pointing to URL
		String requestPayload = "{\"namePostfix\":\"test25\",\"costModel\":\"NOT_TRACKED\"," +
				"\"clickRedirectType\":\"REGULAR\",\"trafficSource\":{\"id\":\"6051135c-a890-4618-9efc-ab6dade95960" +
				"\"}," +
				"\"redirectTarget\":\"DIRECT_URL\",\"client\":{\"id\":\"8045a943-b4bb-4af9-b63b-674e7e758f47\"," +
				"\"clientCode\":\"rskxz\",\"mainDomain\":\"rskxz.voluumtrk.com\",\"defaultDomain\":\"voluumtrk" +
				".com\"," +
				"\"customParam1Available\":false,\"realtimeRoutingAPI\":false,\"rootRedirect\":false}," +
				"\"costModelHidden\":true,\"directRedirectUrl\":\"http://example.com?subid={clickId}\"}";
		url = System.getProperty("voluum.campaign.url");
		HttpClientHelper httpClientCampaign = HttpClientHelper.post(url);
		response = httpClientCampaign.addAccept("application/json").addContentType("application/json").addCwauthToken
				(token).addBody(requestPayload).sendAndGetResponse(201);

		// getting url and redirect url for campaign
		String urlFromBody = response.extractUrlFromBody();
		System.out.println("Url -> " + urlFromBody);

		// perform visit (http://rskxz.voluumtrk.com/voluum/173eb28c-cf1b-411d-9bf1-d57050805c4c)
		HttpClientHelper httpClientVisit = HttpClientHelper.get(urlFromBody);
		httpClientVisit.doNotLogResponseBody().sendAndGetResponse(200);

		// redirection
		HttpClient cl = HttpClientBuilder.create().build();
		HttpGet httpget = new HttpGet(urlFromBody);
		HttpContext context = new BasicHttpContext();
		HttpResponse resp = cl.execute(httpget, context);
		if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
			throw new IOException(resp.getStatusLine().toString());
		HttpUriRequest currentReq = (HttpUriRequest) context.getAttribute("http.request");
		HttpHost currentHost = (HttpHost) context.getAttribute("http.target_host");
		String currentUrl = (currentReq.getURI().isAbsolute()) ? currentReq.getURI().toString() : (currentHost.toURI()
				+ currentReq.getURI());
		System.out.println(currentUrl);

		// perform postback request (http://rskxz.voluumtrk.com/postback?cid=wGC221DPNJU81KQ0HHII5DVS)
		url = String.format(System.getProperty("voluum.postback.url"), currentUrl.split("=")[1]);
		HttpClientHelper httpClientPostback = HttpClientHelper.get(url);
		httpClientPostback.sendAndGetResponse(200);
	}

	@Test
	public void testScenarioTwo() {
	}

	@Test
	public void testScenarioThree() {
	}
}