package com.example.demo.application.service;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.application.shared.command.UploadTemplateCommand;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 範本上傳命令服務 (Upload Command Service - Application Service)
 * <p>
 * 位於應用層服務，負責執行範本上傳的業務流程與交易控制。
 * </p>
 */
@Slf4j
@Service
@AllArgsConstructor
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT, timeout = 36000, rollbackFor = Exception.class)
public class UploadCommandService {

	/**
	 * 上傳功能
	 * 
	 * @param command
	 * @param file
	 */
	public void upload(UploadTemplateCommand command, MultipartFile file) throws IOException {

		log.debug("command:{}, file:{}", command, file);

		// TODO 後續上傳功能
//		System.out.println(vepList);
	}
}
