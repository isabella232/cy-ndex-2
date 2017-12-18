package org.cytoscape.cyndex2.internal.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UploadUtil {

	private static final String LINE_FEED = "\r\n";
	
	private final String boundary;
	private HttpURLConnection httpConn;
	private String charset;
	private OutputStream outputStream;
	private PrintWriter writer;

	public UploadUtil(String requestURL, String charset, String authenticationString) throws IOException {
		this.charset = charset;

		// creates a unique boundary based on time stamp
		boundary = "===" + System.currentTimeMillis() + "===";

		URL url = new URL(requestURL);
		httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setUseCaches(false);
		httpConn.setDoOutput(true); // indicates POST method
		httpConn.setDoInput(true);
		httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		httpConn.setRequestProperty("Authorization", authenticationString);

		outputStream = httpConn.getOutputStream();
		writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);
	}

	public void addFormField(String name, String value) {
		writer.append("--" + boundary).append(LINE_FEED);
		writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(LINE_FEED);
		writer.append("Content-Type: text/plain; charset=" + charset).append(LINE_FEED);
		writer.append(LINE_FEED);
		writer.append(value).append(LINE_FEED);
		writer.flush();
	}

	public void addFormJson(String name, String value) {
		writer.append("--" + boundary).append(LINE_FEED);
		writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(LINE_FEED).append(LINE_FEED);
		writer.append(value).append(LINE_FEED);
		writer.flush();
	}

	/**
	 * Adds a upload file section to the request
	 * 
	 * @param fieldName
	 *            name attribute in <input type="file" name="..." />
	 * @param uploadFile
	 *            a File to be uploaded
	 * @throws IOException
	 */
	public void addFilePart(String fieldName, InputStream cxStream) throws IOException {

		writer.append("--" + boundary).append(LINE_FEED);
		writer.append("Content-Disposition: form-data; name=\"" + fieldName + "\"").append(LINE_FEED);
		writer.append("Content-Type: application/octet-stream").append(LINE_FEED);
		writer.append(LINE_FEED);
		writer.flush();

		byte[] buffer = new byte[4096];
		int bytesRead = -1;
		while ((bytesRead = cxStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}
		outputStream.flush();
		cxStream.close();

		writer.append(LINE_FEED);
		writer.flush();
	}

	/**
	 * Adds a header field to the request.
	 * 
	 * @param name
	 *            - name of the header field
	 * @param value
	 *            - value of the header field
	 */
	public void addHeaderField(String name, String value) {
		writer.append(name + ": " + value).append(LINE_FEED);
		writer.flush();
	}

	/**
	 * Completes the request and receives response from the server.
	 * 
	 * @return a list of Strings as response in case the server returned status
	 *         OK, otherwise an exception is thrown.
	 * @throws IOException
	 */
	public List<String> finish() throws IOException {
		List<String> response = new ArrayList<>();

		writer.append(LINE_FEED).flush();
		writer.append("--" + boundary + "--").append(LINE_FEED);
		writer.close();

		// checks server's status code first
		int status = httpConn.getResponseCode();
		if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_CREATED) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				response.add(line);
			}
			reader.close();
			httpConn.disconnect();
		} else if (status == HttpURLConnection.HTTP_NO_CONTENT) {
			httpConn.disconnect();
			return null;
		} else {
			httpConn.disconnect();
			throw new IOException("Server returned non-OK status: " + status + ".  Response message from server: "
					+ httpConn.getResponseMessage());
		}

		return response;
	}

}
