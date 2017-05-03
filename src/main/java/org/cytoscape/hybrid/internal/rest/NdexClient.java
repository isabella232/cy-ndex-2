package org.cytoscape.hybrid.internal.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.cytoscape.hybrid.internal.rest.errors.ErrorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Minimalistic NDEx V2 client
 *
 */
public class NdexClient {

	private static final Logger logger = LoggerFactory.getLogger(NdexClient.class);

	public static final String UUID_COLUMN_NAME = "ndex.uuid";
	
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

	public InputStream load(String url, String id, String pw) {

		if (url == null || url.isEmpty()) {
			throw new IllegalArgumentException("URL is missing.");
		}

		CloseableHttpClient client = getClient(id, pw);
		HttpGet httpget = new HttpGet(url);
		CloseableHttpResponse response = null;
		try {
			response = client.execute(httpget);
		} catch (IOException e) {
			logger.error(e.getMessage());
			final Response res = buildErrorResponse(Status.INTERNAL_SERVER_ERROR, "Could not fetch network from NDEx");
			throw new InternalServerErrorException(res);
		}

		// Get the response
		InputStream is = null;
		try {
			is = response.getEntity().getContent();
		} catch (UnsupportedOperationException | IOException e) {
			logger.error(e.getMessage());
			final Response res = buildErrorResponse(Status.INTERNAL_SERVER_ERROR, "Failed to open stream from NDEx");
			throw new InternalServerErrorException(res);
		}
		return is;
	}

	public Map<String, ?> getSummary(String url, String uuid) throws Exception {
		return getSummary(url, uuid, null, null);
	}

	/**
	 * Call network summary API
	 */
	public Map<String, ?> getSummary(String url, String uuid, String userId, String pw) throws WebApplicationException {

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
			logger.error(e.getMessage());
			final Response res = buildErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Could not get network summary from NDEx.");
			throw new InternalServerErrorException(res);
		}

		// Check response
		getError(response);

		Map<String, ?> result = null;
		String val;
		try {
			val = EntityUtils.toString(response.getEntity());
			result = mapper.readValue(val, Map.class);
			response.close();
			client.close();
		} catch (ParseException | IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			final Response res = buildErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Could not build network summary object.");
			throw new InternalServerErrorException(res);
		}

		return result;
	}

	private Response buildErrorResponse(Status status, String message) {
		final CIError error = new CIError(status.getStatusCode(), "urn:cytoscape:ci:ndex:v1:networks:errors:2", message,
				"/log");
		NdexResponse<Object> ndexResponse = new NdexResponse<>();
		ndexResponse.getErrors().add(error);

		final Response res = Response.status(status).type(MediaType.APPLICATION_JSON).entity(ndexResponse).build();

		return res;
	}

	private final void getError(CloseableHttpResponse response) throws WebApplicationException {
		final int code = response.getStatusLine().getStatusCode();

		Response res = null;
		if (code == Status.NOT_FOUND.getStatusCode()) {
			res = buildErrorResponse(Status.NOT_FOUND, "Resource not found");
			throw new NotFoundException(res);
		}

		if (code == Status.UNAUTHORIZED.getStatusCode()) {
			throw new NotAuthorizedException(buildErrorResponse(Status.UNAUTHORIZED, "Autholization to failed"));
		}

		if (code == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
			res = buildErrorResponse(Status.INTERNAL_SERVER_ERROR, "Remote service call failed.");
			throw new InternalServerErrorException(res);
		}
	}

	public void updateNetwork(String baseUrl, String uuid, String networkName, InputStream cxis, String id, String pw) {
		
		String url = baseUrl + "/network/" + uuid;
		
		try {
			final UploadUtil multipart = new UploadUtil(url, CHARSET, getAuth(id, pw));
			multipart.addFormJson("filename", networkName);
			multipart.addFilePart("CXNetworkStream", cxis);
			List<String> response = multipart.finish();

			if (response == null || response.isEmpty()) {

				throw new IOException("Could not  network.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			final String message = "Failed to update network to NDEx.";
			logger.error(message, e);
			Response res = ErrorBuilder.buildErrorResponse(Status.INTERNAL_SERVER_ERROR, message);
			throw new InternalServerErrorException(res);
		}
	}

	public String postNetwork(String url, String networkName, InputStream cxis, String id, String pw) {
		List<String> response = null;

		try {
			final UploadUtil multipart = new UploadUtil(url, CHARSET, getAuth(id, pw));
			multipart.addFormJson("filename", networkName);
			multipart.addFilePart("CXNetworkStream", cxis);
			response = multipart.finish();

			if (response == null || response.isEmpty()) {

				throw new IOException("Could not POST network.");
			}

			final String newUrl = response.get(0);
			final String[] parts = newUrl.split("/");
			return parts[parts.length - 1];
		} catch (Exception e) {
			final String message = "Failed to upload network to NDEx.";
			logger.error(message, e);
			Response res = ErrorBuilder.buildErrorResponse(Status.INTERNAL_SERVER_ERROR, message);
			throw new InternalServerErrorException(res);
		}
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
			final String message = "Given parameters are invalid";
			logger.error(message, e1);
			Response res = ErrorBuilder.buildErrorResponse(Status.BAD_REQUEST, message);
			throw new BadRequestException(res);
		}
		StringEntity params = new StringEntity(props, "UTF-8");
		params.setContentType("application/json");
		httpput.setEntity(params);

		CloseableHttpResponse response = null;

		try {
			response = client.execute(httpput);
		} catch (Exception e) {
			final String message = "Could not update visibility.";
			logger.error(message, e);
			Response res = ErrorBuilder.buildErrorResponse(Status.INTERNAL_SERVER_ERROR, message);
			throw new InternalServerErrorException(res);
		}

		getError(response);
	}

}
