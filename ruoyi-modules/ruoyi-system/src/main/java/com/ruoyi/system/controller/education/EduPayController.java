package com.ruoyi.system.controller.education;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.domain.education.EduPayAdminConfig;
import com.ruoyi.system.domain.education.EduPayCreateBody;
import com.ruoyi.system.domain.education.EduPayOrder;
import com.ruoyi.system.service.education.IEduPayConfigService;
import com.ruoyi.system.service.education.IEduPayService;

@RestController
@RequestMapping("/education/pay")
public class EduPayController extends BaseController
{
    @Autowired
    private IEduPayService payService;

    @Autowired
    private IEduPayConfigService payConfigService;

    @GetMapping("/check")
    public AjaxResult check(@RequestParam String bizType,
            @RequestParam(required = false) Long bizId,
            @RequestParam(required = false) String bizRef)
    {
        return success(payService.checkAccess(bizType, bizId, bizRef, safeUsername()));
    }

    @PostMapping("/order")
    public AjaxResult createOrder(@RequestBody EduPayCreateBody body, HttpServletRequest request)
    {
        String clientIp = resolveClientIp(request);
        return success(payService.createOrder(body, SecurityUtils.getUsername(), clientIp));
    }

    @GetMapping("/order/{orderNo}")
    public AjaxResult orderStatus(@PathVariable String orderNo)
    {
        return success(payService.getOrderStatus(orderNo, safeUsername()));
    }

    @GetMapping("/orders/mine")
    public TableDataInfo mineOrders(EduPayOrder query)
    {
        startPage();
        return getDataTable(payService.selectMyOrderList(query, SecurityUtils.getUsername()));
    }

    @GetMapping("/zpay/notify")
    public String zpayNotify(HttpServletRequest request)
    {
        Map<String, String> params = extractParams(request);
        return payService.handleZPayNotify(params);
    }

    @RequiresPermissions("education:library:edit")
    @GetMapping("/admin/settings")
    public AjaxResult adminSettings()
    {
        return success(payConfigService.loadAdminConfig());
    }

    @RequiresPermissions("education:library:edit")
    @PutMapping("/admin/settings")
    public AjaxResult saveAdminSettings(@RequestBody EduPayAdminConfig config)
    {
        payConfigService.saveAdminConfig(config);
        return success();
    }

    private String safeUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private static String resolveClientIp(HttpServletRequest request)
    {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty())
        {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static Map<String, String> extractParams(HttpServletRequest request)
    {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0)
            {
                params.put(key, values[0]);
            }
        });
        return params;
    }
}
