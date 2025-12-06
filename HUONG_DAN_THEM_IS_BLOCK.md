# Hướng dẫn thêm trường `is_block` vào response

## Mục đích
Thêm trường `is_block` vào response của endpoint GET `/users` để frontend có thể hiển thị trạng thái khóa tài khoản của user.

## Các bước thực hiện

### 1. Cập nhật Response DTO
**File:** `src/main/java/com/example/booking_service/dto/response/UserListItemResponse.java`

Thêm trường `isBlock` vào class:

```java
@JsonProperty("is_block")
Boolean isBlock;
```

**Vị trí:** Thêm vào cuối class, sau trường `ownerVerifiedAt`

### 2. Cập nhật Service Mapper
**File:** `src/main/java/com/example/booking_service/service/UserService.java`

Trong method `toListItemResponse()`, thêm vào builder:

```java
.isBlock(user.getIsBlock() != null ? user.getIsBlock() : false)
```

**Vị trí:** Thêm vào cuối builder chain, sau `.ownerVerifiedAt(...)`

## Kết quả

- Endpoint GET `/users` sẽ trả về trường `is_block` (Boolean) cho mỗi user trong danh sách
- Giá trị: `true` = tài khoản bị khóa, `false` = tài khoản hoạt động bình thường
- Không cần thêm tham số filter, chỉ cần thêm trường vào response

## Lưu ý

- Entity `User` đã có trường `isBlock` (Boolean) map với cột `is_block` trong database
- Không cần sửa entity hoặc repository
- Response sẽ có format: `"is_block": true` hoặc `"is_block": false`

