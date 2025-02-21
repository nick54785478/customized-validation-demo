package com.example.demo.exception.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回傳訊息定義
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseExceptionsResponse {

	private String code;

	private List<String> messages;

}
