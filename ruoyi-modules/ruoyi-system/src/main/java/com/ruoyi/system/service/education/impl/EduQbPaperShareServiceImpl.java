package com.ruoyi.system.service.education.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduQbPaperShare;
import com.ruoyi.system.mapper.education.EduQbPaperShareMapper;
import com.ruoyi.system.service.education.IEduQbPaperShareService;

@Service
public class EduQbPaperShareServiceImpl implements IEduQbPaperShareService
{
    private static final int SHARE_TTL_DAYS = 30;

    @Autowired
    private EduQbPaperShareMapper shareMapper;

    @Override
    public String createShare(String snapshotJson, String username)
    {
        if (StringUtils.isEmpty(snapshotJson))
        {
            throw new ServiceException("\u5206\u4eab\u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String shareId = UUID.randomUUID().toString().replace("-", "");
        EduQbPaperShare row = new EduQbPaperShare();
        row.setShareId(shareId);
        row.setSnapshot(snapshotJson);
        row.setCreateBy(username);
        row.setCreateTime(new Date());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, SHARE_TTL_DAYS);
        row.setExpireTime(cal.getTime());
        shareMapper.insertEduQbPaperShare(row);
        return shareId;
    }

    @Override
    public String getShareSnapshot(String shareId)
    {
        if (StringUtils.isEmpty(shareId))
        {
            throw new ServiceException("\u5206\u4eab\u94fe\u63a5\u65e0\u6548");
        }
        EduQbPaperShare row = shareMapper.selectEduQbPaperShareById(shareId.trim());
        if (row == null || StringUtils.isEmpty(row.getSnapshot()))
        {
            throw new ServiceException("\u5206\u4eab\u94fe\u63a5\u65e0\u6548\u6216\u5df2\u8fc7\u671f");
        }
        return row.getSnapshot();
    }
}
