package com.example.sis.service;

import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kho tri thức RAG đọc trực tiếp các file quy định / quy chế đào tạo (.pdf, .txt, .md)
 * trong thư mục class_database/ (ví dụ: 3344-qd-dct-05092025-quy-che-dao-tao-dai-hoc-theo-he-thong-tin-chi.pdf).
 * Hỗ trợ tìm kiếm BM25/TF-IDF không dấu + khớp tiêu đề Điều/Khoản để truyền vào Gemini AI và chế độ trả lời cục bộ.
 */
@Service
public class RegulationKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(RegulationKnowledgeService.class);
    private static final Pattern ARTICLE_PATTERN = Pattern.compile("(?m)^(Điều\\s+\\d+\\..*)$");
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("(?m)^(Chương\\s+[IVXLC]+.*)$");
    private static final Set<String> STOP_WORDS = Set.of(
            "la", "va", "cua", "cac", "co", "duoc", "trong", "cho", "khi", "den", "tu",
            "voi", "tai", "theo", "ve", "nhu", "nao", "bao", "nhieu", "khong", "thi",
            "bi", "se", "da", "dang", "mot", "nhung", "nay", "do", "de", "hay", "hoac",
            "toi", "em", "minh", "ban", "hoi", "gi", "sao", "the", "a", "o", "i", "u"
    );

    private final List<DocChunk> chunks = new ArrayList<>();
    private final Map<String, Integer> docFreq = new HashMap<>();
    private final List<String> loadedFiles = new ArrayList<>();
    private double avgChunkLen = 100.0;

    @PostConstruct
    public void init() {
        reload();
    }

    public synchronized void reload() {
        chunks.clear();
        docFreq.clear();
        loadedFiles.clear();

        Path root = Paths.get("class_database");
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return;
        }

        try {
            List<Path> candidates = new ArrayList<>();
            Files.walk(root, 3)
                    .filter(Files::isRegularFile)
                    .forEach(p -> {
                        String low = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        if (low.endsWith(".pdf") || low.endsWith(".txt") || low.endsWith(".md")) {
                            candidates.add(p);
                        }
                    });

            candidates.sort(Comparator.comparing(p -> p.getFileName().toString()));

            for (Path p : candidates) {
                String name = p.getFileName().toString();
                String low = name.toLowerCase(Locale.ROOT);
                if (low.endsWith(".pdf")) {
                    indexPdfFile(p.toFile());
                } else {
                    String content = Files.readString(p, StandardCharsets.UTF_8);
                    if (content != null && content.trim().length() > 50) {
                        indexPlainText(name, content);
                        loadedFiles.add(name);
                    }
                }
            }

            // Tính document frequency & độ dài trung bình cho BM25
            long totalLen = 0;
            for (DocChunk ch : chunks) {
                totalLen += ch.totalTokens;
                for (String term : ch.termFreq.keySet()) {
                    docFreq.merge(term, 1, Integer::sum);
                }
            }
            if (!chunks.isEmpty()) {
                avgChunkLen = Math.max(1.0, (double) totalLen / chunks.size());
            }

            log.info("[RegulationKnowledgeService] Đã nạp {} tài liệu quy định ({} đoạn tri thức): {}",
                    loadedFiles.size(), chunks.size(), loadedFiles);
        } catch (Exception e) {
            log.warn("[RegulationKnowledgeService] Lỗi khi đọc tài liệu quy định: {}", e.getMessage());
        }
    }

    private void indexPdfFile(File file) {
        try (PDDocument doc = Loader.loadPDF(file)) {
            int numPages = doc.getNumberOfPages();
            if (numPages <= 0) return;

            PDFTextStripper stripper = new PDFTextStripper();
            // Kiểm tra nhanh 3 trang đầu xem PDF có text hay chỉ là file scan ảnh
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(3, numPages));
            String probe = stripper.getText(doc);
            if (probe == null || probe.trim().length() < 80) {
                return; // Bỏ qua PDF dạng ảnh scan không có text layer
            }

            String currentChapter = "";
            String currentArticle = "Quy định chung";
            int addedBefore = chunks.size();

            for (int page = 1; page <= numPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(doc);
                if (pageText == null) continue;
                pageText = cleanText(pageText);
                if (pageText.length() < 30) continue;

                // Tách theo các đoạn bắt đầu bằng "Điều X." trên trang
                List<SectionSlice> slices = splitByArticle(pageText, currentChapter, currentArticle);
                for (SectionSlice slice : slices) {
                    currentChapter = slice.chapter;
                    currentArticle = slice.article;
                    addChunkedText(file.getName(), page, currentChapter, currentArticle, slice.text);
                }
            }

            if (chunks.size() > addedBefore) {
                loadedFiles.add(file.getName() + " (" + numPages + " trang)");
            }
        } catch (Exception e) {
            log.warn("[RegulationKnowledgeService] Bỏ qua file PDF {}: {}", file.getName(), e.getMessage());
        }
    }

    private void indexPlainText(String fileName, String rawText) {
        String text = cleanText(rawText);
        List<SectionSlice> slices = splitByArticle(text, "", "Quy định");
        for (SectionSlice s : slices) {
            addChunkedText(fileName, 1, s.chapter, s.article, s.text);
        }
    }

    private List<SectionSlice> splitByArticle(String text, String initChapter, String initArticle) {
        List<SectionSlice> out = new ArrayList<>();
        String chapter = initChapter;
        String article = initArticle;

        String[] lines = text.split("\n");
        StringBuilder buf = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            Matcher cm = CHAPTER_PATTERN.matcher(line);
            if (cm.matches()) {
                if (buf.length() > 40) {
                    out.add(new SectionSlice(chapter, article, buf.toString().trim()));
                    buf.setLength(0);
                }
                chapter = line;
                continue;
            }

            Matcher am = ARTICLE_PATTERN.matcher(line);
            if (am.matches()) {
                if (buf.length() > 40) {
                    out.add(new SectionSlice(chapter, article, buf.toString().trim()));
                    buf.setLength(0);
                }
                article = line;
                buf.append(line).append("\n");
                continue;
            }

            buf.append(line).append("\n");
        }

        if (buf.length() > 20) {
            out.add(new SectionSlice(chapter, article, buf.toString().trim()));
        }
        return out;
    }

    private void addChunkedText(String sourceFile, int page, String chapter, String article, String text) {
        final int maxLen = 1150;
        final int overlap = 180;
        if (text.length() <= maxLen) {
            chunks.add(new DocChunk(sourceFile, page, chapter, article, text));
            return;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + maxLen);
            if (end < text.length()) {
                int lastBreak = text.lastIndexOf('\n', end);
                if (lastBreak > start + 400) end = lastBreak;
                else {
                    int lastDot = text.lastIndexOf(". ", end);
                    if (lastDot > start + 400) end = lastDot + 1;
                }
            }
            String part = text.substring(start, end).trim();
            if (!part.isEmpty()) {
                chunks.add(new DocChunk(sourceFile, page, chapter, article, part));
            }
            if (end >= text.length()) break;
            start = Math.max(start + 1, end - overlap);
        }
    }

    /**
     * Tìm top-K đoạn quy chế liên quan nhất tới câu hỏi bằng BM25 + boost khớp cụm từ & tiêu đề Điều.
     */
    public List<RetrievedChunk> retrieveRelevantChunks(String query, int topK) {
        if (chunks.isEmpty() || query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String normQuery = normalize(query);
        List<String> qTokens = tokenize(normQuery);
        if (qTokens.isEmpty()) return Collections.emptyList();

        // Mở rộng từ khóa đồng nghĩa học vụ phổ biến của sinh viên HUIT
        Set<String> expanded = new LinkedHashSet<>(qTokens);
        if (normQuery.contains("dkhp") || normQuery.contains("dang ky") || normQuery.contains("toi da") || normQuery.contains("toi thieu")) {
            expanded.add("dang"); expanded.add("ky"); expanded.add("khoi"); expanded.add("luong"); expanded.add("hoc"); expanded.add("tap");
        }
        if (normQuery.contains("rot mon") || normQuery.contains("hoc lai") || normQuery.contains("cai thien")) {
            expanded.add("hoc"); expanded.add("lai"); expanded.add("cai"); expanded.add("thien"); expanded.add("diem"); expanded.add("f");
        }
        if (normQuery.contains("canh bao") || normQuery.contains("thoi hoc") || normQuery.contains("duoi hoc")) {
            expanded.add("canh"); expanded.add("bao"); expanded.add("hoc"); expanded.add("vu"); expanded.add("thoi");
        }
        if (normQuery.contains("tot nghiep") || normQuery.contains("ra truong")) {
            expanded.add("tot"); expanded.add("nghiep"); expanded.add("xet"); expanded.add("bang"); expanded.add("chuan");
        }
        if (normQuery.contains("bao luu") || normQuery.contains("tam dung") || normQuery.contains("nghi hoc")) {
            expanded.add("tam"); expanded.add("dung"); expanded.add("tiep"); expanded.add("nhan");
        }
        if (normQuery.contains("rut") || normQuery.contains("huy mon") || normQuery.contains("huy hoc phan")) {
            expanded.add("rut"); expanded.add("bot"); expanded.add("hoc"); expanded.add("phan");
        }
        if (normQuery.contains("phuc khao") || normQuery.contains("khieu nai") || normQuery.contains("cam thi")) {
            expanded.add("phuc"); expanded.add("khao"); expanded.add("thi"); expanded.add("diem");
        }
        if (normQuery.contains("gpa") || normQuery.contains("cpa") || normQuery.contains("thang diem") || normQuery.contains("xep loai")) {
            expanded.add("trung"); expanded.add("binh"); expanded.add("chung"); expanded.add("he"); expanded.add("xep"); expanded.add("loai");
        }

        // Bigram khớp cụm từ 2 chữ liên tiếp
        List<String> bigrams = new ArrayList<>();
        for (int i = 0; i < qTokens.size() - 1; i++) {
            bigrams.add(qTokens.get(i) + " " + qTokens.get(i + 1));
        }

        int nDocs = chunks.size();
        double k1 = 1.5;
        double b = 0.75;

        List<RetrievedChunk> scored = new ArrayList<>();
        for (DocChunk ch : chunks) {
            double score = 0.0;
            int matchedOriginal = 0;

            for (String term : expanded) {
                int tf = ch.termFreq.getOrDefault(term, 0);
                if (tf == 0) continue;
                int df = docFreq.getOrDefault(term, 1);
                double idf = Math.log(1.0 + (nDocs - df + 0.5) / (df + 0.5));
                double denom = tf + k1 * (1.0 - b + b * (ch.totalTokens / avgChunkLen));
                double termScore = idf * ((tf * (k1 + 1.0)) / denom);

                // Ưu tiên cao hơn cho từ khóa gốc người dùng gõ
                if (qTokens.contains(term)) {
                    termScore *= 1.45;
                    matchedOriginal++;
                }
                // Ưu tiên thêm nếu xuất hiện trong tiêu đề Điều
                if (ch.normArticle.contains(term)) {
                    termScore *= 1.6;
                }
                score += termScore;
            }

            // Boost cụm từ 2 từ liên tiếp (bigram)
            for (String bg : bigrams) {
                if (ch.normContent.contains(bg)) {
                    score += 2.2;
                }
                if (ch.normArticle.contains(bg)) {
                    score += 3.5;
                }
            }

            // Nếu người dùng hỏi đích danh "Điều 21", "Điều 29"...
            Matcher dm = Pattern.compile("dieu\\s+(\\d+)").matcher(normQuery);
            if (dm.find()) {
                String targetArticle = "dieu " + dm.group(1) + ".";
                if (ch.normArticle.startsWith(targetArticle)) {
                    score += 25.0;
                }
            }

            if (score > 1.8 && (matchedOriginal > 0 || score > 5.0)) {
                scored.add(new RetrievedChunk(ch, score));
            }
        }

        scored.sort((a, b2) -> Double.compare(b2.score, a.score));
        if (scored.size() > topK) {
            return scored.subList(0, topK);
        }
        return scored;
    }

    public List<String> getLoadedFiles() {
        return Collections.unmodifiableList(loadedFiles);
    }

    public int getTotalChunks() {
        return chunks.size();
    }

    private static String cleanText(String text) {
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private static String normalize(String input) {
        if (input == null) return "";
        String nfd = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return nfd.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]+", " ").trim();
    }

    private static List<String> tokenize(String normText) {
        List<String> tokens = new ArrayList<>();
        for (String part : normText.split("\\s+")) {
            String t = part.replaceAll("^\\.+|\\.+$", "");
            if (t.length() >= 2 && !STOP_WORDS.contains(t)) {
                tokens.add(t);
            }
        }
        return tokens;
    }

    private static class SectionSlice {
        final String chapter;
        final String article;
        final String text;
        SectionSlice(String chapter, String article, String text) {
            this.chapter = chapter;
            this.article = article;
            this.text = text;
        }
    }

    public static class DocChunk {
        public final String sourceFile;
        public final int page;
        public final String chapter;
        public final String article;
        public final String content;
        final String normArticle;
        final String normContent;
        final Map<String, Integer> termFreq;
        final int totalTokens;

        DocChunk(String sourceFile, int page, String chapter, String article, String content) {
            this.sourceFile = sourceFile;
            this.page = page;
            this.chapter = chapter != null ? chapter : "";
            this.article = article != null ? article : "";
            this.content = content;
            this.normArticle = normalize(this.article);
            this.normContent = normalize(this.article + " " + this.content);
            List<String> toks = tokenize(this.normContent);
            this.totalTokens = Math.max(1, toks.size());
            Map<String, Integer> tf = new HashMap<>();
            for (String t : toks) tf.merge(t, 1, Integer::sum);
            this.termFreq = tf;
        }
    }

    public static class RetrievedChunk {
        public final DocChunk chunk;
        public final double score;
        RetrievedChunk(DocChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
