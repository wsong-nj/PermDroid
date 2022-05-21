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

 Date: 27/04/2022 19:09:58
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for edge
-- ----------------------------
DROP TABLE IF EXISTS `edge`;
CREATE TABLE `edge`  (
  `ID` bigint(20) NULL DEFAULT NULL,
  `edgeLabel` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `srcID` bigint(20) NULL DEFAULT NULL,
  `tgtID` bigint(20) NULL DEFAULT NULL,
  `widgetID` bigint(20) NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
