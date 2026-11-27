# SmartCampus University — Hệ thống Quản lý Đào tạo & Sinh viên Tín chỉ (HUIT SIS)

> Cổng thông tin quản lý đào tạo đại học theo học chế tín chỉ tích hợp **HUIT E-Learning**, **Đăng ký học phần thông minh theo tuần (T1–T30)** và **Trợ lý AI cá nhân Gấu HUIT (Gemini + RAG Quy chế 3344/QĐ-DCT)**.

Xây dựng theo kiến trúc **Spring Web MVC (Model – View – Controller)** kết hợp phân tầng **Service – Repository** (Constructor Injection chuẩn Spring Boot), cơ sở dữ liệu MySQL chuẩn hóa **3NF**.

---

## 1. Công nghệ sử dụng

| Thành phần | Công nghệ / Phiên bản |
| :--- | :--- |
| **Ngôn ngữ & Runtime** | Java 17 (LTS) |
| **Framework chính** | Spring Boot 3.5.14 (Spring Web MVC, Spring Data JPA, Validation) |
| **ORM & Database** | Hibernate 6, MySQL 8.0 / 9.7 (`university_sis` — chuẩn hóa 3NF) |
| **Giao diện (View)** | Thymeleaf, Thymeleaf Layout Dialect, Lucide Icons, Chart.js, Dark/Light Mode |
| **AI Chatbot & RAG** | Google Gemini API + Apache PDFBox 3.0.3 (trích xuất & tìm kiếm văn bản quy chế PDF) |
| **Xử lý Excel** | Apache POI 5.2.5 (`poi-ooxml`) — đồng bộ TKB nhiều học kỳ, xuất/nhập bảng điểm |
| **Tiện ích & Vận hành** | Lombok (`@RequiredArgsConstructor`), Cloudflare Tunnel tích hợp, tự giải phóng port `8081` |

---

## 2. Tính năng nổi bật

### 2.1. Xác thực & Phân quyền đa vai trò (RBAC)
- Giao diện đăng nhập hiện đại với **slideshow 6 ảnh khuôn viên HUIT**, chế độ Sáng/Tối và hỗ trợ đăng nhập bằng `MSSV / MSGV / Email`.
- Phân luồng 5 nhóm vai trò rõ ràng:
  - **Sinh viên (`SINH_VIEN`)**: Dashboard tổng quan, ĐKHP thông minh, TKB, bảng điểm, mô phỏng GPA, công nợ học phí, dịch vụ một cửa, E-Learning, Trợ lý AI Gấu HUIT.
  - **Giảng viên & Cố vấn học tập (`GIANG_VIEN`, `CO_VAN_HOC_TAP`)**: Lịch giảng dạy, điểm danh hàng loạt, nhập điểm, quản lý lớp danh nghĩa, đề xuất đổi lịch.
  - **Phòng Đào tạo / Kế toán / Quản trị (`DAO_TAO`, `KE_TOAN`, `ADMIN`)**: Quản trị người dùng, học kỳ, lớp học phần, lịch học, import TKB Excel, thu học phí/phiếu thu, xét tốt nghiệp, nhật ký hệ thống.

### 2.2. Đăng ký học phần thông minh & Kiểm soát xung đột theo từng tuần
- Khóa bi quan **`LockModeType.PESSIMISTIC_WRITE`** trên `LopHocPhan` chống tranh chấp sĩ số (race condition) khi đăng ký đồng thời.
- Kiểm tra tự động 5 ràng buộc: trạng thái học kỳ (`DANG_MO`), sĩ số tối đa, môn tiên quyết, giới hạn tín chỉ (25 TC bình thường / 18 TC khi cảnh báo học vụ), và **giao thoa lịch học chính xác theo tập tuần thực tế `T1–T30`** (bảng chuẩn hóa `lich_hoc_tuan`).
- Hỗ trợ **Sandbox Planner**, **xếp lịch tự động (Auto-Schedule)**, chia sẻ lịch và phân tích thời khóa biểu.
- Phân loại rõ học phần Lý thuyết / Thực hành, đăng ký Học mới (`HOC_MOI`), Học lại (`HOC_LAI`), Cải thiện (`CAI_THIEN`).

### 2.3. Trợ lý AI Sinh viên — Gấu HUIT (Gemini + RAG)
- Widget nổi tích hợp sẵn trên giao diện sinh viên (`ChatbotController`, `StudentChatbotService`, `RegulationKnowledgeService`).
- Kết hợp ngữ cảnh cá nhân hóa theo tài khoản đang đăng nhập (lịch học kỳ hiện tại, điểm số, CPA, tín chỉ tích lũy, công nợ học phí) với kho tri thức RAG trích xuất từ **Quy chế đào tạo đại học theo hệ thống tín chỉ (QĐ 3344/QĐ-DCT)** và tài liệu học tập PDF.
- Có chế độ fallback thông minh tra cứu trực tiếp CSDL & quy chế khi chưa cấu hình API key.

### 2.4. Cổng HUIT E-Learning
- Đăng nhập liền mạch (SSO) dành cho sinh viên tại `/ELearning`.
- Danh mục học phần chuyên ngành Khoa học Dữ liệu đầy đủ đề cương chi tiết, chuẩn đầu ra CLO/PLO, cấu trúc chương mục, giáo trình và **tích hợp trực tiếp tài liệu bài giảng/slide** theo từng môn học (`learn.html`).

### 2.5. Học vụ, Điểm danh, Học phí & Tốt nghiệp
- **Điểm danh & Tự động cấm thi**: vắng vượt ngưỡng $> 20\%$ tự động gắn cờ `camThi = true` và khóa điểm cuối kỳ `0.0`.
- **Tính điểm & Mô phỏng GPA**: tự động quy đổi thang 10 → điểm chữ → thang 4, giữ điểm cao nhất khi học lại/cải thiện, mô phỏng GPA mục tiêu.
- **Cảnh báo học vụ & Xét tốt nghiệp (Degree Audit)**: tự động kiểm tra đủ tín chỉ, CPA $\ge 2.00$, không nợ môn bắt buộc, chứng chỉ chuẩn đầu ra và hoàn thành học phí.
- **Tài chính học phí**: tính học phí động theo tín chỉ lý thuyết/thực hành, sinh phiếu thu (`PhieuThuHocPhi`) và cập nhật công nợ (`CongNoHocKy`).

---

## 3. Công thức nghiệp vụ cốt lõi

1. **Tỷ lệ vắng mặt** (cấm thi khi $r_{\text{vắng}} > 20\%$):
   $$r_{\text{vắng}} = \frac{\text{Số buổi / tiết vắng}}{\text{Tổng số buổi / tiết học phần}} \times 100\%$$

2. **Điểm tổng kết hệ 10** (với $w_{cc} + w_{qt} + w_{th} + w_{ck} = 1$):
   $$D_{10} = w_{cc}\cdot d_{cc} + w_{qt}\cdot d_{qt} + w_{th}\cdot d_{th} + w_{ck}\cdot d_{ck}$$

3. **Bảng quy đổi điểm chữ & thang 4**:

   | Thang 10 ($D_{10}$) | Điểm chữ | Thang 4 ($D_4$) | Xếp loại |
   | :--- | :---: | :---: | :--- |
   | 8.5 – 10.0 | A | 4.0 | Giỏi / Xuất sắc |
   | 8.0 – 8.4 | B+ | 3.5 | Khá giỏi |
   | 7.0 – 7.9 | B | 3.0 | Khá |
   | 6.5 – 6.9 | C+ | 2.5 | Trung bình khá |
   | 5.5 – 6.4 | C | 2.0 | Trung bình |
   | 5.0 – 5.4 | D+ | 1.5 | Trung bình yếu |
   | 4.0 – 4.9 | D | 1.0 | Yếu (Đạt) |
   | < 4.0 | F | 0.0 | Kém (Không đạt) |

4. **GPA / CPA tích lũy** (môn học lại/cải thiện chỉ giữ $D_4$ cao nhất):
   $$\text{GPA} = \frac{\sum_{i=1}^{M} c_i \cdot D_{4,i}}{\sum_{i=1}^{M} c_i}$$

5. **Học phí học phần tín chỉ**:
   $$\text{HP} = (c_{lt} \cdot P) + (c_{th} \cdot P \cdot k_{th})$$
   Trong đó $c_{lt}, c_{th}$ là số tín chỉ lý thuyết & thực hành, $P$ là đơn giá tín chỉ, $k_{th}$ là hệ số thực hành.

---

## 4. Kiến trúc & Cấu trúc mã nguồn

Dự án gồm **83 lớp Java** tổ chức gọn gàng theo chuẩn Spring Boot MVC + Service–Repository:

```text
src/main/java/com/example/sis/
├── SisApplication.java            # Entry point, tự động giải phóng port 8081 khi khởi động
├── config/                        # 2 lớp cấu hình (ThymeleafConfig, LecturerDataSyncRunner)
├── controller/                    # 9 Controller MVC & REST API
│   ├── AccountController.java     # Đăng nhập, đăng xuất, đổi mật khẩu
│   ├── HomeController.java        # Điều hướng theo vai trò
│   ├── StudentController.java     # Phân hệ Sinh viên (Dashboard, ĐKHP, TKB, Điểm, Học phí...)
│   ├── FacultyController.java     # Phân hệ Giảng viên & CVHT
│   ├── AdminController.java       # Phân hệ Quản trị, Đào tạo, Kế toán
│   ├── ELearningController.java   # Cổng HUIT E-Learning & xem tài liệu môn học
│   ├── ChatbotController.java     # API Trợ lý AI Gấu HUIT
│   ├── ApiController.java         # REST API biểu đồ, lịch học, mô phỏng GPA
│   └── GlobalControllerAdvice.java
├── service/                       # 16 Service nghiệp vụ (@RequiredArgsConstructor, @Transactional)
│   ├── CourseRegistrationService.java
│   ├── ScheduleConflictService.java
│   ├── StudentChatbotService.java
│   ├── RegulationKnowledgeService.java
│   ├── ClassDatabaseImportService.java
│   ├── ELearningService.java
│   ├── AttendanceService.java
│   ├── GradingService.java
│   ├── GpaSimulatorService.java
│   ├── AcademicStandingService.java
│   ├── TuitionService.java
│   ├── ExcelService.java
│   ├── FileStorageService.java
│   ├── NotificationService.java
│   ├── AuditLogService.java
│   └── CloudflareTunnelService.java
├── entity/                        # 28 Entity JPA chuẩn hóa 3NF (bao gồm CoSo, PhongHoc, LichHocTuan)
└── repository/                    # 27 Spring Data JPA Repository

src/main/resources/
├── application.properties         # Cấu hình MySQL, port 8081, Gemini AI, Cloudflare Tunnel
├── static/                        # CSS themes, JS, logo & ảnh khuôn viên HUIT
└── templates/                     # Giao diện Thymeleaf (account, student, faculty, admin, elearning, fragments)

database/
├── init_schema_and_data.sql       # Khởi tạo schema & dữ liệu nền
├── seed_huit_khdl_and_receipts.sql# Dữ liệu mẫu ngành KHDL & phiếu thu
├── upgrade_v2_workflows.sql       # Nâng cấp nghiệp vụ v2
└── huit_normalized_schema.sql     # Lược đồ chuẩn hóa 3NF (co_so, phong_hoc, lich_hoc_tuan)
```

---

## 5. Hướng dẫn cài đặt & chạy dự án

### Bước 1: Yêu cầu môi trường
- **JDK 17+**
- **MySQL 8.0+** (mặc định cấu hình kết nối `localhost:3306/university_sis`, user `root`)

### Bước 2: Khởi tạo CSDL MySQL
```bash
mysql -u root -p < database/init_schema_and_data.sql
mysql -u root -p university_sis < database/seed_huit_khdl_and_receipts.sql
mysql -u root -p university_sis < database/huit_normalized_schema.sql
```
*(Tùy chọn)* Nếu bạn có dữ liệu TKB Excel thực tế và tài liệu PDF/E-Learning nội bộ, đặt vào thư mục `class_database/` ở gốc dự án (thư mục này đã được đưa vào `.gitignore` để không đẩy dữ liệu nội bộ lên Git).

### Bước 3: Cấu hình Gemini API Key (tùy chọn cho Chatbot AI)
**Không dán trực tiếp API key vào `application.properties` khi commit Git.** Hãy khai báo qua biến môi trường trên máy:

```powershell
# Windows PowerShell
$env:GEMINI_API_KEY="YOUR_GEMINI_API_KEY"
```

Nếu không cấu hình key, Gấu HUIT vẫn hoạt động bình thường ở chế độ tra cứu trực tiếp CSDL & quy chế nội bộ.

### Bước 4: Khởi chạy ứng dụng
```powershell
# Chạy ứng dụng (cổng mặc định: 8081)
.\mvnw.cmd spring-boot:run
```
Hoặc chạy trực tiếp `SisApplication.java` trong IDE (VS Code / IntelliJ IDEA).

Truy cập ứng dụng tại: **http://localhost:8081** (hoặc URL HTTPS công khai do `CloudflareTunnelService` in ra ở console nếu `app.cloudflare.tunnel.enabled=true`).

### Bước 5: Chạy kiểm thử tự động
```powershell
.\mvnw.cmd clean test
```

---

## 6. Tài khoản mẫu mặc định (khi nạp dữ liệu seed)

Mật khẩu mặc định: `123456`

| Vai trò | Mã đăng nhập | Chức năng chính |
| :--- | :--- | :--- |
| Sinh viên | `2180601234` | Dashboard, ĐKHP, TKB, Bảng điểm, Mô phỏng GPA, Học phí, E-Learning, AI Chatbot |
| Sinh viên (Cảnh báo) | `2180609999` | Kiểm thử giới hạn đăng ký tối đa 18 TC/kỳ |
| Giảng viên / CVHT | `GV001`, `GV002` | Lịch dạy, điểm danh tự động cấm thi, nhập điểm, quản lý lớp danh nghĩa |
| Phòng Đào tạo | `DAOTAO01` | Quản lý học kỳ, mở/khóa ĐKHP, xét tốt nghiệp |
| Phòng Kế toán | `KETOAN01` | Quản lý công nợ, thu học phí, xuất phiếu thu |
| Quản trị viên | `ADMIN01` | Toàn quyền quản trị hệ thống, người dùng, lịch học, nhật ký |

---

**Disclaimer:** Đây là dự án đồ án môn học phục vụ mục đích học tập và nghiên cứu, xây dựng độc lập lấy cảm hứng từ quy trình đào tạo tín chỉ đại học. Hệ thống không phải là cổng thông tin chính thức của nhà trường.
