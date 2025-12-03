# 🔧 Quick Fix: Context Path Issue

## ❌ Vấn đề

URL không hoạt động:
```
http://localhost:8080/files/court-images/1f1944ec-8e37-42da-a1af-69e745d08784.png
```

## ✅ Giải pháp

Backend sử dụng **context-path: `/api`** nên URL đúng phải là:
```
http://localhost:8080/api/files/court-images/1f1944ec-8e37-42da-a1af-69e745d08784.png
```

## 🔧 Đã Fix

### 1. Backend Auto-Build URL với /api prefix
- ✅ Updated `FileUrlBuilder` để tự động thêm context-path
- ✅ Method `buildImageUrl()` và `buildImageUrls()` tự động include `/api/`

### 2. Frontend Config

**Config cho frontend:**
```javascript
const API_BASE_URL = 'http://localhost:8080/api';

// Upload
const uploadUrl = `${API_BASE_URL}/files/upload`;

// Display image
const buildImageUrl = (filename) => {
  return `${API_BASE_URL}/files/court-images/${filename}`;
};
```

## 📋 URL Mapping

| Type | URL |
|------|-----|
| Upload Single | `POST http://localhost:8080/api/files/upload` |
| Upload Multiple | `POST http://localhost:8080/api/files/upload-multiple` |
| Get Image | `GET http://localhost:8080/api/files/court-images/{filename}` |
| Login | `POST http://localhost:8080/api/auth/login` |
| Users | `GET http://localhost:8080/api/users` |
| Court Groups | `GET http://localhost:8080/api/court-groups` |

**⚠️ TẤT CẢ endpoints đều cần prefix `/api/`**

## 🧪 Test Lại

### 1. Test trực tiếp trên browser
```
http://localhost:8080/api/files/court-images/1f1944ec-8e37-42da-a1af-69e745d08784.png
```

### 2. Test upload và nhận URL
```javascript
const formData = new FormData();
formData.append('file', file);

const response = await fetch('http://localhost:8080/api/files/upload', {
  method: 'POST',
  body: formData
});

const data = await response.json();
console.log(data.result.url);
// → "http://localhost:8080/api/files/court-images/uuid.jpg"

// Dùng URL này để display
<img src={data.result.url} />
```

## 📝 Checklist

- [x] Backend tự động build URL với `/api/`
- [x] SecurityConfig allow public access `/files/**`
- [x] FileController serve images từ `/api/files/court-images/{filename}`
- [x] Upload endpoints trả về full URL
- [x] Documentation updated

## 🎯 Action Required

**Frontend developers cần:**
1. ✅ Add `/api/` prefix vào TẤT CẢ API calls
2. ✅ Update base URL: `http://localhost:8080/api`
3. ✅ Dùng URL từ backend response (đã có `/api/` prefix)

**Example Frontend Config:**
```javascript
// config/api.js
export const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

// services/upload.js
import { API_BASE_URL } from '../config/api';

export const uploadImage = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await fetch(`${API_BASE_URL}/files/upload`, {
    method: 'POST',
    body: formData
  });
  
  return await response.json();
};

// services/image.js
export const buildImageUrl = (filename) => {
  return `${API_BASE_URL}/files/court-images/${filename}`;
};
```

## ✨ Kết quả

- ✅ Backend response tự động có `/api/` prefix
- ✅ Frontend chỉ cần dùng URL từ response
- ✅ Images hiển thị đúng
- ✅ Public access (không cần token)


















