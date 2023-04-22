package com.cqs.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value ="file")
public class FileResource {
    @TableId
    private Integer id;
    /** 文件名 */
    private String fileName;
    /** 文件后缀 */
    private String suffixName;
    /** 文件大小 */
    private Long size;
    /** MD5 */
    private String identifier;
}
