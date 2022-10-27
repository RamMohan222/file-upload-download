package com.example.fileuploaddownload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("com.example")
@SpringBootApplication
public class FileuploadDownloadApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileuploadDownloadApplication.class, args);
	}

}
