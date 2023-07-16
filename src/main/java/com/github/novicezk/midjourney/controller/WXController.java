package com.github.novicezk.midjourney.controller;

import cn.hutool.json.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.novicezk.midjourney.dto.SubmitImagineDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import java.net.URLConnection;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Api(tags = "微信任务")
@RestController
@RequestMapping("/wx")
@RequiredArgsConstructor
public class WXController {
    private final ObjectMapper MAPPER = new ObjectMapper();

    @Data
    public static class WxImageDTO {
        private String imageUrl;
        private String accessToken;
    }

    @Builder
    @Data
    public static class WxImageVO {
        private String mediaId;
        private String errmsg;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String media_id;
        private String errmsg;
    }

    @ApiOperation(value = "提交Imagine任务")
    @PostMapping("/image")
    public WxImageVO transferImage(@RequestBody WxImageDTO imagineDTO) {
        String imageUrl = imagineDTO.getImageUrl();
        String targetPath = "temp/image.jpg";
        try {
            URL url = new URL(imageUrl);
            URLConnection conn = url.openConnection();
            // Set connect timeout to 10 seconds
            conn.setConnectTimeout(30000);
            // Set read timeout to 10 seconds
            conn.setReadTimeout(30000);
            Path outputPath = Path.of(targetPath);

            // Check if the directory exists and create it if it doesn't
            if (!Files.exists(outputPath.getParent())) {
                Files.createDirectories(outputPath.getParent());
            }

            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Image downloaded successfully!");
            }
        } catch (IOException e) {
            System.out.println("download failed: " + e.getMessage());
            return WxImageVO.builder().errmsg("download failed").build();
        }

        // Replace with your access token and type
        String uploadUrl = String.format("https://api.weixin.qq.com/cgi-bin/media/upload?access_token=%s&type=image", imagineDTO.getAccessToken());
        File file = new File(targetPath);

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httppost = new HttpPost(uploadUrl);

            HttpEntity reqEntity = MultipartEntityBuilder.create()
                    .addBinaryBody("media", file, ContentType.APPLICATION_OCTET_STREAM, "image.jpg")
                    .build();

            httppost.setEntity(reqEntity);

            try (CloseableHttpResponse response = httpclient.execute(httppost)) {
                String resultString = EntityUtils.toString(response.getEntity());
                Result result = MAPPER.readValue(resultString, Result.class);
                System.out.println("Image uploaded successfully!" + resultString);
                if (result.getMedia_id() != null) {
                    return WxImageVO.builder().mediaId(result.getMedia_id()).build();
                } else {
                    return WxImageVO.builder().errmsg(result.getErrmsg()).build();
                }
            }
        } catch (IOException e) {
            System.out.println("Error while uploading image: " + e.getMessage());
        }
        return WxImageVO.builder().mediaId("upload failed").build();
    }
}
