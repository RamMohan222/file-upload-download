package com.example.fileuploaddownload.client;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApacheHttpClient {

	private Tika tika = new Tika();
	private TikaConfig config = TikaConfig.getDefaultConfig();
	private CloseableHttpClient httpClient = null;
	private ObjectMapper mapper = new ObjectMapper();
	private TypeReference<Map<String, Object>> mapType = new TypeReference<Map<String, Object>>() {
	};

	public ApacheHttpClient() {
		// @formatter:off
		int timeout = 60;
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeout * 1000)
				.setConnectionRequestTimeout(timeout * 1000)
				.setSocketTimeout(timeout * 1000)
				.build();
		// @formatter:on

		TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
		SSLContext sslContext = null;
		try {
			sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// @formatter:off
		httpClient = HttpClientBuilder.create()
				.setDefaultRequestConfig(config)
				.setSSLContext(sslContext)
				.build();
		// @formatter:on
	}

	public static void main(String[] args) throws Exception {
		String downloadUri = "https://httpbin.org/image";
		downloadUri = "http://localhost:8080/rx-files/download/abc.JPG";
//		downloadUri = "http://localhost:8080/files/download/abc.JPG";
		String uploadUri = "http://localhost:8080/files/upload-extra-param";
		Map<String, Object> json = downloadAndUpload(downloadUri, uploadUri, "jwtToken");
		System.out.println(json);
	}

	public static Map<String, Object> downloadAndUpload(String downloadUri, String uploadUri, String accessToken)
			throws Exception {
		ApacheHttpClient client = new ApacheHttpClient();
		byte[] imageBytes = client.download(downloadUri, accessToken);
		return client.uploadFile(imageBytes, uploadUri, accessToken);
	}

	public byte[] download(String uri, String token) throws Exception {
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader(HttpHeaders.AUTHORIZATION, token);

		try (CloseableHttpResponse response = httpClient.execute(httpGet)) {

			for (Header h : response.getAllHeaders()) {
				System.out.println(h.getName() + " | " + h.getValue());
			}

			InputStream is = response.getEntity().getContent();
			byte[] bytes = is.readAllBytes();
			return bytes;
		}
	}

	public Map<String, Object> uploadFile(byte[] imageBytes, String uri, String token) throws Exception {
		String fileName = getFileName(imageBytes);
		// @formatter:off
		HttpEntity entity = MultipartEntityBuilder
				.create()
				.addTextBody("paramName", "paramValue")
				.addBinaryBody("file", imageBytes, ContentType.APPLICATION_OCTET_STREAM, fileName).build();
		// @formatter:on
		HttpPost httpPost = new HttpPost(uri);
		httpPost.setEntity(entity);
		httpPost.addHeader(HttpHeaders.AUTHORIZATION, token);

		try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
			HttpEntity result = response.getEntity();
			return mapper.readValue(result.getContent(), mapType);
		}
	}

	public String getFileName(byte[] bytes) throws Exception {
		String mediatype = tika.detect(bytes);
		String ext = config.getMimeRepository().getRegisteredMimeType(mediatype).getExtension();
		return UUID.randomUUID() + ext;
	}
}
