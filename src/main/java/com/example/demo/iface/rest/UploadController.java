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

import com.example.demo.application.annotation.CustomValidate;
import com.example.demo.application.exception.ValidationException;
import com.example.demo.application.service.UploadCommandService;
import com.example.demo.application.shared.command.UploadTemplateCommand;
import com.example.demo.iface.assembler.UploadResourceAssembler;
import com.example.demo.iface.resource.in.UploadTemplateResource;
import com.example.demo.iface.resource.out.BaseExceptionsResponse;
import com.example.demo.iface.resource.out.TemplateUploadedResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

/**
 * 範本上傳表現層控制器 (Upload REST Controller - Inbound Adapter)
 * <p>
 * 遵循六角形架構規範，作為 HTTP REST API 的輸入適配器。
 * 負責接收 {@link UploadTemplateResource} 與上傳的 {@link MultipartFile}，
 * 委託 {@link UploadResourceAssembler} 進行防腐資料轉換後，調用應用層服務 {@link UploadCommandService}。
 * </p>
 */
@Tag(name = "範本上傳檢核 API", description = "提供 Excel 範本檔案上傳、雙層防腐正規化轉換與 SpEL 客製檢核執行端點")
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
	@Operation(
			summary = "上傳並校驗 Excel 範本檔案",
			description = "接收範本中繼資訊與 Excel 二進位檔案，進行雙層防腐正規化（工作表與欄位對映），並調用客製驗證引擎執行 SpEL 規則校驗。"
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "校驗通過並上傳成功",
					content = @Content(schema = @Schema(implementation = TemplateUploadedResource.class))
			),
			@ApiResponse(
					responseCode = "400",
					description = "參數檢核失敗或缺少必備欄位",
					content = @Content(schema = @Schema(implementation = BaseExceptionsResponse.class))
			),
			@ApiResponse(
					responseCode = "422",
					description = "Excel 內容商業規則檢核未通過 (業務邏輯檢核錯誤清單)",
					content = @Content(schema = @Schema(implementation = BaseExceptionsResponse.class))
			)
	})
	@CustomValidate(templateName = "#resource.name")
	@PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<TemplateUploadedResource> upload(
			@Parameter(
					description = "範本上傳請求中繼資訊 (JSON 格式)",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = UploadTemplateResource.class),
							examples = @ExampleObject(
									name = "PR_IMPORT",
									summary = "請購單批次匯入範本中繼資訊範例",
									value = """
											{
											  "name": "PR_IMPORT",
											  "type": "PR",
											  "fileType": "xlsx",
											  "filePath": "/TEMPLATE/PR_IMPORT",
											  "fileName": "PR_IMPORT.xlsx"
											}
											"""
							)
					)
			)
			@RequestPart(name = "resource", required = true) @Valid UploadTemplateResource resource,
			@Parameter(
					description = "Excel 試算表檔案二進位串流 (.xlsx)",
					content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
			)
			@RequestPart(name = "file", required = true) MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new ValidationException("PARAM_INVALID", "上傳之 Excel 檔案不可為空");
		}
		UploadTemplateCommand command = uploadResourceAssembler.toCommand(resource);
		uploadCommandService.upload(command, file.getInputStream());
		return new ResponseEntity<>(new TemplateUploadedResource("200", "上傳成功"), HttpStatus.OK);
	}
}
