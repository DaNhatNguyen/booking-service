# Fix: 401 Unauthorized khi truy cập ảnh

## ✅ Đã Fix

### 1. Thêm `/uploads/**` vào SecurityConfig
- Cho phép public access (không cần authentication)
- Bất kỳ file nào trong `/api/uploads/` đều có thể truy cập được

### 2. Tạo UploadsController mới
- Serve files qua endpoint `/uploads/{fileName}`
- Hỗ trợ cả đường dẫn trực tiếp và subdirectory
- Tự động detect content type (image/png, image/jpeg, etc.)

---

## 🔗 Các URL có thể dùng

Giờ bạn có thể truy cập ảnh qua **3 cách**:

### Cách 1: `/api/uploads/{filename}` (MỚI - Khuyên dùng)
```
http://localhost:8080/api/uploads/bankqr.png
```

### Cách 2: `/api/files/court-images/{filename}` (Cũ - vẫn hoạt động)
```
http://localhost:8080/api/files/court-images/bankqr.png
```

### Cách 3: `/api/uploads/court-images/{filename}` (MỚI - Flexible)
```
http://localhost:8080/api/uploads/court-images/bankqr.png
```

---

## 🧪 Test ngay

### Bước 1: Restart backend
```bash
# Stop application nếu đang chạy
# Restart
mvn spring-boot:run
```

### Bước 2: Kiểm tra file tồn tại
```bash
# Windows
dir uploads\court-images\bankqr.png

# Linux/Mac
ls uploads/court-images/bankqr.png
```

Nếu file **KHÔNG TỒN TẠI**, copy vào:
```bash
# Windows
copy bankqr.png uploads\court-images\

# Linux/Mac
cp bankqr.png uploads/court-images/
```

### Bước 3: Test trong browser (Không cần token)

**Mở trình duyệt và truy cập:**
```
http://localhost:8080/api/uploads/bankqr.png
```

**Expected:** Ảnh QR code hiển thị ra

---

## 🎯 Sử dụng trong Frontend

### React Example (Payment Page)

```jsx
import { Image } from '@mantine/core';

const PaymentPage = ({ paymentInfo }) => {
  // Cách 1: Dùng endpoint /uploads (MỚI)
  const qrImageUrl = `${process.env.REACT_APP_API_URL}/uploads/${paymentInfo.owner_bank_qr_image}`;
  
  // Cách 2: Dùng endpoint /files/court-images (Cũ)
  // const qrImageUrl = `${process.env.REACT_APP_API_URL}/files/court-images/${paymentInfo.owner_bank_qr_image}`;

  return (
    <div>
      <h3>Quét mã QR để thanh toán</h3>
      <Image
        src={qrImageUrl}
        alt="QR Code thanh toán"
        radius="md"
        maw={300}
      />
    </div>
  );
};
```

### HTML Example
```html
<!-- Không cần token, truy cập trực tiếp -->
<img 
  src="http://localhost:8080/api/uploads/bankqr.png" 
  alt="QR Code"
  style="max-width: 300px;"
/>
```

### Axios Example
```javascript
// KHÔNG CẦN Authorization header
const imageUrl = 'http://localhost:8080/api/uploads/bankqr.png';

// Hiển thị trực tiếp trong <img>
document.getElementById('qr-image').src = imageUrl;
```

---

## 📋 Cấu trúc thư mục uploads

```
uploads/
└── court-images/
    ├── bankqr.png                          ← QR code ngân hàng
    ├── a1b2c3d4-e5f6-g7h8-i9j0.jpg        ← Ảnh sân
    └── payment_proof_43_1732348759.jpg    ← Ảnh chuyển khoản
```

**Tất cả file trong thư mục này đều có thể truy cập qua:**
- `/api/uploads/{filename}`
- `/api/files/court-images/{filename}`

---

## 🔍 Troubleshooting

### Vẫn bị 401 Unauthorized

**Kiểm tra:**
1. Application đã restart chưa?
   ```bash
   # Phải stop và start lại để load SecurityConfig mới
   ```

2. URL có đúng không?
   ```
   ❌ Sai: http://localhost:8080/uploads/bankqr.png
   ✅ Đúng: http://localhost:8080/api/uploads/bankqr.png
   ```

3. Context path `/api` có trong URL không?

---

### 404 Not Found

**Nguyên nhân:** File không tồn tại hoặc đường dẫn sai

**Giải pháp:**
```bash
# Kiểm tra file tồn tại
ls uploads/court-images/bankqr.png

# Nếu không có, copy vào
cp bankqr.png uploads/court-images/
```

---

### CORS Error

**Nguyên nhân:** Frontend origin chưa được allow

**Giải pháp:** Đã được config trong `SecurityConfig.java`:
```java
config.setAllowedOrigins(List.of(
    "http://localhost:3000",   // React default
    "http://localhost:5173",   // Vite default
    "http://localhost:8080"
));
```

Nếu frontend chạy ở port khác, thêm vào list này.

---

## 📝 Chi tiết Technical

### SecurityConfig Changes
```java
private static final String[] PUBLIC_ENDPOINTS = {
    "/auth/**",
    "/court-groups/**",
    "/court-groups",
    "/courts/search",
    "/files/**",    // Existing
    "/uploads/**"   // NEW - Allow public access
};
```

### UploadsController
- **Mapping:** `/uploads`
- **Method:** `GET /{fileName:.+}`
- **Authentication:** Không cần (public)
- **Features:**
  - Auto detect content type (image/png, image/jpeg, etc.)
  - Cache control headers (cache 1 year)
  - Handle subdirectories

---

## ✅ Checklist

- [x] Thêm `/uploads/**` vào SecurityConfig
- [x] Tạo UploadsController
- [x] Restart application
- [ ] Test URL trong browser: `http://localhost:8080/api/uploads/bankqr.png`
- [ ] Update frontend code để dùng endpoint mới
- [ ] Verify ảnh hiển thị trong PaymentPage

---

## 🎉 Kết luận

Giờ bạn có thể:
1. ✅ Truy cập ảnh **không cần token**
2. ✅ Dùng URL đơn giản: `/api/uploads/{filename}`
3. ✅ Frontend hiển thị ảnh QR code ngân hàng
4. ✅ Upload và access payment proof images

**Khuyến nghị:** Dùng endpoint `/api/uploads/` cho đơn giản và dễ nhớ!








