package com.example.demo.util;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 資料轉換器，轉換資料用
 */
@Slf4j
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BaseDataTransformer {

	protected static final ModelMapper modelMapper = new ModelMapper();

	static {
		// Simple Date Format
		var simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

		// 設置 LocalDate -> String 的 Converter
		modelMapper.addConverter(new Converter<LocalDate, String>() {
			@Override
			public String convert(MappingContext<LocalDate, String> context) {
				return context.getSource() == null ? null
						: context.getSource().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
			}
		});

		// 設置 String -> LocalDate 的 Converter
		modelMapper.addConverter(new Converter<String, LocalDate>() {
			@Override
			public LocalDate convert(MappingContext<String, LocalDate> context) {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
				return context.getSource() == null ? null : LocalDate.parse(context.getSource(), formatter);
			}
		});

		// 設置 Date -> String 的 Converter
		modelMapper.addConverter(new Converter<Date, String>() {
			@Override
			public String convert(MappingContext<Date, String> context) {
				return context.getSource() == null ? null : simpleDateFormat.format(context.getSource());
			}
		});
		// 設置 String -> Date 的 Converter
		modelMapper.addConverter(new Converter<String, Date>() {
			@Override
			public Date convert(MappingContext<String, Date> context) {
				try {
					return context.getSource() == null ? null : simpleDateFormat.parse(context.getSource());
				} catch (ParseException e) {
					log.error("日期轉換發生錯誤", e);
					return null;
				}
			}
		});
		// 設置 Long -> String
		modelMapper.addConverter(new Converter<Long, String>() {
			@Override
			public String convert(MappingContext<Long, String> context) {
				return context.getSource() == null ? null : context.getSource().toString();
			}
		});
		// 設置 String -> Long
		modelMapper.addConverter(new Converter<String, Long>() {
			@Override
			public Long convert(MappingContext<String, Long> context) {
				try {
					return context.getSource() == null ? null : Long.parseLong(context.getSource());
				} catch (Exception e) {
					log.error("轉換 String 為 Long 失敗", e);
					return null;
				}
			}
		});
		// 設置 Long -> String
		modelMapper.addConverter(new Converter<Long, String>() {
			@Override
			public String convert(MappingContext<Long, String> context) {
				return context.getSource() == null ? null : context.getSource().toString();
			}
		});
		// 設置 String -> BigDecimal
		modelMapper.addConverter(new Converter<String, BigDecimal>() {
			@Override
			public BigDecimal convert(MappingContext<String, BigDecimal> context) {
				try {
					return context.getSource() == null ? null : new BigDecimal(context.getSource());
				} catch (Exception e) {
					log.error("轉換 String 為 BigDecimal 失敗", e);
					return null;
				}
			}
		});

		// 設置 BigDecimal -> String
		modelMapper.addConverter(new Converter<BigDecimal, String>() {
			@Override
			public String convert(MappingContext<BigDecimal, String> context) {
				try {
					return context.getSource() == null ? null : context.getSource().toString();
				} catch (Exception e) {
					log.error("轉換 BigDecimal 為 String 失敗", e);
					return null;
				}
			}
		});

	}

	/**
	 * 資料轉換
	 * 
	 * @param target 目標物件
	 * @param clazz  欲轉換的型別
	 * @return 轉換後的物件
	 */
	public static <T> T transformData(Object target, Class<T> clazz) {
		return modelMapper.map(target, clazz);
	}

	/**
	 * List 資料轉換
	 * 
	 * @param target 目標物件列表
	 * @param clazz  欲轉換的型別
	 * @return 轉換後的物件列表
	 */
	public static <S, T> List<T> transformData(List<S> target, Class<T> clazz) {
		return target.stream().map(e -> transformData(e, clazz)).collect(Collectors.toList());
	}

}
