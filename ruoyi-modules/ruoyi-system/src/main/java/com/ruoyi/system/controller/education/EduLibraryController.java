package com.ruoyi.system.controller.education;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.domain.education.EduLibraryAuditBody;
import com.ruoyi.system.domain.education.EduLibraryCoverBody;
import com.ruoyi.system.domain.education.EduLibraryCategory;
import com.ruoyi.system.domain.education.EduLibraryDocument;
import com.ruoyi.system.domain.education.EduLibraryRecommendBody;
import com.ruoyi.system.domain.education.EduLibraryStatusBody;
import com.ruoyi.system.domain.education.EduLibraryAdminConfig;
import com.ruoyi.system.domain.education.EduLibraryReadProgressBody;
import com.ruoyi.system.service.education.IEduLibraryAdminConfigService;
import com.ruoyi.system.service.education.IEduLibraryCategoryService;
import com.ruoyi.system.service.education.IEduLibraryDocumentService;
import com.ruoyi.system.config.EduLibraryProperties;
import com.ruoyi.system.service.education.support.EduLibraryAdminHealthSupport;
import com.ruoyi.system.service.education.support.EduLibraryCoverGenerator;

@RestController
@RequestMapping("/education/library")
public class EduLibraryController extends BaseController
{
    @Autowired
    private IEduLibraryDocumentService documentService;

    @Autowired
    private IEduLibraryCategoryService categoryService;

    @Autowired
    private EduLibraryProperties libraryProperties;

    @Autowired
    private EduLibraryCoverGenerator coverGenerator;

    @Autowired
    private EduLibraryAdminHealthSupport adminHealthSupport;

    @Autowired
    private IEduLibraryAdminConfigService adminConfigService;

    @GetMapping("/document/{documentId}/related")
    public AjaxResult related(@PathVariable Long documentId)
    {
        return success(documentService.selectRelatedDocuments(documentId, safeUsername()));
    }

    @GetMapping("/reading/continue")
    public AjaxResult continueReading(@RequestParam(value = "limit", defaultValue = "5") int limit)
    {
        return success(documentService.selectContinueReadingList(SecurityUtils.getUsername(), limit));
    }

    @PostMapping("/document/{documentId}/progress")
    public AjaxResult saveProgress(@PathVariable Long documentId, @RequestBody(required = false) EduLibraryReadProgressBody body)
    {
        java.math.BigDecimal progress = body != null ? body.getReadProgress() : null;
        documentService.saveReadProgress(documentId, SecurityUtils.getUsername(), progress);
        return success();
    }

    @GetMapping("/document/{documentId}/download")
    public AjaxResult download(@PathVariable Long documentId)
    {
        String fileUrl = documentService.resolveDownloadUrl(documentId, safeUsername());
        String redirect = fileUrl;
        if (!fileUrl.startsWith("http"))
        {
            redirect = libraryProperties.getPreview().getFilePublicBaseUrl().replaceAll("/$", "") + fileUrl;
        }
        AjaxResult result = success();
        result.put("url", redirect);
        return result;
    }

    @GetMapping("/document/list")
    public TableDataInfo listDocuments(EduLibraryDocument query,
            @RequestParam(value = "portal", defaultValue = "false") boolean portal,
            @RequestParam(value = "orderBy", required = false) String orderBy)
    {
        if (query.getParams() == null)
        {
            query.setParams(new java.util.HashMap<>());
        }
        if (StringUtils.isNotEmpty(orderBy))
        {
            query.getParams().put("orderBy", orderBy);
        }
        String viewer = safeUsername();
        startPage();
        List<EduLibraryDocument> list = documentService.selectEduLibraryDocumentList(query, portal, viewer);
        return getDataTable(list);
    }

    @GetMapping("/document/mine")
    public TableDataInfo mine(EduLibraryDocument query)
    {
        startPage();
        List<EduLibraryDocument> list = documentService.selectMineList(query, SecurityUtils.getUsername());
        return getDataTable(list);
    }

    @GetMapping("/document/{documentId}")
    public AjaxResult getDocument(@PathVariable Long documentId)
    {
        return success(documentService.selectEduLibraryDocumentById(documentId, safeUsername()));
    }

    @GetMapping("/document/{documentId}/preview")
    public AjaxResult preview(@PathVariable Long documentId)
    {
        return success(documentService.buildPreviewPayload(documentId, safeUsername()));
    }

    @GetMapping("/document/{documentId}/preview-content")
    public void previewContent(@PathVariable Long documentId, HttpServletResponse response)
    {
        documentService.streamPreviewContent(documentId, safeUsername(), response);
    }

    @PostMapping("/document/{documentId}/view")
    public AjaxResult view(@PathVariable Long documentId)
    {
        documentService.recordView(documentId, safeUsername());
        return success();
    }

    @RequiresPermissions("education:library:add")
    @PostMapping("/document")
    public AjaxResult addDocument(@RequestBody EduLibraryDocument document)
    {
        int rows = documentService.insertEduLibraryDocument(document, SecurityUtils.getUsername());
        if (rows > 0)
        {
            return success(document.getDocumentId());
        }
        return toAjax(rows);
    }

    @PostMapping("/document/portal")
    public AjaxResult addPortalDocument(@RequestBody EduLibraryDocument document)
    {
        int rows = documentService.insertEduLibraryDocument(document, SecurityUtils.getUsername(), true);
        if (rows > 0)
        {
            AjaxResult result = success(document.getDocumentId());
            if (libraryProperties.getAudit().isEnabled())
            {
                result.put("auditPending", true);
            }
            return result;
        }
        return toAjax(rows);
    }

    @PostMapping("/document/cover/generate")
    public AjaxResult generateCover(@RequestBody EduLibraryCoverBody body)
    {
        if (body == null || StringUtils.isEmpty(body.getFileUrl()))
        {
            return error("fileUrl is required");
        }
        String coverUrl = coverGenerator.generateCover(body.getFileUrl(), body.getFileExt());
        if (StringUtils.isEmpty(coverUrl))
        {
            return error("Unable to generate cover from document");
        }
        return success(coverUrl);
    }

    @PutMapping("/document/portal")
    public AjaxResult editPortalDocument(@RequestBody EduLibraryDocument document)
    {
        return toAjax(documentService.updateEduLibraryDocument(document, SecurityUtils.getUsername(), false));
    }

    @RequiresPermissions("education:library:edit")
    @PutMapping("/document")
    public AjaxResult editDocument(@RequestBody EduLibraryDocument document)
    {
        return toAjax(documentService.updateEduLibraryDocument(document, SecurityUtils.getUsername(), true));
    }

    @RequiresPermissions("education:library:remove")
    @DeleteMapping("/document/{documentIds}")
    public AjaxResult removeDocument(@PathVariable Long[] documentIds)
    {
        return toAjax(documentService.deleteEduLibraryDocumentByIds(documentIds, SecurityUtils.getUsername(), true));
    }

    @RequiresPermissions("education:library:audit")
    @PostMapping("/document/recommend")
    public AjaxResult recommend(@RequestBody EduLibraryRecommendBody body)
    {
        return toAjax(documentService.recommendDocuments(body, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:library:edit")
    @PostMapping("/document/status")
    public AjaxResult changeStatus(@RequestBody EduLibraryStatusBody body)
    {
        return toAjax(documentService.changeDocumentStatus(body, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:library:edit")
    @PostMapping("/document/{documentId}/reconvert")
    public AjaxResult reconvert(@PathVariable Long documentId)
    {
        return toAjax(documentService.reconvertDocument(documentId, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("education:library:audit")
    @PostMapping("/document/audit")
    public AjaxResult audit(@RequestBody EduLibraryAuditBody body)
    {
        return toAjax(documentService.auditDocuments(body, SecurityUtils.getUsername()));
    }

    @PostMapping("/favorite/{documentId}")
    public AjaxResult favorite(@PathVariable Long documentId)
    {
        return toAjax(documentService.addFavorite(documentId, SecurityUtils.getUsername()));
    }

    @DeleteMapping("/favorite/{documentId}")
    public AjaxResult unfavorite(@PathVariable Long documentId)
    {
        return toAjax(documentService.removeFavorite(documentId, SecurityUtils.getUsername()));
    }

    @GetMapping("/favorite/list")
    public TableDataInfo favoriteList()
    {
        startPage();
        return getDataTable(documentService.selectFavoriteList(SecurityUtils.getUsername()));
    }

    @GetMapping("/category/list")
    public AjaxResult categoryList()
    {
        EduLibraryCategory query = new EduLibraryCategory();
        query.setStatus("0");
        return success(categoryService.selectEduLibraryCategoryList(query));
    }

    @RequiresPermissions("education:library:category")
    @GetMapping("/category/admin/list")
    public TableDataInfo adminCategoryList(EduLibraryCategory query)
    {
        startPage();
        return getDataTable(categoryService.selectEduLibraryCategoryList(query));
    }

    @RequiresPermissions("education:library:category")
    @GetMapping("/category/{categoryId}")
    public AjaxResult getCategory(@PathVariable Long categoryId)
    {
        return success(categoryService.selectEduLibraryCategoryById(categoryId));
    }

    @RequiresPermissions("education:library:category")
    @PostMapping("/category")
    public AjaxResult addCategory(@RequestBody EduLibraryCategory category)
    {
        return toAjax(categoryService.insertEduLibraryCategory(category));
    }

    @RequiresPermissions("education:library:category")
    @PutMapping("/category")
    public AjaxResult editCategory(@RequestBody EduLibraryCategory category)
    {
        return toAjax(categoryService.updateEduLibraryCategory(category));
    }

    @RequiresPermissions("education:library:category")
    @DeleteMapping("/category/{categoryIds}")
    public AjaxResult removeCategory(@PathVariable Long[] categoryIds)
    {
        return toAjax(categoryService.deleteEduLibraryCategoryByIds(categoryIds));
    }

    @RequiresPermissions("education:library:list")
    @GetMapping("/admin/health")
    public AjaxResult adminHealth()
    {
        return success(adminHealthSupport.buildHealthReport());
    }

    @RequiresPermissions("education:library:list")
    @GetMapping("/admin/settings")
    public AjaxResult adminSettings()
    {
        return success(adminConfigService.loadAdminConfig());
    }

    @RequiresPermissions("education:library:edit")
    @PutMapping("/admin/settings")
    public AjaxResult saveAdminSettings(@RequestBody EduLibraryAdminConfig config)
    {
        adminConfigService.saveAdminConfig(config);
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
}
