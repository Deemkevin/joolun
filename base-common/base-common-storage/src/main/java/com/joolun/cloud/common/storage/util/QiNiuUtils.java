package com.joolun.cloud.common.storage.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.joolun.cloud.common.storage.entity.StorageConfig;
import com.qiniu.storage.Region;
import lombok.AllArgsConstructor;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import org.apache.commons.lang.RandomStringUtils;
import java.io.File;
import java.util.UUID;

/**
 * 七牛云
 * 各操作调用
 * @JL
 */
@AllArgsConstructor
public class QiNiuUtils {
	private final StorageConfig storageConfig;

	/**
	 * 上传文件
	 * @param file
	 * @param dir 用户上传文件时指定的文件夹。
	 */
	public String uploadFile(File file,String dir) throws QiniuException {
		//构造一个带指定 Region 对象的配置类
		Configuration cfg = new Configuration(Region.autoRegion());
		//...其他参数参考类注释
		UploadManager uploadManager = new UploadManager(cfg);
		//...生成上传凭证，然后准备上传
		String accessKey = storageConfig.getAccessKeyId();
		String secretKey = storageConfig.getAccessKeySecret();
		String bucket = storageConfig.getBucket();
		//默认不指定key的情况下，以文件内容的hash值作为文件名
		String fileName = file.getName();
		String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
		String key = preHandle(UUID.randomUUID()+ "." + suffix, dir);
		Auth auth = Auth.create(accessKey, secretKey);
		String upToken = auth.uploadToken(bucket);
		Response response = uploadManager.put(file, key, upToken);
		String resultStr = getUrlPath(response);
		return resultStr;
	}

	private String preHandle(String fileName, String dir) {
		if (StrUtil.isNotBlank(dir) && !dir.startsWith("/")) {
			dir = "/" + dir;
		}
		String name = StrUtil.isBlank(fileName) ? RandomStringUtils.randomAlphanumeric(32) : fileName;
		if (StrUtil.isBlank(dir)) {
			return name;
		}
		String prefix = dir.replaceFirst("/", "");
		return (prefix.endsWith("/") ? prefix : prefix.concat("/")).concat(name);
	}

	private String getUrlPath(Response response) throws QiniuException {
		DefaultPutRet putRet = JSONUtil.toBean(response.bodyString(), DefaultPutRet.class);
		String key = putRet.key;
		return "http://" + storageConfig.getEndpoint() + (key.startsWith("/") ? key : "/" + key);
	}

}
