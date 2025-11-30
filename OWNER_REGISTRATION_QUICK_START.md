# Quick Start - Owner Registration API

## 🚀 Bắt đầu nhanh trong 5 phút

### Bước 1: Chạy Database Migration

```bash
# Mở MySQL Workbench hoặc command line
mysql -u root -p

# Chọn database
USE `booking-service`;

# Import migration script
source database_migration_owner_registration.sql;

# Hoặc copy-paste nội dung file vào MySQL Workbench và chạy
```

**Verify migration:**
```sql
-- Check new columns
DESCRIBE users;

-- Should see:
-- owner_status
-- id_card_front
-- id_card_back
-- owner_verified_at
```

---

### Bước 2: Restart Backend

```bash
# Stop application nếu đang chạy (Ctrl+C)

# Restart
mvn spring-boot:run

# Hoặc run từ IDE (IntelliJ/Eclipse)
```

**Check logs:**
```
Application started successfully on port 8080
```

---

### Bước 3: Test API với Postman

#### 3.1. Tạo New Request

- **Method:** POST
- **URL:** `http://localhost:8080/api/auth/register-owner`
- **Body:** form-data

#### 3.2. Thêm Fields

| Key | Type | Value |
|-----|------|-------|
| `fullName` | Text | Nguyen Van A |
| `email` | Text | owner1@example.com |
| `password` | Text | 123456 |
| `phone` | Text | 0912345678 |
| `bankName` | Text | Vietcombank |
| `bankAccountNumber` | Text | 1234567890 |
| `bankAccountName` | Text | NGUYEN VAN A |
| `idCardFront` | File | [Chọn ảnh] |
| `idCardBack` | File | [Chọn ảnh] |
| `bankQrImage` | File | [Chọn ảnh] (optional) |

#### 3.3. Send Request

**Expected Response (201 Created):**
```json
{
  "code": 1000,
  "message": "Đăng ký thành công! Chúng tôi sẽ xem xét và phản hồi trong 24-48 giờ.",
  "result": {
    "id": 10,
    "fullName": "Nguyen Van A",
    "email": "owner1@example.com",
    "phone": "0912345678",
    "role": "OWNER",
    "ownerStatus": "PENDING",
    "createdAt": "2025-11-23T16:30:00"
  }
}
```

---

### Bước 4: Verify trong Database

```sql
-- Xem owner vừa tạo
SELECT id, full_name, email, role, owner_status, 
       id_card_front, id_card_back, bank_qr_image,
       bank_name, bank_account_number, created_at
FROM users 
WHERE email = 'owner1@example.com';
```

**Expected:**
- `role`: OWNER
- `owner_status`: PENDING
- `id_card_front`: UUID filename
- `id_card_back`: UUID filename
- `password`: BCrypt hashed

---

### Bước 5: Xem Files đã Upload

**Check thư mục:**
```bash
# Windows
dir uploads\court-images\

# Linux/Mac
ls uploads/court-images/
```

**Truy cập file trong browser:**
```
http://localhost:8080/api/uploads/{filename}
```

**Example:**
```
http://localhost:8080/api/uploads/a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6.jpg
```

---

## 🎯 Test Scenarios

### Test 1: Successful Registration

**Input:** Valid data + valid images

**Expected:** 
- Status 201
- User created with PENDING status
- 3 files uploaded

---

### Test 2: Duplicate Email

**Input:** Email đã tồn tại

**Request:**
```json
{
  "email": "owner1@example.com", // Already exists
  ...
}
```

**Expected Response:**
```json
{
  "code": 1002,
  "message": "User existed"
}
```

---

### Test 3: Missing Required File

**Input:** Không gửi idCardFront

**Expected Response (400):**
```json
{
  "code": 1000,
  "message": "ID card front image is required"
}
```

---

### Test 4: Invalid File Type

**Input:** Upload PDF thay vì ảnh

**Expected Response (400):**
```json
{
  "code": 1000,
  "message": "ID card front must be an image file"
}
```

---

### Test 5: File Too Large

**Input:** Upload ảnh > 10MB

**Expected Response (413):**
```json
{
  "code": 1000,
  "message": "ID card front size must be less than 10MB"
}
```

---

## 🔍 Debug Tips

### Check application logs

```bash
tail -f logs/application.log

# Hoặc xem console output
```

**Successful registration log:**
```
INFO - Processing owner registration for email: owner1@example.com
INFO - Files uploaded successfully: front=..., back=..., qr=...
INFO - Owner registration successful: id=10, email=owner1@example.com
```

---

### Check database

```sql
-- Count owners by status
SELECT owner_status, COUNT(*) 
FROM users 
WHERE role = 'OWNER' 
GROUP BY owner_status;

-- Recent registrations
SELECT id, full_name, email, owner_status, created_at 
FROM users 
WHERE role = 'OWNER' 
ORDER BY created_at DESC 
LIMIT 10;
```

---

### Check uploaded files

```bash
# List files in uploads directory
ls -lh uploads/court-images/

# Check file sizes
du -h uploads/court-images/*
```

---

## 🐛 Common Issues

### Issue 1: "Could not create the directory"

**Cause:** Thư mục uploads không tồn tại hoặc không có quyền ghi

**Solution:**
```bash
# Windows
mkdir uploads\court-images
icacls uploads /grant Users:F

# Linux/Mac
mkdir -p uploads/court-images
chmod 755 uploads
```

---

### Issue 2: "User existed" khi test lại

**Cause:** Email đã được dùng trong lần test trước

**Solution:**
```sql
-- Xóa test user
DELETE FROM users WHERE email = 'owner1@example.com';

-- Hoặc dùng email khác
-- owner2@example.com, owner3@example.com, ...
```

---

### Issue 3: 404 Not Found

**Cause:** Endpoint URL sai

**Check:**
- ✅ Đúng: `http://localhost:8080/api/auth/register-owner`
- ❌ Sai: `http://localhost:8080/auth/register-owner` (thiếu /api)

---

### Issue 4: Connection refused

**Cause:** Backend chưa chạy hoặc chạy port khác

**Solution:**
```bash
# Check backend đang chạy
curl http://localhost:8080/api/health

# Hoặc check trong browser
http://localhost:8080/api/actuator/health
```

---

## 📋 Checklist

- [ ] Database migration chạy thành công
- [ ] Bảng users có cột owner_status, id_card_front, id_card_back
- [ ] Backend đã restart và chạy ổn định
- [ ] Thư mục uploads/court-images tồn tại và có quyền ghi
- [ ] Test API thành công với Postman
- [ ] Owner được tạo với status PENDING
- [ ] Files được upload vào uploads/court-images/
- [ ] Có thể truy cập files qua browser
- [ ] Password được hash bằng BCrypt

---

## 🎉 Next Steps

### 1. Integrate với Frontend

Frontend đã sẵn sàng tại `src/pages/CollaborationPage.tsx`

**Test frontend:**
```bash
cd frontend
npm start
# Navigate to: http://localhost:3000/collaboration
```

---

### 2. Implement Admin Dashboard

**APIs cần có:**
```
GET  /api/admin/owners?status=PENDING     # Danh sách pending
PUT  /api/admin/owners/{id}/approve       # Duyệt đơn
PUT  /api/admin/owners/{id}/reject        # Từ chối
GET  /api/admin/owners/{id}/documents     # Xem giấy tờ
```

---

### 3. Thêm Email Notification

**Gửi email khi:**
- Owner đăng ký thành công (status: PENDING)
- Admin duyệt đơn (status: APPROVED)
- Admin từ chối (status: REJECTED)

---

### 4. Login cho Owner

**Logic:**
- Owner với `owner_status = APPROVED` → Cho phép login
- Owner với `owner_status = PENDING` → Hiển thị "Đơn đang được xét duyệt"
- Owner với `owner_status = REJECTED` → Hiển thị "Đơn bị từ chối"

**Update AuthenticationService.authenticate():**
```java
// After password verification
if (user.getRole() == Role.OWNER) {
    if (user.getOwnerStatus() == OwnerStatus.PENDING) {
        throw new AppException(ErrorCode.OWNER_PENDING);
    }
    if (user.getOwnerStatus() == OwnerStatus.REJECTED) {
        throw new AppException(ErrorCode.OWNER_REJECTED);
    }
    if (user.getOwnerStatus() == OwnerStatus.BANNED) {
        throw new AppException(ErrorCode.OWNER_BANNED);
    }
}
```

---

## 📚 Documentation

- **Full API Docs:** [OWNER_REGISTRATION_API_DOCUMENTATION.md](OWNER_REGISTRATION_API_DOCUMENTATION.md)
- **Database Migration:** [database_migration_owner_registration.sql](database_migration_owner_registration.sql)

---

**Happy Coding! 🚀**








