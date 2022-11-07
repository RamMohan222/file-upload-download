package com.example.fileuploaddownload.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.Timeout;

public class ApacheHttpAsyncClient {

	private final CloseableHttpAsyncClient client;

	public ApacheHttpAsyncClient() {
		// @formatter:off
		final IOReactorConfig ioReactorConfig = IOReactorConfig
				.custom()
				.setSoTimeout(Timeout.ofSeconds(5))
				.build();

		this.client = HttpAsyncClients
				.custom()
				.setIOReactorConfig(ioReactorConfig)
				.build();
		// @formatter:on
	}

	public static Map<String, Object> upload(String uploadUri, byte[] bytes) {
		return null;
	}

	public byte[] download(final String downloadUri) throws Exception {

		// @formatter:off
		client.start();

		final SimpleHttpRequest request = SimpleRequestBuilder
				.get(downloadUri)
				.build();

		final Future<SimpleHttpResponse> future = client.execute(
				SimpleRequestProducer.create(request),
				SimpleResponseConsumer.create(), 
				new FutureCallback<SimpleHttpResponse>() {

					@Override
					public void completed(final SimpleHttpResponse response) {
						System.out.println(request + "->" + new StatusLine(response));
						System.out.println(response.getBody());
					}

					@Override
					public void failed(final Exception ex) {
						System.out.println(request + "->" + ex);
					}

					@Override
					public void cancelled() {
						System.out.println(request + " cancelled");
					}

				});
		SimpleHttpResponse response = future.get();
		// @formatter:on
		client.close(CloseMode.GRACEFUL);
		return response.getBodyBytes();
	}

	public void printHeaders(SimpleHttpResponse response) {
		Header[] headers = response.getHeaders();
		for (Header h : headers) {
			System.out.println(h.getName() + "\t" + h.getValue());
		}
	}

	public static void main(String[] args) throws Exception {
		String downloadUri = "http://localhost:8080/rx-files/download/abc.JPG";
		ApacheHttpAsyncClient client = new ApacheHttpAsyncClient();
		byte[] bytes = client.download(downloadUri);
		System.out.println("==>" + bytes.length);
		FileOutputStream os = new FileOutputStream(new File("E:\\new-file.jpg"));
		IOUtils.copyLarge(new ByteArrayInputStream(bytes), os);
	}
}
