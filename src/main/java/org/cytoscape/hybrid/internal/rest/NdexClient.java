package org.cytoscape.hybrid.internal.rest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

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
	
	private final CloseableHttpClient getClient(String id, String pw) {
		
		if(id == null || id.isEmpty() || pw == null || pw.isEmpty()) {
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

		if(url == null || url.isEmpty()) {
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
		
		if(response.getStatusLine().getStatusCode() != 200) {
			throw new RuntimeException("Could not get response from NDEx: code " + response.getStatusLine());			
		}

		Map<String, ?> result = null;
		String val = EntityUtils.toString(response.getEntity());
		result = mapper.readValue(val, Map.class);

		response.close();
		client.close();
		return result;
	}
	
	public void postNetworkAsMultipartObject(String route, String fileToUpload) throws JsonProcessingException, IOException {

		Preconditions.checkState(!Strings.isNullOrEmpty(fileToUpload), "No file name specified.");
		
		String charset  = "UTF-8";
		
		Path p = Paths.get(fileToUpload);
		String fileNameForPostRequest = p.getFileName().toString(); // get the filename only; remove the path
		
//		UploadUtil multipart = new UploadUtil(_baseroute + route, charset, getAuthenticationString());
//		
//        multipart.addFormJson("filename", fileNameForPostRequest);
//	    multipart.addFilePart("fileUpload", uploadFile);
//
//	    List<String> response = multipart.finish();
//	    
//	    if (null == response) 
//	    	return;
//	             
//	    for (String line : response) {
//	        System.out.println(line);
//	    }
	}

	public void saveToNdex(String cx) {
		
	}
	
}
