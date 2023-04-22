package com.cqs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cqs.entity.FileResource;
import com.cqs.response.RestApiResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
* @author cqs
* @description 针对表【file】的数据库操作Service
* @createDate 2023-04-17 18:06:10
*/
public interface FileService extends IService<FileResource> {
    RestApiResponse<Object> dowload(String identifier, HttpServletResponse response) throws IOException;
}
