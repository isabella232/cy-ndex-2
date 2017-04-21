package org.cytoscape.hybrid.internal.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Minimalistic NDEx V2 client
 *
 */
public class NdexClient {

	private static final String PUBLIC_NDEX_URL = "http://www.ndexbio.org/v2";

	private final ObjectMapper mapper;

	public NdexClient() {
		mapper = new ObjectMapper();
	}

	public InputStream load(String url) throws IOException {

		CloseableHttpClient httpclient = HttpClients.custom().build();
		HttpGet httpget = new HttpGet(url);
		CloseableHttpResponse response = httpclient.execute(httpget);

		// Get the response
		return response.getEntity().getContent();
	}

	public Map<String, ?> getSummary(String url, String uuid, String userId, String token) throws IOException {

		String serverUrl = null;
		if (url == null || url.isEmpty()) {
			serverUrl = PUBLIC_NDEX_URL + "/network/" + uuid + "/summary";
		} else {
			serverUrl = url + "/network/" + uuid + "/summary";
		}

		CloseableHttpClient httpclient = HttpClients.custom().build();
		HttpGet httpget = new HttpGet(serverUrl);
		CloseableHttpResponse response = httpclient.execute(httpget);

		Map<String, ?> result = null;
		try {
			String val = EntityUtils.toString(response.getEntity());
			result = mapper.readValue(val, Map.class);

		} finally {
			response.close();
		}

		httpclient.close();

		return result;
	}
}
