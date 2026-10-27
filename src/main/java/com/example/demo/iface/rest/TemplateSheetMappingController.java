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

import com.example.demo.application.dto.TemplateSheetMappingGottenResult;
import com.example.demo.application.dto.TemplateSheetMappingSearchedResult;
import com.example.demo.application.port.in.CreateTemplateSheetMappingUseCase;
import com.example.demo.application.port.in.DeleteTemplateSheetMappingUseCase;
import com.example.demo.application.port.in.GetTemplateSheetMappingUseCase;
import com.example.demo.application.port.in.SearchTemplateSheetMappingUseCase;
import com.example.demo.application.port.in.UpdateTemplateSheetMappingUseCase;
import com.example.demo.application.shared.command.CreateTemplateSheetMappingCommand;
import com.example.demo.application.shared.command.DeleteTemplateSheetMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateSheetMappingCommand;
import com.example.demo.application.shared.query.GetTemplateSheetMappingQuery;
import com.example.demo.application.shared.query.SearchTemplateSheetMappingQuery;
import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.iface.assembler.TemplateSheetMappingResourceAssembler;
import com.example.demo.iface.resource.in.CreateTemplateSheetMappingResource;
import com.example.demo.iface.resource.in.UpdateTemplateSheetMappingResource;
import com.example.demo.iface.resource.out.TemplateSheetMappingCreatedResource;
import com.example.demo.iface.resource.out.TemplateSheetMappingDeletedResource;
import com.example.demo.iface.resource.out.TemplateSheetMappingRetrievedResource;
import com.example.demo.iface.resource.out.TemplateSheetMappingSearchedResource;
import com.example.demo.iface.resource.out.TemplateSheetMappingUpdatedResource;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 範本工作表對映管理表現層控制器 (Template Sheet Mapping REST Controller - Inbound Adapter)
 * <p>
 * 遵循六角形架構規範，作為 HTTP REST API 的輸入適配器。
 * 負責接收 HTTP Request，委託 {@link TemplateSheetMappingResourceAssembler} 進行防腐資料轉換後，
 * 調用對應之應用層 Inbound Port (UseCase) 介面，並將結果封裝為標準 HTTP Response 回傳。
 * </p>
 */
@Tag(name = "範本工作表對映維護 API", description = "提供 Level 1 工作表層級防腐對映 (Template Sheet Mapping) 之 CRUD 與查詢端點")
@Slf4j
@Validated
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/template-sheet-mappings")
public class TemplateSheetMappingController {

	private final CreateTemplateSheetMappingUseCase createTemplateSheetMappingUseCase;
	private final UpdateTemplateSheetMappingUseCase updateTemplateSheetMappingUseCase;
	private final DeleteTemplateSheetMappingUseCase deleteTemplateSheetMappingUseCase;
	private final GetTemplateSheetMappingUseCase getTemplateSheetMappingUseCase;
	private final SearchTemplateSheetMappingUseCase searchTemplateSheetMappingUseCase;
	private final TemplateSheetMappingResourceAssembler templateSheetMappingResourceAssembler;

	/**
	 * 新增範本工作表對映
	 *
	 * @param resource 建立請求資源
	 * @return 建立成功回應資源
	 */
	@PostMapping
	public ResponseEntity<TemplateSheetMappingCreatedResource> create(
			@Valid @RequestBody CreateTemplateSheetMappingResource resource) {
		log.info("REST API 請求新增工作表對映: {}", resource);
		CreateTemplateSheetMappingCommand command = templateSheetMappingResourceAssembler.toCommand(resource);
		TemplateSheetMappingGottenResult result = createTemplateSheetMappingUseCase.create(command);
		return new ResponseEntity<>(templateSheetMappingResourceAssembler.toCreatedResource(result), HttpStatus.CREATED);
	}

	/**
	 * 更新範本工作表對映
	 *
	 * @param id       工作表對映識別碼
	 * @param resource 更新請求資源
	 * @return 更新成功回應資源
	 */
	@PutMapping("/{id}")
	public ResponseEntity<TemplateSheetMappingUpdatedResource> update(
			@PathVariable @Positive(message = "識別碼必須大於 0") Long id,
			@Valid @RequestBody UpdateTemplateSheetMappingResource resource) {
		log.info("REST API 請求更新工作表對映: id={}, resource={}", id, resource);
		UpdateTemplateSheetMappingCommand command = templateSheetMappingResourceAssembler.toCommand(id, resource);
		TemplateSheetMappingGottenResult result = updateTemplateSheetMappingUseCase.update(command);
		return ResponseEntity.ok(templateSheetMappingResourceAssembler.toUpdatedResource(result));
	}

	/**
	 * 刪除範本工作表對映
	 *
	 * @param id 工作表對映識別碼
	 * @return 刪除成功回應資源
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<TemplateSheetMappingDeletedResource> delete(
			@PathVariable @Positive(message = "識別碼必須大於 0") Long id) {
		log.info("REST API 請求刪除工作表對映: id={}", id);
		DeleteTemplateSheetMappingCommand command = templateSheetMappingResourceAssembler.toDeleteCommand(id);
		deleteTemplateSheetMappingUseCase.delete(command);
		return ResponseEntity.ok(templateSheetMappingResourceAssembler.toDeletedResource(id));
	}

	/**
	 * 依識別碼取得單一工作表對映
	 *
	 * @param id 工作表對映識別碼
	 * @return 單筆查詢回應資源
	 */
	@GetMapping("/{id}")
	public ResponseEntity<TemplateSheetMappingRetrievedResource> get(
			@PathVariable @Positive(message = "識別碼必須大於 0") Long id) {
		log.info("REST API 請求取得單筆工作表對映: id={}", id);
		GetTemplateSheetMappingQuery query = templateSheetMappingResourceAssembler.toGetQuery(id);
		TemplateSheetMappingGottenResult result = getTemplateSheetMappingUseCase.get(query);
		return ResponseEntity.ok(templateSheetMappingResourceAssembler.toRetrievedResource(result));
	}

	/**
	 * 條件搜尋範本工作表對映清單
	 *
	 * @param templateName 範本名稱 (選填)
	 * @param sheetCode    工作表代碼 (選填)
	 * @param displayName  工作表顯示名稱 (選填)
	 * @param activeFlag   啟用狀態 (選填)
	 * @return 搜尋回應清單資源
	 */
	@GetMapping
	public ResponseEntity<TemplateSheetMappingSearchedResource> search(
			@RequestParam(required = false) String templateName,
			@RequestParam(required = false) String sheetCode,
			@RequestParam(required = false) String displayName,
			@RequestParam(required = false) YesNo activeFlag) {
		log.info("REST API 條件搜尋工作表對映: templateName={}, sheetCode={}, displayName={}, activeFlag={}",
				templateName, sheetCode, displayName, activeFlag);
		SearchTemplateSheetMappingQuery query = templateSheetMappingResourceAssembler
				.toSearchQuery(templateName, sheetCode, displayName, activeFlag);
		TemplateSheetMappingSearchedResult result = searchTemplateSheetMappingUseCase.search(query);
		return ResponseEntity.ok(templateSheetMappingResourceAssembler.toSearchedResource(result));
	}
}
