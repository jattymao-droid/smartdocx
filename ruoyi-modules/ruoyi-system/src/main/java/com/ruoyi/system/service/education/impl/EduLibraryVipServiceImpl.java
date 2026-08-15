package com.ruoyi.system.service.education.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.config.EduPayProperties;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.domain.education.EduLibraryVipConfig;
import com.ruoyi.system.domain.education.EduLibraryVipExtendBody;
import com.ruoyi.system.domain.education.EduLibraryVipGrantBody;
import com.ruoyi.system.domain.education.EduLibraryVipMember;
import com.ruoyi.system.domain.education.EduLibraryVipStatus;
import com.ruoyi.system.domain.education.EduLibraryVipPlan;
import com.ruoyi.system.domain.education.EduLibraryVipRecentOrder;
import com.ruoyi.system.domain.education.EduPayOrder;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.mapper.education.EduLibraryVipMemberMapper;
import com.ruoyi.system.mapper.education.EduPayOrderMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.education.IEduLibraryVipService;
import com.ruoyi.system.service.education.IEduPayConfigService;

@Service
public class EduLibraryVipServiceImpl implements IEduLibraryVipService
{
    private static final String KEY_ENABLED = "edu.library.vip.enabled";
    private static final String KEY_PRICE = "edu.library.vip.price";
    private static final String KEY_DURATION_DAYS = "edu.library.vip.duration-days";
    private static final String KEY_FREE_DOWNLOAD = "edu.library.vip.free-download";
    private static final String KEY_PREVIEW_PAGES = "edu.library.vip.preview-pages";
    private static final String PLAN_SUPREME = "supreme";
    private static final String PLAN_DIAMOND = "diamond";
    private static final String PLAN_GOLD = "gold";
    private static final String PLAN_TEST = "test";
    private static final int MIN_DURATION_DAYS = 1;
    private static final int MAX_DURATION_DAYS = 3650;
    private static final int MIN_PREVIEW_PAGES = 0;
    private static final int MAX_PREVIEW_PAGES = 100;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SysConfigMapper configMapper;

    @Autowired
    private EduLibraryVipMemberMapper vipMemberMapper;

    @Autowired
    private EduPayOrderMapper payOrderMapper;

    @Autowired
    private IEduPayConfigService payConfigService;

    @Override
    public EduLibraryVipConfig resolveConfig()
    {
        EduLibraryVipConfig config = new EduLibraryVipConfig();
        config.setEnabled(parseBoolean(configService.selectConfigByKey(KEY_ENABLED), false));
        config.setPrice(parsePrice(configService.selectConfigByKey(KEY_PRICE), new BigDecimal("29.00")));
        config.setDurationDays(parseDurationDays(configService.selectConfigByKey(KEY_DURATION_DAYS), 30));
        config.setFreeDownload(parseBoolean(configService.selectConfigByKey(KEY_FREE_DOWNLOAD), true));
        config.setPreviewPages(parsePreviewPages(configService.selectConfigByKey(KEY_PREVIEW_PAGES), 0));
        return config;
    }

    @Override
    public void saveConfig(EduLibraryVipConfig config)
    {
        if (config == null)
        {
            throw new ServiceException("\u8bf7\u8bbe\u7f6e VIP \u53c2\u6570");
        }
        String operator = safeOperator();
        updateConfigValue(KEY_ENABLED, String.valueOf(config.isEnabled()), operator);
        updateConfigValue(KEY_PRICE, formatPrice(config.getPrice()), operator);
        updateConfigValue(KEY_DURATION_DAYS, String.valueOf(normalizeDurationDays(config.getDurationDays())), operator);
        updateConfigValue(KEY_FREE_DOWNLOAD, String.valueOf(config.isFreeDownload()), operator);
        updateConfigValue(KEY_PREVIEW_PAGES, String.valueOf(normalizePreviewPages(config.getPreviewPages())), operator);
    }

    @Override
    public EduLibraryVipStatus getStatus(String username)
    {
        refreshExpiredMembers();
        EduLibraryVipConfig config = resolveConfig();
        EduPayProperties payConfig = payConfigService.resolveRuntimeConfig();

        EduLibraryVipStatus status = new EduLibraryVipStatus();
        status.setEnabled(config.isEnabled());
        status.setPrice(config.getPrice());
        status.setDurationDays(config.getDurationDays());
        status.setFreeDownload(config.isFreeDownload());
        status.setPreviewPages(config.getPreviewPages());
        status.setPayEnabled(payConfig.getZpay().isEnabled());
        List<EduLibraryVipPlan> plans = resolvePlans();
        status.setPlans(plans);
        status.setDefaultPlanCode(resolveDefaultPlanCode(plans));

        EduLibraryVipMember member = StringUtils.isEmpty(username) ? null : vipMemberMapper.selectByUsername(username);
        boolean active = isActiveMember(member);
        status.setActive(active);
        if (active && member != null)
        {
            status.setPlanCode(member.getPlanCode());
            status.setExpireTime(member.getExpireTime());
            status.setRemainDays(calcRemainDays(member.getExpireTime()));
        }
        return status;
    }

    @Override
    public List<EduLibraryVipPlan> resolvePlans()
    {
        EduLibraryVipConfig config = resolveConfig();
        List<EduLibraryVipPlan> plans = buildDefaultPlans();
        if (config.getPrice() != null && config.getPrice().compareTo(BigDecimal.ZERO) > 0)
        {
            EduLibraryVipPlan supreme = findPlan(plans, PLAN_SUPREME);
            if (supreme != null)
            {
                supreme.setPrice(config.getPrice());
                if (config.getDurationDays() != null && config.getDurationDays() > 0)
                {
                    supreme.setDurationDays(config.getDurationDays());
                }
            }
        }
        return plans;
    }

    @Override
    public EduLibraryVipPlan resolvePlan(String planCode)
    {
        String code = StringUtils.isEmpty(planCode) ? resolveDefaultPlanCode(resolvePlans()) : planCode.trim();
        for (EduLibraryVipPlan plan : resolvePlans())
        {
            if (code.equals(plan.getCode()))
            {
                return plan;
            }
        }
        throw new ServiceException("\u4f1a\u5458\u5957\u9910\u4e0d\u5b58\u5728");
    }

    @Override
    public List<EduLibraryVipRecentOrder> selectRecentVipOrders(int limit)
    {
        int size = limit <= 0 ? 8 : Math.min(limit, 20);
        List<EduPayOrder> orders = payOrderMapper.selectRecentPaidOrders(EduPayOrder.BIZ_LIBRARY_VIP, size);
        List<EduLibraryVipRecentOrder> rows = new ArrayList<>();
        for (EduPayOrder order : orders)
        {
            EduLibraryVipRecentOrder row = new EduLibraryVipRecentOrder();
            row.setDisplayName(maskUsername(order.getUsername()));
            row.setPlanName(resolvePlanName(order.getBizRef(), order.getProductName()));
            row.setAmount(order.getAmount());
            row.setSavedAmount(resolveSavedAmount(order));
            row.setTimeLabel(formatRecentTime(order.getPayTime() != null ? order.getPayTime() : order.getCreateTime()));
            rows.add(row);
        }
        if (rows.size() < 5)
        {
            rows.addAll(buildFallbackRecentOrders(5 - rows.size()));
        }
        return rows.size() > size ? rows.subList(0, size) : rows;
    }

    @Override
    public boolean isActiveVip(String username)
    {
        if (StringUtils.isEmpty(username))
        {
            return false;
        }
        refreshExpiredMembers();
        EduLibraryVipConfig config = resolveConfig();
        if (!config.isEnabled())
        {
            return false;
        }
        EduLibraryVipMember member = vipMemberMapper.selectByUsername(username);
        return isActiveMember(member);
    }

    @Override
    public boolean grantsFreeDownload(String username)
    {
        return isActiveVip(username) && resolveConfig().isFreeDownload();
    }

    @Override
    public int resolvePreviewMaxPages(String username, int defaultPages)
    {
        if (!isActiveVip(username))
        {
            return defaultPages;
        }
        int vipPages = normalizePreviewPages(resolveConfig().getPreviewPages());
        return vipPages > 0 ? vipPages : defaultPages;
    }

    @Override
    public List<EduLibraryVipMember> selectVipMemberList(EduLibraryVipMember query)
    {
        refreshExpiredMembers();
        return vipMemberMapper.selectVipMemberList(query == null ? new EduLibraryVipMember() : query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantVip(EduLibraryVipGrantBody body, String operator)
    {
        validateGrantBody(body);
        int days = normalizeDurationDays(body.getDurationDays() != null ? body.getDurationDays() : resolveConfig().getDurationDays());
        upsertMembership(body.getUsername().trim(), days, PLAN_SUPREME, EduLibraryVipMember.SOURCE_ADMIN, null, body.getRemark(), operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void extendVip(EduLibraryVipExtendBody body, String operator)
    {
        validateUsername(body == null ? null : body.getUsername());
        int days = normalizeDurationDays(body.getDurationDays() != null ? body.getDurationDays() : resolveConfig().getDurationDays());
        EduLibraryVipMember existing = vipMemberMapper.selectByUsername(body.getUsername().trim());
        if (existing == null)
        {
            throw new ServiceException("\u8be5\u7528\u6237\u5c1a\u672a\u5f00\u901a VIP");
        }
        Date base = isActiveMember(existing) ? existing.getExpireTime() : new Date();
        Date expireTime = addDays(base, days);
        EduLibraryVipMember patch = new EduLibraryVipMember();
        patch.setVipId(existing.getVipId());
        patch.setStatus(EduLibraryVipMember.STATUS_ACTIVE);
        patch.setExpireTime(expireTime);
        patch.setRemark(body.getRemark());
        patch.setUpdateBy(operator);
        vipMemberMapper.updateVipMember(patch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableVip(String username, String operator)
    {
        if (StringUtils.isEmpty(username))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u7528\u6237\u540d");
        }
        EduLibraryVipMember existing = vipMemberMapper.selectByUsername(username.trim());
        if (existing == null)
        {
            throw new ServiceException("\u8be5\u7528\u6237\u5c1a\u672a\u5f00\u901a VIP");
        }
        EduLibraryVipMember patch = new EduLibraryVipMember();
        patch.setVipId(existing.getVipId());
        patch.setStatus(EduLibraryVipMember.STATUS_DISABLED);
        patch.setUpdateBy(operator);
        vipMemberMapper.updateVipMember(patch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateFromOrder(EduPayOrder order)
    {
        if (order == null || !EduPayOrder.BIZ_LIBRARY_VIP.equals(order.getBizType()))
        {
            return;
        }
        EduLibraryVipPlan plan = resolvePlan(order.getBizRef());
        int days = plan.getDurationDays() == null ? resolveConfig().getDurationDays() : plan.getDurationDays();
        upsertMembership(order.getUsername(), days, plan.getCode(), EduLibraryVipMember.SOURCE_PAY, order.getOrderNo(), null, order.getUsername());
    }

    private void upsertMembership(String username, int days, String planCode, String source, String orderNo, String remark, String operator)
    {
        Date now = new Date();
        EduLibraryVipMember existing = vipMemberMapper.selectByUsername(username);
        if (existing == null)
        {
            EduLibraryVipMember member = new EduLibraryVipMember();
            member.setUsername(username);
            member.setPlanCode(StringUtils.isEmpty(planCode) ? PLAN_SUPREME : planCode);
            member.setStatus(EduLibraryVipMember.STATUS_ACTIVE);
            member.setStartTime(now);
            member.setExpireTime(addDays(now, days));
            member.setSource(source);
            member.setOrderNo(orderNo);
            member.setRemark(remark);
            member.setCreateBy(operator);
            vipMemberMapper.insertVipMember(member);
            return;
        }

        Date base = isActiveMember(existing) ? existing.getExpireTime() : now;
        EduLibraryVipMember patch = new EduLibraryVipMember();
        patch.setVipId(existing.getVipId());
        patch.setStatus(EduLibraryVipMember.STATUS_ACTIVE);
        patch.setExpireTime(addDays(base, days));
        if (StringUtils.isNotEmpty(planCode))
        {
            patch.setPlanCode(planCode);
        }
        patch.setSource(source);
        if (StringUtils.isNotEmpty(orderNo))
        {
            patch.setOrderNo(orderNo);
        }
        if (StringUtils.isNotEmpty(remark))
        {
            patch.setRemark(remark);
        }
        patch.setUpdateBy(operator);
        vipMemberMapper.updateVipMember(patch);
    }

    private void refreshExpiredMembers()
    {
        vipMemberMapper.markExpiredBefore(new Date());
    }

    private static boolean isActiveMember(EduLibraryVipMember member)
    {
        if (member == null || !EduLibraryVipMember.STATUS_ACTIVE.equals(member.getStatus()))
        {
            return false;
        }
        return member.getExpireTime() != null && member.getExpireTime().after(new Date());
    }

    private static int calcRemainDays(Date expireTime)
    {
        if (expireTime == null)
        {
            return 0;
        }
        long diff = expireTime.getTime() - System.currentTimeMillis();
        if (diff <= 0)
        {
            return 0;
        }
        return (int) TimeUnit.MILLISECONDS.toDays(diff) + 1;
    }

    private static Date addDays(Date base, int days)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(base);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    private void validateGrantBody(EduLibraryVipGrantBody body)
    {
        validateUsername(body == null ? null : body.getUsername());
    }

    private static void validateUsername(String username)
    {
        if (StringUtils.isEmpty(username))
        {
            throw new ServiceException("\u8bf7\u8f93\u5165\u7528\u6237\u540d");
        }
    }

    private void updateConfigValue(String configKey, String configValue, String username)
    {
        SysConfig query = new SysConfig();
        query.setConfigKey(configKey);
        SysConfig existing = configMapper.selectConfig(query);
        if (existing == null)
        {
            throw new ServiceException("\u7f3a\u5c11\u53c2\u6570\u914d\u7f6e: " + configKey);
        }
        SysConfig update = new SysConfig();
        update.setConfigId(existing.getConfigId());
        update.setConfigKey(configKey);
        update.setConfigValue(StringUtils.nvl(configValue, ""));
        update.setUpdateBy(username);
        configService.updateConfig(update);
    }

    private static String safeOperator()
    {
        try
        {
            return com.ruoyi.common.security.utils.SecurityUtils.getUsername();
        }
        catch (Exception ex)
        {
            return "system";
        }
    }

    private static boolean parseBoolean(String raw, boolean fallback)
    {
        if (StringUtils.isEmpty(raw))
        {
            return fallback;
        }
        return "true".equalsIgnoreCase(raw.trim()) || "1".equals(raw.trim()) || "Y".equalsIgnoreCase(raw.trim());
    }

    private static BigDecimal parsePrice(String raw, BigDecimal fallback)
    {
        if (StringUtils.isEmpty(raw))
        {
            return fallback;
        }
        try
        {
            BigDecimal value = new BigDecimal(raw.trim());
            return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        catch (Exception ex)
        {
            return fallback;
        }
    }

    private static String formatPrice(BigDecimal price)
    {
        BigDecimal value = price == null ? BigDecimal.ZERO : price;
        if (value.compareTo(BigDecimal.ZERO) < 0)
        {
            value = BigDecimal.ZERO;
        }
        return value.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }

    private static int parseDurationDays(String raw, int fallback)
    {
        if (StringUtils.isEmpty(raw))
        {
            return normalizeDurationDays(fallback);
        }
        try
        {
            return normalizeDurationDays(Integer.parseInt(raw.trim()));
        }
        catch (Exception ex)
        {
            return normalizeDurationDays(fallback);
        }
    }

    private static int normalizeDurationDays(int value)
    {
        if (value < MIN_DURATION_DAYS)
        {
            return MIN_DURATION_DAYS;
        }
        if (value > MAX_DURATION_DAYS)
        {
            return MAX_DURATION_DAYS;
        }
        return value;
    }

    private static int parsePreviewPages(String raw, int fallback)
    {
        if (StringUtils.isEmpty(raw))
        {
            return normalizePreviewPages(fallback);
        }
        try
        {
            return normalizePreviewPages(Integer.parseInt(raw.trim()));
        }
        catch (Exception ex)
        {
            return normalizePreviewPages(fallback);
        }
    }

    private static int normalizePreviewPages(int value)
    {
        if (value < MIN_PREVIEW_PAGES)
        {
            return MIN_PREVIEW_PAGES;
        }
        if (value > MAX_PREVIEW_PAGES)
        {
            return MAX_PREVIEW_PAGES;
        }
        return value;
    }

    private static List<EduLibraryVipPlan> buildDefaultPlans()
    {
        List<EduLibraryVipPlan> plans = new ArrayList<>();
        plans.add(buildPlan(PLAN_SUPREME, "\u81f3\u5c0a\u4f1a\u5458", "388.00", "598.00", 1095, "\u5b98\u65b9\u63a8\u8350", true));
        plans.add(buildPlan(PLAN_DIAMOND, "\u94bb\u77f3\u4f1a\u5458", "188.00", "398.00", 365, null, false));
        plans.add(buildPlan(PLAN_GOLD, "\u9ec4\u91d1\u4f1a\u5458", "128.00", "288.00", 180, "\u9650\u65f6\u6298\u6263", false));
        plans.add(buildPlan(PLAN_TEST, "\u6d4b\u8bd5\u4f1a\u5458", "0.10", "128.00", 30, "100\u6b21\u514d\u8d39\u4e0b\u8f7d", false));
        return plans;
    }

    private static EduLibraryVipPlan buildPlan(String code, String name, String price, String originalPrice, int days, String badge, boolean recommended)
    {
        EduLibraryVipPlan plan = new EduLibraryVipPlan();
        plan.setCode(code);
        plan.setName(name);
        plan.setPrice(new BigDecimal(price));
        plan.setOriginalPrice(new BigDecimal(originalPrice));
        plan.setDurationDays(days);
        plan.setBadge(badge);
        plan.setRecommended(recommended);
        return plan;
    }

    private static EduLibraryVipPlan findPlan(List<EduLibraryVipPlan> plans, String code)
    {
        for (EduLibraryVipPlan plan : plans)
        {
            if (code.equals(plan.getCode()))
            {
                return plan;
            }
        }
        return null;
    }

    private static String resolveDefaultPlanCode(List<EduLibraryVipPlan> plans)
    {
        if (plans == null || plans.isEmpty())
        {
            return PLAN_SUPREME;
        }
        for (EduLibraryVipPlan plan : plans)
        {
            if (plan.isRecommended())
            {
                return plan.getCode();
            }
        }
        return plans.get(0).getCode();
    }

    private String resolvePlanName(String planCode, String productName)
    {
        if (StringUtils.isNotEmpty(planCode))
        {
            try
            {
                return resolvePlan(planCode).getName();
            }
            catch (Exception ex)
            {
                // ignore
            }
        }
        return StringUtils.isNotEmpty(productName) ? productName : "\u4f1a\u5458\u5957\u9910";
    }

    private BigDecimal resolveSavedAmount(EduPayOrder order)
    {
        if (order == null)
        {
            return BigDecimal.ZERO;
        }
        try
        {
            EduLibraryVipPlan plan = resolvePlan(order.getBizRef());
            if (plan.getOriginalPrice() != null && order.getAmount() != null)
            {
                BigDecimal saved = plan.getOriginalPrice().subtract(order.getAmount());
                return saved.compareTo(BigDecimal.ZERO) > 0 ? saved.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
            }
        }
        catch (Exception ex)
        {
            // ignore
        }
        return BigDecimal.ZERO;
    }

    private static String maskUsername(String username)
    {
        if (StringUtils.isEmpty(username))
        {
            return "\u7528\u6237";
        }
        String value = username.trim();
        if (value.length() <= 2)
        {
            return value.charAt(0) + "*";
        }
        return value.charAt(0) + "***" + value.charAt(value.length() - 1);
    }

    private static String formatRecentTime(Date time)
    {
        if (time == null)
        {
            return "\u521a\u521a";
        }
        long diff = System.currentTimeMillis() - time.getTime();
        if (diff < 60_000L)
        {
            return "\u521a\u521a";
        }
        if (diff < 3600_000L)
        {
            return Math.max(1, diff / 60_000L) + "\u5206\u949f\u524d";
        }
        if (diff < 86400_000L)
        {
            return Math.max(1, diff / 3600_000L) + "\u5c0f\u65f6\u524d";
        }
        return Math.max(1, diff / 86400_000L) + "\u5929\u524d";
    }

    private List<EduLibraryVipRecentOrder> buildFallbackRecentOrders(int count)
    {
        String[][] samples = {
            { "\u5c0f***y", "\u6d4b\u8bd5\u4f1a\u5458", "0.10", "127.90", "\u521a\u521a" },
            { "\u6559***6", "\u9ec4\u91d1\u4f1a\u5458", "128.00", "160.00", "3\u5206\u949f\u524d" },
            { "\u738b***o", "\u94bb\u77f3\u4f1a\u5458", "188.00", "210.00", "8\u5206\u949f\u524d" },
            { "\u5218***n", "\u81f3\u5c0a\u4f1a\u5458", "388.00", "210.00", "15\u5206\u949f\u524d" },
            { "\u5f20***g", "\u9ec4\u91d1\u4f1a\u5458", "128.00", "160.00", "22\u5206\u949f\u524d" }
        };
        List<EduLibraryVipRecentOrder> rows = new ArrayList<>();
        for (int i = 0; i < count && i < samples.length; i++)
        {
            EduLibraryVipRecentOrder row = new EduLibraryVipRecentOrder();
            row.setDisplayName(samples[i][0]);
            row.setPlanName(samples[i][1]);
            row.setAmount(new BigDecimal(samples[i][2]));
            row.setSavedAmount(new BigDecimal(samples[i][3]));
            row.setTimeLabel(samples[i][4]);
            rows.add(row);
        }
        return rows;
    }
}
