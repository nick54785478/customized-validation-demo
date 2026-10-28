/**
 * 批次檔案處理適配器套件 (Batch File Processors - Inbound Adapters)
 * <p>
 * 位於六角形架構之輸入適配器層 (Inbound Adapters)，與 REST Controller 處於對等地位。
 * 負責處理非 HTTP 協定來源之批次 Excel 檔案或二進位串流 (如 SFTP 下載檔案、MinIO / S3 物件儲存串流、訊息佇列等)。
 * 透過通用 {@link com.example.demo.iface.schedule.processor.BatchFileProcessor} 介面宣告之
 * {@link com.example.demo.application.annotation.CustomValidate}，在進入核心應用層前完成邊界輸入防腐驗證。
 * </p>
 */
package com.example.demo.iface.schedule;
