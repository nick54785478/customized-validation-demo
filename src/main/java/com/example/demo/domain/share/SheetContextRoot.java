package com.example.demo.domain.share;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetContextRoot {

	private List<Map<String, String>> sheet; // Sheet 資料( Excel 轉換的 List<Map>)
	
	private Map<String, Object> params; // ParamMap 參數設置


}
