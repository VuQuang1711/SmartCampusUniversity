package com.example.sis.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ELearningService {

    public static class CloItem {
        public final String code;
        public final String description;
        public final String ploMapping;
        public final String level;

        public CloItem(String code, String description, String ploMapping, String level) {
            this.code = code;
            this.description = description;
            this.ploMapping = ploMapping;
            this.level = level;
        }
    }

    public static class ChapterItem {
        public final int number;
        public final String title;
        public final String duration;
        public final String summary;

        public ChapterItem(int number, String title, String duration, String summary) {
            this.number = number;
            this.title = title;
            this.duration = duration;
            this.summary = summary;
        }
    }

    public static class ELearningCourse {
        public final String slug;
        public final String code;
        public final String title;
        public final String englishTitle;
        public final String category;      // DAI_CUONG, CO_SO_NGANH, CHUYEN_NGANH, CONG_NGHE
        public final String categoryLabel;
        public final int credits;
        public final String creditSplit;
        public final String lecturer;
        public final String level;
        public final String duration;
        public final int enrolledCount;
        public final String prerequisite;
        public final String shortDesc;
        public final String fullDesc;
        public final String accentFrom;
        public final String accentTo;
        public final String iconKey;
        public final List<String> tracks;  // analyst, ml, bigdata, dev
        public final List<String> skills;
        public final List<CloItem> clos;
        public final List<ChapterItem> chapters;
        public final List<String> textbooks;

        public ELearningCourse(String slug, String code, String title, String englishTitle,
                               String category, String categoryLabel, int credits, String creditSplit,
                               String lecturer, String level, String duration, int enrolledCount,
                               String prerequisite, String shortDesc, String fullDesc,
                               String accentFrom, String accentTo, String iconKey,
                               List<String> tracks, List<String> skills, List<CloItem> clos,
                               List<ChapterItem> chapters, List<String> textbooks) {
            this.slug = slug;
            this.code = code;
            this.title = title;
            this.englishTitle = englishTitle;
            this.category = category;
            this.categoryLabel = categoryLabel;
            this.credits = credits;
            this.creditSplit = creditSplit;
            this.lecturer = lecturer;
            this.level = level;
            this.duration = duration;
            this.enrolledCount = enrolledCount;
            this.prerequisite = prerequisite;
            this.shortDesc = shortDesc;
            this.fullDesc = fullDesc;
            this.accentFrom = accentFrom;
            this.accentTo = accentTo;
            this.iconKey = iconKey;
            this.tracks = tracks;
            this.skills = skills;
            this.clos = clos;
            this.chapters = chapters;
            this.textbooks = textbooks;
        }
    }

    private final List<ELearningCourse> courses;

    public ELearningService() {
        List<ELearningCourse> list = new ArrayList<>();

        // 1. Trực quan hóa dữ liệu
        list.add(new ELearningCourse(
                "truc-quan-hoa-du-lieu", "0101102572",
                "Trực quan hóa dữ liệu", "Data Analytics & Visualization",
                "CHUYEN_NGANH", "Chuyên ngành KHDL", 3, "1 LT + 2 TH",
                "ThS. Lê Văn Cường", "Trung cấp", "12 tuần • 45 tiết", 248,
                "Cơ sở dữ liệu, Nhập môn lập trình",
                "Thiết kế biểu đồ trực quan, dashboard tương tác và kể chuyện bằng dữ liệu với Python, Power BI và Tableau.",
                "Học phần trang bị cho sinh viên tư duy trực quan hóa dữ liệu hiện đại: từ quy trình thu thập, làm sạch dữ liệu đến lựa chọn loại biểu đồ tối ưu, thiết kế Dashboard BI chuyên nghiệp hỗ trợ ra quyết định quản trị tại doanh nghiệp.",
                "#0284C7", "#0EA5E9", "chart",
                List.of("analyst", "bigdata"),
                List.of("Data Storytelling", "Power BI", "Matplotlib & Seaborn", "Plotly Dash", "EDA", "Dashboard Design"),
                List.of(
                        new CloItem("CLO1", "Giải thích nguyên lý nhận thức thị giác và chọn đúng biểu đồ cho từng kiểu dữ liệu.", "PLO2.2", "C3"),
                        new CloItem("CLO2", "Tiền xử lý và khám phá dữ liệu (EDA) bằng Python (Pandas, Seaborn, Plotly).", "PLO3.1", "P3"),
                        new CloItem("CLO3", "Xây dựng Dashboard tương tác hoàn chỉnh trên Power BI/Tableau giải quyết bài toán kinh doanh.", "PLO3.2", "P3"),
                        new CloItem("CLO4", "Thuyết trình và truyền đạt insight dữ liệu rõ ràng, thuyết phục trước nhóm.", "PLO6, PLO7", "P2")
                ),
                List.of(
                        new ChapterItem(1, "Tổng quan & Nguyên lý thiết kế trực quan hóa", "6 tiết", "Tâm lý thị giác, bảng màu, tỷ lệ và các lỗi biểu đồ thường gặp."),
                        new ChapterItem(2, "Khám phá dữ liệu (EDA) với Pandas & Seaborn", "9 tiết", "Phân phối, tương quan, ngoại lai và trực quan hóa dữ liệu đa biến."),
                        new ChapterItem(3, "Biểu đồ tương tác với Plotly & Web Dashboard", "9 tiết", "Tạo biểu đồ động, bộ lọc thời gian thực và bản đồ địa lý."),
                        new ChapterItem(4, "Xây dựng báo cáo quản trị trên Power BI", "12 tiết", "Mô hình sao (Star Schema), DAX cơ bản và xuất bản báo cáo."),
                        new ChapterItem(5, "Đồ án Data Storytelling doanh nghiệp", "9 tiết", "Phân tích bộ dữ liệu thực tế và bảo vệ Dashboard trước hội đồng.")
                ),
                List.of("Wes McKinney (2022) — Python for Data Analysis, O'Reilly", "Cole Nussbaumer Knaflic — Storytelling with Data, Wiley")
        ));

        // 2. Xác suất và thống kê cho KHDL
        list.add(new ELearningCourse(
                "xac-suat-thong-ke-khdl", "0101102552",
                "Xác suất và Thống kê cho KHDL", "Probability & Statistics for Data Science",
                "DAI_CUONG", "Đại cương & Toán", 3, "3 LT + 0 TH",
                "TS. Trần Thị Bình", "Cơ bản", "15 tuần • 45 tiết", 312,
                "Giải tích (0101100933)",
                "Nền tảng xác suất, phân phối, ước lượng, kiểm định giả thuyết và hồi quy ứng dụng trực tiếp trong phân tích dữ liệu.",
                "Học phần cung cấp hệ thống lý thuyết và công cụ tính toán về không gian xác suất, biến ngẫu nhiên, các quy luật phân phối xác suất, ước lượng khoảng tin cậy, kiểm định giả thuyết thống kê và phân tích tương quan hồi quy phục vụ trực tiếp cho Học máy và Khoa học dữ liệu.",
                "#2563EB", "#4F46E5", "stats",
                List.of("analyst", "ml"),
                List.of("Lý thuyết xác suất", "Kiểm định giả thuyết", "A/B Testing", "Hồi quy tuyến tính", "Thống kê suy luận", "R / Python SciPy"),
                List.of(
                        new CloItem("CLO1", "Tính toán xác suất biến cố, xác suất có điều kiện và áp dụng định lý Bayes.", "PLO1.1", "C3"),
                        new CloItem("CLO2", "Nhận diện và mô hình hóa dữ liệu bằng các phân phối rời rạc & liên tục chuẩn.", "PLO1.1", "C3"),
                        new CloItem("CLO3", "Thực hiện ước lượng khoảng tin cậy và kiểm định giả thuyết thống kê (t-test, ANOVA, Chi-square).", "PLO4", "P3"),
                        new CloItem("CLO4", "Vận dụng phân tích tương quan, hồi quy để giải quyết bài toán thực tiễn và trình bày nhóm.", "PLO6", "P3")
                ),
                List.of(
                        new ChapterItem(1, "Không gian xác suất & Định lý Bayes", "9 tiết", "Biến cố, công thức xác suất đầy đủ, Bayes và ứng dụng phân loại."),
                        new ChapterItem(2, "Biến ngẫu nhiên & Các quy luật phân phối", "9 tiết", "Nhị thức, Poisson, Chuẩn (Gaussian), Student và phân phối mẫu."),
                        new ChapterItem(3, "Thống kê mô tả & Ước lượng tham số", "9 tiết", "Ước lượng điểm, khoảng tin cậy cho trung bình và tỷ lệ."),
                        new ChapterItem(4, "Kiểm định giả thuyết & A/B Testing", "9 tiết", "Sai lầm loại I/II, p-value, kiểm định tham số và phi tham số."),
                        new ChapterItem(5, "Phân tích tương quan & Hồi quy tuyến tính", "9 tiết", "Hệ số tương quan Pearson, mô hình hồi quy và đánh giá độ phù hợp.")
                ),
                List.of("Michael Baron (2014) — Probability and Statistics for Computer Scientists, CRC Press", "Trần Mạnh Tuấn — Xác suất & Thống kê: Lý thuyết và Thực hành")
        ));

        // 3. Nhập môn Lập trình & Python cho KHDL
        list.add(new ELearningCourse(
                "lap-trinh-python-khdl", "0101003472",
                "Lập trình Python cho Khoa học Dữ liệu", "Python Programming for Data Science",
                "CO_SO_NGANH", "Cơ sở ngành", 4, "3 LT + 1 TH",
                "PGS.TS Nguyễn Văn An", "Cơ bản", "15 tuần • 60 tiết", 415,
                "Không có (Học phần nền tảng)",
                "Làm chủ tư duy thuật toán, lập trình Python từ cơ bản đến thao tác mảng NumPy, bảng dữ liệu Pandas và Jupyter Notebook.",
                "Trang bị nền tảng tư duy lập trình hiện đại bằng ngôn ngữ Python: cấu trúc điều khiển, hàm, lập trình hướng đối tượng, xử lý tập tin, đồng thời thực hành thành thạo hệ sinh thái Khoa học dữ liệu với NumPy, Pandas và Jupyter Notebook.",
                "#0D9488", "#0284C7", "python",
                List.of("analyst", "ml", "bigdata", "dev"),
                List.of("Python 3", "NumPy", "Pandas", "Jupyter Notebook", "Thuật toán cơ bản", "Xử lý JSON/CSV/API"),
                List.of(
                        new CloItem("CLO1", "Hiểu rõ cú pháp, kiểu dữ liệu, cấu trúc điều khiển và tổ chức chương trình Python.", "PLO1.3", "C3"),
                        new CloItem("CLO2", "Cài đặt thuật toán xử lý danh sách, từ điển, tập hợp và đọc/ghi dữ liệu từ tập tin, API.", "PLO2.1", "C3"),
                        new CloItem("CLO3", "Sử dụng thành thạo NumPy và Pandas để làm sạch, biến đổi và tổng hợp tập dữ liệu thực tế.", "PLO3.1", "P3")
                ),
                List.of(
                        new ChapterItem(1, "Tư duy thuật toán & Căn bản Python", "10 tiết", "Biến, kiểu dữ liệu, biểu thức, rẽ nhánh và vòng lặp."),
                        new ChapterItem(2, "Hàm, Module & Cấu trúc dữ liệu tích hợp", "12 tiết", "List, Tuple, Dict, Set, List Comprehension và xử lý ngoại lệ."),
                        new ChapterItem(3, "Lập trình hướng đối tượng & Xử lý tập tin", "12 tiết", "Class, kế thừa, đọc ghi CSV/JSON và gọi REST API."),
                        new ChapterItem(4, "Tính toán ma trận với NumPy", "12 tiết", "Mảng đa chiều ndarray, Broadcasting và đại số tuyến tính nhanh."),
                        new ChapterItem(5, "Phân tích bảng dữ liệu với Pandas", "14 tiết", "DataFrame, GroupBy, Merge, xử lý dữ liệu khuyết và Mini-Project.")
                ),
                List.of("Jake VanderPlas — Python Data Science Handbook, O'Reilly", "Wes McKinney — Python for Data Analysis")
        ));

        // 4. Cấu trúc dữ liệu và Giải thuật
        list.add(new ELearningCourse(
                "cau-truc-du-lieu-giai-thuat", "0101101943",
                "Cấu trúc dữ liệu và Giải thuật", "Data Structures & Algorithms",
                "CO_SO_NGANH", "Cơ sở ngành", 3, "2 LT + 1 TH",
                "PGS.TS Nguyễn Văn An", "Trung cấp", "15 tuần • 45 tiết", 289,
                "Nhập môn lập trình (0101003472)",
                "Tối ưu độ phức tạp thuật toán với danh sách liên kết, ngăn xếp, hàng đợi, cây tìm kiếm nhị phân, bảng băm và đồ thị.",
                "Học phần cốt lõi giúp sinh viên nắm vững cách tổ chức dữ liệu trong bộ nhớ và thiết kế các giải thuật hiệu quả (tìm kiếm, sắp xếp, cây cân bằng, bảng băm, đồ thị), đánh giá độ phức tạp thời gian/không gian bằng ký hiệu Big-O.",
                "#4F46E5", "#7C3AED", "algo",
                List.of("ml", "dev", "bigdata"),
                List.of("Độ phức tạp Big-O", "Linked List / Stack / Queue", "Binary Search Tree", "Hash Table", "Graph Algorithms", "Sorting & Searching"),
                List.of(
                        new CloItem("CLO1", "Phân tích và đánh giá độ phức tạp thời gian, không gian của giải thuật.", "PLO2.1", "C3"),
                        new CloItem("CLO2", "Cài đặt và vận dụng danh sách liên kết, ngăn xếp, hàng đợi, bảng băm vào bài toán cụ thể.", "PLO3.1", "P3"),
                        new CloItem("CLO3", "Thiết kế giải thuật trên cấu trúc cây (BST, AVL, Heap) và đồ thị (BFS, DFS, Dijkstra).", "PLO3.2", "P3"),
                        new CloItem("CLO4", "Phối hợp nhóm hoàn thiện bài toán tối ưu hiệu năng truy vấn.", "PLO6", "P2")
                ),
                List.of(
                        new ChapterItem(1, "Độ phức tạp thuật toán & Đệ quy", "6 tiết", "Phân tích Big-O, Omega, Theta và giải thuật chia để trị."),
                        new ChapterItem(2, "Giải thuật Tìm kiếm & Sắp xếp nâng cao", "9 tiết", "QuickSort, MergeSort, HeapSort và tìm kiếm nhị phân."),
                        new ChapterItem(3, "Danh sách liên kết, Ngăn xếp & Hàng đợi", "9 tiết", "Cài đặt và ứng dụng trong trình biên dịch, lập lịch."),
                        new ChapterItem(4, "Cấu trúc Cây & Bảng băm (Hash Table)", "12 tiết", "Cây nhị phân tìm kiếm, cây cân bằng AVL, xử lý đụng độ băm."),
                        new ChapterItem(5, "Đồ thị & Đường đi ngắn nhất", "9 tiết", "Biểu diễn đồ thị, duyệt BFS/DFS, Dijkstra và cây khung nhỏ nhất.")
                ),
                List.of("Đỗ Văn Nhơn, Trịnh Quốc Sơn (2015) — Giáo trình Cấu trúc dữ liệu và Giải thuật, NXB ĐHQG-HCM", "Thomas H. Cormen — Introduction to Algorithms (CLRS)")
        ));

        // 5. Cơ sở dữ liệu & SQL
        list.add(new ELearningCourse(
                "co-so-du-lieu-sql", "0101000605",
                "Cơ sở dữ liệu & SQL", "Fundamentals of Database Systems",
                "CO_SO_NGANH", "Cơ sở ngành", 4, "3 LT + 1 TH",
                "TS. Trần Thị Bình", "Cơ bản", "15 tuần • 60 tiết", 356,
                "Cấu trúc rời rạc (0101100986)",
                "Thiết kế mô hình thực thể kết hợp (ERD), chuẩn hóa 1NF–BCNF, viết truy vấn SQL nâng cao, Index, View và Stored Procedure.",
                "Cung cấp kiến thức nền tảng về hệ quản trị cơ sở dữ liệu quan hệ: thiết kế lược đồ ERD, ánh xạ sang mô hình quan hệ, lý thuyết chuẩn hóa dữ liệu, đại số quan hệ và thực hành thành thạo SQL (MySQL / SQL Server) phục vụ quản trị & phân tích dữ liệu.",
                "#0369A1", "#0891B2", "db",
                List.of("analyst", "bigdata", "dev"),
                List.of("Thiết kế ERD", "Chuẩn hóa 1NF-BCNF", "SQL Nâng Cao", "Window Functions", "Indexing & Tối ưu", "Transaction ACID"),
                List.of(
                        new CloItem("CLO1", "Phân tích nghiệp vụ thực tế và thiết kế mô hình ERD chuẩn hóa đến dạng 3NF/BCNF.", "PLO2.1", "C3"),
                        new CloItem("CLO2", "Viết truy vấn SQL phức tạp (JOIN, Subquery, CTE, Window Functions) trích xuất dữ liệu.", "PLO3.1", "P3"),
                        new CloItem("CLO3", "Xây dựng View, Trigger, Stored Procedure và đảm bảo toàn vẹn giao dịch (ACID).", "PLO3.2", "P2")
                ),
                List.of(
                        new ChapterItem(1, "Tổng quan Hệ quản trị CSDL & Mô hình ERD", "10 tiết", "Thực thể, thuộc tính, quan hệ và chuyển đổi ERD sang bảng."),
                        new ChapterItem(2, "Mô hình quan hệ & Đại số quan hệ", "10 tiết", "Khóa chính, khóa ngoại, ràng buộc toàn vẹn và phép toán quan hệ."),
                        new ChapterItem(3, "Ngôn ngữ truy vấn SQL cơ bản đến nâng cao", "16 tiết", "DDL, DML, JOIN, GROUP BY, CTE và Window Functions."),
                        new ChapterItem(4, "Phụ thuộc hàm & Chuẩn hóa lược đồ CSDL", "12 tiết", "Dạng chuẩn 1NF, 2NF, 3NF, BCNF và khử dư thừa dữ liệu."),
                        new ChapterItem(5, "Chỉ mục (Index), Giao dịch & Bảo mật CSDL", "12 tiết", "B-Tree Index, Transaction Isolation Levels và phân quyền.")
                ),
                List.of("Hector Garcia-Molina, Jeffrey D. Ullman — Database Systems: The Complete Book, Prentice Hall")
        ));

        // 6. Học máy thống kê (Machine Learning)
        list.add(new ELearningCourse(
                "hoc-may-thong-ke", "0101101036",
                "Học máy thống kê (Machine Learning)", "Statistical Machine Learning",
                "CHUYEN_NGANH", "Chuyên ngành cốt lõi", 4, "3 LT + 1 TH",
                "PGS.TS Nguyễn Văn An", "Nâng cao", "15 tuần • 60 tiết", 274,
                "CTDL & GT, Thống kê chuyên sâu",
                "Xây dựng và đánh giá mô hình học có giám sát, không giám sát (Regression, SVM, Random Forest, XGBoost) bằng Scikit-learn.",
                "Học phần cốt lõi của ngành Khoa học Dữ liệu: trang bị nguyên lý toán học và kỹ năng thực hành xây dựng mô hình dự báo từ dữ liệu (Hồi quy, Phân lớp, Phân cụm, Giảm chiều dữ liệu), xử lý mất cân bằng dữ liệu, chống quá khớp (Overfitting) và tối ưu siêu tham số.",
                "#7C3AED", "#2563EB", "ml",
                List.of("ml", "analyst"),
                List.of("Scikit-learn", "Supervised Learning", "XGBoost / Random Forest", "Clustering & PCA", "Cross-Validation", "Feature Engineering"),
                List.of(
                        new CloItem("CLO1", "Phân biệt và lựa chọn mô hình Học máy phù hợp cho bài toán hồi quy, phân lớp, phân cụm.", "PLO2.2", "C3"),
                        new CloItem("CLO2", "Xử lý các vấn đề thực tế: dữ liệu nhiễu, mất cân bằng lớp, đa cộng tuyến và Overfitting.", "PLO3.1", "P3"),
                        new CloItem("CLO3", "Triển khai pipeline huấn luyện, đánh giá (AUC-ROC, F1, RMSE) và tinh chỉnh mô hình với Scikit-learn.", "PLO3.2", "P3"),
                        new CloItem("CLO4", "Nghiên cứu tài liệu khoa học và trình bày kết quả thực nghiệm mô hình.", "PLO4, PLO7", "P3")
                ),
                List.of(
                        new ChapterItem(1, "Giới thiệu Học máy & Quy trình Pipeline dữ liệu", "8 tiết", "Bias-Variance tradeoff, chia tập Train/Val/Test và Cross-Validation."),
                        new ChapterItem(2, "Mô hình Hồi quy & Chính quy hóa (Ridge/Lasso)", "12 tiết", "Hồi quy tuyến tính, đa thức, Logistic Regression và đánh giá sai số."),
                        new ChapterItem(3, "Cây quyết định, SVM & Ensemble Learning", "16 tiết", "Decision Tree, Random Forest, Gradient Boosting, XGBoost & SVM."),
                        new ChapterItem(4, "Học không giám sát: Phân cụm & Giảm chiều", "12 tiết", "K-Means, DBSCAN, Hierarchical Clustering và PCA."),
                        new ChapterItem(5, "Đánh giá mô hình, Explainable AI & Đồ án môn học", "12 tiết", "SHAP values, tối ưu siêu tham số Grid/Optuna và triển khai demo.")
                ),
                List.of("Gareth James, Trevor Hastie et al. — An Introduction to Statistical Learning (ISLR), Springer", "Aurélien Géron — Hands-On Machine Learning with Scikit-Learn")
        ));

        // 7. Học sâu (Deep Learning)
        list.add(new ELearningCourse(
                "hoc-sau-deep-learning", "0101102560",
                "Học sâu (Deep Learning)", "Deep Learning for Data Science",
                "CHUYEN_NGANH", "Chuyên ngành KHDL", 3, "2 LT + 1 TH",
                "PGS.TS Nguyễn Văn An", "Nâng cao", "15 tuần • 45 tiết", 198,
                "Học máy (0101101036)",
                "Kiến trúc mạng nơ-ron sâu: MLP, CNN cho thị giác máy tính, RNN/LSTM, Transformer và huấn luyện thực chiến với PyTorch.",
                "Học phần chuyên sâu về biểu diễn dữ liệu phi cấu trúc (ảnh, văn bản, chuỗi thời gian) thông qua mạng nơ-ron sâu: thuật toán lan truyền ngược, tối ưu hóa Adam/SGD, mạng tích chập (CNN), mạng hồi quy (LSTM/GRU), cơ chế Attention và Transformer.",
                "#9333EA", "#EC4899", "dl",
                List.of("ml"),
                List.of("PyTorch", "Neural Networks", "CNN (ResNet/YOLO)", "RNN / LSTM", "Attention & Transformer", "Transfer Learning"),
                List.of(
                        new CloItem("CLO1", "Giải thích cơ chế lan truyền xuôi, lan truyền ngược (Backpropagation) và các thuật toán tối ưu.", "PLO2.2", "C3"),
                        new CloItem("CLO2", "Thiết kế và huấn luyện mạng CNN giải quyết bài toán phân loại và nhận diện ảnh.", "PLO3.2", "P3"),
                        new CloItem("CLO3", "Áp dụng Transfer Learning và Fine-tuning mô hình tiền huấn luyện (ResNet, BERT) trên PyTorch.", "PLO4", "P3")
                ),
                List.of(
                        new ChapterItem(1, "Mạng nơ-ron nhân tạo (MLP) & Backpropagation", "9 tiết", "Hàm kích hoạt, hàm mất mát, BatchNorm, Dropout và tối ưu hóa."),
                        new ChapterItem(2, "Mạng tích chập (CNN) & Thị giác máy tính", "12 tiết", "Convolution, Pooling, ResNet, Transfer Learning và Data Augmentation."),
                        new ChapterItem(3, "Mạng hồi quy (RNN, LSTM, GRU) cho dữ liệu chuỗi", "9 tiết", "Dự báo chuỗi thời gian và phân tích dữ liệu tuần tự."),
                        new ChapterItem(4, "Cơ chế Attention & Kiến trúc Transformer", "9 tiết", "Self-Attention, Encoder-Decoder và giới thiệu mô hình ngôn ngữ lớn."),
                        new ChapterItem(5, "Thực hành triển khai mô hình Deep Learning", "6 tiết", "Đóng gói mô hình ONNX/FastAPI và đánh giá hiệu năng GPU.")
                ),
                List.of("Ian Goodfellow, Yoshua Bengio — Deep Learning, MIT Press", "Aston Zhang et al. — Dive into Deep Learning (d2l.ai)")
        ));

        // 8. Phân tích dữ liệu lớn (Big Data)
        list.add(new ELearningCourse(
                "phan-tich-du-lieu-lon", "0101101086",
                "Phân tích dữ liệu lớn (Big Data)", "Big Data Analysis",
                "CHUYEN_NGANH", "Chuyên ngành KHDL", 3, "2 LT + 1 TH",
                "ThS. Lê Văn Cường", "Nâng cao", "15 tuần • 45 tiết", 214,
                "Cơ sở dữ liệu (0101000605)",
                "Kiến trúc lưu trữ và xử lý phân tán quy mô lớn với hệ sinh thái Hadoop HDFS, MapReduce, Apache Spark và PySpark.",
                "Học phần cung cấp bức tranh toàn cảnh và kỹ năng thực hành về kỹ thuật dữ liệu lớn (Big Data): đặc trưng 5V, hệ thống tệp phân tán HDFS, lập trình xử lý phân tán bằng Apache Spark (RDD, Spark SQL, Spark MLlib) và xây dựng pipeline ETL thời gian thực.",
                "#EA580C", "#F59E0B", "bigdata",
                List.of("bigdata", "dev"),
                List.of("Apache Spark", "PySpark", "Hadoop HDFS", "Spark SQL", "ETL Pipeline", "Streaming Kafka"),
                List.of(
                        new CloItem("CLO1", "Phân tích kiến trúc lưu trữ và xử lý song song/phân tán trên Hadoop và Apache Spark.", "PLO2.2", "C3"),
                        new CloItem("CLO2", "Lập trình xử lý tập dữ liệu lớn (hàng chục triệu dòng) bằng PySpark & Spark SQL.", "PLO3.2", "P3"),
                        new CloItem("CLO3", "Thiết kế luồng ETL và xây dựng ứng dụng phân tích Big Data trên dữ liệu doanh nghiệp.", "PLO6", "P3")
                ),
                List.of(
                        new ChapterItem(1, "Tổng quan Dữ liệu lớn (5V) & Hệ sinh thái Hadoop", "9 tiết", "Kiến trúc HDFS, YARN và mô hình tính toán phân tán MapReduce."),
                        new ChapterItem(2, "Lập trình phân tán với Apache Spark & PySpark", "12 tiết", "RDD, Transformation vs Action, Lazy Evaluation và Partitioning."),
                        new ChapterItem(3, "Spark SQL, DataFrame & Định dạng Parquet", "9 tiết", "Truy vấn phân tán hiệu năng cao và tối ưu Catalyst Optimizer."),
                        new ChapterItem(4, "Xử lý dữ liệu dòng (Spark Streaming) & MLlib", "9 tiết", "Phân tích dữ liệu thời gian thực và huấn luyện mô hình phân tán."),
                        new ChapterItem(5, "Đồ án xây dựng Data Lakehouse mini", "6 tiết", "Tích hợp thu thập, làm sạch và phân tích log quy mô lớn.")
                ),
                List.of("Holden Karau — Learning Spark, O'Reilly", "Jure Leskovec, Anand Rajaraman — Mining of Massive Datasets")
        ));

        // 9. Kỹ thuật phân tích & Khai thác dữ liệu
        list.add(new ELearningCourse(
                "ky-thuat-phan-tich-khai-thac-du-lieu", "0101102558",
                "Kỹ thuật phân tích & Khai thác dữ liệu", "Data Mining & Analytics Techniques",
                "CHUYEN_NGANH", "Chuyên ngành cốt lõi", 4, "2 LT + 2 TH",
                "TS. Trần Thị Bình", "Trung cấp", "15 tuần • 60 tiết", 263,
                "Cấu trúc dữ liệu & Giải thuật, Cấu trúc rời rạc",
                "Quy trình CRISP-DM, tiền xử lý dữ liệu đa nguồn, khai phá luật kết hợp (Apriori, FP-Growth), phát hiện bất thường và phân khúc khách hàng.",
                "Trang bị quy trình chuẩn công nghiệp CRISP-DM để khai phá tri thức từ kho dữ liệu doanh nghiệp: thu thập từ Web/API, làm sạch dữ liệu bẩn, khai phá tập phổ biến & luật kết hợp (Market Basket Analysis), phát hiện gian lận và phân khúc khách hàng RFM.",
                "#059669", "#0D9488", "mining",
                List.of("analyst", "ml", "bigdata"),
                List.of("CRISP-DM", "Data Cleaning", "Apriori & FP-Growth", "RFM Segmentation", "Anomaly Detection", "Web Scraping"),
                List.of(
                        new CloItem("CLO1", "Vận dụng quy trình CRISP-DM để xác định bài toán và chuẩn bị dữ liệu phân tích.", "PLO2.2", "C4"),
                        new CloItem("CLO2", "Cài đặt thuật toán khai phá luật kết hợp (Apriori, FP-Growth) cho hệ thống gợi ý bán lẻ.", "PLO3.1", "P3"),
                        new CloItem("CLO3", "Xây dựng mô hình phát hiện bất thường và phân khúc khách hàng phục vụ quyết định kinh doanh.", "PLO5, PLO9", "P3")
                ),
                List.of(
                        new ChapterItem(1, "Quy trình CRISP-DM & Thu thập dữ liệu đa nguồn", "12 tiết", "Hiểu bài toán doanh nghiệp, thu thập dữ liệu Web/API/DB."),
                        new ChapterItem(2, "Tiền xử lý, Làm sạch & Biến đổi đặc trưng", "12 tiết", "Xử lý giá trị thiếu, chuẩn hóa, mã hóa và giảm nhiễu dữ liệu."),
                        new ChapterItem(3, "Khai phá tập phổ biến & Luật kết hợp", "12 tiết", "Thuật toán Apriori, FP-Growth và phân tích giỏ hàng siêu thị."),
                        new ChapterItem(4, "Phân khúc khách hàng (RFM) & Phát hiện bất thường", "12 tiết", "Isolation Forest, LOF và ứng dụng chống gian lận giao dịch."),
                        new ChapterItem(5, "Thực hành dự án Khai thác dữ liệu thực tế", "12 tiết", "Trình bày báo cáo phân tích và đề xuất chiến lược kinh doanh.")
                ),
                List.of("Jiawei Han, Micheline Kamber — Data Mining: Concepts and Techniques", "Tan, Steinbach, Kumar — Introduction to Data Mining")
        ));

        // 10. Xử lý ngôn ngữ tự nhiên (NLP)
        list.add(new ELearningCourse(
                "xu-ly-ngon-ngu-tu-nhien", "0101102204",
                "Xử lý ngôn ngữ tự nhiên (NLP)", "Natural Language Processing",
                "CONG_NGHE", "Chuyên ngành tự chọn", 3, "2 LT + 1 TH",
                "ThS. Lê Văn Cường", "Nâng cao", "15 tuần • 45 tiết", 185,
                "Trí tuệ nhân tạo (0101102529)",
                "Biểu diễn văn bản tiếng Việt (TF-IDF, Word2Vec, PhoBERT), phân tích cảm xúc, trích xuất thực thể (NER) và ứng dụng LLM/RAG.",
                "Học phần giới thiệu từ nền tảng xử lý ngôn ngữ tự nhiên đến các mô hình ngôn ngữ hiện đại: tách từ tiếng Việt, biểu diễn vector từ (TF-IDF, Word2Vec), phân loại văn bản, phân tích cảm xúc mạng xã hội, nhận diện thực thể tên (NER) và ứng dụng mô hình Transformer / PhoBERT.",
                "#E11D48", "#F43F5E", "nlp",
                List.of("ml", "dev"),
                List.of("NLP Tiếng Việt", "TF-IDF & Word2Vec", "Sentiment Analysis", "PhoBERT / HuggingFace", "NER & Chatbot", "RAG Basics"),
                List.of(
                        new CloItem("CLO1", "Áp dụng các kỹ thuật tiền xử lý văn bản tiếng Việt (tách từ, chuẩn hóa, loại bỏ stopword).", "PLO2.2", "C4"),
                        new CloItem("CLO2", "Xây dựng mô hình biểu diễn văn bản và phân tích cảm xúc bình luận bằng Machine Learning/PhoBERT.", "PLO3.2", "P4"),
                        new CloItem("CLO3", "Triển khai bài toán trích xuất thông tin hoặc hỏi-đáp tự động trên tập văn bản chuyên ngành.", "PLO4, PLO6", "P3")
                ),
                List.of(
                        new ChapterItem(1, "Tiền xử lý văn bản & Đặc thù Tiếng Việt", "9 tiết", "Tokenization tiếng Việt (Underthesea/VnCoreNLP), Regex và chuẩn hóa."),
                        new ChapterItem(2, "Mô hình ngôn ngữ & Biểu diễn từ (TF-IDF, Word2Vec)", "9 tiết", "Bag-of-Words, N-gram, Word Embeddings và độ tương đồng ngữ nghĩa."),
                        new ChapterItem(3, "Phân loại văn bản & Phân tích cảm xúc", "9 tiết", "Sentiment Analysis trên đánh giá thương mại điện tử & mạng xã hội."),
                        new ChapterItem(4, "Transformer, PhoBERT & Nhận diện thực thể (NER)", "9 tiết", "Fine-tuning mô hình HuggingFace cho dữ liệu tiếng Việt."),
                        new ChapterItem(5, "Ứng dụng LLM & Hệ thống hỏi đáp tài liệu (RAG)", "9 tiết", "Vector search, xây dựng trợ lý tra cứu quy chế học vụ.")
                ),
                List.of("Daniel Jurafsky, James H. Martin — Speech and Language Processing (3rd Ed.)", "Lewis Tunstall et al. — Natural Language Processing with Transformers, O'Reilly")
        ));

        this.courses = Collections.unmodifiableList(list);
    }

    public List<ELearningCourse> getAllCourses() {
        return courses;
    }

    public Optional<ELearningCourse> findBySlug(String slug) {
        return courses.stream().filter(c -> c.slug.equalsIgnoreCase(slug)).findFirst();
    }

    public List<ELearningCourse> filterCourses(String category, String query, String track) {
        String q = query != null ? query.trim().toLowerCase() : "";
        return courses.stream().filter(c -> {
            boolean matchCat = (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category))
                    || c.category.equalsIgnoreCase(category);
            boolean matchTrack = (track == null || track.isBlank() || "ALL".equalsIgnoreCase(track))
                    || c.tracks.contains(track.toLowerCase());
            boolean matchQuery = q.isEmpty()
                    || c.title.toLowerCase().contains(q)
                    || c.englishTitle.toLowerCase().contains(q)
                    || c.code.toLowerCase().contains(q)
                    || c.skills.stream().anyMatch(s -> s.toLowerCase().contains(q));
            return matchCat && matchTrack && matchQuery;
        }).toList();
    }
}
