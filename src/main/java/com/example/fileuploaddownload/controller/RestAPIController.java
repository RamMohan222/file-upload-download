package com.example.fileuploaddownload.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.fileuploaddownload.entity.Document;
import com.example.fileuploaddownload.repo.DocumentRepo;

@RestController
@RequestMapping("/files")
public class RestAPIController {
	@Value("${file.upload-dir}")
	private String uploadDir;
	private DocumentRepo documentRepo;

	public RestAPIController(DocumentRepo documentRepo) {
		this.documentRepo = documentRepo;
	}

	@GetMapping("/ping")
	public ResponseEntity<?> test() {
		return ResponseEntity.ok("OK");
	}

	@PostMapping("/upload")
	public ResponseEntity<?> uploadToLocalFileSystem(@RequestParam("file") MultipartFile file) {
		String fileName = StringUtils.cleanPath(file.getOriginalFilename());
		Path path = Paths.get(uploadDir + fileName);
		try {
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/files/download/")
				.path(fileName).toUriString();

		Map<String, Object> response = new HashMap<>();
		response.put("fileUri", fileDownloadUri);
		response.put("fileId", new Random().nextInt());

		return ResponseEntity.ok(response);
	}

	@PostMapping("/multi-upload")
	public ResponseEntity<?> multiUpload(@RequestParam("files") MultipartFile[] files) {
		List<Object> fileDownloadUrls = new ArrayList<>();
		Arrays.asList(files).stream().forEach(file -> fileDownloadUrls.add(uploadToLocalFileSystem(file).getBody()));
		return ResponseEntity.ok(fileDownloadUrls);
	}

	@PostMapping("/upload-extra-param")
	public ResponseEntity<?> uploadWithExtraParams(@RequestParam("file") MultipartFile file,
			@RequestParam String paramName) {
		System.out.println("Extra param " + paramName);
		return uploadToLocalFileSystem(file);
	}

	@PostMapping("/upload/db")
	public ResponseEntity<?> uploadToDB(@RequestParam("file") MultipartFile file) {
		Document doc = new Document();
		String fileName = StringUtils.cleanPath(file.getOriginalFilename());
		doc.setDocName(fileName);
		try {
			doc.setFile(file.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		documentRepo.save(doc);
		String fileDownloadUriById = ServletUriComponentsBuilder.fromCurrentContextPath().path("/files/download/db")
				.queryParam("fileId", doc.getId()).toUriString();
		String fileDownloadUriByName = ServletUriComponentsBuilder.fromCurrentContextPath().path("/files/download/db")
				.queryParam("fileName", doc.getDocName()).toUriString();

		Map<String, String> maps = new HashMap<>();
		maps.put("fileId", fileDownloadUriById);
		maps.put("fileName", fileDownloadUriByName);
		return ResponseEntity.ok(maps);
	}

	@GetMapping("/download/{fileName:.+}")
	public ResponseEntity<?> downloadFileFromLocal(@PathVariable String fileName) {
		Path path = Paths.get(uploadDir + fileName);
		Resource resource = null;
		try {
			resource = new UrlResource(path.toUri());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return ResponseEntity.ok()
				// .contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}

	@GetMapping(value = "/download/db", params = { "fileId" })
	public ResponseEntity<?> downloadFileFromDb(@RequestParam long fileId) {
		Optional<Document> document = documentRepo.findById(fileId);

		if (document.isPresent()) {
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=\"" + document.get().getDocName() + "\"")
					.body(document.get().getFile());
		} else {
			return ResponseEntity.ok("Files not found");
		}

	}

	@GetMapping(value = "/download/db", params = { "fileName" })
	public void downloadFileFromDb(@RequestParam String fileName, HttpServletResponse response) throws IOException {
		List<Document> documents = documentRepo.findByDocName(fileName);
		ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
		for (Document document : documents) {
			ZipEntry zipEntry = new ZipEntry(UUID.randomUUID() + document.getDocName());
			zipEntry.setSize(document.getFile().length);
			zipOut.putNextEntry(zipEntry);
			StreamUtils.copy(new ByteArrayInputStream(document.getFile()), zipOut);
			zipOut.closeEntry();
		}
		zipOut.finish();
		zipOut.close();
		String zipFileName = "download.zip";
		response.setStatus(HttpServletResponse.SC_OK);
		response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"");
	}

	@GetMapping(value = "/zip-download", produces = "application/zip")
	public void zipDownload(@RequestParam List<String> name, HttpServletResponse response) throws IOException {
		ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
		for (String fileName : name) {
			FileSystemResource resource = new FileSystemResource(uploadDir + fileName);
			ZipEntry zipEntry = new ZipEntry(resource.getFilename());
			zipEntry.setSize(resource.contentLength());
			zipOut.putNextEntry(zipEntry);
			StreamUtils.copy(resource.getInputStream(), zipOut);
			zipOut.closeEntry();
		}
		zipOut.finish();
		zipOut.close();
		String zipFileName = "download.zip";
		response.setStatus(HttpServletResponse.SC_OK);
		response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"");
	}
}
