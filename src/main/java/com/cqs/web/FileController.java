package com.cqs.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqs.dto.FileDTO;
import com.cqs.entity.FileResource;
import com.cqs.response.RestApiResponse;
import com.cqs.service.FileService;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/file")
@CrossOrigin
public class FileController {

    @Resource
    private FileService fileService;

    @PostMapping
    public RestApiResponse<Object> file(@RequestBody FileDTO file, HttpServletResponse response){
        String suffix = file.getFilename().substring(file.getFilename().lastIndexOf("."));
        FileResource fileResource = new FileResource(0, file.getFilename(),
                                                    suffix,
                                                    file.getTotalSize().longValue(),
                                                    file.getIdentifier());
        LambdaQueryWrapper<FileResource> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(FileResource::getIdentifier, file.getIdentifier());
        FileResource one = fileService.getOne(lambdaQueryWrapper);
        if (!ObjectUtils.isEmpty(one)){
            return RestApiResponse.success(fileService.update(fileResource, lambdaQueryWrapper));
        }
        return RestApiResponse.success(fileService.save(fileResource));
    }


    @GetMapping
    public RestApiResponse<Object> file(){
        return RestApiResponse.success(fileService.list());
    }
    
    @GetMapping("/dowload/{identifier}")
    public RestApiResponse<Object> dowload(@PathVariable String identifier, HttpServletResponse response) throws IOException {
        fileService.dowload(identifier, response);
        return null;
    }
}
