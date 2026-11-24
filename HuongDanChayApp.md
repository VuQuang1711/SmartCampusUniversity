# Hướng dẫn cài đặt, khởi chạy và vận hành ứng dụng (University SIS & HUIT E-Learning)

Tài liệu này hướng dẫn chi tiết cách cấu hình cơ sở dữ liệu MySQL, khởi chạy, dừng ứng dụng, truy cập qua mạng nội bộ / Cloudflare Tunnel và xử lý các sự cố thường gặp theo đúng mã nguồn hiện tại của dự án.

## 1. Yêu cầu môi trường

| Thành phần | Phiên bản yêu cầu | Ghi chú |
| :--- | :--- | :--- |
| Java JDK | Java 17 hoặc Java 21+ | Khuyên dùng Eclipse Temurin OpenJDK 17/21 |
| MySQL Server | MySQL 8.0 hoặc MySQL 9.x | Chạy mặc định tại cổng `3306` |
| Maven | Tích hợp sẵn | Dùng Maven Wrapper (`mvnw.cmd` / `mvnw`), không cần cài riêng |
| IDE | VS Code / IntelliJ IDEA  | Cài sẵn Java Extension Pack (nếu dùng VS Code) |

## 2. Cấu hình cơ sở dữ liệu MySQL

### 2.1. Kiểm tra file `application.properties`

Mở `src/main/resources/application.properties` và kiểm tra thông tin kết nối:

```properties
server.port=8081

spring.datasource.url=jdbc:mysql://localhost:3306/university_sis?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=1234

spring.jpa.hibernate.ddl-auto=update
app.cloudflare.tunnel.enabled=true
```

Lưu ý:
- Nếu mật khẩu tài khoản `root` của MySQL trên máy bạn khác `1234`, hãy cập nhật lại dòng `spring.datasource.password`.
- Nếu chỉ muốn chạy nội bộ `localhost` và không tạo link HTTPS công khai, đổi `app.cloudflare.tunnel.enabled=false`.

### 2.2. Nạp schema và dữ liệu mẫu

Chạy lần lượt 2 file SQL trong thư mục `database/` bằng MySQL Workbench / DBeaver hoặc dòng lệnh:

```bash
mysql -u root -p < database/init_schema_and_data.sql
mysql -u root -p university_sis < database/seed_huit_khdl_and_receipts.sql
```

- `database/init_schema_and_data.sql`: tạo database `university_sis`, khởi tạo cấu trúc bảng chuẩn 3NF và dữ liệu tài khoản/học vụ cơ bản.
- `database/seed_huit_khdl_and_receipts.sql`: bổ sung chương trình đào tạo ngành Khoa học Dữ liệu (HUIT) và dữ liệu biên lai học phí mẫu.

## 3. Hướng dẫn khởi chạy ứng dụng

### Cách 1: Chạy trực tiếp từ IDE (Khuyên dùng)

1. Mở thư mục dự án `Quan_Ly_Hoc_Sinh_Java`.
2. Mở file `src/main/java/com/example/sis/SisApplication.java`.
3. Nhấn nút **Run** phía trên hàm `main` (hoặc nhấn `F5`).

Lưu ý: `SisApplication.main()` đã tích hợp sẵn cơ chế `freePortIfOccupied(8081)` để tự động giải phóng cổng `8081` nếu có tiến trình cũ còn chạy ngầm.

### Cách 2: Chạy bằng dòng lệnh (Maven Wrapper)

Di chuyển vào thư mục dự án và chạy:

- Trên Windows (PowerShell / CMD):

```powershell
.\mvnw.cmd spring-boot:run
```

- Trên Linux / macOS:

```bash
./mvnw spring-boot:run
```

Nếu vừa chỉnh sửa nhiều file Java và muốn biên dịch sạch trước khi chạy:

```powershell
.\mvnw.cmd -DskipTests clean compile
.\mvnw.cmd spring-boot:run
```

## 4. Truy cập hệ thống (Localhost & Public HTTPS)

Khi ứng dụng khởi động xong, console sẽ hiển thị:

```text
Tomcat started on port 8081 (http) with context path '/'
Started SisApplication in ... seconds
```

### 4.1. Địa chỉ truy cập

- Cổng nội bộ (Localhost): `http://localhost:8081` (tự động chuyển tới `/Account/Login`).
- Cổng HUIT E-Learning (chỉ dành cho sinh viên): `http://localhost:8081/ELearning` hoặc bấm nút **HUIT E-Learning** từ menu Sinh viên (hỗ trợ SSO).
- Đường dẫn công khai (Cloudflare Tunnel): khi `app.cloudflare.tunnel.enabled=true`, ứng dụng tự khởi chạy `cloudflared.exe` và in ra link `https://...trycloudflare.com` trên console để mở trên điện thoại hoặc chia sẻ demo. Link tự đóng ngay khi dừng ứng dụng.

### 4.2. Danh sách tài khoản thử nghiệm

Mật khẩu dùng chung cho tất cả tài khoản: `123456`.

| Vai trò | Mã đăng nhập | Email | Họ và tên | Chức năng kiểm thử chính |
| :--- | :--- | :--- | :--- | :--- |
| Sinh viên (Khá/Giỏi) | `2180601234` | `2180601234@sinhvien.sis.edu.vn` | Nguyễn Minh Khoa | ĐKHP, TKB, bảng điểm, CTĐT, nộp học phí, mô phỏng GPA, E-Learning |
| Sinh viên (Cảnh báo) | `2180609999` | `2180609999@sinhvien.sis.edu.vn` | Trần Thị Hoa | Kiểm tra giới hạn tối đa 18 TC/kỳ do `GPA < 1.8` |
| Giảng viên | `GV001` | `gv.an@sis.edu.vn` | PGS.TS Nguyễn Văn An | Lịch giảng dạy, điểm danh (tự động cấm thi khi vắng > 20%), nhập điểm |
| Cố vấn học tập | `GV002` | `gv.binh@sis.edu.vn` | TS. Trần Thị Bình | Theo dõi lớp danh nghĩa và kết quả sinh viên |
| Phòng Đào tạo | `DAOTAO01` | `daotao@sis.edu.vn` | Phòng Đào Tạo HUIT | Mở/đóng cổng ĐKHP, quản lý lớp học phần, xét tốt nghiệp |
| Quản trị viên | `ADMIN01` | `admin@sis.edu.vn` | Quản Trị Hệ Thống | Quản lý người dùng, lịch học, phân quyền |
| Phòng Kế toán | `KETOAN01` | `ketoan@sis.edu.vn` | Kế Toán Tài Chính | Quản lý công nợ học phí, gạch nợ, in phiếu thu |

## 5. Hướng dẫn dừng ứng dụng

- Trong Terminal: nhấn `Ctrl + C`, nhập `Y` rồi nhấn `Enter`.
- Trong IDE: nhấn nút **Stop** (hình vuông đỏ) hoặc tổ hợp phím `Shift + F5`.
- Giải phóng cổng `8081` thủ công (nếu cần trên PowerShell):

```powershell
Stop-Process -Id (Get-NetTCPConnection -LocalPort 8081).OwningProcess -Force
```

## 6. Xử lý sự cố thường gặp

### 6.1. Lỗi không kết nối được MySQL (`Communications link failure` / `Access denied`)

- Kiểm tra dịch vụ MySQL đã chạy hay chưa (`Win + R` -> `services.msc` -> khởi động dịch vụ `MySQL`).
- Kiểm tra lại `spring.datasource.username` và `spring.datasource.password` trong `src/main/resources/application.properties`.

### 6.2. Lỗi `Port 8081 was already in use`

- Khi chạy bằng `SisApplication.java`, ứng dụng đã tự động dọn tiến trình cũ trên cổng `8081`.
- Nếu chạy qua Maven và vẫn gặp lỗi chiếm cổng, chạy lệnh PowerShell ở Mục 5 hoặc đổi `server.port=8082` trong `application.properties`.

### 6.3. Không mở được link Cloudflare Tunnel

- Kiểm tra kết nối Internet và đảm bảo `app.cloudflare.tunnel.enabled=true` trong `application.properties`.
- Trong trường hợp mạng nội bộ chặn Cloudflare, đổi `app.cloudflare.tunnel.enabled=false` và sử dụng bình thường qua `http://localhost:8081`.
