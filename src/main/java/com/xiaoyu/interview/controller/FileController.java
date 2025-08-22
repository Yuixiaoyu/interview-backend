package com.xiaoyu.interview.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.xiaoyu.interview.ai.FileAnalyzeModel;
import com.xiaoyu.interview.common.BaseResponse;
import com.xiaoyu.interview.common.ErrorCode;
import com.xiaoyu.interview.common.ResultUtils;
import com.xiaoyu.interview.constant.FileConstant;
import com.xiaoyu.interview.exception.BusinessException;
import com.xiaoyu.interview.manager.CosManager;
import com.xiaoyu.interview.model.entity.User;
import com.xiaoyu.interview.model.entity.aifileresponse.ResumeDocument;
import com.xiaoyu.interview.model.enums.FileUploadBizEnum;
import com.xiaoyu.interview.service.UserService;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xiaoyu.interview.utils.PdfUtils;
import com.xiaoyu.interview.utils.ResumeParser;
import com.xiaoyu.interview.utils.WordUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    @Resource
    private FileAnalyzeModel fileAnalyzeModel;

    /**
     * 文件上传
     *
     * @param multipartFile
     * @param biz
     * @param request
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile,
                                           @RequestParam(name = "biz", required = false, defaultValue = "resume") String biz,
                                           HttpServletRequest request) {
        //String biz = uploadFileRequest.getBiz();
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);
        if (fileUploadBizEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validFile(multipartFile, fileUploadBizEnum);
        User loginUser = userService.getLoginUser(request);
        // 文件目录：根据业务、用户来划分
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String filename = uuid + "-" + multipartFile.getOriginalFilename();
        String filepath = String.format("/%s/%s/%s", fileUploadBizEnum.getValue(), loginUser.getId(), filename);
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            // 返回可访问地址
            return ResultUtils.success(FileConstant.COS_HOST + filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 校验文件
     *
     * @param multipartFile
     * @param fileUploadBizEnum 业务类型
     */
    private void validFile(MultipartFile multipartFile, FileUploadBizEnum fileUploadBizEnum) {
        // 文件大小
        long fileSize = multipartFile.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final long ONE_M = 1024 * 1024L;
        if (FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)) {
            if (fileSize > ONE_M) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 1M");
            }
            if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
        } else if (FileUploadBizEnum.RESUME.equals(fileUploadBizEnum)) {
            if (fileSize > ONE_M * 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 20M");
            }
            if (!Arrays.asList("pdf", "doc", "docx").contains(fileSuffix)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
        }
    }

    @PostMapping("/resume/analyze/upload")
    public BaseResponse<ResumeDocument> resumeUploadFileAnalyzeLocal(@RequestPart("file") MultipartFile multipartFile,
                                           @RequestParam(name = "biz", required = false, defaultValue = "resume") String biz,
                                           HttpServletRequest request) {
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);
        if (fileUploadBizEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validFile(multipartFile, fileUploadBizEnum);
        User loginUser = userService.getLoginUser(request);
        Map<String, Object> result = new HashMap<>();
        try {
            String filename = multipartFile.getOriginalFilename();
            if(filename.endsWith(".pdf")) {
                List<String> paragraphs = PdfUtils.extractParagraphs(multipartFile.getInputStream());
                result = ResumeParser.parseParagraphs(paragraphs);
            } else if(filename.endsWith(".doc") || filename.endsWith(".docx")) {
                List<String> paragraphs = WordUtils.extractParagraphs(multipartFile.getInputStream());
                result = ResumeParser.parseParagraphs(paragraphs);
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        ResumeDocument resumeDocument = fileAnalyzeModel.analyzeResume(JSONUtil.toJsonStr(result), UUID.fastUUID().toString());
        return ResultUtils.success(resumeDocument);
    }
}
