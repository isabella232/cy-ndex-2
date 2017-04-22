package org.cytoscape.hybrid.internal.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
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

	private final String getAuth(final String id, final String pw) {
		String credentials = id + ":" + pw;
		return "Basic " + new String(new Base64().encode(credentials.getBytes()));
	}

	public InputStream load(String url, String id, String pw) throws IOException {

		final Header header = new BasicHeader(HttpHeaders.AUTHORIZATION, getAuth(id, pw));
		final List<Header> headers = new ArrayList<>();
		headers.add(header);
		CloseableHttpClient client = HttpClients.custom().setDefaultHeaders(headers).build();

		HttpGet httpget = new HttpGet(url);
		CloseableHttpResponse response = client.execute(httpget);

		// Get the response
		return response.getEntity().getContent();
	}

	public Map<String, ?> getSummary(String url, String uuid, String userId, String pw) throws IOException {

		String serverUrl = null;
		if (url == null || url.isEmpty()) {
			serverUrl = PUBLIC_NDEX_URL + "/network/" + uuid + "/summary";
		} else {
			serverUrl = url + "/network/" + uuid + "/summary";
		}

		final Header header = new BasicHeader(HttpHeaders.AUTHORIZATION, getAuth(userId, pw));
		final List<Header> headers = new ArrayList<>();
		 headers.add(header);

		CloseableHttpClient client = HttpClients.custom().setDefaultHeaders(headers).build();
		HttpGet httpget = new HttpGet(serverUrl);

		CloseableHttpResponse response = null;
		try {
			response = client.execute(httpget);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}

		Map<String, ?> result = null;
		String val = EntityUtils.toString(response.getEntity());
		result = mapper.readValue(val, Map.class);

		response.close();
		client.close();
		return result;
	}
}
