package com.gk.study.service;

import com.gk.study.entity.Notice;
import org.springframework.stereotype.Service;

import java.util.List;

public interface NoticeService {

    void createNotice(Notice notice);

    void deleteNotice(String id);

    void updateNotice(Notice notice);

    List<Notice> getNoticeList(String userId);

    List<String> getCompanyUserIds(String companyId);
}
