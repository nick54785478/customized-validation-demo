package com.example.demo.application.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 宣告式客製化 Excel 驗證註解 (Custom Validation Annotation)
 * <p>
 * 遵循六角形架構與 Clean Architecture 規範，作為跨層通用的宣告式校驗契約。
 * 可標註於表現層控制器 (Inbound Adapter / Controller) 或應用層服務 (Application Service) 方法上，
 * 由切面 {@code CustomValidationAspect} 攔截並提取檔案串流，驅動 {@code ValidateExcelUseCase} 執行雙層防腐正規化與 SpEL 規則校驗。
 * </p>
 *
 * <p><b>核心特性：</b></p>
 * <ul>
 *   <li><b>解耦硬編碼切點：</b>擺脫傳統 execution pointcut 對特定服務與類別名稱的僵固綁定，具備高擴充性 (Open-Closed Principle)。</li>
 *   <li><b>動態與靜態範本名稱：</b>支援靜態字串常數 (如 {@code "PR_IMPORT"}) 與 SpEL 動態表達式 (如 {@code "#resource.name"})。</li>
 *   <li><b>多源檔案自動探測：</b>自動識別方法入參中之 {@code MultipartFile}、{@code InputStream}、{@code byte[]} 或 {@code Resource}。</li>
 *   <li><b>守護分層邊界：</b>在請求進入核心業務邏輯前達成快速失敗 (Fail-Fast)，阻絕技術類物件侵入應用核心。</li>
 * </ul>
 *
 * <p><b>使用範例：</b></p>
 * <pre>{@code
 * // 1. 於 Controller 方法使用 SpEL 動態取值
 * @PostMapping("/upload")
 * @CustomValidate(templateName = "#resource.name")
 * public ResponseEntity<?> upload(@RequestPart UploadTemplateResource resource, @RequestPart MultipartFile file)
 *
 * // 2. 靜態指定範本名稱
 * @PostMapping("/pr-import")
 * @CustomValidate(templateName = "PR_IMPORT")
 * public ResponseEntity<?> importPr(@RequestPart MultipartFile file)
 *
 * // 3. 於 Service 方法使用 Command 物件
 * @CustomValidate(templateName = "#command.name()")
 * public void processImport(UploadTemplateCommand command, InputStream inputStream)
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CustomValidate {

	/**
	 * 範本唯一識別名稱 (Template Name)。
	 * <p>
	 * 支援兩種配置模式：
	 * <ul>
	 *   <li><b>常數字串：</b>直接填寫靜態範本名稱，例如 {@code "PR_IMPORT"}、{@code "USER_PROFILE"}。</li>
	 *   <li><b>SpEL 動態表達式：</b>以 {@code "#"} 開頭，由方法參數中動態取值，例如 {@code "#resource.name"} 或 {@code "#command.name()"}。</li>
	 * </ul>
	 * 若為空字串，切面將自動嘗試由方法參數物件（如含有 {@code name()} 或 {@code getTemplateName()} 之物件）中自動探測。
	 * </p>
	 *
	 * @return 範本名稱或 SpEL 表達式
	 */
	@AliasFor("value")
	String templateName() default "";

	/**
	 * {@link #templateName()} 的別名。
	 *
	 * @return 範本名稱或 SpEL 表達式
	 */
	@AliasFor("templateName")
	String value() default "";

	/**
	 * 指定 Excel 檔案來源之參數名稱或 SpEL 表達式（選填）。
	 * <p>
	 * 預設為空字串，切面將自動掃描參數列表中的 {@link org.springframework.web.multipart.MultipartFile}、
	 * {@link java.io.InputStream}、{@code byte[]} 或 {@link org.springframework.core.io.Resource}。
	 * </p>
	 *
	 * @return 檔案參數名稱或 SpEL 表達式
	 */
	String file() default "";
}
