package com.gk.study.controller;

import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.Post;
import com.gk.study.entity.Thing;
import com.gk.study.permission.Access;
import com.gk.study.permission.AccessLevel;
import com.gk.study.service.PostService;
import com.gk.study.service.ThingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/post")
public class PostController {

    private final static Logger logger = LoggerFactory.getLogger(PostController.class);

    @Autowired
    PostService service;

    @Autowired
    NoticeController noticeController;

    @Autowired
    ThingService thingService;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public APIResponse list(){
        List<Post> list =  service.getPostList();

        return new APIResponse(ResponeCode.SUCCESS, "查询成功", list);
    }

    @Access(level = AccessLevel.ADMIN)
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @Transactional
    public APIResponse create(Post post) throws IOException {
        service.createPost(post);
        Thing thing = thingService.getThingById(post.getThingId());
        noticeController.createResumeNotice(post.getUserId(), post.getCompanyId(), thing.getTitle());
        return new APIResponse(ResponeCode.SUCCESS, "创建成功");
    }

    @Access(level = AccessLevel.ADMIN)
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public APIResponse delete(String ids){
        System.out.println("ids===" + ids);
        // 批量删除
        String[] arr = ids.split(",");
        for (String id : arr) {
            service.deletePost(id);
        }
        return new APIResponse(ResponeCode.SUCCESS, "删除成功");
    }

    @Access(level = AccessLevel.ADMIN)
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @Transactional
    public APIResponse update(Post post) throws IOException {
        service.updatePost(post);
        return new APIResponse(ResponeCode.SUCCESS, "更新成功");
    }

    @RequestMapping(value = "/listUserPostApi", method = RequestMethod.GET)
    public APIResponse listUserPostApi(String userId){
        List<Post> list =  service.getUserPost(userId);

        return new APIResponse(ResponeCode.SUCCESS, "查询成功", list);
    }

    @RequestMapping(value = "/listCompanyPostApi", method = RequestMethod.GET)
    public APIResponse listCompanyPostApi(String companyId){
        List<Post> list =  service.getCompanyPost(companyId);

        return new APIResponse(ResponeCode.SUCCESS, "查询成功", list);
    }

    @RequestMapping(value = "/updateStatus", method = RequestMethod.POST)
    @Transactional
    public APIResponse updatePostStatus(@RequestBody Post post) {
        try {
            Post existingPost = service.getPostById(post.getId());
            if (existingPost == null) {
                return new APIResponse(ResponeCode.FAIL, "投递记录不存在");
            }

            // 更新状态
            existingPost.setStatus(post.getStatus());
            
            // 如果是面试邀请，保存面试信息
            if ("interviewing".equals(post.getStatus())) {
                existingPost.setInterviewTime(post.getInterviewTime());
                existingPost.setInterviewLocation(post.getInterviewLocation());
                existingPost.setNotes(post.getNotes());
            } else {
                // 如果是其他状态，只更新备注
                existingPost.setNotes(post.getNotes());
            }

            service.updatePost(existingPost);
            
            Thing thing = thingService.getThingById(existingPost.getThingId());
            noticeController.createStatusChangeNotice(existingPost.getUserId(), post.getStatus(), thing.getTitle());

            return new APIResponse(ResponeCode.SUCCESS, "更新成功");
        } catch (Exception e) {
            logger.error("更新状态失败", e);
            return new APIResponse(ResponeCode.FAIL, "更新失败");
        }
    }
}
