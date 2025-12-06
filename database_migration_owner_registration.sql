-- =====================================================
-- Database Migration for Owner Registration Feature
-- =====================================================
-- This script adds necessary columns for owner registration
-- Run this script on your booking-service database

USE `booking-service`;

-- 1. Add owner_status column to users table
-- Stores the approval status of owner applications
ALTER TABLE `users` 
ADD COLUMN `owner_status` ENUM('PENDING', 'APPROVED', 'REJECTED', 'BANNED') DEFAULT NULL
COMMENT 'Status của đơn đăng ký owner: PENDING=Chờ duyệt, APPROVED=Đã duyệt, REJECTED=Từ chối, BANNED=Bị cấm'
AFTER `role`;

-- 2. Add ID card image columns
-- Stores filenames of uploaded ID card images
ALTER TABLE `users` 
ADD COLUMN `id_card_front` VARCHAR(255) DEFAULT NULL 
COMMENT 'Tên file ảnh CMND/CCCD mặt trước' 
AFTER `avatar`;

ALTER TABLE `users` 
ADD COLUMN `id_card_back` VARCHAR(255) DEFAULT NULL 
COMMENT 'Tên file ảnh CMND/CCCD mặt sau' 
AFTER `id_card_front`;

-- 3. Add owner verification timestamp
-- Records when admin approved the owner application
ALTER TABLE `users` 
ADD COLUMN `owner_verified_at` DATETIME DEFAULT NULL 
COMMENT 'Thời điểm admin duyệt đơn đăng ký owner' 
AFTER `bank_account_name`;

-- 4. Add indexes for better query performance
ALTER TABLE `users` 
ADD INDEX `idx_role` (`role`);

ALTER TABLE `users` 
ADD INDEX `idx_owner_status` (`owner_status`);

ALTER TABLE `users` 
ADD INDEX `idx_role_owner_status` (`role`, `owner_status`);

-- =====================================================
-- Verification Queries
-- =====================================================
-- Run these to verify the migration was successful:

-- Check users table structure
DESCRIBE `users`;

-- Check if new columns exist
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'booking-service' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME IN ('owner_status', 'id_card_front', 'id_card_back', 'owner_verified_at');

-- Check indexes
SHOW INDEX FROM `users` WHERE Key_name IN ('idx_role', 'idx_owner_status', 'idx_role_owner_status');

-- =====================================================
-- Sample Data for Testing (Optional)
-- =====================================================
-- Test owner with PENDING status
-- Note: Password is hashed version of "123456" using BCrypt

-- INSERT INTO `users` (full_name, email, password, phone, role, owner_status, 
--     bank_name, bank_account_number, bank_account_name, 
--     id_card_front, id_card_back, bank_qr_image, created_at)
-- VALUES (
--     'Test Owner',
--     'testowner@example.com',
--     '$2a$10$XLGiZ7.PMDdtGgpoQxAWOuR.Py9qlCIA3HwQTITYUeAkxbiHLU8za',
--     '0912345678',
--     'OWNER',
--     'PENDING',
--     'Vietcombank',
--     '1234567890',
--     'NGUYEN VAN A',
--     'id_card_front_uuid.jpg',
--     'id_card_back_uuid.jpg',
--     'bank_qr_uuid.png',
--     NOW()
-- );

-- =====================================================
-- Queries for Admin Dashboard
-- =====================================================

-- Get all PENDING owner applications
-- SELECT id, full_name, email, phone, bank_name, bank_account_number, 
--        id_card_front, id_card_back, bank_qr_image, created_at
-- FROM users 
-- WHERE role = 'OWNER' AND owner_status = 'PENDING'
-- ORDER BY created_at DESC;

-- Approve an owner application
-- UPDATE users 
-- SET owner_status = 'APPROVED',
--     owner_verified_at = NOW()
-- WHERE id = ? AND role = 'OWNER';

-- Reject an owner application
-- UPDATE users 
-- SET owner_status = 'REJECTED'
-- WHERE id = ? AND role = 'OWNER';

-- Ban an owner
-- UPDATE users 
-- SET owner_status = 'BANNED'
-- WHERE id = ? AND role = 'OWNER';

-- Get statistics
-- SELECT 
--     COUNT(*) as total_owners,
--     SUM(CASE WHEN owner_status = 'PENDING' THEN 1 ELSE 0 END) as pending,
--     SUM(CASE WHEN owner_status = 'APPROVED' THEN 1 ELSE 0 END) as approved,
--     SUM(CASE WHEN owner_status = 'REJECTED' THEN 1 ELSE 0 END) as rejected,
--     SUM(CASE WHEN owner_status = 'BANNED' THEN 1 ELSE 0 END) as banned
-- FROM users 
-- WHERE role = 'OWNER';

-- =====================================================
-- Rollback Script (if needed)
-- =====================================================
-- Uncomment and run if you need to rollback the migration:

-- ALTER TABLE `users` DROP INDEX `idx_role_owner_status`;
-- ALTER TABLE `users` DROP INDEX `idx_owner_status`;
-- ALTER TABLE `users` DROP INDEX `idx_role`;
-- ALTER TABLE `users` DROP COLUMN `owner_verified_at`;
-- ALTER TABLE `users` DROP COLUMN `id_card_back`;
-- ALTER TABLE `users` DROP COLUMN `id_card_front`;
-- ALTER TABLE `users` DROP COLUMN `owner_status`;

-- =====================================================
-- Important Notes
-- =====================================================
-- 1. owner_status is NULL for regular USER and ADMIN accounts
-- 2. owner_status is set to PENDING when owner registers
-- 3. Only APPROVED owners can login and manage courts
-- 4. ID card images and bank QR images are stored in uploads/court-images/
-- 5. Files are served via /api/uploads/{filename} endpoint




















