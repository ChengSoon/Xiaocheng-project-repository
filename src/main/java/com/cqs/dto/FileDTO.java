package com.cqs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileDTO {
    /**
     * 文件md5
     */
    private String identifier;
    /**
     * 分块文件
     */
    MultipartFile file;
    /**
     * 当前分块序号
     */
    private Integer chunkNumber;
    /**
     * 分块大小
     */
    private Integer chunkSize;
    /**
     * 当前分块大小
     */
    private Integer currentChunkSize;
    /**
     * 文件总大小
     */
    private Integer totalSize;
    /**
     * 分块总数
     */
    private Integer totalChunks;
    /**
     * 文件名
     */
    private String filename;

}
