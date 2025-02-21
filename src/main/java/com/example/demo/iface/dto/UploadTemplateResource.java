package com.example.demo.iface.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadTemplateResource {

	private String name; 

	private String type; // 範本種類

	private String fileType; // 檔案種類

	private String filePath; // 檔案路徑

	private String fileName; // 檔案名稱

}
