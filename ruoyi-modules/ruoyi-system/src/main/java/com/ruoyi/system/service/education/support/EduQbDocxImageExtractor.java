package com.ruoyi.system.service.education.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import com.ruoyi.common.core.utils.StringUtils;

/**
 * Extract embedded / anchored / VML images from DOCX paragraphs.
 * POI {@link XWPFRun#getEmbeddedPictures()} alone misses many real-world homework files.
 */
final class EduQbDocxImageExtractor
{
    private static final String NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String NS_A = "http://schemas.openxmlformats.org/drawingml/2006/main";
    private static final String NS_V = "urn:schemas-microsoft-com:vml";
    private static final String NS_PIC = "http://schemas.openxmlformats.org/drawingml/2006/picture";

    private EduQbDocxImageExtractor()
    {
    }

    static List<String> extractParagraphImages(XWPFParagraph paragraph, XWPFDocument document,
            EduQbDocxParseService.ImageCache imageCache) throws IOException
    {
        Set<String> relIds = new LinkedHashSet<>();
        for (XWPFRun run : paragraph.getRuns())
        {
            for (XWPFPicture picture : run.getEmbeddedPictures())
            {
                XWPFPictureData data = picture.getPictureData();
                if (data != null)
                {
                    relIds.add(data.getPackagePart().getPartName().getName());
                }
            }
            if (run.getCTR() != null)
            {
                collectRelIdsFromXml(run.getCTR(), relIds);
            }
        }
        CTP ctp = paragraph.getCTP();
        if (ctp != null)
        {
            collectRelIdsFromXml(ctp, relIds);
        }
        List<String> urls = new ArrayList<>();
        for (String relOrPart : relIds)
        {
            String url = resolveAndSave(relOrPart, document, imageCache);
            if (StringUtils.isNotEmpty(url) && !urls.contains(url))
            {
                urls.add(url);
            }
        }
        return urls;
    }

    static List<String> appendOrphanPictures(XWPFDocument document, EduQbDocxParseService.ImageCache imageCache)
            throws IOException
    {
        List<String> orphanUrls = new ArrayList<>();
        List<XWPFPictureData> all = document.getAllPictures();
        if (all == null)
        {
            return orphanUrls;
        }
        for (XWPFPictureData picData : all)
        {
            if (picData == null)
            {
                continue;
            }
            String partKey = picData.getPackagePart().getPartName().getName();
            if (imageCache.isReferenced(partKey))
            {
                continue;
            }
            String url = imageCache.savePicture(picData);
            if (StringUtils.isNotEmpty(url))
            {
                imageCache.markReferenced(partKey);
                orphanUrls.add(url);
            }
        }
        return orphanUrls;
    }

    private static String resolveAndSave(String relOrPart, XWPFDocument document,
            EduQbDocxParseService.ImageCache imageCache) throws IOException
    {
        if (relOrPart.startsWith("/"))
        {
            XWPFPictureData byPart = findPictureByPartName(document, relOrPart);
            if (byPart != null)
            {
                return imageCache.savePicture(byPart);
            }
            return null;
        }
        XWPFPictureData picData = document.getPictureDataByID(relOrPart);
        if (picData == null)
        {
            picData = findPictureByRelId(document, relOrPart);
        }
        if (picData == null)
        {
            return null;
        }
        return imageCache.savePicture(picData);
    }

    private static XWPFPictureData findPictureByRelId(XWPFDocument document, String relId)
    {
        List<XWPFPictureData> all = document.getAllPictures();
        if (all == null)
        {
            return null;
        }
        for (XWPFPictureData data : all)
        {
            if (data == null)
            {
                continue;
            }
            try
            {
                String id = document.getRelationId(data);
                if (relId.equals(id))
                {
                    return data;
                }
            }
            catch (Exception ignored)
            {
                // ignore relation lookup errors
            }
        }
        return null;
    }

    private static XWPFPictureData findPictureByPartName(XWPFDocument document, String partName)
    {
        List<XWPFPictureData> all = document.getAllPictures();
        if (all == null)
        {
            return null;
        }
        for (XWPFPictureData data : all)
        {
            if (data != null && partName.equals(data.getPackagePart().getPartName().getName()))
            {
                return data;
            }
        }
        return null;
    }

    private static void collectRelIdsFromXml(XmlObject xmlObject, Set<String> relIds)
    {
        if (xmlObject == null)
        {
            return;
        }
        XmlCursor cursor = xmlObject.newCursor();
        try
        {
            cursor.selectPath("declare namespace a='" + NS_A + "' "
                    + "declare namespace r='" + NS_R + "' "
                    + "declare namespace v='" + NS_V + "' "
                    + "declare namespace pic='" + NS_PIC + "' "
                    + ".//a:blip | .//v:imagedata | .//pic:blipFill");
            while (cursor.toNextSelection())
            {
                XmlObject selected = cursor.getObject();
                if (selected == null)
                {
                    continue;
                }
                Node node = selected.getDomNode();
                if (node == null)
                {
                    continue;
                }
                String embed = attr(node, NS_R, "embed");
                if (StringUtils.isEmpty(embed))
                {
                    embed = attr(node, NS_R, "id");
                }
                if (StringUtils.isNotEmpty(embed))
                {
                    relIds.add(embed);
                }
            }
        }
        finally
        {
            cursor.dispose();
        }
    }

    private static String attr(Node node, String ns, String local)
    {
        if (node == null)
        {
            return null;
        }
        NamedNodeMap attrs = node.getAttributes();
        if (attrs == null)
        {
            return null;
        }
        Node item = attrs.getNamedItemNS(ns, local);
        if (item == null && NS_R.equals(ns))
        {
            item = attrs.getNamedItem("r:" + local);
        }
        return item != null ? item.getNodeValue() : null;
    }
}
