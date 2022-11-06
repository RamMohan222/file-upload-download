package com.example.fileuploaddownload.controller;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/rx-files")
public class ReactiveRestAPIController {
	@Value("${file.upload-dir}")
	private String uploadDir;

	@PostMapping("upload")
	public Mono<String> upload(@RequestPart("file") FilePart filePart, ServerHttpRequest httpRequest) throws Exception {
		String fileName = System.nanoTime() + "-" + filePart.filename();
		return Mono.<Boolean>fromCallable(() -> {
			return BlockingUtils.createFile(filePart, uploadDir, fileName);
		}).subscribeOn(Schedulers.boundedElastic()).<String>flatMap(createFileState -> {
			Object resp = false;
			if (createFileState) {
				String baseUrl = BlockingUtils.baseUrl(httpRequest);
				resp = baseUrl + "/rx-files/download/" + fileName;
			}
			return Mono.just(resp.toString());
		});
	}

	@GetMapping(value = "/download/{fileName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public Mono<ResponseEntity<Resource>> downloadCsv(@PathVariable("fileName") String fileName) throws Exception {
		return Mono.<Resource>fromCallable(() -> {
			String fileLocation = uploadDir + "/" + fileName;
			String path = Paths.get(fileLocation).toAbsolutePath().normalize().toString();
			return new FileSystemResource(path);
		}).subscribeOn(Schedulers.boundedElastic()).<ResponseEntity<Resource>>flatMap(resource -> {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentDispositionFormData(fileName, fileName);
			return Mono.just(ResponseEntity.ok().cacheControl(CacheControl.noCache()).headers(headers).body(resource));
		});
	}
}
