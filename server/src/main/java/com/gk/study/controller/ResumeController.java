package com.gk.study.controller;

import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.Resume;
import com.gk.study.permission.Access;
import com.gk.study.permission.AccessLevel;
import com.gk.study.service.ResumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/resume")
public class ResumeController {

    private final static Logger logger = LoggerFactory.getLogger(ResumeController.class);

    @Autowired
    ResumeService service;

    @Value("${File.uploadPath}")
    private String uploadPath;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public APIResponse list(){
        List<Resume> list =  service.getResumeList();

        return new APIResponse(ResponeCode.SUCCESS, "查询成功", list);
    }

    @Access(level = AccessLevel.ADMIN)
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @Transactional
    public APIResponse create(Resume resume) throws IOException {
        saveResume(resume);

        service.createResume(resume);
        return new APIResponse(ResponeCode.SUCCESS, "创建成功");
    }

    @Access(level = AccessLevel.ADMIN)
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public APIResponse delete(String ids){
        System.out.println("ids===" + ids);
        // 批量删除
        String[] arr = ids.split(",");
        for (String id : arr) {
            service.deleteResume(id);
        }
        return new APIResponse(ResponeCode.SUCCESS, "删除成功");
    }

    @Access(level = AccessLevel.ADMIN)
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @Transactional
    public APIResponse update(Resume resume) throws IOException {
        saveResume(resume);

        service.updateResume(resume);
        return new APIResponse(ResponeCode.SUCCESS, "更新成功");
    }

    public void saveResume(Resume resume) throws IOException {
        // 处理头像文件
        MultipartFile coverFile = resume.getCoverFile();
        if(coverFile != null && !coverFile.isEmpty()) {
            String oldFileName = coverFile.getOriginalFilename();
            String randomStr = UUID.randomUUID().toString();
            String newFileName = randomStr + oldFileName.substring(oldFileName.lastIndexOf("."));
            
            // 确保目录存在
            String uploadDir = uploadPath + "/resume";
            File destDir = new File(uploadDir);
            if(!destDir.exists()) {
                destDir.mkdirs();
            }
            
            String filePath = uploadDir + "/" + newFileName;
            logger.info("Saving cover to: " + filePath);
            File destFile = new File(filePath);
            coverFile.transferTo(destFile);
            
            resume.setCover(newFileName);
        }

        // 处理简历文件
        MultipartFile file = resume.getRawFile();
        String newFileName = null;
        if(file != null && !file.isEmpty()) {
            // 存文件
            String oldFileName = file.getOriginalFilename();
            String randomStr = UUID.randomUUID().toString();
            newFileName = randomStr + oldFileName.substring(oldFileName.lastIndexOf("."));
            
            // 确保使用正确的路径分隔符
            String uploadDir = uploadPath + "/resume";
            File destDir = new File(uploadDir);
            if(!destDir.exists()) {
                destDir.mkdirs();
            }
            
            String filePath = uploadDir + "/" + newFileName;
            logger.info("Saving file to: " + filePath);
            File destFile = new File(filePath);
            file.transferTo(destFile);
            
            if(!StringUtils.isEmpty(newFileName)) {
                resume.raw = newFileName;
            }
        }
    }

    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public APIResponse detail(String userId){
        Resume resume = service.getDetail(userId);
        if(resume != null && !StringUtils.isEmpty(resume.getRaw())) {
            // 默认使用下载链接
            String fileUrl = "/api/resume/download/" + resume.getRaw();
            
            String actualPath = uploadPath + "/resume/" + resume.getRaw();
            File file = new File(actualPath);
            
            if(file.exists()) {
                resume.setRaw(fileUrl);
            }
        }
        return new APIResponse(ResponeCode.SUCCESS, "查询成功", resume);
    }

    @RequestMapping(value = "/download/{filename:.+}", method = RequestMethod.GET)
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            String filePath = uploadPath + "/resume/" + filename;
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
            } else {
                logger.error("File not found: " + filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error downloading file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // 添加一个直接的文件访问接口
    @RequestMapping(value = "/file/{filename:.+}", method = RequestMethod.GET)
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        try {
            String filePath = uploadPath + "/resume/" + filename;
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                // 根据文件扩展名设置正确的 Content-Type
                String contentType = "application/octet-stream";
                if (filename.toLowerCase().endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (filename.toLowerCase().endsWith(".doc")) {
                    contentType = "application/msword";
                } else if (filename.toLowerCase().endsWith(".docx")) {
                    contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                }
                
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error serving file: " + filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
