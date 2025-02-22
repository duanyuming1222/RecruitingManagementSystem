package com.gk.study.controller;

import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.Notice;
import com.gk.study.permission.Access;
import com.gk.study.permission.AccessLevel;
import com.gk.study.service.NoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/notice")
public class NoticeController {

    private final static Logger logger = LoggerFactory.getLogger(NoticeController.class);

    @Autowired
    NoticeService service;

    /**
     * 创建简历投递通知
     */
    public void createResumeNotice(String userId, String companyId, String thingTitle) {
        try {
            Notice notice = new Notice();
            // 查询公司关联的用户ID
            List<String> companyUserIds = service.getCompanyUserIds(companyId);
            for (String companyUserId : companyUserIds) {
                notice.setUserId(companyUserId);
                notice.setTitle("新简历投递通知");
                notice.setContent("您收到了一份新的简历投递,职位:" + thingTitle);
                notice.setCreateTime(String.valueOf(System.currentTimeMillis()));
                service.createNotice(notice);
            }
        } catch (Exception e) {
            logger.error("创建简历投递通知失败", e);
        }
    }

    /**
     * 创建简历状态变更通知
     */
    public void createStatusChangeNotice(String userId, String status, String thingTitle) {
        try {
            Notice notice = new Notice();
            notice.setUserId(userId); // 通知发给用户
            notice.setTitle("简历状态更新通知");
            
            String statusText;
            String content;
            switch(status) {
                case "interviewing":
                    statusText = "已安排面试";
                    content = "您投递的职位\"" + thingTitle + "\"" + statusText;
                    break;
                case "rejected":
                    statusText = "未通过";
                    content = "您投递的职位\"" + thingTitle + "\"" + statusText;
                    break;
                case "hired":
                    statusText = "已录用";
                    content = "恭喜您! 您投递的职位\"" + thingTitle + "\"" + statusText + 
                             "，offer将在3个工作日内发放至您的邮箱，请注意查收。";
                    break;
                default:
                    statusText = "状态已更新";
                    content = "您投递的职位\"" + thingTitle + "\"" + statusText;
            }
            
            notice.setContent(content);
            notice.setCreateTime(String.valueOf(System.currentTimeMillis()));
            service.createNotice(notice);
        } catch (Exception e) {
            logger.error("创建状态变更通知失败", e);
        }
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public APIResponse list(String userId){
        if (userId == null || userId.isEmpty()) {
            return new APIResponse(ResponeCode.FAIL, "参数错误");
        }
        // 获取用户的消息
        List<Notice> list =  service.getNoticeList(userId);
        return new APIResponse(ResponeCode.SUCCESS, "查询成功", list);
    }

    @Access(level = AccessLevel.ADMIN)
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @Transactional
    public APIResponse create(Notice notice) throws IOException {
        service.createNotice(notice);
        return new APIResponse(ResponeCode.SUCCESS, "创建成功");
    }

    @Access(level = AccessLevel.ADMIN)
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public APIResponse delete(String ids){
        System.out.println("ids===" + ids);
        // 批量删除
        String[] arr = ids.split(",");
        for (String id : arr) {
            service.deleteNotice(id);
        }
        return new APIResponse(ResponeCode.SUCCESS, "删除成功");
    }

    @Access(level = AccessLevel.ADMIN)
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @Transactional
    public APIResponse update(Notice notice) throws IOException {
        service.updateNotice(notice);
        return new APIResponse(ResponeCode.SUCCESS, "更新成功");
    }

}
