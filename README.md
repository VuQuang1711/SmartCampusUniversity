# Hệ thống Quản lý Đào tạo & Sinh viên Đại học theo Học chế Tín chỉ (University SIS & HUIT E-Learning)

Tài liệu này mô tả lại toàn bộ hệ thống quản lý đào tạo đại học theo học chế tín chỉ (SIS) tích hợp cổng học trực tuyến HUIT E-Learning theo đúng code hiện tại của nhóm, theo mô hình 3 lớp, có công thức nghiệp vụ và ví dụ thế số.

## 0. Mục tiêu bài toán

- Input: yêu cầu thao tác HTTP từ người dùng theo 5 vai trò (Sinh viên, Giảng viên / Cố vấn học tập, Phòng Đào tạo, Phòng Kế toán, Quản trị viên) cùng dữ liệu CSDL MySQL (`university_sis`).
- Output: trang giao diện web Thymeleaf, bảng điểm đa hệ, thời khóa biểu, biên lai học phí, báo cáo học vụ, file Excel và cổng học tập HUIT E-Learning.
- Nền tảng công nghệ: Java 17+, Spring Boot 3.5.14, Spring Data JPA (Hibernate 6), Thymeleaf, MySQL 9.7 / 8.0, Apache POI 5.2.5, Cloudflare Tunnel tích hợp.
- Kết quả kiểm thử E2E gần nhất: 175 / 175 kịch bản PASS (0 FAIL, 0 ERROR, 0 WARN) bao phủ toàn bộ phân luồng từ Guest/RBAC, Sinh viên, Giảng viên đến Admin và E-Learning.

## 1. Ý tưởng cốt lõi: từ dữ liệu học vụ đến hệ thống tín chỉ tự động

### 1.1. Vì sao cần kiểm soát chặt quy trình tín chỉ

Trong đào tạo tín chỉ, một thao tác của sinh viên hoặc giảng viên kéo theo nhiều ràng buộc liên thông:

- Đăng ký học phần phải kiểm tra sĩ số, môn tiên quyết, giới hạn tín chỉ và trùng lịch học.
- Điểm danh vắng vượt ngưỡng tự động kích hoạt cấm thi.
- Điểm học phần quyết định GPA tích lũy, mức cảnh báo học vụ và điều kiện xét tốt nghiệp.
- Tín chỉ đăng ký quyết định trực tiếp công nợ học phí của học kỳ.

Nếu xử lý rời rạc ở giao diện hoặc bằng câu lệnh thủ công sẽ dễ phát sinh lỗi tranh chấp sĩ số (race condition) hoặc sai lệch dữ liệu giữa học vụ và tài chính.

### 1.2. Cơ chế khóa và công thức chuẩn hóa

Hệ thống tách toàn bộ luật nghiệp vụ vào tầng Service:

- Dùng `LockModeType.PESSIMISTIC_WRITE` trên `LopHocPhan` để tránh vượt sĩ số khi nhiều sinh viên đăng ký cùng lúc.
- Quy đổi điểm tự động từ thang 10 sang điểm chữ và thang 4 theo chuẩn Bộ GD&ĐT.
- Tính GPA tích lũy theo quy tắc môn học lại / cải thiện chỉ giữ điểm cao nhất.
- Tính học phí động theo số tín chỉ lý thuyết, tín chỉ thực hành và hệ số thực hành.

### 1.3. Chuẩn hóa vận hành

- Tự động giải phóng cổng `8081` nếu có tiến trình cũ chiếm dụng trước khi khởi chạy Tomcat (`SisApplication.java`).
- Tự động khởi tạo và đóng đường hầm HTTPS công khai qua `CloudflareTunnelService.java` khi chạy/dừng ứng dụng.

## 2. Mô hình 3 lớp trong dự án

### 2.1. Tổng quan 3 lớp

1. Presentation Layer (điều hướng giao diện và HTTP): gói `com.example.sis.controller` và thư mục `src/main/resources/templates`.
2. Business/Service Layer (xử lý toàn bộ logic nghiệp vụ): gói `com.example.sis.service`.
3. Data Layer (ánh xạ CSDL và truy vấn): gói `com.example.sis.entity`, `com.example.sis.repository` và thư mục `database/`.

Luồng tổng quát:

1. Người dùng gửi request HTTP đến Controller tương ứng với vai trò.
2. Controller kiểm tra phiên đăng nhập (`HttpSession`) và quyền truy cập (RBAC).
3. Controller gọi các Service nghiệp vụ để kiểm tra ràng buộc và tính toán.
4. Service thao tác với MySQL thông qua Repository (Spring Data JPA).
5. Controller trả dữ liệu vào Model để Thymeleaf render giao diện HTML hoặc trả JSON qua `ApiController`.

### 2.2. Vai trò từng lớp

#### Presentation Layer (`controller/`, `templates/`)

- Tiếp nhận request từ trình duyệt và kiểm tra phân quyền theo vai trò (`VaiTro`).
- Ràng buộc dữ liệu đầu vào (kiểm tra trùng mã/email, số tiền thanh toán hợp lệ, điểm rèn luyện trong `[0, 100]`).
- Trả về giao diện Thymeleaf hoặc JSON cho biểu đồ và mô phỏng GPA.

#### Business/Service Layer (`service/`)

- Chứa 13 service chuyên biệt cho từng mảng nghiệp vụ: đăng ký học phần, kiểm tra trùng lịch, điểm danh, tính điểm, mô phỏng GPA, cảnh báo học vụ, học phí, E-Learning, xuất/nhập Excel, lưu file, thông báo, nhật ký hệ thống và Cloudflare Tunnel.
- Đảm bảo tính toàn vẹn giao dịch bằng `@Transactional`.

#### Data Layer (`entity/`, `repository/`, `database/`)

- Gồm 25 Entity và 24 Repository ánh xạ trực tiếp hệ thống bảng chuẩn 3NF trong MySQL (`university_sis`).
- Tích hợp các getter an toàn (null-safe) và chuẩn hóa hiển thị danh xưng giảng viên (`GiangVien.getTenDayDu()`).

## 3. Quy trình chi tiết: Đăng nhập -> ĐKHP -> Điểm danh -> Nhập điểm -> GPA -> Học phí -> E-Learning

### 3.1. Giai đoạn A: Xác thực và phân quyền (RBAC)

- Người dùng đăng nhập tại `/Account/Login` bằng `maDinhDanh` hoặc `email`.
- Sau khi xác thực, hệ thống lưu thông tin vào `HttpSession` và điều hướng theo `VaiTro`:
  - `SINH_VIEN` -> `/Student/Dashboard` (và được phép truy cập `/ELearning`).
  - `GIANG_VIEN` -> `/Faculty/Dashboard`.
  - `ADMIN`, `DAO_TAO`, `KE_TOAN` -> `/Admin/Dashboard`.
- Các vai trò khác `SINH_VIEN` bị chặn truy cập vào `/ELearning`.

### 3.2. Giai đoạn B: Đăng ký học phần (`CourseRegistrationService`)

Khi sinh viên bấm đăng ký một `LopHocPhan`, hệ thống khóa ghi bản ghi lớp học phần và kiểm tra tuần tự 5 điều kiện:

1. Học kỳ hiện tại phải đang mở đăng ký (`DANG_MO`).
2. Sĩ số hiện tại nhỏ hơn sĩ số tối đa: $n_{\text{hiện tại}} < n_{\text{tối đa}}$.
3. Đã đạt tất cả học phần tiên quyết (không có môn tiên quyết nào điểm `F` hoặc chưa học).
4. Tổng tín chỉ đăng ký trong kỳ không vượt ngưỡng cho phép:
   - Sinh viên bình thường: tối đa $25$ tín chỉ.
   - Sinh viên đang bị cảnh báo học vụ: tối đa $18$ tín chỉ.
5. Không trùng thời khóa biểu (so sánh thứ, tiết bắt đầu, tiết kết thúc và tuần học qua `ScheduleConflictService`).

Nếu hợp lệ, hệ thống tự phân loại đăng ký (`HOC_MOI`, `HOC_LAI`, `CAI_THIEN`), tăng sĩ số lớp và cập nhật công nợ học phí.

### 3.3. Giai đoạn C: Điểm danh và tự động khóa cấm thi (`AttendanceService`)

Tỷ lệ vắng mặt của sinh viên ở một học phần được tính theo công thức:

$$
r_{\text{vắng}} = \frac{\text{Tổng số tiết vắng}}{\text{Tổng số tiết học phần}} \times 100\%
$$

Quy tắc cấm thi:

- Nếu $r_{\text{vắng}} > 20\%$ thì hệ thống tự động bật cờ `camThi = true` và khóa điểm thi cuối kỳ bằng $0.0$.

Ví dụ thế số:

- Học phần có 3 tín chỉ lý thuyết, tổng số tiết là $45$ tiết.
- Sinh viên vắng $10$ tiết:

$$
r_{\text{vắng}} = \frac{10}{45} \times 100\% \approx 22.22\% > 20\%
$$

Kết quả: sinh viên bị đánh dấu cấm thi (`camThi = true`).

### 3.4. Giai đoạn D: Tính điểm tổng kết và GPA tích lũy (`GradingService`)

#### D1. Điểm tổng kết hệ 10

Mỗi học phần có bộ trọng số thành phần $(w_{cc}, w_{qt}, w_{th}, w_{ck})$ với tổng bằng $1$:

$$
D_{10} = w_{cc}\cdot d_{cc} + w_{qt}\cdot d_{qt} + w_{th}\cdot d_{th} + w_{ck}\cdot d_{ck}
$$

Ví dụ thế số:

- Trọng số học phần: chuyên cần $10\%$, quá trình $20\%$, thực hành $20\%$, cuối kỳ $50\%$.
- Điểm sinh viên: $d_{cc}=9.0,\ d_{qt}=8.0,\ d_{th}=8.5,\ d_{ck}=7.5$.

$$
D_{10} = 0.1\cdot 9.0 + 0.2\cdot 8.0 + 0.2\cdot 8.5 + 0.5\cdot 7.5 = 0.9 + 1.6 + 1.7 + 3.75 = 7.95
$$

#### D2. Bảng quy đổi sang điểm chữ và thang 4

| Điểm hệ 10 ($D_{10}$) | Điểm chữ | Điểm hệ 4 ($D_4$) | Xếp loại |
| :--- | :---: | :---: | :--- |
| 8.5 - 10.0 | A | 4.0 | Giỏi / Xuất sắc |
| 8.0 - 8.4 | B+ | 3.5 | Khá giỏi |
| 7.0 - 7.9 | B | 3.0 | Khá |
| 6.5 - 6.9 | C+ | 2.5 | Trung bình khá |
| 5.5 - 6.4 | C | 2.0 | Trung bình |
| 5.0 - 5.4 | D+ | 1.5 | Trung bình yếu |
| 4.0 - 4.9 | D | 1.0 | Yếu (Đạt) |
| < 4.0 | F | 0.0 | Kém (Không đạt) |

#### D3. Công thức tính GPA tích lũy

Với tập các học phần đã học (nếu học lại hoặc cải thiện thì chỉ lấy điểm $D_4$ cao nhất của học phần đó):

$$
\text{GPA} = \frac{\sum_{i=1}^{M} c_i \cdot D_{4,i}}{\sum_{i=1}^{M} c_i}
$$

Trong đó $c_i$ là số tín chỉ của học phần $i$, $D_{4,i}$ là điểm hệ 4 cao nhất của học phần $i$.

Ví dụ thế số (3 học phần):

- Môn 1: $3$ TC, điểm A ($4.0$)
- Môn 2: $3$ TC, điểm B ($3.0$)
- Môn 3: $4$ TC, điểm B+ ($3.5$)

$$
\text{GPA} = \frac{3\cdot 4.0 + 3\cdot 3.0 + 4\cdot 3.5}{3 + 3 + 4} = \frac{12.0 + 9.0 + 14.0}{10} = \frac{35.0}{10} = 3.50
$$

### 3.5. Giai đoạn E: Tính học phí tín chỉ và công nợ (`TuitionService`)

Công thức tính học phí cho một học phần:

$$
\text{HP} = (c_{lt} \cdot P) + (c_{th} \cdot P \cdot k_{th})
$$

- $c_{lt}$: số tín chỉ lý thuyết.
- $c_{th}$: số tín chỉ thực hành.
- $P$: đơn giá một tín chỉ.
- $k_{th}$: hệ số thực hành.

Ví dụ thế số:

- Học phần có $c_{lt} = 2$, $c_{th} = 1$, đơn giá $P = 800{,}000$ VNĐ/TC, hệ số thực hành $k_{th} = 1.2$:

$$
\text{HP} = (2 \cdot 800{,}000) + (1 \cdot 800{,}000 \cdot 1.2) = 1{,}600{,}000 + 960{,}000 = 2{,}560{,}000\text{ VNĐ}
$$

Khi sinh viên thanh toán trực tuyến, hệ thống kiểm tra $0 < \text{số tiền nộp} \le \text{còn nợ}$, ghi nhận `PhieuThuHocPhi` và cập nhật lại dư nợ `CongNoHocKy`.

### 3.6. Giai đoạn F: Cảnh báo học vụ, xét tốt nghiệp và HUIT E-Learning

- Cảnh báo học vụ (`AcademicStandingService`):
  - `GPA < 1.8`: Cảnh báo mức 1 (giới hạn 18 TC/kỳ).
  - `GPA < 1.5`: Cảnh báo mức 2.
  - `GPA < 1.0`: Buộc thôi học.
- Xét tốt nghiệp (Degree Audit): kiểm tra đủ tín chỉ khung CTĐT, `GPA >= 2.00`, không nợ môn bắt buộc, đủ chuẩn đầu ra (Ngoại ngữ, Tin học, GDTC, GDQP) và không còn nợ học phí.
- Cổng HUIT E-Learning (`ELearningService`, `ELearningController`): đăng nhập SSO từ cổng sinh viên, cung cấp 10 khóa học chuyên ngành Khoa học Dữ liệu kèm đề cương chi tiết, CLO/PLO, kỹ năng và giáo trình.

## 4. Phân tích các file code trong dự án

### 4.1. Nhóm file khởi chạy và cấu hình

1. `src/main/java/com/example/sis/SisApplication.java`
   - Entry point của ứng dụng Spring Boot.
   - Tích hợp `freePortIfOccupied(port)` tự động giải phóng cổng `8081` trước khi khởi chạy Tomcat.
2. `src/main/java/com/example/sis/config/ThymeleafConfig.java`
   - Cấu hình Thymeleaf Layout Dialect.
3. `src/main/resources/application.properties`
   - Cấu hình kết nối MySQL, cổng `8081`, JPA/Hibernate và upload file.

### 4.2. Nhóm Controller (Presentation Layer)

1. `AccountController.java`: đăng nhập, đăng xuất, đổi mật khẩu, quản lý session.
2. `HomeController.java`: điều hướng trang chủ theo vai trò.
3. `StudentController.java`: dashboard sinh viên, ĐKHP, thời khóa biểu, bảng điểm, chương trình đào tạo, học phí, rèn luyện, dịch vụ một cửa, mô phỏng GPA.
4. `FacultyController.java`: dashboard giảng viên, lịch dạy, điểm danh, nhập điểm, đề xuất đổi lịch.
5. `AdminController.java`: quản trị người dùng, học kỳ, lớp học phần, lịch học, duyệt đổi lịch, duyệt dịch vụ một cửa, thu học phí, xét tốt nghiệp.
6. `ELearningController.java`: cổng HUIT E-Learning dành riêng cho sinh viên (`/ELearning`).
7. `ApiController.java`: cung cấp REST API cho biểu đồ và mô phỏng GPA.
8. `GlobalControllerAdvice.java`: nạp dữ liệu dùng chung (thông tin user, số thông báo chưa đọc) cho mọi view.

### 4.3. Nhóm Service (Business Layer)

1. `CourseRegistrationService.java`: đăng ký/hủy học phần có Pessimistic Locking.
2. `ScheduleConflictService.java`: kiểm tra xung đột thời khóa biểu và phòng học.
3. `AttendanceService.java`: điểm danh hàng loạt và tự động khóa cấm thi.
4. `GradingService.java`: tính điểm tổng kết, quy đổi thang 4 và cập nhật GPA tích lũy.
5. `GpaSimulatorService.java`: mô phỏng GPA mục tiêu và gợi ý môn cải thiện.
6. `AcademicStandingService.java`: phân loại cảnh báo học vụ và xét tốt nghiệp tự động.
7. `TuitionService.java`: tính học phí, gạch nợ và xuất phiếu thu.
8. `ELearningService.java`: cung cấp dữ liệu 10 học phần Khoa học Dữ liệu cho cổng E-Learning.
9. `ExcelService.java`: xuất/nhập bảng điểm và danh sách sinh viên bằng Apache POI.
10. `FileStorageService.java`: quản lý lưu trữ tệp đính kèm.
11. `NotificationService.java`: gửi và đánh dấu thông báo người dùng.
12. `AuditLogService.java`: ghi nhật ký thao tác hệ thống.
13. `CloudflareTunnelService.java`: tự động khởi tạo và đóng đường hầm HTTPS công khai khi chạy/dừng app.

### 4.4. Nhóm Entity & Repository (Data Layer)

- Gồm 25 lớp Entity trong `com.example.sis.entity` và 24 interface Repository trong `com.example.sis.repository`, tương ứng với các nhóm bảng:
  - Tổ chức & Người dùng: `NguoiDung`, `VaiTro`, `Khoa`, `NganhHoc`, `ChuyenNganh`, `SinhVien`, `GiangVien`, `LopDanhNghia`.
  - Chương trình & Lớp học phần: `HocKy`, `HocPhan`, `DieuKienHocPhan`, `KhungChuongTrinh`, `LopHocPhan`, `LichHoc`, `DangKyHocPhan`.
  - Đánh giá & Học vụ: `DiemDanh`, `DiemHocPhan`, `DiemRenLuyen`, `ChungChiChuanDauRa`, `YeuCauDichVuMotCua`, `YeuCauDoiLich`.
  - Tài chính & Hệ thống: `CongNoHocKy`, `PhieuThuHocPhi`, `ThongBao`, `NhatKyHeThong`.

### 4.5. Nhóm giao diện và CSDL

- `src/main/resources/templates/`: chia theo phân hệ `account/`, `student/`, `faculty/`, `admin/`, `elearning/`, `layout/`.
- `src/main/resources/static/`: CSS hệ thống (Monochrome Luxury) và tài nguyên tĩnh.
- `database/init_schema_and_data.sql` và `database/seed_huit_khdl_and_receipts.sql`: khởi tạo schema MySQL và dữ liệu mẫu ngành Khoa học Dữ liệu HUIT.

## 5. Ví dụ luồng thực tế end-to-end

Giả sử sinh viên `2180601234` thực hiện một chu trình học kỳ:

1. Đăng nhập tại `/Account/Login` -> hệ thống tạo session `SINH_VIEN` và chuyển tới `/Student/Dashboard`.
2. Vào `/Student/Registration`, chọn lớp học phần mới -> `CourseRegistrationService` khóa lớp, kiểm tra 5 điều kiện và lưu `DangKyHocPhan`.
3. `TuitionService` tự động cộng học phí môn mới vào `CongNoHocKy`.
4. Sinh viên vào `/Student/Tuition`, thanh toán trực tuyến -> hệ thống sinh `PhieuThuHocPhi` và trừ dư nợ.
5. Giảng viên `GV001` đăng nhập `/Faculty/Dashboard`, thực hiện điểm danh và nhập điểm thành phần -> `GradingService` tính $D_{10}$, quy đổi $D_4$, cập nhật GPA tích lũy của sinh viên.
6. Sinh viên vào `/Student/Curriculum` xem tiến độ tích lũy và nhấn sang `/ELearning` (SSO) để xem đề cương chi tiết học phần.

## 6. Hướng dẫn chạy dự án và tài khoản thử nghiệm

### 6.1. Nạp CSDL MySQL

```bash
mysql -u root -p < database/init_schema_and_data.sql
mysql -u root -p university_sis < database/seed_huit_khdl_and_receipts.sql
```

### 6.2. Khởi chạy ứng dụng

- Cách 1 (IDE): mở `SisApplication.java` và bấm Run (tự động giải phóng port `8081` nếu bị chiếm).
- Cách 2 (Terminal Windows):

```powershell
.\mvnw.cmd spring-boot:run
```

Truy cập trình duyệt tại `http://localhost:8081` (hoặc link HTTPS công khai do Cloudflare Tunnel in ra ở console).

### 6.3. Tài khoản thử nghiệm (mật khẩu chung: `123456`)

| Vai trò | Mã đăng nhập | Email | Ghi chú kiểm thử |
| :--- | :--- | :--- | :--- |
| Sinh viên (Khá/Giỏi) | `2180601234` | `2180601234@sinhvien.sis.edu.vn` | ĐKHP, TKB, học phí, mô phỏng GPA, E-Learning |
| Sinh viên (Cảnh báo) | `2180609999` | `2180609999@sinhvien.sis.edu.vn` | Kiểm tra giới hạn tối đa 18 TC |
| Giảng viên | `GV001` | `gv.an@sis.edu.vn` | Lịch dạy, điểm danh, nhập điểm |
| Cố vấn học tập | `GV002` | `gv.binh@sis.edu.vn` | Theo dõi lớp danh nghĩa |
| Phòng Đào tạo | `DAOTAO01` | `daotao@sis.edu.vn` | Mở/đóng ĐKHP, xét tốt nghiệp |
| Quản trị viên | `ADMIN01` | `admin@sis.edu.vn` | Quản lý người dùng, lịch học, hệ thống |
| Phòng Kế toán | `KETOAN01` | `ketoan@sis.edu.vn` | Thu học phí, gạch nợ, in phiếu thu |

## 7. Gợi ý mở rộng

1. Tích hợp cổng thanh toán thực tế (VNPay / MoMo QR) thay cho bước xác nhận chuyển khoản mô phỏng.
2. Nâng cấp bảo mật mật khẩu bằng Spring Security + BCrypt và xác thực 2 lớp (2FA).
3. Bổ sung tính năng làm bài kiểm tra trực tuyến và nộp bài tập ngay trên cổng HUIT E-Learning.
4. Thêm báo cáo thống kê tỷ lệ qua môn / cảnh báo học vụ theo từng khoa và khóa học.

## 8. Công thức tóm tắt nhanh theo từng giai đoạn

1. Tỷ lệ vắng mặt (cấm thi khi $> 20\%$):

$$
r_{\text{vắng}} = \frac{\text{Số tiết vắng}}{\text{Tổng số tiết học phần}} \times 100\%
$$

2. Điểm tổng kết hệ 10:

$$
D_{10} = w_{cc}\cdot d_{cc} + w_{qt}\cdot d_{qt} + w_{th}\cdot d_{th} + w_{ck}\cdot d_{ck}
$$

3. Điểm trung bình tích lũy GPA (chỉ lấy điểm cao nhất của mỗi học phần):

$$
\text{GPA} = \frac{\sum_{i=1}^{M} c_i \cdot D_{4,i}}{\sum_{i=1}^{M} c_i}
$$

4. Học phí học phần tín chỉ:

$$
\text{HP} = (c_{lt} \cdot P) + (c_{th} \cdot P \cdot k_{th})
$$

5. Số dư công nợ còn lại sau thanh toán:

$$
\text{Còn nợ} = \text{Tổng học phí kỳ} - \sum \text{Phiếu thu đã nộp}
$$

---

Nếu cần, có thể tách tài liệu này thành 2 file riêng:

1. `README.md` ngắn gọn cho hướng dẫn cài đặt và chạy nhanh.
2. `REPORT.md` chi tiết cho phần kiến trúc, công thức nghiệp vụ và ví dụ thế số.

---

**Disclaimer:** Đây là dự án đồ án môn học phục vụ mục đích học tập và nghiên cứu, được xây dựng hoàn toàn độc lập lấy cảm hứng từ quy trình đào tạo tín chỉ đại học (HUIT/UIT). Hệ thống và dữ liệu mẫu trong dự án không phải là cổng thông tin chính thức của nhà trường.

