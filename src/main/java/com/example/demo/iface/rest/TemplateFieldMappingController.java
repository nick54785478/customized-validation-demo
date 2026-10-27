package com.example.demo.iface.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.dto.TemplateFieldMappingGottenResult;
import com.example.demo.application.dto.TemplateFieldMappingSearchedResult;
import com.example.demo.application.port.in.CreateTemplateFieldMappingUseCase;
import com.example.demo.application.port.in.DeleteTemplateFieldMappingUseCase;
import com.example.demo.application.port.in.GetTemplateFieldMappingUseCase;
import com.example.demo.application.port.in.SearchTemplateFieldMappingUseCase;
import com.example.demo.application.port.in.UpdateTemplateFieldMappingUseCase;
import com.example.demo.application.shared.command.CreateTemplateFieldMappingCommand;
import com.example.demo.application.shared.command.DeleteTemplateFieldMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateFieldMappingCommand;
import com.example.demo.application.shared.query.GetTemplateFieldMappingQuery;
import com.example.demo.application.shared.query.SearchTemplateFieldMappingQuery;
import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.iface.assembler.TemplateFieldMappingResourceAssembler;
import com.example.demo.iface.resource.in.CreateTemplateFieldMappingResource;
import com.example.demo.iface.resource.in.UpdateTemplateFieldMappingResource;
import com.example.demo.iface.resource.out.TemplateFieldMappingCreatedResource;
import com.example.demo.iface.resource.out.TemplateFieldMappingDeletedResource;
import com.example.demo.iface.resource.out.TemplateFieldMappingRetrievedResource;
import com.example.demo.iface.resource.out.TemplateFieldMappingSearchedResource;
import com.example.demo.iface.resource.out.TemplateFieldMappingUpdatedResource;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 範本欄位對映管理表現層控制器 (Template Field Mapping REST Controller - Inbound Adapter)
 * <p>
 * 遵循六角形架構規範，作為 HTTP REST API 的輸入適配器。
 * 負責接收 HTTP Request，委託 {@link TemplateFieldMappingResourceAssembler} 進行防腐資料轉換後，
 * 調用對應之應用層 Inbound Port (UseCase) 介面，並將結果封裝為標準 HTTP Response 回傳。
 * </p>
 */
@Tag(name = "範本欄位對映維護 API", description = "提供 Level 2 欄位標題層級防腐對映 (Template Field Mapping) 之 CRUD 與查詢端點")
@Slf4j
@Validated
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/template-field-mappings")
public class TemplateFieldMappingController {

	private final CreateTemplateFieldMappingUseCase createTemplateFieldMappingUseCase;
	private final UpdateTemplateFieldMappingUseCase updateTemplateFieldMappingUseCase;
	private final DeleteTemplateFieldMappingUseCase deleteTemplateFieldMappingUseCase;
	private final GetTemplateFieldMappingUseCase getTemplateFieldMappingUseCase;
	private final SearchTemplateFieldMappingUseCase searchTemplateFieldMappingUseCase;
	private final TemplateFieldMappingResourceAssembler templateFieldMappingResourceAssembler;

	/**
	 * 新增範本欄位對映
	 *
	 * @param resource 建立請求資源
	 * @return 建立成功回應資源
	 */
	@PostMapping
	public ResponseEntity<TemplateFieldMappingCreatedResource> create(
			@Valid @RequestBody CreateTemplateFieldMappingResource resource) {
		log.info("REST API 請求新增欄位對映: {}", resource);
		CreateTemplateFieldMappingCommand command = templateFieldMappingResourceAssembler.toCommand(resource);
		TemplateFieldMappingGottenResult result = createTemplateFieldMappingUseCase.create(command);
		return new ResponseEntity<>(templateFieldMappingResourceAssembler.toCreatedResource(result), HttpStatus.CREATED);
	}

	/**
	 * 更新範本欄位對映
	 *
	 * @param id       欄位對映識別碼
	 * @param resource 更新請求資源
	 * @return 更新成功回應資源
	 */
	@PutMapping("/{id}")
	public ResponseEntity<TemplateFieldMappingUpdatedResource> update(
			@PathVariable @Positive(message = "識別碼必須大於 0") Long id,
			@Valid @RequestBody UpdateTemplateFieldMappingResource resource) {
		log.info("REST API 請求更新欄位對映: id={}, resource={}", id, resource);
		UpdateTemplateFieldMappingCommand command = templateFieldMappingResourceAssembler.toCommand(id, resource);
		TemplateFieldMappingGottenResult result = updateTemplateFieldMappingUseCase.update(command);
		return ResponseEntity.ok(templateFieldMappingResourceAssembler.toUpdatedResource(result));
	}

	/**
	 * 刪除範本欄位對映
	 *
	 * @param id 欄位對映識別碼
	 * @return 刪除成功回應資源
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<TemplateFieldMappingDeletedResource> delete(
			@PathVariable @Positive(message = "識別碼必須大於 0") Long id) {
		log.info("REST API 請求刪除欄位對映: id={}", id);
		DeleteTemplateFieldMappingCommand command = templateFieldMappingResourceAssembler.toDeleteCommand(id);
		deleteTemplateFieldMappingUseCase.delete(command);
		return ResponseEntity.ok(templateFieldMappingResourceAssembler.toDeletedResource(id));
	}

	/**
	 * 依識別碼取得單一欄位對映
	 *
	 * @param id 欄位對映識別碼
	 * @return 單筆查詢回應資源
	 */
	@GetMapping("/{id}")
	public ResponseEntity<TemplateFieldMappingRetrievedResource> get(
			@PathVariable @Positive(message = "識別碼必須大於 0") Long id) {
		log.info("REST API 請求取得單筆欄位對映: id={}", id);
		GetTemplateFieldMappingQuery query = templateFieldMappingResourceAssembler.toGetQuery(id);
		TemplateFieldMappingGottenResult result = getTemplateFieldMappingUseCase.get(query);
		return ResponseEntity.ok(templateFieldMappingResourceAssembler.toRetrievedResource(result));
	}

	/**
	 * 條件搜尋範本欄位對映清單
	 *
	 * @param templateName       範本名稱 (選填)
	 * @param templateSheetName 所屬工作表代碼 (選填)
	 * @param fieldCode          屬性代碼 (選填)
	 * @param displayName        欄位顯示名稱 (選填)
	 * @param activeFlag         啟用狀態 (選填)
	 * @return 搜尋回應清單資源
	 */
	@GetMapping
	public ResponseEntity<TemplateFieldMappingSearchedResource> search(
			@RequestParam(required = false) String templateName,
			@RequestParam(required = false) String templateSheetName,
			@RequestParam(required = false) String fieldCode,
			@RequestParam(required = false) String displayName,
			@RequestParam(required = false) YesNo activeFlag) {
		log.info("REST API 條件搜尋欄位對映: templateName={}, templateSheetName={}, fieldCode={}, displayName={}, activeFlag={}",
				templateName, templateSheetName, fieldCode, displayName, activeFlag);
		SearchTemplateFieldMappingQuery query = templateFieldMappingResourceAssembler
				.toSearchQuery(templateName, templateSheetName, fieldCode, displayName, activeFlag);
		TemplateFieldMappingSearchedResult result = searchTemplateFieldMappingUseCase.search(query);
		return ResponseEntity.ok(templateFieldMappingResourceAssembler.toSearchedResource(result));
	}
}
