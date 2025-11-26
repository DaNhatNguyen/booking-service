# Payment API Documentation

## Tổng quan

Hệ thống thanh toán cho phép người dùng:
1. Đặt sân và nhận thông tin thanh toán
2. Chuyển khoản qua ngân hàng của chủ sân
3. Upload ảnh chụp màn hình chuyển khoản
4. Tự động hủy booking nếu không thanh toán trong 5 phút

---

## Flow hoạt động

```
User chọn sân → Tạo booking (status = PAYING) → Xem thông tin thanh toán
    ↓
Chuyển khoản ngân hàng → Upload ảnh chuyển khoản → Status = PENDING
    ↓
Owner xác nhận → Status = CONFIRMED
```

**Timeout:** Nếu không thanh toán trong 5 phút, booking tự động bị xóa bởi scheduled job.

---

## API Endpoints

### 1. Tạo Booking (Modified)

**Endpoint:** `POST /api/bookings/confirmation`

**Thay đổi:** 
- Status mặc định từ `PENDING` → `PAYING`
- Message thay đổi thành "Đã tạo booking. Vui lòng thanh toán trong 5 phút"

**Request:**
```json
{
  "user_id": 1,
  "court_id": 30,
  "booking_date": "2025-11-23",
  "time_slots": [
    {
      "start_time": "18:00",
      "end_time": "19:00"
    }
  ],
  "total_price": 200000,
  "court_group_id": 22,
  "full_address": "18 Tam Trinh, Hoàng Mai, Hà Nội"
}
```

**Response:**
```json
{
  "result": {
    "booking_id": 43,
    "user_id": 1,
    "court_id": 30,
    "booking_date": "2025-11-23",
    "start_time": "18:00",
    "end_time": "19:00",
    "status": "PAYING",
    "total_price": 200000,
    "address": "18 Tam Trinh, Hoàng Mai, Hà Nội",
    "message": "Đã tạo booking. Vui lòng thanh toán trong 5 phút"
  }
}
```

---

### 2. Lấy Thông Tin Thanh Toán ⭐ NEW

**Endpoint:** `GET /api/bookings/{booking_id}/payment-info`

**Mục đích:** Lấy thông tin ngân hàng của chủ sân để thanh toán

**Headers:**
```
Authorization: Bearer {token}
```

**Response:**
```json
{
  "result": {
    "booking_id": 43,
    "owner_bank_name": "MB BANK",
    "owner_bank_account_number": "2136668885959",
    "owner_bank_account_name": "NGUYEN DA NHAT",
    "owner_bank_qr_image": "bankqr.png",
    "total_price": 200000,
    "booking_date": "2025-11-23",
    "time_slots": [
      {
        "start_time": "18:00",
        "end_time": "19:00"
      }
    ],
    "court_name": "Sân 4",
    "full_address": "18 Tam Trinh, Hoàng Mai, Hà Nội",
    "created_at": "2025-11-23T15:52:39"
  }
}
```

**Lưu ý quan trọng:**
- `created_at` dùng để tính thời gian còn lại (5 phút - (now - created_at))
- Frontend cần tính: `remainingSeconds = 300 - (currentTime - createdAt)`

**Error Responses:**
- `404 Not Found`: Booking không tồn tại
- `400 Bad Request`: Booking không ở trạng thái PAYING

---

### 3. Xác Nhận Thanh Toán ⭐ NEW

**Endpoint:** `POST /api/bookings/{booking_id}/confirm-payment`

**Mục đích:** Upload ảnh chuyển khoản và chuyển status sang PENDING

**Headers:**
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Request (FormData):**
```
payment_proof: File (image file)
```

**Response:**
```json
{
  "result": {
    "booking_id": 43,
    "status": "PENDING",
    "payment_proof_url": "d4e5f6g7-h8i9-j0k1-l2m3-n4o5p6q7r8s9.jpg",
    "message": "Đã xác nhận thanh toán. Chúng tôi sẽ xác minh trong thời gian sớm nhất."
  }
}
```

**Frontend Example (React/Axios):**
```javascript
const confirmPayment = async (bookingId, imageFile) => {
  const formData = new FormData();
  formData.append('payment_proof', imageFile);
  
  const response = await axios.post(
    `${API_URL}/bookings/${bookingId}/confirm-payment`,
    formData,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    }
  );
  
  return response.data;
};
```

**Error Responses:**
- `404 Not Found`: Booking không tồn tại
- `400 Bad Request`: Booking không ở trạng thái PAYING
- `413 Payload Too Large`: File quá lớn (max 10MB)

---

### 4. Hủy Booking Hết Hạn (Optional) ⭐ NEW

**Endpoint:** `DELETE /api/bookings/{booking_id}/cancel-expired`

**Mục đích:** Xóa booking đã quá 5 phút (thường không cần gọi vì có scheduled job)

**Headers:**
```
Authorization: Bearer {token}
```

**Response:**
```json
{
  "result": {
    "booking_id": 43,
    "message": "Đã hủy booking do hết thời gian thanh toán"
  }
}
```

**Error Responses:**
- `404 Not Found`: Booking không tồn tại
- `400 Bad Request`: Booking chưa hết hạn hoặc không ở trạng thái PAYING

---

## Backend Scheduled Job ⭐ QUAN TRỌNG

### Tự động xóa booking hết hạn

**File:** `BookingCleanupScheduler.java`

**Chức năng:**
- Chạy mỗi **1 phút** (60000ms)
- Tự động xóa tất cả booking có:
  - `status = 'PAYING'`
  - `created_at < (hiện tại - 5 phút)`

**Tại sao cần:**
- User có thể thoát trang thanh toán
- User đóng browser
- Mất kết nối mạng
- → Booking vẫn còn trong DB và chiếm slot

**Log Output:**
```
2025-11-23 16:00:00 INFO  - ✓ Cleaned up 3 expired booking(s) with PAYING status
```

---

## Database Schema Changes

### Bảng `bookings`

**Cột mới:**
```sql
payment_proof VARCHAR(255) DEFAULT NULL 
COMMENT 'Tên file ảnh chuyển khoản'
```

**Index mới:**
```sql
INDEX idx_status_created_at (status, created_at)
```

### Bảng `users`

**Cột mới (cho OWNER):**
```sql
bank_qr_image VARCHAR(255) DEFAULT NULL
bank_name VARCHAR(100) DEFAULT NULL
bank_account_number VARCHAR(50) DEFAULT NULL
bank_account_name VARCHAR(255) DEFAULT NULL
```

**Migration Script:** Xem file `database_migration_payment.sql`

---

## Testing Guide

### 1. Test Flow hoàn chỉnh

```bash
# 1. Tạo booking
curl -X POST http://localhost:8080/api/bookings/confirmation \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "user_id": 1,
    "court_id": 30,
    "booking_date": "2025-11-25",
    "time_slots": [{"start_time": "18:00", "end_time": "19:00"}],
    "total_price": 200000,
    "court_group_id": 22,
    "full_address": "18 Tam Trinh"
  }'

# Response: booking_id = 44, status = PAYING

# 2. Lấy thông tin thanh toán
curl -X GET http://localhost:8080/api/bookings/44/payment-info \
  -H "Authorization: Bearer YOUR_TOKEN"

# 3. Upload ảnh chuyển khoản
curl -X POST http://localhost:8080/api/bookings/44/confirm-payment \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "payment_proof=@/path/to/payment_screenshot.jpg"

# 4. Kiểm tra status đã chuyển sang PENDING
curl -X GET http://localhost:8080/api/bookings/44 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 2. Test Timeout (Expired Booking)

```bash
# 1. Tạo booking
curl -X POST http://localhost:8080/api/bookings/confirmation \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{ ... }'

# 2. Đợi > 5 phút (hoặc sửa thời gian trong code để test nhanh)

# 3. Kiểm tra booking đã bị xóa
curl -X GET http://localhost:8080/api/bookings/45 \
  -H "Authorization: Bearer YOUR_TOKEN"
# Expected: 404 Not Found hoặc status đã thay đổi
```

### 3. Test Scheduled Job

**Cách 1: Đợi 1 phút và xem log**
```
2025-11-23 16:01:00 DEBUG - Running scheduled cleanup of expired bookings...
2025-11-23 16:01:00 INFO  - ✓ Cleaned up 1 expired booking(s) with PAYING status
```

**Cách 2: Sửa thời gian chạy để test nhanh**
```java
// Trong BookingCleanupScheduler.java
@Scheduled(fixedRate = 10000) // 10 giây thay vì 60000
```

---

## Frontend Integration

### React Example

```jsx
import { useState, useEffect } from 'react';
import axios from 'axios';

const PaymentPage = ({ bookingId }) => {
  const [paymentInfo, setPaymentInfo] = useState(null);
  const [countdown, setCountdown] = useState(300); // 5 minutes
  const [imageFile, setImageFile] = useState(null);

  // Fetch payment info
  useEffect(() => {
    const fetchPaymentInfo = async () => {
      const response = await axios.get(
        `${API_URL}/bookings/${bookingId}/payment-info`,
        {
          headers: { Authorization: `Bearer ${token}` }
        }
      );
      setPaymentInfo(response.data.result);
      
      // Calculate remaining time
      const createdAt = new Date(response.data.result.created_at);
      const elapsed = Math.floor((Date.now() - createdAt) / 1000);
      const remaining = Math.max(0, 300 - elapsed);
      setCountdown(remaining);
    };

    fetchPaymentInfo();
  }, [bookingId]);

  // Countdown timer
  useEffect(() => {
    if (countdown <= 0) {
      alert('Hết thời gian thanh toán!');
      window.location.href = '/bookings';
      return;
    }

    const timer = setInterval(() => {
      setCountdown(prev => prev - 1);
    }, 1000);

    return () => clearInterval(timer);
  }, [countdown]);

  // Handle image upload
  const handleConfirmPayment = async () => {
    if (!imageFile) {
      alert('Vui lòng chọn ảnh chuyển khoản');
      return;
    }

    const formData = new FormData();
    formData.append('payment_proof', imageFile);

    try {
      const response = await axios.post(
        `${API_URL}/bookings/${bookingId}/confirm-payment`,
        formData,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'multipart/form-data'
          }
        }
      );

      alert(response.data.result.message);
      window.location.href = '/bookings';
    } catch (error) {
      alert('Lỗi: ' + error.response?.data?.message);
    }
  };

  const minutes = Math.floor(countdown / 60);
  const seconds = countdown % 60;

  return (
    <div>
      <h2>Thời gian còn lại: {minutes}:{seconds.toString().padStart(2, '0')}</h2>
      
      {paymentInfo && (
        <>
          <div>
            <h3>Thông tin chuyển khoản</h3>
            <p>Ngân hàng: {paymentInfo.owner_bank_name}</p>
            <p>Số TK: {paymentInfo.owner_bank_account_number}</p>
            <p>Chủ TK: {paymentInfo.owner_bank_account_name}</p>
            <p>Số tiền: {paymentInfo.total_price.toLocaleString()} VNĐ</p>
            
            <img 
              src={`${API_URL}/uploads/${paymentInfo.owner_bank_qr_image}`}
              alt="QR Code"
            />
          </div>

          <div>
            <input 
              type="file" 
              accept="image/*"
              onChange={(e) => setImageFile(e.target.files[0])}
            />
            <button onClick={handleConfirmPayment}>
              Xác nhận đã chuyển khoản
            </button>
          </div>
        </>
      )}
    </div>
  );
};
```

---

## Troubleshooting

### 1. Scheduled job không chạy

**Nguyên nhân:**
- Thiếu `@EnableScheduling` trong configuration
- Application không khởi động đúng

**Giải pháp:**
```java
// Kiểm tra log khi application start
2025-11-23 15:00:00 INFO  - Scheduling has been enabled
```

### 2. Upload file lỗi 413

**Nguyên nhân:** File quá lớn

**Giải pháp:** Tăng limit trong `application.yaml`
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 50MB
```

### 3. QR Code không hiển thị

**Nguyên nhân:** 
- Path sai
- Owner chưa có bank_qr_image

**Giải pháp:**
- Kiểm tra URL: `http://localhost:8080/api/uploads/{filename}`
- Update bank info cho owner

---

## Booking Status Flow

```
PAYING → PENDING → CONFIRMED/REJECTED
  ↓
CANCELLED (nếu hết thời gian)
```

**Chi tiết:**
- `PAYING`: Chờ thanh toán (max 5 phút)
- `PENDING`: Đã upload ảnh, chờ owner xác nhận
- `CONFIRMED`: Owner xác nhận, booking thành công
- `REJECTED`: Owner từ chối
- `CANCELLED`: Hết thời gian hoặc user hủy

---

## Security Notes

1. **Authentication:** Tất cả API đều cần Bearer token
2. **Authorization:** User chỉ có thể thao tác với booking của mình
3. **File Upload:** 
   - Validate file type (image only)
   - Limit file size (max 10MB)
   - Store file với UUID filename để tránh conflict
4. **Database:** 
   - Index trên `(status, created_at)` để scheduled job query nhanh
   - Soft delete booking để giữ lại lịch sử

---

## Performance Considerations

1. **Scheduled Job:**
   - Chạy mỗi 1 phút là hợp lý (không quá thường xuyên, không quá ít)
   - Query có index nên rất nhanh
   - Error handling để không crash app

2. **File Storage:**
   - Store file trên disk (không qua DB)
   - UUID filename để tránh collision
   - Có thể migrate sang S3/Cloud Storage sau

3. **Database Query:**
   - JOIN với 3 bảng (booking, court, court_group, user)
   - Index đã được thêm
   - Cân nhắc cache nếu traffic cao

---

## Maintenance

### Cleanup Payment Proof Images

File ảnh chuyển khoản sẽ tích lũy theo thời gian. Nên có job định kỳ xóa:

```java
// Xóa ảnh của booking > 30 ngày
@Scheduled(cron = "0 0 2 * * ?") // 2AM daily
public void cleanupOldPaymentProofs() {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
    List<Booking> oldBookings = bookingRepository
        .findByCreatedAtBeforeAndPaymentProofNotNull(cutoffDate);
    
    oldBookings.forEach(booking -> {
        fileStorageService.deleteFile(booking.getPaymentProof());
    });
}
```

---

## Contact & Support

Nếu có vấn đề, kiểm tra:
1. Application logs
2. Database structure (run migration script)
3. File permissions (uploads folder)
4. Network/CORS settings

---

**Version:** 1.0  
**Last Updated:** 2025-11-23  
**Author:** DATN Team




