package com.xiaoyu.interview.controller;

import cn.hutool.core.io.FileUtil;
import com.xiaoyu.interview.ai.FileAnalyzeModel;
import com.xiaoyu.interview.ai.ResumeAiModel;
import com.xiaoyu.interview.common.BaseResponse;
import com.xiaoyu.interview.common.ErrorCode;
import com.xiaoyu.interview.common.ResultUtils;
import com.xiaoyu.interview.constant.FileConstant;
import com.xiaoyu.interview.exception.BusinessException;
import com.xiaoyu.interview.manager.CosManager;
import com.xiaoyu.interview.model.entity.ResumeDocument;
import com.xiaoyu.interview.model.entity.User;
import com.xiaoyu.interview.model.enums.FileUploadBizEnum;
import com.xiaoyu.interview.service.UserService;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

import com.xiaoyu.interview.utils.RedisUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;

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

    @Resource
    private ResumeAiModel resumeAiModel;

    @Resource
    private RedisUtil redisUtil;

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
        String uuid = UUID.randomUUID().toString();
        String filename = uuid + "-" + multipartFile.getOriginalFilename();
        String filepath = String.format("/%s/%s/%s", fileUploadBizEnum.getValue(), loginUser.getId(), filename);
        File file = null;
        try {

            // === PDF 转图片 ===
            PDDocument document = PDDocument.load(multipartFile.getInputStream());
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            File dir = new File("src/main/resources/resumeImage");
            if (!dir.exists()) dir.mkdirs();

            // 上传文件
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);

            String fileUrl = FileConstant.COS_HOST + filepath;

            // 这里只取第一页，若要支持多页，可循环
            BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 200, ImageType.RGB);
            String fileName = UUID.randomUUID().toString();
            File imageFile = new File(dir, fileName + ".png");
            ImageIO.write(bim, "png", imageFile);
            document.close();

            // === 异步调用 AI 解析简历 ===
            resumeAiModel.analyzeResume(imageFile.getAbsolutePath(),loginUser.getId());

            // 返回可访问地址
            return ResultUtils.success(fileUrl);
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
     * 查看redis简历信息
     */
    @GetMapping("/resume/get")
    public BaseResponse<ResumeDocument> resumeGet(HttpServletRequest request) {
        if (request == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ResumeDocument resumeDocument = userService.getCurrentUserResume(request);
        return ResultUtils.success(resumeDocument);
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

    @Deprecated
    @PostMapping("/resume/analyze/upload")
    public BaseResponse<ResumeDocument> resumeUploadFileAnalyzeLocal(
            @RequestPart("file") MultipartFile multipartFile,
            @RequestParam(name = "biz", required = false, defaultValue = "resume") String biz,
            HttpServletRequest request) {

        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);
        if (fileUploadBizEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validFile(multipartFile, fileUploadBizEnum);
        User loginUser = userService.getLoginUser(request);

        ResumeDocument resumeDocument = null;
        try {
            String filename = multipartFile.getOriginalFilename();
            if (filename.endsWith(".pdf")) {
                // === PDF 转图片 ===
                PDDocument document = PDDocument.load(multipartFile.getInputStream());
                PDFRenderer pdfRenderer = new PDFRenderer(document);

                File dir = new File("src/main/resources/resumeImage");
                if (!dir.exists()) dir.mkdirs();

                // 这里只取第一页，若要支持多页，可循环
                BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 200, ImageType.RGB);
                String uuid = UUID.randomUUID().toString();
                File imageFile = new File(dir, uuid + ".png");
                ImageIO.write(bim, "png", imageFile);
                document.close();

                // === 调用 AI 解析简历 ===
                // resumeDocument = resumeAiClient.analyzeResume(imageFile.getAbsolutePath());

            }
        } catch (Exception e) {
            log.error("解析简历失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "简历解析失败: " + e.getMessage());
        }

        return ResultUtils.success(resumeDocument);
    }

}
