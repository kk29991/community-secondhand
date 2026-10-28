package com.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.entity.Announcement;
import com.mall.entity.Banner;
import com.mall.mapper.AnnouncementMapper;
import com.mall.mapper.BannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容服务: 公告 + 轮播图
 */
@Service
@RequiredArgsConstructor
public class ContentService {

    private final AnnouncementMapper announcementMapper;
    private final BannerMapper bannerMapper;

    // ---------------------------------------------------------
    // 公告
    // ---------------------------------------------------------

    /**
     * 前台公告列表 (已发布, 置顶优先)
     */
    public List<Announcement> listPublished(int limit) {
        Page<Announcement> page = new Page<>(1, limit);
        return announcementMapper.selectPage(page, Wrappers.<Announcement>lambdaQuery()
                .eq(Announcement::getStatus, 1)
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getCreateTime)).getRecords();
    }

    public IPage<Announcement> pageAnnouncements(String keyword, Integer status, int pageNum, int pageSize) {
        Page<Announcement> page = new Page<>(Math.max(pageNum, 1), pageSize < 1 ? 10 : pageSize);
        return announcementMapper.selectPage(page, Wrappers.<Announcement>lambdaQuery()
                .like(keyword != null && !keyword.isBlank(), Announcement::getTitle, keyword)
                .eq(status != null, Announcement::getStatus, status)
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getCreateTime));
    }

    public Announcement getAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw BusinessException.notFound("公告不存在");
        }
        return announcement;
    }

    @Transactional(rollbackFor = Exception.class)
    public Announcement saveAnnouncement(Announcement announcement, Long adminId) {
        if (announcement.getTitle() == null || announcement.getTitle().isBlank()) {
            throw new BusinessException("公告标题不能为空");
        }
        if (announcement.getAdminId() == null) {
            announcement.setAdminId(adminId);
        }
        if (announcement.getStatus() == null) {
            announcement.setStatus(1);
        }
        if (announcement.getIsTop() == null) {
            announcement.setIsTop(0);
        }
        if (announcement.getType() == null) {
            announcement.setType(1);
        }
        if (announcement.getStatus() == 1 && announcement.getPublishTime() == null) {
            announcement.setPublishTime(LocalDateTime.now());
        }
        if (announcement.getId() == null) {
            announcementMapper.insert(announcement);
        } else {
            announcementMapper.updateById(announcement);
        }
        return announcement;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAnnouncement(Long id) {
        announcementMapper.deleteById(id);
    }

    // ---------------------------------------------------------
    // 轮播图
    // ---------------------------------------------------------

    /**
     * 前台轮播图
     */
    public List<Banner> listActiveBanners(Integer position) {
        return bannerMapper.selectList(Wrappers.<Banner>lambdaQuery()
                .eq(Banner::getStatus, 1)
                .eq(position != null, Banner::getPosition, position)
                .orderByAsc(Banner::getSort)
                .orderByAsc(Banner::getId));
    }

    public IPage<Banner> pageBanners(Integer position, Integer status, int pageNum, int pageSize) {
        Page<Banner> page = new Page<>(Math.max(pageNum, 1), pageSize < 1 ? 10 : pageSize);
        return bannerMapper.selectPage(page, Wrappers.<Banner>lambdaQuery()
                .eq(position != null, Banner::getPosition, position)
                .eq(status != null, Banner::getStatus, status)
                .orderByAsc(Banner::getSort)
                .orderByAsc(Banner::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Banner saveBanner(Banner banner) {
        if (banner.getImage() == null || banner.getImage().isBlank()) {
            throw new BusinessException("请上传轮播图片");
        }
        if (banner.getPosition() == null) {
            banner.setPosition(1);
        }
        if (banner.getSort() == null) {
            banner.setSort(0);
        }
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        if (banner.getId() == null) {
            bannerMapper.insert(banner);
        } else {
            bannerMapper.updateById(banner);
        }
        return banner;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBanner(Long id) {
        bannerMapper.deleteById(id);
    }
}
