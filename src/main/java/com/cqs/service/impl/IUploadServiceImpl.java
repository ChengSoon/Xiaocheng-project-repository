package com.cqs.service.impl;

import com.cqs.dto.FileDTO;
import com.cqs.dto.FileResultDTO;
import com.cqs.response.error.BusinessErrorCode;
import com.cqs.response.error.BusinessException;
import com.cqs.service.IUploadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@SuppressWarnings("unchecked")
public class IUploadServiceImpl implements IUploadService {

    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public IUploadServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 文件存储位置
     */
    @Value("${file.url}")
    private String uploadFolder;

    /**
     * 上传分片
     *
     * @param file
     */
    @Override
    @SuppressWarnings("unchecked")
    public void upload(FileDTO file) throws BusinessException {
        String chunkFileFolderPath = getChunkFileFolderPath(file.getIdentifier());
        File chunkFileFolder = new File(chunkFileFolderPath);
        //判断文件是否存在，不存在则创建
        if (!chunkFileFolder.exists()) {
            boolean mkdirs = chunkFileFolder.mkdirs();
            log.info("创建分片文件夹:{}", mkdirs);
        }
        // 写入分片
        try (
                InputStream inputStream = file.getFile().getInputStream();
                FileOutputStream outputStream =
                        new FileOutputStream(new File(chunkFileFolderPath + file.getChunkNumber()));
        ) {
            IOUtils.copy(inputStream, outputStream);
            log.info("文件标识:{},chunkNumber:{}", file.getIdentifier(), file.getChunkNumber());
            //将该分片写入redis
            long size = saveToRedis(file);
            //合并分片
            if (size == file.getTotalChunks()) {
                File mergeFile = mergeChunks(file.getIdentifier(), file.getFilename());
                if (ObjectUtils.isEmpty(mergeFile)) {
                    throw new BusinessException(BusinessErrorCode.INVALID_PARAMETER, "合并文件失败");
                } else {

                }
            }
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAMETER, e.getMessage());
        }
    }

    /**
     * 检查文件是否存在，如果存在则跳过该文件的上传，如果不存在，返回需要上传的分片集合
     *
     * @param file
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public FileResultDTO checkChunkExist(FileDTO file) throws BusinessException {
        //1.检查文件是否已上传过
        //1.1)检查在磁盘中是否存在
        String fileFolderPath = getFileFolderPath(file.getIdentifier());
        String filePath = getFilePath(file.getIdentifier(), file.getFilename());
        File p_file = new File(filePath);
        boolean exists = p_file.exists(); //判断文件是否存在
        //1.2)检查Redis中是否存在,并且所有分片已经上传完成。
        Set<Integer> uploaded = (Set<Integer>) redisTemplate.opsForHash().get(file.getIdentifier(), "uploaded");
        if (!ObjectUtils.isEmpty(uploaded) && uploaded.size() == file.getTotalChunks() && exists) {
            return new FileResultDTO(true);
        }
        File fileFolder = new File(fileFolderPath);
        if (!fileFolder.exists()) {
            boolean mkdirs = fileFolder.mkdirs();
            log.info("准备工作,创建文件夹,fileFolderPath:{},mkdirs:{}", fileFolderPath, mkdirs);
        }
        return new FileResultDTO(false, uploaded);
    }

    private File mergeChunks(String identifier, String filename) throws BusinessException {
        String chunkFileFolderPath = getChunkFileFolderPath(identifier); //获取文件所属目录
        String filePath = getFilePath(identifier, filename); // 绝对路径
        File chunkFileFolder = new File(chunkFileFolderPath);
        File mergeFile = new File(filePath);
        File[] chunks = chunkFileFolder.listFiles();
        //排序
        List<File> collect = Arrays.stream(chunks).sorted(Comparator.comparing(o -> Integer.valueOf(o.getName()))).collect(Collectors.toList());
        try {
            RandomAccessFile randomAccessFileWriter = new RandomAccessFile(mergeFile, "rw");
            byte[] bytes = new byte[1024];
            collect.forEach(chunk -> {
                RandomAccessFile randomAccessFileReader = null;
                try {
                    randomAccessFileReader = new RandomAccessFile(chunk, "r");
                    int len;
                    while ((len = randomAccessFileReader.read(bytes)) != -1) {
                        randomAccessFileWriter.write(bytes, 0, len);
                    }
                    randomAccessFileReader.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            });
            randomAccessFileWriter.close();
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAMETER);
        }
        return mergeFile;
    }

    /**
     * 分片写入Redis
     *
     * @param file
     */
    public synchronized long saveToRedis(FileDTO file) {
        Set<Integer> uploaded
                = (Set<Integer>) redisTemplate.opsForHash().get(file.getIdentifier(), "uploaded");
        // 判断文件是否在redis中上传
        if (ObjectUtils.isEmpty(uploaded)) {
            uploaded = new HashSet<>(Arrays.asList(file.getChunkNumber()));
            HashMap<String, Object> objectObjectHashMap = new HashMap<>();
            objectObjectHashMap.put("uploaded", uploaded);
            objectObjectHashMap.put("totalChunks", file.getTotalChunks());
            objectObjectHashMap.put("totalSize", file.getTotalSize());
            objectObjectHashMap.put("path", getFileRelativelyPath(file.getIdentifier(), file.getFilename()));
            redisTemplate.opsForHash().putAll(file.getIdentifier(), objectObjectHashMap);
        } else {
            uploaded.add(file.getChunkNumber());
            redisTemplate.opsForHash().put(file.getIdentifier(), "uploaded", uploaded);
        }
        return uploaded.size();
    }

    /**
     * 得到分块文件所属的目录
     *
     * @param identifier
     * @return
     */
    public String getChunkFileFolderPath(String identifier) {
        return getFileFolderPath(identifier) + "chunks" + File.separator;
    }

    /**
     * 得到文件的相对路径
     * @param identifier
     * @param filename
     * @return
     */
    private String getFileRelativelyPath(String identifier, String filename) {
        String ext = filename.substring(filename.lastIndexOf("."));
        return "/" + identifier.substring(0, 1) + "/" +
                identifier.substring(1, 2) + "/" +
                identifier + "/" + identifier
                + ext;
    }

    /**
     * 得到文件的绝对路径
     *
     * @param identifier
     * @param filename
     * @return
     */
    public String getFilePath(String identifier, String filename) {
        String ext = filename.substring(filename.lastIndexOf(".")); //获取文件名
        return getFileFolderPath(identifier) + identifier + ext;
    }

    /**
     * 得到文件所属的目录
     *
     * @param identifier
     * @return
     */
    public String getFileFolderPath(String identifier) {
        return uploadFolder + File.separator + identifier + File.separator;
    }

}
