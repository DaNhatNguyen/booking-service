# Owner Registration API Documentation

## 📋 Tổng quan

API cho phép người dùng đăng ký trở thành chủ sân (Owner) với trạng thái PENDING, chờ admin xét duyệt.

---

## 🔌 API Endpoint

### **POST** `/api/auth/register-owner`

Đăng ký tài khoản Owner mới với đầy đủ thông tin cá nhân, ngân hàng và giấy tờ.

---

## 📤 Request

### Headers
```
Content-Type: multipart/form-data
```

### Body Parameters (Form Data)

| Field | Type | Required | Description | Validation |
|-------|------|----------|-------------|------------|
| `fullName` | String | ✅ Yes | Họ và tên | Min 2 chars |
| `email` | String | ✅ Yes | Email (unique) | Valid email format |
| `password` | String | ✅ Yes | Mật khẩu | Min 6 chars |
| `phone` | String | ✅ Yes | Số điện thoại | 10 digits |
| `bankName` | String | ✅ Yes | Tên ngân hàng | - |
| `bankAccountNumber` | String | ✅ Yes | Số tài khoản | - |
| `bankAccountName` | String | ✅ Yes | Tên chủ tài khoản | - |
| `idCardFront` | File | ✅ Yes | Ảnh CMND/CCCD mặt trước | Image, max 10MB |
| `idCardBack` | File | ✅ Yes | Ảnh CMND/CCCD mặt sau | Image, max 10MB |
| `bankQrImage` | File | ❌ No | Ảnh QR code ngân hàng | Image, max 10MB |

---

## 📥 Response

### Success Response (201 Created)

```json
{
  "code": 1000,
  "message": "Đăng ký thành công! Chúng tôi sẽ xem xét và phản hồi trong 24-48 giờ.",
  "result": {
    "id": 10,
    "fullName": "Nguyen Van A",
    "email": "owner@example.com",
    "phone": "0912345678",
    "role": "OWNER",
    "ownerStatus": "PENDING",
    "createdAt": "2025-11-23T16:30:00",
    "message": "Đăng ký thành công! Chúng tôi sẽ xem xét và phản hồi trong 24-48 giờ."
  }
}
```

### Error Responses

#### 400 Bad Request - Email đã tồn tại
```json
{
  "code": 1002,
  "message": "User existed"
}
```

#### 400 Bad Request - Missing required file
```json
{
  "code": 1000,
  "message": "ID card front image is required"
}
```

#### 400 Bad Request - Invalid file type
```json
{
  "code": 1000,
  "message": "ID card front must be an image file"
}
```

#### 413 Payload Too Large - File quá lớn
```json
{
  "code": 1000,
  "message": "ID card front size must be less than 10MB"
}
```

---

## 🧪 Testing

### Postman / Thunder Client

**Endpoint:**
```
POST http://localhost:8080/api/auth/register-owner
```

**Headers:**
```
Content-Type: multipart/form-data
```

**Body (form-data):**
```
fullName: Nguyen Van A
email: owner@example.com
password: 123456
phone: 0912345678
bankName: Vietcombank
bankAccountNumber: 1234567890
bankAccountName: NGUYEN VAN A
idCardFront: [Upload file]
idCardBack: [Upload file]
bankQrImage: [Upload file] (optional)
```

### cURL Example

```bash
curl -X POST http://localhost:8080/api/auth/register-owner \
  -F "fullName=Nguyen Van A" \
  -F "email=owner@example.com" \
  -F "password=123456" \
  -F "phone=0912345678" \
  -F "bankName=Vietcombank" \
  -F "bankAccountNumber=1234567890" \
  -F "bankAccountName=NGUYEN VAN A" \
  -F "idCardFront=@/path/to/id_front.jpg" \
  -F "idCardBack=@/path/to/id_back.jpg" \
  -F "bankQrImage=@/path/to/qr.png"
```

### JavaScript/Axios Example

```javascript
const registerOwner = async (formData) => {
  const data = new FormData();
  
  // Text fields
  data.append('fullName', formData.fullName);
  data.append('email', formData.email);
  data.append('password', formData.password);
  data.append('phone', formData.phone);
  data.append('bankName', formData.bankName);
  data.append('bankAccountNumber', formData.bankAccountNumber);
  data.append('bankAccountName', formData.bankAccountName);
  
  // Files
  data.append('idCardFront', formData.idCardFront); // File object
  data.append('idCardBack', formData.idCardBack);
  if (formData.bankQrImage) {
    data.append('bankQrImage', formData.bankQrImage);
  }
  
  try {
    const response = await axios.post(
      'http://localhost:8080/api/auth/register-owner',
      data,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      }
    );
    
    console.log('Registration successful:', response.data);
    return response.data;
  } catch (error) {
    console.error('Registration failed:', error.response?.data);
    throw error;
  }
};
```

---

## 🔐 Backend Logic Flow

```
1. Validate email doesn't exist
   ↓
2. Validate required files (idCardFront, idCardBack)
   ↓
3. Validate file types (must be images)
   ↓
4. Validate file sizes (max 10MB each)
   ↓
5. Upload files to uploads/court-images/
   ↓
6. Hash password using BCrypt (cost factor 10)
   ↓
7. Create User entity:
   - role = OWNER
   - ownerStatus = PENDING
   - Save filenames (not full paths)
   ↓
8. Save to database
   ↓
9. Return response (201 Created)
```

---

## 🗄️ Database Schema

### Table: `users`

```sql
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `full_name` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL UNIQUE,
  `password` varchar(255) DEFAULT NULL, -- BCrypt hashed
  `phone` varchar(255) DEFAULT NULL,
  `role` enum('USER','ADMIN','OWNER') DEFAULT 'USER',
  `owner_status` enum('PENDING','APPROVED','REJECTED','BANNED') DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `id_card_front` varchar(255) DEFAULT NULL,
  `id_card_back` varchar(255) DEFAULT NULL,
  `bank_qr_image` varchar(255) DEFAULT NULL,
  `bank_name` varchar(100) DEFAULT NULL,
  `bank_account_number` varchar(50) DEFAULT NULL,
  `bank_account_name` varchar(255) DEFAULT NULL,
  `owner_verified_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_role` (`role`),
  KEY `idx_owner_status` (`owner_status`),
  KEY `idx_role_owner_status` (`role`,`owner_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 📊 Owner Status Flow

```
User registers
    ↓
[PENDING] ← Waiting for admin review (cannot login yet)
    ↓
Admin reviews application
    ↓
    ├─→ [APPROVED] → Owner can login and manage courts
    ├─→ [REJECTED] → Application denied
    └─→ [BANNED] → Owner account banned
```

### Status Descriptions

| Status | Description | Can Login? |
|--------|-------------|------------|
| `PENDING` | Chờ admin duyệt | ❌ No |
| `APPROVED` | Đã được duyệt | ✅ Yes |
| `REJECTED` | Bị từ chối | ❌ No |
| `BANNED` | Bị cấm | ❌ No |

---

## 🔒 Security Features

### 1. Password Hashing
- **Algorithm:** BCrypt
- **Cost Factor:** 10
- **Example:** `$2a$10$XLGiZ7.PMDdtGgpoQxAWOuR...`

### 2. File Validation
- **Allowed types:** image/jpeg, image/png, image/gif, image/webp
- **Max size:** 10MB per file
- **Storage:** Unique UUID filenames to prevent conflicts

### 3. Email Uniqueness
- Checked before registration
- Returns error if email already exists

### 4. SQL Injection Prevention
- Uses JPA/Hibernate parameterized queries
- No raw SQL with user input

---

## 📁 File Storage

### Directory Structure
```
uploads/
└── court-images/
    ├── a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6.jpg (ID front)
    ├── b2c3d4e5-f6g7-h8i9-j0k1-l2m3n4o5p6q7.jpg (ID back)
    └── c3d4e5f6-g7h8-i9j0-k1l2-m3n4o5p6q7r8.png (QR code)
```

### File Naming Convention
- Format: `{UUID}.{extension}`
- Example: `a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6.jpg`
- Prevents filename collisions
- Preserves original file extension

### Accessing Uploaded Files
```
http://localhost:8080/api/uploads/{filename}
```

**Example:**
```
http://localhost:8080/api/uploads/a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6.jpg
```

**No authentication required** (configured in SecurityConfig)

---

## 🛠️ Admin APIs (To be implemented)

### Get Pending Owner Applications
```
GET /api/admin/owners?status=PENDING
```

### Approve Owner Application
```
PUT /api/admin/owners/{id}/approve
```

### Reject Owner Application
```
PUT /api/admin/owners/{id}/reject
```

### Ban Owner
```
PUT /api/admin/owners/{id}/ban
```

### View Owner Documents
```
GET /api/admin/owners/{id}/documents
```

---

## 🐛 Common Issues & Solutions

### Issue 1: "User existed" error

**Cause:** Email already registered

**Solution:** Use a different email or check if user already exists

---

### Issue 2: "ID card front image is required"

**Cause:** Missing file in request

**Solution:** Ensure file is attached in form-data with correct field name

---

### Issue 3: File upload fails with 413 error

**Cause:** File size > 10MB

**Solution:** 
- Compress image before upload
- Increase size limit in `application.yaml`:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 60MB
```

---

### Issue 4: "must be an image file" error

**Cause:** Wrong file type (e.g., PDF, DOCX)

**Solution:** Upload only image files (JPG, PNG, GIF, WEBP)

---

## 📊 Sample Data

### Test Owner Registration

```json
{
  "fullName": "Nguyen Van Test",
  "email": "testowner1@example.com",
  "password": "123456",
  "phone": "0987654321",
  "bankName": "Vietcombank",
  "bankAccountNumber": "9876543210",
  "bankAccountName": "NGUYEN VAN TEST"
}
```

**Files to upload:**
- ID Front: Test image (JPG/PNG, < 10MB)
- ID Back: Test image (JPG/PNG, < 10MB)
- Bank QR: Test image (Optional, PNG, < 10MB)

---

## 🚀 Deployment Checklist

- [ ] Run database migration script
- [ ] Create `uploads/court-images/` directory
- [ ] Set directory permissions (755)
- [ ] Configure file size limits in `application.yaml`
- [ ] Test file upload functionality
- [ ] Test all error scenarios
- [ ] Set up monitoring for failed uploads
- [ ] Configure backup for uploaded files

---

## 📝 Related Documentation

- [Database Migration Script](database_migration_owner_registration.sql)
- [Security Configuration](src/main/java/com/example/booking_service/configuration/SecurityConfig.java)
- [File Storage Service](src/main/java/com/example/booking_service/service/FileStorageService.java)

---

**Version:** 1.0  
**Last Updated:** 2025-11-23  
**Author:** DATN Team






















