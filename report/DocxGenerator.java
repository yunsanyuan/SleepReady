package report;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 直接构造 OOXML（.docx 是 zip + 一组固定 xml 文件）。
 *
 * 不依赖 POI 等第三方库。生成的 docx Word / WPS / Pages 都能打开。
 *
 * 段落 / 标题 / 列表 / 代码块 / 表格 / 分页符 全部支持。
 *
 * 用法：
 *   javac -encoding UTF-8 report/*.java
 *   java  -cp . report.DocxGenerator output.docx
 */
public final class DocxGenerator {

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "拾刻项目报告.docx";
        Path outPath = Paths.get(out).toAbsolutePath();
        System.out.println("Generating: " + outPath);
        new DocxGenerator().generate(outPath);
        long size = Files.size(outPath);
        System.out.println("Done. Size = " + size + " bytes");
    }

    public void generate(Path outFile) throws Exception {
        List<ReportContent.Block> blocks = ReportContent.build();
        StringBuilder body = new StringBuilder();
        for (ReportContent.Block b : blocks) {
            renderBlock(b, body);
        }
        // 末尾必须有一个 sectPr 段落定义页面
        body.append("<w:sectPr>")
            .append("<w:pgSz w:w=\"11906\" w:h=\"16838\"/>") // A4
            .append("<w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\" w:header=\"720\" w:footer=\"720\" w:gutter=\"0\"/>")
            .append("</w:sectPr>");

        String document = DOCUMENT_PREFIX + body.toString() + DOCUMENT_SUFFIX;

        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream(outFile.toFile()))) {
            put(zos, "[Content_Types].xml", CONTENT_TYPES);
            put(zos, "_rels/.rels", ROOT_RELS);
            put(zos, "word/_rels/document.xml.rels", DOC_RELS);
            put(zos, "word/document.xml", document);
            put(zos, "word/styles.xml", STYLES);
        }
    }

    private void renderBlock(ReportContent.Block b, StringBuilder out) {
        switch (b.type) {
            case "COVER":
                renderCover(out);
                break;
            case "H1":
                paragraph(out, "Heading1", b.text, false, false, false, true);
                break;
            case "H2":
                paragraph(out, "Heading2", b.text, false, false, false, false);
                break;
            case "H3":
                paragraph(out, "Heading3", b.text, false, false, false, false);
                break;
            case "P":
                paragraph(out, "BodyIndent", b.text, false, false, false, false);
                break;
            case "BUL":
                paragraph(out, "Bullet", "• " + b.text, false, false, false, false);
                break;
            case "CODE":
                renderCode(out, b.text);
                break;
            case "PB":
                pageBreak(out);
                break;
            case "TBL":
                renderTable(out, b.headers, b.rows);
                break;
        }
    }

    private void renderCover(StringBuilder out) {
        // 多个空行使封面居中
        for (int i = 0; i < 6; i++) emptyParagraph(out);
        centerLine(out, "拾刻 SleepReady", 36, true);
        centerLine(out, "Android 单机离线生活记录应用", 18, false);
        centerLine(out, "—— 项目设计与实现报告 ——", 14, false);
        for (int i = 0; i < 6; i++) emptyParagraph(out);
        centerLine(out, "技术栈：Kotlin / Java / Jetpack Compose / Material 3 / Room", 12, false);
        centerLine(out, "运行环境：Android 8.0 (API 26) 及以上", 12, false);
        centerLine(out, "开发工具：Android Studio + Gradle 8.2.2", 12, false);
        for (int i = 0; i < 2; i++) emptyParagraph(out);
        centerLine(out, "报告日期：2026 年", 12, false);
        pageBreak(out);
    }

    private void centerLine(StringBuilder out, String text, int sizePt, boolean bold) {
        out.append("<w:p><w:pPr>")
           .append("<w:jc w:val=\"center\"/>")
           .append("</w:pPr>")
           .append("<w:r><w:rPr>")
           .append("<w:rFonts w:ascii=\"Calibri\" w:hAnsi=\"Calibri\" w:eastAsia=\"Microsoft YaHei\"/>")
           .append("<w:sz w:val=\"").append(sizePt * 2).append("\"/>")
           .append("<w:szCs w:val=\"").append(sizePt * 2).append("\"/>");
        if (bold) out.append("<w:b/><w:bCs/>");
        out.append("</w:rPr><w:t xml:space=\"preserve\">").append(escape(text)).append("</w:t></w:r></w:p>");
    }

    private void emptyParagraph(StringBuilder out) {
        out.append("<w:p/>");
    }

    private void paragraph(StringBuilder out, String style, String text,
                           boolean italic, boolean codeFont, boolean bold,
                           boolean isH1) {
        out.append("<w:p><w:pPr><w:pStyle w:val=\"").append(style).append("\"/>");
        if (isH1) {
            out.append("<w:pageBreakBefore/>");
        }
        out.append("</w:pPr>");
        appendRun(out, text, italic, codeFont, bold);
        out.append("</w:p>");
    }

    private void appendRun(StringBuilder out, String text, boolean italic, boolean codeFont, boolean bold) {
        out.append("<w:r><w:rPr>");
        if (codeFont) {
            out.append("<w:rFonts w:ascii=\"Consolas\" w:hAnsi=\"Consolas\" w:eastAsia=\"Microsoft YaHei\"/>");
            out.append("<w:sz w:val=\"18\"/><w:szCs w:val=\"18\"/>");
        } else {
            out.append("<w:rFonts w:ascii=\"Calibri\" w:hAnsi=\"Calibri\" w:eastAsia=\"Microsoft YaHei\"/>");
        }
        if (italic) out.append("<w:i/><w:iCs/>");
        if (bold) out.append("<w:b/><w:bCs/>");
        out.append("</w:rPr><w:t xml:space=\"preserve\">").append(escape(text)).append("</w:t></w:r>");
    }

    private void renderCode(StringBuilder out, String text) {
        // 多行：每行一个 paragraph，避免 Word 把 \n 吞掉
        String[] lines = text.split("\n", -1);
        for (String line : lines) {
            out.append("<w:p><w:pPr><w:pStyle w:val=\"Code\"/></w:pPr>");
            appendRun(out, line, false, true, false);
            out.append("</w:p>");
        }
    }

    private void pageBreak(StringBuilder out) {
        out.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>");
    }

    private void renderTable(StringBuilder out, List<String> headers, List<List<String>> rows) {
        out.append("<w:tbl>");
        // tblPr：边框 + 100% 宽度
        out.append("<w:tblPr>")
           .append("<w:tblW w:w=\"5000\" w:type=\"pct\"/>")
           .append("<w:tblBorders>")
           .append("<w:top w:val=\"single\" w:sz=\"4\" w:color=\"888888\"/>")
           .append("<w:left w:val=\"single\" w:sz=\"4\" w:color=\"888888\"/>")
           .append("<w:bottom w:val=\"single\" w:sz=\"4\" w:color=\"888888\"/>")
           .append("<w:right w:val=\"single\" w:sz=\"4\" w:color=\"888888\"/>")
           .append("<w:insideH w:val=\"single\" w:sz=\"4\" w:color=\"BBBBBB\"/>")
           .append("<w:insideV w:val=\"single\" w:sz=\"4\" w:color=\"BBBBBB\"/>")
           .append("</w:tblBorders>")
           .append("<w:tblLook w:val=\"04A0\"/>")
           .append("</w:tblPr>");
        // tblGrid
        out.append("<w:tblGrid>");
        for (int i = 0; i < headers.size(); i++) {
            out.append("<w:gridCol w:w=\"2000\"/>");
        }
        out.append("</w:tblGrid>");

        // header row
        appendTableRow(out, headers, true);
        for (List<String> row : rows) {
            appendTableRow(out, row, false);
        }
        out.append("</w:tbl>");
        // 表格后加空段落避免后续段落被吞
        emptyParagraph(out);
    }

    private void appendTableRow(StringBuilder out, List<String> cells, boolean isHeader) {
        out.append("<w:tr>");
        for (String cell : cells) {
            out.append("<w:tc><w:tcPr>");
            if (isHeader) {
                out.append("<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"E8E8E8\"/>");
            }
            out.append("</w:tcPr>");
            out.append("<w:p><w:pPr><w:pStyle w:val=\"Cell\"/></w:pPr>");
            appendRun(out, cell == null ? "" : cell, false, false, isHeader);
            out.append("</w:p></w:tc>");
        }
        out.append("</w:tr>");
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '&': sb.append("&amp;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&apos;"); break;
                default:
                    if (c < 0x20 && c != '\t') {
                        // 控制字符跳过
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static void put(ZipOutputStream zos, String name, String content) throws Exception {
        ZipEntry e = new ZipEntry(name);
        zos.putNextEntry(e);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        zos.write(bytes);
        zos.closeEntry();
    }

    // -- OOXML 静态部件 -------------------------------------------------------

    private static final String CONTENT_TYPES =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
        "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
        "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
        "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
        "<Override PartName=\"/word/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml\"/>" +
        "</Types>";

    private static final String ROOT_RELS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>" +
        "</Relationships>";

    private static final String DOC_RELS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
        "</Relationships>";

    private static final String DOCUMENT_PREFIX =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
        "<w:body>";

    private static final String DOCUMENT_SUFFIX = "</w:body></w:document>";

    /**
     * 样式定义。包含 Normal / Heading1 / Heading2 / Heading3 / BodyIndent / Bullet / Code / Cell。
     * 字号单位 half-point（即 24 = 12pt）。
     */
    private static final String STYLES =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
        // docDefaults
        "<w:docDefaults><w:rPrDefault><w:rPr>" +
        "<w:rFonts w:ascii=\"Calibri\" w:hAnsi=\"Calibri\" w:eastAsia=\"Microsoft YaHei\" w:cs=\"Calibri\"/>" +
        "<w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/>" +
        "<w:lang w:val=\"zh-CN\" w:eastAsia=\"zh-CN\" w:bidi=\"ar-SA\"/>" +
        "</w:rPr></w:rPrDefault>" +
        "<w:pPrDefault><w:pPr><w:spacing w:before=\"40\" w:after=\"60\" w:line=\"360\" w:lineRule=\"auto\"/></w:pPr></w:pPrDefault>" +
        "</w:docDefaults>" +
        // Normal
        "<w:style w:type=\"paragraph\" w:default=\"1\" w:styleId=\"Normal\">" +
        "<w:name w:val=\"Normal\"/><w:qFormat/></w:style>" +
        // Heading 1（章）
        "<w:style w:type=\"paragraph\" w:styleId=\"Heading1\">" +
        "<w:name w:val=\"heading 1\"/><w:basedOn w:val=\"Normal\"/><w:next w:val=\"Normal\"/><w:qFormat/>" +
        "<w:pPr><w:keepNext/><w:spacing w:before=\"480\" w:after=\"240\"/><w:outlineLvl w:val=\"0\"/></w:pPr>" +
        "<w:rPr><w:b/><w:bCs/><w:sz w:val=\"44\"/><w:szCs w:val=\"44\"/><w:color w:val=\"222222\"/></w:rPr>" +
        "</w:style>" +
        // Heading 2（节）
        "<w:style w:type=\"paragraph\" w:styleId=\"Heading2\">" +
        "<w:name w:val=\"heading 2\"/><w:basedOn w:val=\"Normal\"/><w:next w:val=\"Normal\"/><w:qFormat/>" +
        "<w:pPr><w:keepNext/><w:spacing w:before=\"320\" w:after=\"160\"/><w:outlineLvl w:val=\"1\"/></w:pPr>" +
        "<w:rPr><w:b/><w:bCs/><w:sz w:val=\"32\"/><w:szCs w:val=\"32\"/><w:color w:val=\"3F51B5\"/></w:rPr>" +
        "</w:style>" +
        // Heading 3
        "<w:style w:type=\"paragraph\" w:styleId=\"Heading3\">" +
        "<w:name w:val=\"heading 3\"/><w:basedOn w:val=\"Normal\"/><w:next w:val=\"Normal\"/><w:qFormat/>" +
        "<w:pPr><w:keepNext/><w:spacing w:before=\"240\" w:after=\"120\"/><w:outlineLvl w:val=\"2\"/></w:pPr>" +
        "<w:rPr><w:b/><w:bCs/><w:sz w:val=\"26\"/><w:szCs w:val=\"26\"/><w:color w:val=\"555555\"/></w:rPr>" +
        "</w:style>" +
        // BodyIndent (首行缩进 2 字 = 480 twentieths)
        "<w:style w:type=\"paragraph\" w:styleId=\"BodyIndent\">" +
        "<w:name w:val=\"Body Indent\"/><w:basedOn w:val=\"Normal\"/><w:qFormat/>" +
        "<w:pPr><w:ind w:firstLineChars=\"200\" w:firstLine=\"480\"/></w:pPr>" +
        "<w:rPr><w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/></w:rPr>" +
        "</w:style>" +
        // Bullet
        "<w:style w:type=\"paragraph\" w:styleId=\"Bullet\">" +
        "<w:name w:val=\"Bullet\"/><w:basedOn w:val=\"Normal\"/><w:qFormat/>" +
        "<w:pPr><w:ind w:left=\"360\"/></w:pPr>" +
        "<w:rPr><w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/></w:rPr>" +
        "</w:style>" +
        // Code
        "<w:style w:type=\"paragraph\" w:styleId=\"Code\">" +
        "<w:name w:val=\"Code\"/><w:basedOn w:val=\"Normal\"/><w:qFormat/>" +
        "<w:pPr><w:spacing w:before=\"0\" w:after=\"0\" w:line=\"260\" w:lineRule=\"auto\"/><w:ind w:left=\"280\"/></w:pPr>" +
        "<w:rPr><w:rFonts w:ascii=\"Consolas\" w:hAnsi=\"Consolas\" w:cs=\"Consolas\"/><w:sz w:val=\"18\"/><w:szCs w:val=\"18\"/></w:rPr>" +
        "</w:style>" +
        // Cell
        "<w:style w:type=\"paragraph\" w:styleId=\"Cell\">" +
        "<w:name w:val=\"Cell\"/><w:basedOn w:val=\"Normal\"/><w:qFormat/>" +
        "<w:pPr><w:spacing w:before=\"40\" w:after=\"40\"/></w:pPr>" +
        "<w:rPr><w:sz w:val=\"20\"/><w:szCs w:val=\"20\"/></w:rPr>" +
        "</w:style>" +
        "</w:styles>";
}
