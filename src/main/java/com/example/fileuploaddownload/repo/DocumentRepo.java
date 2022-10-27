package com.example.fileuploaddownload.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fileuploaddownload.entity.Document;

@Repository
public interface DocumentRepo extends JpaRepository<Document, Long> {
	public List<Document> findByDocName(String docName);
}
