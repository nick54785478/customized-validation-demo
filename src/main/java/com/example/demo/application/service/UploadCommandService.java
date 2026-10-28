package com.example.demo.application.service;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.shared.command.UploadTemplateCommand;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 範本上傳命令服務 (Upload Command Service - Application Service)
 * <p>
 * 位於應用層服務，負責執行範本上傳的業務流程與交易控制。
 * 遵循 Clean Architecture 規範，僅接收純業務 Command 與 JRE 標準 InputStream，完全解耦 Web 技術類別。
 * </p>
 */
@Slf4j
@Service
@AllArgsConstructor
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT, timeout = 36000, rollbackFor = Exception.class)
public class UploadCommandService {

	/**
	 * 範本上傳與業務處理
	 * 
	 * @param command    範本上傳命令
	 * @param fileStream 二進位檔案輸入串流
	 * @throws IOException 檔案讀取異常
	 */
	public void upload(UploadTemplateCommand command, InputStream fileStream) throws IOException {

		log.debug("command:{}, fileStream:{}", command, fileStream);

		// 後續上傳與業務入庫功能
	}
}
