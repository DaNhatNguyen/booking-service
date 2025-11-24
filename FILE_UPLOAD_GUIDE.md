# File Upload & Access Guide

## ⚠️ QUAN TRỌNG: Context Path

Backend sử dụng **context-path: `/api`**

**TẤT CẢ** endpoints đều có prefix `/api/`:
- ❌ Sai: `http://localhost:8080/files/upload`
- ✅ Đúng: `http://localhost:8080/api/files/upload`

---

## Tổng quan

Backend đã được cấu hình để:
1. ✅ Upload files và lưu vào thư mục `uploads/court-images/`
2. ✅ Trả về URL đầy đủ (bao gồm `/api/`) để frontend có thể truy cập ảnh
3. ✅ Cho phép public access (không cần token) để hiển thị ảnh

---

## API Endpoints

### 1. Upload Single File (NEW)

**Endpoint:** `POST /api/files/upload`

**Request:**
```http
POST http://localhost:8080/api/files/upload
Content-Type: multipart/form-data

file: [binary data]
```

**Response:**
```json
{
  "code": 1000,
  "message": "File uploaded successfully",
  "result": {
    "filename": "uuid-generated-name.jpg",
    "url": "http://localhost:8080/api/files/court-images/uuid-generated-name.jpg"
  }
}
```

**Frontend Example (JavaScript):**
```javascript
const uploadFile = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await fetch('http://localhost:8080/api/files/upload', {
    method: 'POST',
    body: formData
  });
  
  const data = await response.json();
  console.log('File URL:', data.result.url);
  // URL will be: http://localhost:8080/api/files/court-images/uuid.jpg
  
  // Hiển thị ảnh
  document.getElementById('image').src = data.result.url;
  
  return data.result;
};
```

**React Example:**
```jsx
const [imageUrl, setImageUrl] = useState('');

const handleFileUpload = async (e) => {
  const file = e.target.files[0];
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await fetch('http://localhost:8080/api/files/upload', {
    method: 'POST',
    body: formData
  });
  
  const data = await response.json();
  setImageUrl(data.result.url);
  // URL will include /api/ prefix automatically
};

return (
  <div>
    <input type="file" onChange={handleFileUpload} />
    {imageUrl && <img src={imageUrl} alt="Uploaded" />}
  </div>
);
```

---

### 2. Upload Multiple Files (NEW)

**Endpoint:** `POST /api/files/upload-multiple`

**Request:**
```http
POST http://localhost:8080/api/files/upload-multiple
Content-Type: multipart/form-data

files: [binary data]
files: [binary data]
files: [binary data]
```

**Response:**
```json
{
  "code": 1000,
  "message": "Files uploaded successfully",
  "result": [
    {
      "filename": "uuid1.jpg",
      "url": "http://localhost:8080/api/files/court-images/uuid1.jpg"
    },
    {
      "filename": "uuid2.jpg",
      "url": "http://localhost:8080/api/files/court-images/uuid2.jpg"
    }
  ]
}
```

**Frontend Example:**
```javascript
const uploadMultipleFiles = async (files) => {
  const formData = new FormData();
  
  // Append multiple files with the same field name
  files.forEach(file => {
    formData.append('files', file);
  });
  
  const response = await fetch('http://localhost:8080/api/files/upload-multiple', {
    method: 'POST',
    body: formData
  });
  
  const data = await response.json();
  return data.result; // Array of {filename, url}
};
```

**React Example:**
```jsx
const [imageUrls, setImageUrls] = useState([]);

const handleMultipleFileUpload = async (e) => {
  const files = Array.from(e.target.files);
  const formData = new FormData();
  
  files.forEach(file => {
    formData.append('files', file);
  });
  
  const response = await fetch('http://localhost:8080/api/files/upload-multiple', {
    method: 'POST',
    body: formData
  });
  
  const data = await response.json();
  const urls = data.result.map(item => item.url);
  setImageUrls(urls);
  // URLs will include /api/ prefix
};

return (
  <div>
    <input type="file" multiple onChange={handleMultipleFileUpload} />
    <div>
      {imageUrls.map((url, index) => (
        <img key={index} src={url} alt={`Image ${index}`} />
      ))}
    </div>
  </div>
);
```

---

### 3. Get/Display Image (Public)

**Endpoint:** `GET /api/files/court-images/{filename}`

**Request:**
```http
GET http://localhost:8080/api/files/court-images/uuid-generated-name.jpg
```

**Response:** Binary image data (JPEG, PNG, GIF, WebP)

**Frontend Usage:**
```html
<!-- Chỉ cần dùng URL trực tiếp trong <img> tag -->
<img src="http://localhost:8080/api/files/court-images/uuid-generated-name.jpg" alt="Court" />
```

**Notes:**
- ✅ Endpoint này là **PUBLIC** - không cần Authorization token
- ✅ Tự động set Content-Type phù hợp (image/jpeg, image/png, etc.)
- ✅ Trả về ảnh với header `inline` để browser có thể hiển thị trực tiếp

---

## Khi làm việc với existing data

### Nếu backend trả về chỉ filename (old API):

**Example Response:**
```json
{
  "image": "uuid1.jpg,uuid2.jpg,uuid3.jpg"
}
```

**Frontend phải tự build URL:**
```javascript
const BASE_URL = 'http://localhost:8080';
const API_PREFIX = '/api';

const buildImageUrl = (filename) => {
  return `${BASE_URL}${API_PREFIX}/files/court-images/${filename}`;
};

const buildImageUrls = (imageString) => {
  if (!imageString) return [];
  
  return imageString
    .split(',')
    .map(filename => filename.trim())
    .filter(filename => filename)
    .map(filename => buildImageUrl(filename));
};

// Usage
const courtGroup = {
  image: "uuid1.jpg,uuid2.jpg,uuid3.jpg"
};

const imageUrls = buildImageUrls(courtGroup.image);
// Result: [
//   "http://localhost:8080/api/files/court-images/uuid1.jpg",
//   "http://localhost:8080/api/files/court-images/uuid2.jpg",
//   "http://localhost:8080/api/files/court-images/uuid3.jpg"
// ]
```

---

## Complete React Component Example

```jsx
import React, { useState } from 'react';

const ImageUploadComponent = () => {
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [imageUrls, setImageUrls] = useState([]);
  const [uploading, setUploading] = useState(false);

  const handleFileSelect = (e) => {
    setSelectedFiles(Array.from(e.target.files));
  };

  const handleUpload = async () => {
    if (selectedFiles.length === 0) return;

    setUploading(true);
    const formData = new FormData();

    selectedFiles.forEach(file => {
      formData.append('files', file);
    });

    try {
      const response = await fetch('http://localhost:8080/api/files/upload-multiple', {
        method: 'POST',
        body: formData
      });

      const data = await response.json();
      
      if (data.code === 1000) {
        const urls = data.result.map(item => item.url);
        setImageUrls(urls);
        alert('Upload thành công!');
      } else {
        alert('Upload thất bại!');
      }
    } catch (error) {
      console.error('Upload error:', error);
      alert('Upload thất bại!');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <h2>Upload Hình Ảnh</h2>
      
      <input 
        type="file" 
        multiple 
        accept="image/*"
        onChange={handleFileSelect} 
      />
      
      <button 
        onClick={handleUpload} 
        disabled={uploading || selectedFiles.length === 0}
      >
        {uploading ? 'Đang upload...' : 'Upload'}
      </button>

      {imageUrls.length > 0 && (
        <div style={{ marginTop: '20px' }}>
          <h3>Ảnh đã upload:</h3>
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            {imageUrls.map((url, index) => (
              <img 
                key={index} 
                src={url} 
                alt={`Image ${index + 1}`}
                style={{ width: '200px', height: '200px', objectFit: 'cover' }}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default ImageUploadComponent;
```

---

## Vue.js Example

```vue
<template>
  <div>
    <h2>Upload Hình Ảnh</h2>
    
    <input 
      type="file" 
      multiple 
      accept="image/*"
      @change="handleFileSelect" 
    />
    
    <button 
      @click="handleUpload" 
      :disabled="uploading || selectedFiles.length === 0"
    >
      {{ uploading ? 'Đang upload...' : 'Upload' }}
    </button>

    <div v-if="imageUrls.length > 0" style="margin-top: 20px">
      <h3>Ảnh đã upload:</h3>
      <div style="display: flex; gap: 10px; flex-wrap: wrap">
        <img 
          v-for="(url, index) in imageUrls" 
          :key="index"
          :src="url" 
          :alt="`Image ${index + 1}`"
          style="width: 200px; height: 200px; object-fit: cover"
        />
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      selectedFiles: [],
      imageUrls: [],
      uploading: false
    };
  },
  methods: {
    handleFileSelect(e) {
      this.selectedFiles = Array.from(e.target.files);
    },
    async handleUpload() {
      if (this.selectedFiles.length === 0) return;

      this.uploading = true;
      const formData = new FormData();

      this.selectedFiles.forEach(file => {
        formData.append('files', file);
      });

      try {
        const response = await fetch('http://localhost:8080/api/files/upload-multiple', {
          method: 'POST',
          body: formData
        });

        const data = await response.json();
        
        if (data.code === 1000) {
          this.imageUrls = data.result.map(item => item.url);
          alert('Upload thành công!');
        } else {
          alert('Upload thất bại!');
        }
      } catch (error) {
        console.error('Upload error:', error);
        alert('Upload thất bại!');
      } finally {
        this.uploading = false;
      }
    }
  }
};
</script>
```

---

## Environment Variables

**Production:** Cần update BASE_URL dựa trên environment

```javascript
// config.js
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

export const buildImageUrl = (filename) => {
  return `${API_BASE_URL}/files/court-images/${filename}`;
};
```

**Environment Files:**
```bash
# .env.development
REACT_APP_API_URL=http://localhost:8080

# .env.production
REACT_APP_API_URL=https://api.yourdomain.com
```

---

## Security Notes

1. ✅ **Public Access:** Endpoint `/files/**` không cần authentication
2. ✅ **CORS:** Đã được config để cho phép frontend access
3. ⚠️ **File Upload:** Nếu cần bảo mật upload, có thể thêm authentication cho `/files/upload` và `/files/upload-multiple`

**Thêm authentication cho upload (optional):**
```java
// In SecurityConfig.java
private static final String[] PUBLIC_ENDPOINTS = {
    "/auth/**",
    "/court-groups/**",
    "/courts/search",
    "/files/court-images/**"  // Only allow GET images public
    // "/files/upload*" requires authentication
};
```

---

## Troubleshooting

### Lỗi: Cannot load image / 404

**Nguyên nhân:** URL không đúng hoặc file không tồn tại

**Giải pháp:**
1. Kiểm tra URL đầy đủ: `http://localhost:8080/files/court-images/filename.jpg`
2. Verify file tồn tại trong thư mục `uploads/court-images/`
3. Check console logs của backend

### Lỗi: 403 Forbidden khi access ảnh

**Nguyên nhân:** `/files/**` chưa được thêm vào PUBLIC_ENDPOINTS

**Giải pháp:** Đã fix trong SecurityConfig

### Lỗi: CORS error

**Nguyên nhân:** Frontend origin không được allow

**Giải pháp:** Kiểm tra SecurityConfig CORS config:
```java
config.setAllowedOrigins(List.of(
    "http://localhost:3000",
    "http://localhost:5173",
    "http://localhost:8080"
));
```

Add origin của frontend nếu cần.

---

## Summary

### ✅ Backend Changes
1. Thêm `/files/**` vào PUBLIC_ENDPOINTS
2. Tạo `FileUrlBuilder` utility
3. Thêm methods `getFileUrl()` và `getFileUrls()` trong FileStorageService
4. Tạo endpoints `/files/upload` và `/files/upload-multiple` trả về URL đầy đủ

### 📱 Frontend Usage
1. **Upload mới:** Dùng `/files/upload` hoặc `/files/upload-multiple` → nhận URL đầy đủ
2. **Data cũ:** Tự build URL từ filename: `http://localhost:8080/files/court-images/{filename}`
3. **Hiển thị ảnh:** Dùng URL trực tiếp trong `<img src="..." />`

### 🔗 URL Format
```
http://localhost:8080/files/court-images/uuid-generated-name.jpg
```

Không cần token, public access, browser có thể cache!

