package com.voluum.steps;

import com.voluum.httpClient.HttpClientHelper;
import com.voluum.httpClient.HttpResponseWrapper;
import com.voluum.logger.ProjectLogger;
import org.joda.time.LocalDate;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Created by bogdan on 01.11.16.
 * Test framework
 */
public class VoluumSteps {

	private static final Logger LOG = ProjectLogger.getLogger(VoluumSteps.class.getSimpleName());
	private String authBody = "{\"namePostfix\":\"Google\",\"url\":\"http://google.com/search?q=google\"}";

	public String getTokenFromLogInResponse() {
		LOG.info("Getting token from Log In response");
		String login = System.getProperty("voluum.login");
		String password = System.getProperty("voluum.password");
		String url = System.getProperty("voluum.login.url");
		HttpClientHelper httpClient = HttpClientHelper.get(url, false);
		HttpResponseWrapper response = httpClient.addBasicAuth(login, password).sendAndGetResponse(200);
		return response.extractRequestTokenFromBody();
	}

	public void performAuthToCoreAndReportingServices(final String token) {
		LOG.info("Performing auth through the http to Core and Reporting services");
		String url = System.getProperty("voluum.campaign.authorisation.url");
		HttpClientHelper httpClientCore = HttpClientHelper.put(url);
		httpClientCore.addContentType("application/json").addCwauthToken(token).addBody(authBody).sendAndGetResponse();
	}

	public void performAuthToCoreAndReportingServicesByCurl(final String token) {
		LOG.info("Performing auth through the curl to Core and Reporting services");
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
	}

	public String createNewDirectLinkingCampaign(final String token) {
		LOG.info("New direct linking campaign creation");
		String requestPayload = String.format(System.getProperty("request.payload"), getUniqueCampaignId());
		String url = System.getProperty("voluum.campaign.url");
		HttpClientHelper httpClientCampaign = HttpClientHelper.post(url);
		HttpResponseWrapper response = httpClientCampaign.addAccept("application/json").addContentType
				("application/json").addCwauthToken
				(token).addBody(requestPayload).sendAndGetResponse(201);
		return response.extractUrlFromBody();
	}

	public String performCampaignVisit(final String directUrl) {
		LOG.info("Performing Campaign visit");
		HttpClientHelper httpClientVisit = HttpClientHelper.get(directUrl, true);
		HttpResponseWrapper response = httpClientVisit.sendAndGetResponse(302);
		return response.getHeader("Location");
	}

	public String getRedirectionLink(final String directUrl) {
		LOG.info("Getting redirection link");
		HttpClientHelper httpClientVisit = HttpClientHelper.get(directUrl, false);
		return httpClientVisit.getRedirectedLink();
	}

	public String getSubIdValue(final String redirectedLink) {
		return redirectedLink.split("=")[1];
	}

	public void performPostbackRequest(final String redirectUrl) {
		LOG.info("Performing postback request");
		String url = String.format(System.getProperty("voluum.postback.url"), getSubIdValue(redirectUrl));
		HttpClientHelper httpClientPostback = HttpClientHelper.get(url, false);
		httpClientPostback.sendAndGetResponse(200);
	}

	public Integer getVisitsCount(final String token) {
		return Integer.parseInt(getReport(token).extractVisitsFromBody());
	}

	public Integer getConversionsCount(final String token) {
		return Integer.parseInt(getReport(token).extractConversionsFromBody());
	}

	private HttpResponseWrapper getReport(final String token) {
		LOG.info("Getting report");
		String mainUrl = System.getProperty("voluum.report.url");
		LocalDate localDate = new LocalDate();
		String parameters = System.getProperty("report.get.body").replace("DATE_1", localDate.toString()).replace
				("DATE_2", localDate.plusDays(1).toString());
		String url = mainUrl + parameters;
		HttpClientHelper httpClient = HttpClientHelper.get(url, false);
		return httpClient.addAccept("application/json").addContentType("application/json").addCwauthToken(token)
				.doNotLogResponseBody().sendAndGetResponse(200);
	}

	private String getUniqueCampaignId() {
		SecureRandom random = new SecureRandom();
		return new BigInteger(130, random).toString(32);
	}
}
