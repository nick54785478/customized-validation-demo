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
import com.example.demo.iface.assembler.UploadResourceAssembler;
import com.example.demo.iface.resource.in.UploadTemplateResource;
import com.example.demo.iface.resource.out.TemplateUploadedResource;
import com.example.demo.application.shared.command.UploadTemplateCommand;

import lombok.AllArgsConstructor;

/**
 * 範本上傳表現層控制器 (Upload REST Controller - Inbound Adapter)
 * <p>
 * 遵循六角形架構規範，作為 HTTP REST API 的輸入適配器。
 * 負責接收 {@link UploadTemplateResource} 與上傳的 {@link MultipartFile}，
 * 委託 {@link UploadResourceAssembler} 進行防腐資料轉換後，調用應用層服務 {@link UploadCommandService}。
 * </p>
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/upload")
public class UploadController {

	private final UploadCommandService uploadCommandService;
	private final UploadResourceAssembler uploadResourceAssembler;

	/**
	 * 上傳並校驗 Excel 範本檔案
	 *
	 * @param resource 範本上傳請求資源 (JSON)
	 * @param file     Excel 範本二進位檔案
	 * @return 上傳結果狀態資源
	 * @throws IOException 檔案傳輸異常
	 */
	@PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<TemplateUploadedResource> upload(
			@RequestPart(name = "resource", required = true) UploadTemplateResource resource,
			@RequestPart(name = "file", required = true) MultipartFile file) throws IOException {
		UploadTemplateCommand command = uploadResourceAssembler.toCommand(resource);
		uploadCommandService.upload(command, file);
		return new ResponseEntity<>(new TemplateUploadedResource("200", "上傳成功"), HttpStatus.OK);
	}
}
