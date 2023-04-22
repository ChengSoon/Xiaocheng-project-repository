package com.cqs.service;

import com.cqs.dto.FileDTO;
import com.cqs.dto.FileResultDTO;
import com.cqs.response.error.BusinessException;

public interface IUploadService {

    /**
     * 上传文件分片
     * @param file
     */
    void upload(FileDTO file) throws BusinessException;

    FileResultDTO checkChunkExist(FileDTO file) throws BusinessException;
}
