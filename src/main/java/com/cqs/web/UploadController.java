package com.cqs.web;

import com.cqs.dto.FileDTO;
import com.cqs.dto.FileResultDTO;
import com.cqs.response.RestApiResponse;
import com.cqs.response.error.BusinessException;
import com.cqs.service.IUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class UploadController {

    private final IUploadService uploadService;

    @Autowired
    public UploadController(IUploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * 检查分片是否存在
     * @return RestApiResponse
     */
    @GetMapping("/upload")
    public RestApiResponse<Object> checkChunkExist(FileDTO file, HttpServletResponse response){
        FileResultDTO fileResultDTO;
        try {
            fileResultDTO = uploadService.checkChunkExist(file);
            return RestApiResponse.success(fileResultDTO);
        } catch (Exception e) {
            Map<String, Object> error = getErrorMap(response, e);
            log.error("check chunk exist error :{}", e.getMessage());
            return RestApiResponse.error(error);
        }
    }

    /**
     * 上传文件分片
     * @param file
     * @return RestApiResponse
     */
    @PostMapping("/upload")
    public RestApiResponse<Object> upload(FileDTO file, HttpServletResponse response){
        try {
            uploadService.upload(file);
            return RestApiResponse.success(file.getIdentifier());
        } catch (BusinessException e) {
            Map<String, Object> error = getErrorMap(response, e);
            log.error("upload chunk error :{}", e.getMessage());
            return RestApiResponse.error(error);
        }
    }

    /**
     * 设置上传错误响应
     * @param response
     * @param e
     * @return
     */
    private Map<String, Object> getErrorMap(HttpServletResponse response, Exception e) {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        Map<String, Object> error = new HashMap<>();
        error.put("errorMsg", e.getMessage());
        error.put("errorCode", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return error;
    }
}
