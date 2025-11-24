-- =====================================================
-- Database Migration for Payment Feature
-- =====================================================
-- This script adds necessary columns for payment functionality
-- Run this script on your booking-service database

USE `booking-service`;

-- 1. Add payment_proof column to bookings table
-- Stores the filename of the payment proof image uploaded by user
ALTER TABLE `bookings` 
ADD COLUMN `payment_proof` VARCHAR(255) DEFAULT NULL COMMENT 'Tên file ảnh chuyển khoản' 
AFTER `address`;

-- 2. Ensure created_at has default value for auto-timestamp
ALTER TABLE `bookings` 
MODIFY COLUMN `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 3. Add bank information columns to users table (if not already exists)
-- These store bank account details of court owners for receiving payments

-- Check if columns exist before adding (MySQL 5.7+ syntax)
-- If you're using older MySQL version, run each ALTER TABLE separately and handle errors manually

ALTER TABLE `users` 
ADD COLUMN IF NOT EXISTS `bank_qr_image` VARCHAR(255) DEFAULT NULL 
COMMENT 'Link ảnh QR code ngân hàng (VD: https://.../qr-vietcombank.jpg)' 
AFTER `avatar`;

ALTER TABLE `users` 
ADD COLUMN IF NOT EXISTS `bank_name` VARCHAR(100) DEFAULT NULL 
COMMENT 'Tên ngân hàng (VD: Vietcombank, Techcombank, BIDV...)' 
AFTER `bank_qr_image`;

ALTER TABLE `users` 
ADD COLUMN IF NOT EXISTS `bank_account_number` VARCHAR(50) DEFAULT NULL 
COMMENT 'Số tài khoản ngân hàng' 
AFTER `bank_name`;

ALTER TABLE `users` 
ADD COLUMN IF NOT EXISTS `bank_account_name` VARCHAR(255) DEFAULT NULL 
COMMENT 'Chủ tài khoản (họ tên in hoa)' 
AFTER `bank_account_number`;

-- 4. Add indexes for better query performance
ALTER TABLE `bookings` 
ADD INDEX `idx_status_created_at` (`status`, `created_at`);

-- =====================================================
-- Verification Queries
-- =====================================================
-- Run these to verify the migration was successful:

-- Check bookings table structure
DESCRIBE `bookings`;

-- Check users table structure  
DESCRIBE `users`;

-- Check if payment_proof column exists
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'booking-service' 
  AND TABLE_NAME = 'bookings' 
  AND COLUMN_NAME = 'payment_proof';

-- Check if bank columns exist in users table
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'booking-service' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME IN ('bank_qr_image', 'bank_name', 'bank_account_number', 'bank_account_name');

-- =====================================================
-- Sample Data Update (Optional)
-- =====================================================
-- Update existing owner's bank information for testing

-- UPDATE `users` 
-- SET 
--     `bank_name` = 'MB BANK',
--     `bank_account_number` = '2136668885959',
--     `bank_account_name` = 'NGUYEN DA NHAT',
--     `bank_qr_image` = 'bankqr.png'
-- WHERE `id` = 5 AND `role` = 'OWNER';

-- =====================================================
-- Rollback Script (if needed)
-- =====================================================
-- Uncomment and run if you need to rollback the migration:

-- ALTER TABLE `bookings` DROP COLUMN `payment_proof`;
-- ALTER TABLE `bookings` DROP INDEX `idx_status_created_at`;
-- ALTER TABLE `users` DROP COLUMN `bank_qr_image`;
-- ALTER TABLE `users` DROP COLUMN `bank_name`;
-- ALTER TABLE `users` DROP COLUMN `bank_account_number`;
-- ALTER TABLE `users` DROP COLUMN `bank_account_name`;


