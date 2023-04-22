package com.cqs.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqs.entity.FileResource;
import com.cqs.mapper.FileMapper;
import com.cqs.response.RestApiResponse;
import com.cqs.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

/**
* @author cqs
* @description 针对表【file】的数据库操作Service实现
* @createDate 2023-04-17 18:06:10
*/
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, FileResource>
    implements FileService{

    @Resource
    private FileMapper fileMapper;

    @Value("${file.url}")
    private String url;

    @Override
    public RestApiResponse<Object> dowload(String identifier, HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<FileResource> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(FileResource::getIdentifier, identifier);
        FileResource fileResource = fileMapper.selectOne(lambdaQueryWrapper);
        if (ObjectUtils.isEmpty(fileResource)){
            throw new RuntimeException("文件已被删除或不存在");
        }
        String fileName = fileResource.getIdentifier()+fileResource.getSuffixName();
        String urlPath = url+File.separator+fileResource.getIdentifier()+File.separator+fileName;
        System.out.println(urlPath);
        File file = new File(urlPath);
        if (!file.exists()){
            throw new RuntimeException("文件已被删除或不存在");
        }
        response.setCharacterEncoding("utf-8");
        response.setContentLength((int) file.length());
        response.addHeader("Content-Disposition",
                "attachment;filename="+ URLEncoder.encode(fileResource.getFileName(), "UTF-8"));
        // 任意类型的二进制流
        response.setContentType("application/x-download;charset=utf-8");
        //读取文件字节流
        byte[] readBytes = FileUtil.readBytes(file);
        OutputStream outputStream = response.getOutputStream();
        outputStream.write(readBytes);
        outputStream.flush();
        outputStream.close();
        return null;
    }
}




