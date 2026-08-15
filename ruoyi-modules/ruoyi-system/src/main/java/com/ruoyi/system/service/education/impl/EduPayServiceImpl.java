package com.ruoyi.system.service.education.impl;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.config.EduPayProperties;
import com.ruoyi.system.domain.education.EduLibraryDocument;
import com.ruoyi.system.domain.education.EduLibraryTopic;
import com.ruoyi.system.domain.education.EduPayCheckResult;
import com.ruoyi.system.domain.education.EduPayCreateBody;
import com.ruoyi.system.domain.education.EduPayEntitlement;
import com.ruoyi.system.domain.education.EduPayOrder;
import com.ruoyi.system.mapper.education.EduLibraryDocumentMapper;
import com.ruoyi.system.mapper.education.EduLibraryTopicMapper;
import com.ruoyi.system.mapper.education.EduPayEntitlementMapper;
import com.ruoyi.system.mapper.education.EduPayOrderMapper;
import com.ruoyi.system.service.education.IEduLibraryVipService;
import com.ruoyi.system.service.education.IEduPayConfigService;
import com.ruoyi.system.service.education.IEduPayService;
import com.ruoyi.system.service.education.support.EduQbHttpUtils;
import com.ruoyi.system.service.education.support.ZPaySignUtils;

@Service
public class EduPayServiceImpl implements IEduPayService
{
    @Autowired
    private IEduPayConfigService payConfigService;

    @Autowired
    private EduPayOrderMapper orderMapper;

    @Autowired
    private EduPayEntitlementMapper entitlementMapper;

    @Autowired
    private EduLibraryDocumentMapper documentMapper;

    @Autowired
    private EduLibraryTopicMapper topicMapper;

    @Autowired
    private IEduLibraryVipService vipService;

    @Override
    public EduPayCheckResult checkAccess(String bizType, Long bizId, String bizRef, String username)
    {
        EduPayCheckResult result = new EduPayCheckResult();
        result.setBizType(bizType);
        result.setBizId(bizId == null ? 0L : bizId);
        result.setBizRef(normalizeBizRef(bizRef));

        EduPayProperties config = payConfigService.resolveRuntimeConfig();
        result.setEnabled(config.getZpay().isEnabled());

        ProductQuote quote = resolveQuote(bizType, result.getBizId(), result.getBizRef());
        result.setProductName(quote.productName);
        result.setPrice(quote.price);

        boolean needPay = quote.price != null && quote.price.compareTo(BigDecimal.ZERO) > 0;
        result.setNeedPay(needPay);

        if (!needPay)
        {
            result.setPurchased(true);
            return result;
        }
        if (isLibraryDocumentOwner(bizType, bizId, username))
        {
            result.setPurchased(true);
            return result;
        }
        if (StringUtils.isEmpty(username))
        {
            result.setPurchased(false);
            return result;
        }
        if ((EduPayOrder.BIZ_LIBRARY_DOCUMENT.equals(bizType) || EduPayOrder.BIZ_LIBRARY_TOPIC.equals(bizType))
                && vipService.grantsFreeDownload(username))
        {
            result.setPurchased(true);
            return result;
        }
        if (EduPayOrder.BIZ_LIBRARY_VIP.equals(bizType))
        {
            com.ruoyi.system.domain.education.EduLibraryVipConfig vipConfig = vipService.resolveConfig();
            result.setEnabled(vipConfig.isEnabled() && config.getZpay().isEnabled());
            result.setPurchased(false);
            return result;
        }
        int count = entitlementMapper.countEntitlement(username, bizType, result.getBizId(), result.getBizRef());
        result.setPurchased(count > 0);
        return result;
    }

    @Override
    public void assertAccess(String bizType, Long bizId, String bizRef, String username)
    {
        EduPayCheckResult check = checkAccess(bizType, bizId, bizRef, username);
        if (check.isNeedPay() && !check.isPurchased())
        {
            throw new ServiceException("\u9700\u8981\u652f\u4ed8\u540e\u624d\u53ef\u4e0b\u8f7d", PAY_REQUIRED_CODE);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EduPayOrder createOrder(EduPayCreateBody body, String username, String clientIp)
    {
        if (body == null || StringUtils.isEmpty(body.getBizType()))
        {
            throw new ServiceException("\u4e1a\u52a1\u7c7b\u578b\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (StringUtils.isEmpty(username))
        {
            throw new ServiceException("\u8bf7\u5148\u767b\u5f55");
        }
        String payType = normalizePayType(body.getPayType());
        Long bizId = body.getBizId() == null ? 0L : body.getBizId();
        String bizRef = normalizeBizRef(body.getBizRef());

        EduPayCheckResult check = checkAccess(body.getBizType(), bizId, bizRef, username);
        if (!check.isNeedPay())
        {
            throw new ServiceException("\u5f53\u524d\u5185\u5bb9\u65e0\u9700\u652f\u4ed8");
        }
        if (check.isPurchased() && !EduPayOrder.BIZ_LIBRARY_VIP.equals(body.getBizType()))
        {
            throw new ServiceException("\u5df2\u8d2d\u4e70\uff0c\u65e0\u9700\u91cd\u590d\u652f\u4ed8");
        }

        EduPayOrder pending = orderMapper.selectPendingOrder(username, body.getBizType(), bizId, bizRef);
        if (pending != null && StringUtils.isNotEmpty(pending.getPayUrl()))
        {
            return pending;
        }

        EduPayProperties config = payConfigService.resolveRuntimeConfig();
        EduPayProperties.ZPay zpay = config.getZpay();
        if (!zpay.isEnabled())
        {
            throw new ServiceException("\u652f\u4ed8\u529f\u80fd\u672a\u5f00\u542f");
        }
        if (StringUtils.isEmpty(zpay.getPid()) || StringUtils.isEmpty(zpay.getKey()))
        {
            throw new ServiceException("\u652f\u4ed8\u5e73\u53f0\u672a\u914d\u7f6e\u5b8c\u6574");
        }
        if (StringUtils.isEmpty(zpay.getNotifyUrl()))
        {
            throw new ServiceException("\u8bf7\u5148\u914d\u7f6e\u652f\u4ed8\u56de\u8c03\u5730\u5740");
        }

        String orderNo = buildOrderNo();
        Map<String, String> params = new HashMap<>();
        params.put("pid", zpay.getPid());
        params.put("type", payType);
        params.put("out_trade_no", orderNo);
        params.put("notify_url", zpay.getNotifyUrl());
        params.put("name", truncate(check.getProductName(), 120));
        params.put("money", formatMoney(check.getPrice()));
        params.put("clientip", StringUtils.defaultIfEmpty(clientIp, "127.0.0.1"));
        params.put("param", body.getBizType() + ":" + bizId + ":" + StringUtils.defaultString(bizRef, ""));
        if (StringUtils.isNotEmpty(body.getReturnUrl()))
        {
            params.put("return_url", body.getReturnUrl());
        }
        params.put("sign_type", "MD5");
        params.put("sign", ZPaySignUtils.sign(params, zpay.getKey()));

        String gateway = zpay.getGatewayUrl().replaceAll("/$", "");
        String response = EduQbHttpUtils.sendPost(gateway + "/mapi.php", buildFormBody(params));
        JSONObject json = JSON.parseObject(response);
        if (json == null || json.getIntValue("code") != 1)
        {
            String msg = json != null ? json.getString("msg") : response;
            throw new ServiceException("\u521b\u5efa\u652f\u4ed8\u8ba2\u5355\u5931\u8d25: " + StringUtils.defaultString(msg, "unknown"));
        }

        EduPayOrder order = new EduPayOrder();
        order.setOrderNo(orderNo);
        order.setUsername(username);
        order.setBizType(body.getBizType());
        order.setBizId(bizId);
        order.setBizRef(bizRef);
        order.setProductName(check.getProductName());
        order.setAmount(check.getPrice());
        order.setPayType(payType);
        order.setStatus(EduPayOrder.STATUS_PENDING);
        order.setPayUrl(json.getString("payurl"));
        order.setQrcodeUrl(firstNonBlank(json.getString("qrcode"), json.getString("img")));
        order.setClientIp(clientIp);
        orderMapper.insertEduPayOrder(order);
        return order;
    }

    @Override
    public EduPayOrder getOrderStatus(String orderNo, String username)
    {
        if (StringUtils.isEmpty(orderNo))
        {
            throw new ServiceException("\u8ba2\u5355\u53f7\u4e0d\u80fd\u4e3a\u7a7a");
        }
        EduPayOrder order = orderMapper.selectEduPayOrderByOrderNo(orderNo);
        if (order == null)
        {
            throw new ServiceException("\u8ba2\u5355\u4e0d\u5b58\u5728");
        }
        if (StringUtils.isNotEmpty(username) && !username.equals(order.getUsername()))
        {
            throw new ServiceException("\u65e0\u6743\u67e5\u770b\u8be5\u8ba2\u5355");
        }
        return order;
    }

    @Override
    public List<EduPayOrder> selectMyOrderList(EduPayOrder query, String username)
    {
        if (StringUtils.isEmpty(username))
        {
            throw new ServiceException("\u8bf7\u5148\u767b\u5f55");
        }
        EduPayOrder criteria = query == null ? new EduPayOrder() : query;
        criteria.setUsername(username);
        return orderMapper.selectMyPayOrderList(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleZPayNotify(Map<String, String> params)
    {
        if (params == null || params.isEmpty())
        {
            return "fail";
        }
        EduPayProperties config = payConfigService.resolveRuntimeConfig();
        String merchantKey = config.getZpay().getKey();
        String sign = params.get("sign");
        if (!ZPaySignUtils.verify(params, merchantKey, sign))
        {
            return "fail";
        }
        if (!"TRADE_SUCCESS".equalsIgnoreCase(params.get("trade_status")))
        {
            return "fail";
        }

        String orderNo = params.get("out_trade_no");
        EduPayOrder existing = orderMapper.selectEduPayOrderByOrderNo(orderNo);
        if (existing == null)
        {
            return "fail";
        }
        String configuredPid = config.getZpay().getPid();
        if (StringUtils.isNotEmpty(configuredPid) && !configuredPid.equals(params.get("pid")))
        {
            return "fail";
        }
        if (!verifyNotifyAmount(params.get("money"), existing.getAmount()))
        {
            return "fail";
        }
        if (EduPayOrder.STATUS_PAID.equals(existing.getStatus()))
        {
            return "success";
        }

        Date now = new Date();
        EduPayOrder patch = new EduPayOrder();
        patch.setOrderNo(orderNo);
        patch.setStatus(EduPayOrder.STATUS_PAID);
        patch.setTradeNo(params.get("trade_no"));
        patch.setNotifyTime(now);
        patch.setPayTime(now);
        int rows = orderMapper.updateEduPayOrderPaid(patch);
        if (rows <= 0)
        {
            return "success";
        }

        EduPayEntitlement entitlement = new EduPayEntitlement();
        entitlement.setUsername(existing.getUsername());
        entitlement.setBizType(existing.getBizType());
        entitlement.setBizId(existing.getBizId());
        entitlement.setBizRef(existing.getBizRef());
        entitlement.setOrderNo(orderNo);
        entitlementMapper.insertEntitlement(entitlement);
        vipService.activateFromOrder(existing);
        return "success";
    }

    private boolean isLibraryDocumentOwner(String bizType, Long bizId, String username)
    {
        if (!EduPayOrder.BIZ_LIBRARY_DOCUMENT.equals(bizType) || bizId == null || bizId <= 0 || StringUtils.isEmpty(username))
        {
            return false;
        }
        EduLibraryDocument document = documentMapper.selectEduLibraryDocumentById(bizId);
        return document != null && username.equals(document.getCreateBy());
    }

    private static boolean verifyNotifyAmount(String paidMoney, BigDecimal orderAmount)
    {
        if (orderAmount == null || StringUtils.isEmpty(paidMoney))
        {
            return false;
        }
        try
        {
            BigDecimal paid = new BigDecimal(paidMoney.trim());
            return paid.compareTo(orderAmount.setScale(2, BigDecimal.ROUND_HALF_UP)) == 0;
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    private ProductQuote resolveQuote(String bizType, Long bizId, String bizRef)
    {
        if (EduPayOrder.BIZ_LIBRARY_DOCUMENT.equals(bizType))
        {
            if (bizId == null || bizId <= 0)
            {
                throw new ServiceException("\u6587\u6863 ID \u4e0d\u80fd\u4e3a\u7a7a");
            }
            EduLibraryDocument document = documentMapper.selectEduLibraryDocumentById(bizId);
            if (document == null)
            {
                throw new ServiceException("\u6587\u6863\u4e0d\u5b58\u5728");
            }
            BigDecimal price = document.getDownloadPrice() == null ? BigDecimal.ZERO : document.getDownloadPrice();
            return new ProductQuote("\u6587\u6863\u4e0b\u8f7d\u300c" + document.getTitle() + "\u300d", price);
        }
        if (EduPayOrder.BIZ_LIBRARY_TOPIC.equals(bizType))
        {
            if (bizId == null || bizId <= 0)
            {
                throw new ServiceException("\u4e13\u9898 ID \u4e0d\u80fd\u4e3a\u7a7a");
            }
            EduLibraryTopic topic = topicMapper.selectEduLibraryTopicById(bizId);
            if (topic == null)
            {
                throw new ServiceException("\u4e13\u9898\u4e0d\u5b58\u5728");
            }
            BigDecimal price = topic.getBundlePrice() == null ? BigDecimal.ZERO : topic.getBundlePrice();
            return new ProductQuote("\u4e13\u9898\u6253\u5305\u300c" + topic.getTitle() + "\u300d", price);
        }
        if (EduPayOrder.BIZ_PAPER_EXPORT.equals(bizType))
        {
            EduPayProperties config = payConfigService.resolveRuntimeConfig();
            BigDecimal price = config.getPaperExportFee() == null ? BigDecimal.ZERO : config.getPaperExportFee();
            String suffix = bizId != null && bizId > 0 ? ("\u5377" + bizId) : "";
            if (StringUtils.isNotEmpty(bizRef))
            {
                suffix = suffix + "-" + bizRef.substring(0, Math.min(8, bizRef.length()));
            }
            return new ProductQuote("\u7ec4\u5377\u8bd5\u5377\u5bfc\u51fa" + suffix, price);
        }
        if (EduPayOrder.BIZ_LIBRARY_VIP.equals(bizType))
        {
            com.ruoyi.system.domain.education.EduLibraryVipConfig vipConfig = vipService.resolveConfig();
            if (!vipConfig.isEnabled())
            {
                return new ProductQuote("\u6587\u5e93 VIP \u4f1a\u5458", BigDecimal.ZERO);
            }
            com.ruoyi.system.domain.education.EduLibraryVipPlan plan = vipService.resolvePlan(bizRef);
            BigDecimal price = plan.getPrice() == null ? BigDecimal.ZERO : plan.getPrice();
            int days = plan.getDurationDays() == null ? 30 : plan.getDurationDays();
            return new ProductQuote(plan.getName() + "\uff08" + days + "\u5929\uff09", price);
        }
        throw new ServiceException("\u4e0d\u652f\u6301\u7684\u4e1a\u52a1\u7c7b\u578b");
    }

    private static String buildOrderNo()
    {
        return "PAY" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private static String normalizePayType(String payType)
    {
        if ("wxpay".equalsIgnoreCase(payType) || "wechat".equalsIgnoreCase(payType))
        {
            return "wxpay";
        }
        return "alipay";
    }

    private static String normalizeBizRef(String bizRef)
    {
        return StringUtils.isEmpty(bizRef) ? null : bizRef.trim();
    }

    private static String formatMoney(BigDecimal amount)
    {
        return amount.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }

    private static String truncate(String text, int max)
    {
        if (text == null)
        {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max);
    }

    private static String firstNonBlank(String primary, String fallback)
    {
        return StringUtils.isNotEmpty(primary) ? primary : fallback;
    }

    private static String buildFormBody(Map<String, String> params)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet())
        {
            if (!first)
            {
                sb.append('&');
            }
            sb.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
            first = false;
        }
        return sb.toString();
    }

    private static String urlEncode(String value)
    {
        try
        {
            return URLEncoder.encode(StringUtils.defaultString(value, ""), StandardCharsets.UTF_8.name());
        }
        catch (Exception ex)
        {
            return value;
        }
    }

    private static final class ProductQuote
    {
        private final String productName;
        private final BigDecimal price;

        private ProductQuote(String productName, BigDecimal price)
        {
            this.productName = productName;
            this.price = price == null ? BigDecimal.ZERO : price;
        }
    }
}
