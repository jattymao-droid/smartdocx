package com.ruoyi.system.service.education;

public interface IEduQbPaperShareService
{
    String createShare(String snapshotJson, String username);

    String getShareSnapshot(String shareId);
}
