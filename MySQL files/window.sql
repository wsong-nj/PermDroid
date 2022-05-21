/*
 Navicat MySQL Data Transfer

 Source Server         : connection
 Source Server Type    : MySQL
 Source Server Version : 50713
 Source Host           : localhost:3306
 Source Schema         : stg

 Target Server Type    : MySQL
 Target Server Version : 50713
 File Encoding         : 65001

 Date: 27/04/2022 19:10:35
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for window
-- ----------------------------
DROP TABLE IF EXISTS `window`;
CREATE TABLE `window`  (
  `ID` bigint(20) NULL DEFAULT NULL,
  `winName` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `optionsMenuID` bigint(20) NULL DEFAULT NULL,
  `contextMenuID` bigint(20) NULL DEFAULT NULL,
  `leftDrawerID` bigint(20) NULL DEFAULT NULL,
  `rightDrawerID` bigint(20) NULL DEFAULT NULL,
  `fragSizes` int(11) NULL DEFAULT NULL,
  `fragIDString` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `widgetSizes` int(11) NULL DEFAULT NULL,
  `isTest` tinyint(255) NULL DEFAULT NULL,
  `permissions` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `isActivityTest` int(11) NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
