package org.cytoscape.hybrid.internal.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.Endpoint;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Minimalistic NDEx V2 client
 *
 */
public class NdexClient {

	private static final String PUBLIC_NDEX_URL = "http://www.ndexbio.org/v2";
	private static final String CHARSET = "UTF-8";

	private final ObjectMapper mapper;

	public NdexClient() {
		mapper = new ObjectMapper();
	}

	private final String getAuth(final String id, final String pw) {
		String credentials = id + ":" + pw;
		return "Basic " + new String(new Base64().encode(credentials.getBytes()));
	}

	private final CloseableHttpClient getClient(String id, String pw) {

		if (id == null || id.isEmpty() || pw == null || pw.isEmpty()) {
			return HttpClients.custom().build();
		}

		final Header header = new BasicHeader(HttpHeaders.AUTHORIZATION, getAuth(id, pw));
		final List<Header> headers = new ArrayList<>();
		headers.add(header);
		return HttpClients.custom().setDefaultHeaders(headers).build();
	}

	public InputStream load(String url) throws IOException {
		return load(url, null, null);
	}

	public InputStream load(String url, String id, String pw) throws IOException {

		if (url == null || url.isEmpty()) {
			throw new IllegalArgumentException("URL is missing.");
		}

		CloseableHttpClient client = getClient(id, pw);
		HttpGet httpget = new HttpGet(url);
		CloseableHttpResponse response = client.execute(httpget);

		// Get the response
		return response.getEntity().getContent();
	}

	public Map<String, ?> getSummary(String url, String uuid) throws IOException {
		return getSummary(url, uuid, null, null);
	}

	public Map<String, ?> getSummary(String url, String uuid, String userId, String pw) throws IOException {

		String serverUrl = null;
		if (url == null || url.isEmpty()) {
			serverUrl = PUBLIC_NDEX_URL + "/network/" + uuid + "/summary";
		} else {
			serverUrl = url + "/network/" + uuid + "/summary";
		}

		CloseableHttpClient client = getClient(userId, pw);
		HttpGet httpget = new HttpGet(serverUrl);

		CloseableHttpResponse response = null;
		try {
			response = client.execute(httpget);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not get response from NDEx.", e);
		}

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new RuntimeException("Could not get response from NDEx: code " + response.getStatusLine());
		}

		Map<String, ?> result = null;
		String val = EntityUtils.toString(response.getEntity());
		result = mapper.readValue(val, Map.class);

		response.close();
		client.close();
		return result;
	}

	public String postNetwork(String url, String networkName, InputStream cxis, String id, String pw)
			throws IOException {
		final UploadUtil multipart = new UploadUtil(url, CHARSET, getAuth(id, pw));
		multipart.addFormJson("filename", networkName);
		multipart.addFilePart("CXNetworkStream", cxis);
		final List<String> response = multipart.finish();

		if (response == null || response.isEmpty()) {
			throw new IOException("Could not POST network.");
		}

		final String newUrl = response.get(0);
		final String[] parts = newUrl.split("/");
		return parts[parts.length - 1];
	}

	public void setVisibility(String url, String uuid, Boolean isPublic, String id, String pw) {
		final String endpoint = url + "/network/" + uuid + "/systemproperty";

		String visibility;

		if (isPublic) {
			visibility = "PUBLIC";
		} else {
			visibility = "PRIVATE";
		}

		final Map<String, String> propMap = new HashMap<>();
		propMap.put("visibility", visibility);

		CloseableHttpClient client = getClient(id, pw);
		HttpPut httpput = new HttpPut(endpoint);
		
		String props = null;
		try {
			props = mapper.writeValueAsString(propMap);
		} catch (JsonProcessingException e1) {
			e1.printStackTrace();
		}
		StringEntity params = new StringEntity(props, "UTF-8");
		params.setContentType("application/json");
		httpput.setEntity(params);

		CloseableHttpResponse response = null;
		
		try {
			response = client.execute(httpput);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not update visibility.", e);
		}

		final int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 200 && statusCode != 204) {
			throw new RuntimeException("Could not get response from NDEx: code " + response.getStatusLine());
		}
	}

}
