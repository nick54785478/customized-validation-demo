package com.example.demo.iface.rest;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.application.service.UploadCommandService;
import com.example.demo.iface.dto.TemplateUploadedResource;
import com.example.demo.iface.dto.UploadTemplateResource;
import com.example.demo.share.command.UploadTemplateCommand;
import com.example.demo.util.BaseDataTransformer;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/upload")
public class UploadController {

	private UploadCommandService uploadCommandService;

	@PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<TemplateUploadedResource> upload(
			@RequestPart(name = "resource", required = true) UploadTemplateResource resource,
			@RequestPart(name = "file", required = true) MultipartFile file) throws IOException {
		UploadTemplateCommand command = BaseDataTransformer.transformData(resource, UploadTemplateCommand.class);
		uploadCommandService.upload(command, file);
		return new ResponseEntity<>(new TemplateUploadedResource("200", "上傳成功"), HttpStatus.OK);
	}
}
