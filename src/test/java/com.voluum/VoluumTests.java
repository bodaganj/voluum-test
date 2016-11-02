package com.voluum;

import com.voluum.logger.ProjectLogger;
import com.voluum.steps.VoluumSteps;
import org.slf4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by bogdan on 31.10.16.
 * Test framework
 */
public class VoluumTests {

	private static final Logger LOG = ProjectLogger.getLogger(VoluumTests.class.getSimpleName());
	private VoluumSteps voluumSteps = new VoluumSteps();

	@BeforeClass
	public void beforeClass() {
		try {
			LOG.debug("Initialising properties");
			Class.forName("com.voluum.VoluumProperties");
		} catch (ClassNotFoundException e) {
			LOG.error("Error instantiating VoluumProperties", e);
		}
	}

	/**
	 * Scenario 1: New Campaign should be able to redirect to destination URL
	 */
	@Test
	public void testDestinationUrlRedirection() {
		String token = voluumSteps.getTokenFromLogInResponse();
		voluumSteps.performAuthToCoreAndReportingServices(token);
		String directUrl = voluumSteps.createNewDirectLinkingCampaign(token);
		voluumSteps.performCampaignVisit(directUrl);
		String redirectUrl = voluumSteps.getRedirectionLink(directUrl);
		String subIdValue = voluumSteps.getSubIdValue(redirectUrl);
		assertThat(subIdValue.length()).as("subid parameter’s value should be resolved to 24 Characters random ID")
				.isEqualTo(24);
	}

	/**
	 * Scenario 2: HTTP Get request to Campaign URL should result with incrementing campaign visits counter by 1
	 */
	@Test
	public void testVisitCounterIncrementation() {
		String token = voluumSteps.getTokenFromLogInResponse();
		voluumSteps.performAuthToCoreAndReportingServices(token);
		String directUrl = voluumSteps.createNewDirectLinkingCampaign(token);
		Integer visitsCount = voluumSteps.getVisitsCount(token);
		voluumSteps.performCampaignVisit(directUrl);
		await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> voluumSteps.getVisitsCount
				(token), is(visitsCount + 1));
	}

	/**
	 * Scenario 3: HTTP GET request to Postback URL with valid ClickID token should increment Campaign Conversions
	 * count by 1
	 */
	@Test
	public void testConversionCounterIncrementation() {
		String token = voluumSteps.getTokenFromLogInResponse();
		voluumSteps.performAuthToCoreAndReportingServices(token);
		String directUrl = voluumSteps.createNewDirectLinkingCampaign(token);
		voluumSteps.performCampaignVisit(directUrl);
		String redirectUrl = voluumSteps.getRedirectionLink(directUrl);
		Integer conversionsCount = voluumSteps.getConversionsCount(token);
		voluumSteps.performPostbackRequest(redirectUrl);
		await().atMost(30, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> voluumSteps
				.getConversionsCount(token), is(conversionsCount + 1));
	}
}