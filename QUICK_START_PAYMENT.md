# Quick Start Guide - Payment Feature

## 🚀 Bắt đầu nhanh

### Bước 1: Chạy Database Migration

```bash
# Mở MySQL Workbench hoặc command line
mysql -u root -p

# Chọn database
USE booking-service;

# Import migration script
source database_migration_payment.sql;

# Hoặc copy-paste nội dung file vào MySQL Workbench và chạy
```

**Kiểm tra:**
```sql
-- Xem cột mới trong bookings
DESCRIBE bookings;

-- Xem cột bank trong users
DESCRIBE users;
```

---

### Bước 2: Update Bank Info cho Owner (Testing)

```sql
-- Update thông tin ngân hàng cho user ID 5 (owner đã có trong DB)
UPDATE users 
SET 
    bank_name = 'MB BANK',
    bank_account_number = '2136668885959',
    bank_account_name = 'NGUYEN DA NHAT',
    bank_qr_image = 'bankqr.png'
WHERE id = 5 AND role = 'OWNER';

-- Verify
SELECT id, full_name, role, bank_name, bank_account_number, bank_account_name, bank_qr_image 
FROM users 
WHERE id = 5;
```

---

### Bước 3: Upload QR Code Image

1. Chuẩn bị file ảnh QR code ngân hàng (đặt tên: `bankqr.png`)
2. Copy vào thư mục uploads:

```bash
# Windows
copy bankqr.png uploads\court-images\

# Linux/Mac
cp bankqr.png uploads/court-images/
```

**Lưu ý:** Đường dẫn upload mặc định là `uploads/court-images/` (config trong `application.yaml`)

---

### Bước 4: Khởi động Backend

```bash
# Đảm bảo MySQL đang chạy
# Khởi động Spring Boot application

# Maven
mvn spring-boot:run

# Hoặc run từ IDE (IntelliJ/Eclipse)
```

**Kiểm tra log:**
```
Scheduling has been enabled
✓ Application started successfully
```

---

### Bước 5: Test API

#### 5.1. Tạo Booking (Status = PAYING)

```bash
curl -X POST http://localhost:8080/api/bookings/confirmation \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "user_id": 1,
    "court_id": 30,
    "booking_date": "2025-11-25",
    "time_slots": [
      {
        "start_time": "18:00",
        "end_time": "19:00"
      }
    ],
    "total_price": 200000,
    "court_group_id": 22,
    "full_address": "18 Tam Trinh, Hoàng Mai, Hà Nội"
  }'
```

**Expected Response:**
```json
{
  "result": {
    "booking_id": 44,
    "status": "PAYING",
    "message": "Đã tạo booking. Vui lòng thanh toán trong 5 phút"
  }
}
```

**✅ Lưu lại `booking_id` để dùng cho bước tiếp theo**

---

#### 5.2. Lấy Thông Tin Thanh Toán

```bash
curl -X GET http://localhost:8080/api/bookings/44/payment-info \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
```json
{
  "result": {
    "booking_id": 44,
    "owner_bank_name": "MB BANK",
    "owner_bank_account_number": "2136668885959",
    "owner_bank_account_name": "NGUYEN DA NHAT",
    "owner_bank_qr_image": "bankqr.png",
    "total_price": 200000,
    "booking_date": "2025-11-25",
    "time_slots": [
      {
        "start_time": "18:00",
        "end_time": "19:00"
      }
    ],
    "court_name": "Sân 4",
    "full_address": "18 Tam Trinh, Hoàng Mai, Hà Nội",
    "created_at": "2025-11-23T16:30:00"
  }
}
```

---

#### 5.3. Xem QR Code trong Browser

Mở browser và truy cập:
```
http://localhost:8080/api/uploads/court-images/bankqr.png
```

Nếu hiển thị ảnh → ✅ File upload đã hoạt động

---

#### 5.4. Upload Ảnh Chuyển Khoản

**Chuẩn bị:**
- Chụp màn hình giao dịch chuyển khoản (hoặc dùng ảnh test bất kỳ)
- Lưu file: `payment_proof.jpg`

**Upload:**
```bash
curl -X POST http://localhost:8080/api/bookings/44/confirm-payment \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "payment_proof=@payment_proof.jpg"
```

**Expected Response:**
```json
{
  "result": {
    "booking_id": 44,
    "status": "PENDING",
    "payment_proof_url": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6.jpg",
    "message": "Đã xác nhận thanh toán. Chúng tôi sẽ xác minh trong thời gian sớm nhất."
  }
}
```

---

#### 5.5. Verify Status Changed

```bash
curl -X GET http://localhost:8080/api/bookings/44 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected:**
- `status`: "PENDING"
- `payment_proof`: "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6.jpg"

---

### Bước 6: Test Scheduled Job (Auto Delete Expired Bookings)

#### Option 1: Đợi 5 phút (Production)

1. Tạo booking mới
2. **Không** upload ảnh
3. Đợi > 5 phút
4. Kiểm tra log:

```
2025-11-23 16:35:00 DEBUG - Running scheduled cleanup of expired bookings...
2025-11-23 16:35:00 INFO  - ✓ Cleaned up 1 expired booking(s) with PAYING status
```

5. Verify booking đã bị xóa:
```sql
SELECT * FROM bookings WHERE id = 44;
-- Expected: 0 rows
```

---

#### Option 2: Test nhanh (Development)

**Sửa timeout thành 1 phút để test nhanh:**

Mở file: `src/main/java/com/example/booking_service/service/BookingService.java`

Tìm method `deleteExpiredBookings()`:
```java
// Sửa từ 5 phút thành 1 phút
LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(1);
```

**Sửa scheduled job chạy 10 giây 1 lần:**

Mở file: `src/main/java/com/example/booking_service/configuration/BookingCleanupScheduler.java`

```java
@Scheduled(fixedRate = 10000) // Chạy mỗi 10 giây thay vì 60000
```

**Restart application và test:**
1. Tạo booking
2. Đợi > 1 phút
3. Sau 10 giây, scheduled job sẽ chạy và xóa booking

---

## 🐛 Troubleshooting

### Lỗi: "Booking not found"

**Nguyên nhân:** Booking đã bị scheduled job xóa

**Giải pháp:** Tạo booking mới và test ngay lập tức (< 5 phút)

---

### Lỗi: "Owner bank information not found"

**Nguyên nhân:** Owner chưa có bank info

**Giải pháp:**
```sql
-- Kiểm tra owner
SELECT id, full_name, role, bank_name FROM users WHERE role = 'OWNER';

-- Update bank info
UPDATE users 
SET bank_name = 'MB BANK', 
    bank_account_number = '2136668885959',
    bank_account_name = 'NGUYEN DA NHAT',
    bank_qr_image = 'bankqr.png'
WHERE id = 5;
```

---

### Lỗi: "Could not store file"

**Nguyên nhân:** Thư mục uploads không có quyền ghi

**Giải pháp:**
```bash
# Windows
mkdir uploads\court-images
icacls uploads /grant Users:F

# Linux/Mac
mkdir -p uploads/court-images
chmod 755 uploads
```

---

### QR Code không hiển thị

**Kiểm tra:**
1. File tồn tại: `ls uploads/court-images/bankqr.png`
2. Browser URL đúng: `http://localhost:8080/api/uploads/court-images/bankqr.png`
3. Nếu dùng React, check CORS và proxy config

**Sửa Frontend:**
```javascript
// Đúng
const qrUrl = `${process.env.REACT_APP_API_URL}/uploads/court-images/${filename}`;

// Hoặc hardcode để test
const qrUrl = `http://localhost:8080/api/uploads/court-images/${filename}`;
```

---

### Scheduled job không chạy

**Kiểm tra:**
```java
// File: BookingCleanupScheduler.java
// Đảm bảo có @EnableScheduling

@Configuration
@EnableScheduling  // ← Phải có annotation này
public class BookingCleanupScheduler {
    ...
}
```

**Xem log:**
```
# Khi application start, phải có log này:
Scheduling has been enabled
```

---

## 📋 Checklist

- [ ] Database migration đã chạy thành công
- [ ] Bảng `bookings` có cột `payment_proof`
- [ ] Bảng `users` có các cột bank (bank_name, bank_account_number, ...)
- [ ] Owner (user_id = 5) đã có bank info
- [ ] File QR code đã upload vào `uploads/court-images/`
- [ ] Backend đã khởi động thành công
- [ ] Scheduled job đã được enable (xem log)
- [ ] API tạo booking trả về status = PAYING
- [ ] API payment-info trả về bank info
- [ ] API confirm-payment upload file thành công
- [ ] Scheduled job tự động xóa booking hết hạn

---

## 📚 Tài liệu chi tiết

- **API Documentation:** `PAYMENT_API_DOCUMENTATION.md`
- **Database Migration:** `database_migration_payment.sql`
- **Frontend Guide:** Xem phần "Frontend Integration" trong `PAYMENT_API_DOCUMENTATION.md`

---

## 🎯 Next Steps

1. Integrate với Frontend (React/Angular/Vue)
2. Thêm notification (email/SMS) khi booking confirmed
3. Dashboard cho Owner xem pending payments
4. Report doanh thu theo ngày/tháng
5. Thêm refund flow nếu Owner reject

---

**Happy Coding! 🚀**

















