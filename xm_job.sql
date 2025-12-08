/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80040 (8.0.40)
 Source Host           : localhost:3306
 Source Schema         : xm_job

 Target Server Type    : MySQL
 Target Server Version : 80040 (8.0.40)
 File Encoding         : 65001

 Date: 08/12/2025 17:03:24
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for account_balance
-- ----------------------------
DROP TABLE IF EXISTS `account_balance`;
CREATE TABLE `account_balance`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int NOT NULL COMMENT '用户ID（企业employ_id或自由职业者user_id）',
  `user_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户类型：ENTERPRISE/FREELANCER',
  `balance` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
  `frozen_balance` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结余额',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_type`(`user_id` ASC, `user_type` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '账户余额表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of account_balance
-- ----------------------------
INSERT INTO `account_balance` VALUES (1, 2, 'ENTERPRISE', 2400.00, 0.00, '2025-12-07 21:11:21', '2025-12-08 15:35:45');
INSERT INTO `account_balance` VALUES (3, 5, 'FREELANCER', 0.00, 0.00, '2025-12-07 22:38:42', '2025-12-08 16:58:09');

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '账号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '姓名',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像',
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电话',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of admin
-- ----------------------------
INSERT INTO `admin` VALUES (1, 'admin', 'admin', '管理员', 'http://localhost:9090/files/download/1721114905635-柴犬.jpeg', 'ADMIN', '18899990011', 'admin2@xm.com');

-- ----------------------------
-- Table structure for advertise
-- ----------------------------
DROP TABLE IF EXISTS `advertise`;
CREATE TABLE `advertise`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `img` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '主图',
  `position_id` int NULL DEFAULT NULL COMMENT '职位ID',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '位置',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '广告信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of advertise
-- ----------------------------
INSERT INTO `advertise` VALUES (1, 'http://localhost:9090/files/download/1726220275307-center.png', 2, '中心大图');
INSERT INTO `advertise` VALUES (2, 'http://localhost:9090/files/download/1726220394906-center-down.png', 3, '中心小图');
INSERT INTO `advertise` VALUES (3, 'http://localhost:9090/files/download/1726221007583-right-up.png', 6, '右侧大图');
INSERT INTO `advertise` VALUES (4, 'http://localhost:9090/files/download/1726221026122-right-up.png', 10, '左侧大图');
INSERT INTO `advertise` VALUES (5, 'http://localhost:9090/files/download/1726221038869-right-down.png', 7, '右侧小图');
INSERT INTO `advertise` VALUES (6, 'http://localhost:9090/files/download/1726221053051-right-down.png', 12, '左侧小图');

-- ----------------------------
-- Table structure for collect
-- ----------------------------
DROP TABLE IF EXISTS `collect`;
CREATE TABLE `collect`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` int NULL DEFAULT NULL COMMENT '学生ID',
  `position_id` int NULL DEFAULT NULL COMMENT '岗位ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '收藏信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of collect
-- ----------------------------
INSERT INTO `collect` VALUES (1, 1, 11);
INSERT INTO `collect` VALUES (3, 1, 7);
INSERT INTO `collect` VALUES (5, 2, 3);
INSERT INTO `collect` VALUES (6, 2, 7);
INSERT INTO `collect` VALUES (7, 2, 6);

-- ----------------------------
-- Table structure for deliverable
-- ----------------------------
DROP TABLE IF EXISTS `deliverable`;
CREATE TABLE `deliverable`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '成品ID',
  `submission_id` int NOT NULL COMMENT '稿件ID',
  `project_id` int NOT NULL COMMENT '项目ID',
  `freelancer_id` int NOT NULL COMMENT '自由职业者ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '成品标题',
  `description` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '成品描述',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'SUBMITTED' COMMENT '状态：SUBMITTED（已提交）/APPROVED（验收通过）/REJECTED（验收不通过）/EXPIRED（已过期）',
  `submit_count` int NOT NULL DEFAULT 1 COMMENT '提交次数（最多3次）',
  `submitted_at` timestamp NULL DEFAULT NULL COMMENT '提交时间',
  `reviewed_at` timestamp NULL DEFAULT NULL COMMENT '验收时间',
  `review_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '验收意见',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_submission_id`(`submission_id` ASC) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_freelancer_id`(`freelancer_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  CONSTRAINT `deliverable_ibfk_1` FOREIGN KEY (`submission_id`) REFERENCES `submission` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `deliverable_ibfk_2` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `deliverable_ibfk_3` FOREIGN KEY (`freelancer_id`) REFERENCES `freelancer` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '成品提交表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of deliverable
-- ----------------------------
INSERT INTO `deliverable` VALUES (1, 6, 8, 1, '111', '111', 'APPROVED', 3, '2025-12-08 15:50:08', '2025-12-08 15:53:23', '', '2025-12-08 15:13:01', '2025-12-08 15:53:22');

-- ----------------------------
-- Table structure for deliverable_attachment
-- ----------------------------
DROP TABLE IF EXISTS `deliverable_attachment`;
CREATE TABLE `deliverable_attachment`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `deliverable_id` int NOT NULL COMMENT '成品ID',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件URL',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '原文件名',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小(bytes)',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件类型(ext)',
  `upload_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_deliverable_id`(`deliverable_id` ASC) USING BTREE,
  CONSTRAINT `deliverable_attachment_ibfk_1` FOREIGN KEY (`deliverable_id`) REFERENCES `deliverable` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '成品附件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of deliverable_attachment
-- ----------------------------
INSERT INTO `deliverable_attachment` VALUES (1, 1, 'http://localhost:9090/api/attachments/deliverable/download/1', 'OIP.jpg', 11544, 'jpg', '2025-12-08 15:36:40');

-- ----------------------------
-- Table structure for employ
-- ----------------------------
DROP TABLE IF EXISTS `employ`;
CREATE TABLE `employ`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '账号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '名称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'logo',
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色',
  `city` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '城市',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地址',
  `industry_id` int NULL DEFAULT NULL COMMENT '行业id',
  `scale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '规模',
  `stage` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '融资',
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '企业信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of employ
-- ----------------------------
INSERT INTO `employ` VALUES (1, 'meituan', '123456', '美团', 'http://localhost:9090/files/download/1725961087981-meituan.jpg', 'EMPLOY', '北京市', '北京朝阳区美团(恒电大厦)C座', 9, '10000人以上', '已上市', '待审核');
INSERT INTO `employ` VALUES (2, 'pdd', '123456', '拼多多', 'http://localhost:9090/files/download/1725961524658-pinduoduo.jpg', 'EMPLOY', '上海市', '上海长宁区金虹桥国际中心娄山关路533号', 10, '10000人以上', '已上市', '审核通过');
INSERT INTO `employ` VALUES (3, 'boss', '123456', 'Boss直聘', 'http://localhost:9090/files/download/1725961560167-boss.jpg', 'EMPLOY', '北京市', '北京朝阳区冠捷大厦(太阳宫中路)', 8, '10000人以上', '已上市', '审核通过');
INSERT INTO `employ` VALUES (4, 'xunfei', '123456', '科大讯飞', 'http://localhost:9090/files/download/1725961610644-kedaxunfei.jpg', 'EMPLOY', '合肥市', '合肥蜀山区科大讯飞股份有限公司望江西路666号', 11, '10000人以上', '已上市', '审核通过');
INSERT INTO `employ` VALUES (5, 'huawei', '123456', '华为', 'http://localhost:9090/files/download/1726125435606-huawei.jpg', 'EMPLOY', '上海市', '上海市浦东新区金桥经济技术开发区新金桥路2223号', 11, '10000人以上', '不需要融资', '待审核');

-- ----------------------------
-- Table structure for enterprise
-- ----------------------------
DROP TABLE IF EXISTS `enterprise`;
CREATE TABLE `enterprise`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '企业ID',
  `employ_id` int NOT NULL COMMENT '关联Employ表ID',
  `business_license` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '营业执照',
  `verified` tinyint(1) NULL DEFAULT 0 COMMENT '是否认证',
  `verified_at` timestamp NULL DEFAULT NULL COMMENT '认证时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_employ_id`(`employ_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '企业扩展表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of enterprise
-- ----------------------------
INSERT INTO `enterprise` VALUES (1, 2, 'http://localhost:9090/files/download/1765175340177-v2-f1d244083449a21ab70d4e949688f55c_r.jpg', 1, '2025-12-08 06:42:11', '2025-12-07 15:07:53', '2025-12-08 14:42:11');

-- ----------------------------
-- Table structure for freelancer
-- ----------------------------
DROP TABLE IF EXISTS `freelancer`;
CREATE TABLE `freelancer`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自由职业者ID',
  `user_id` int NOT NULL COMMENT '关联User表ID',
  `skills` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '技能标签(逗号分隔)',
  `portfolio_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '作品集链接',
  `portfolio_count` int NULL DEFAULT 0 COMMENT '作品数量',
  `verified` tinyint(1) NULL DEFAULT 0 COMMENT '是否认证',
  `verified_at` timestamp NULL DEFAULT NULL COMMENT '认证时间',
  `verification_info` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '认证信息(JSON)',
  `rating` decimal(3, 2) NULL DEFAULT 0.00 COMMENT '评分',
  `completed_projects` int NULL DEFAULT 0 COMMENT '完成项目数',
  `credit_score` int NULL DEFAULT 100 COMMENT '信誉分（默认100分）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '自由职业者扩展表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of freelancer
-- ----------------------------
INSERT INTO `freelancer` VALUES (1, 5, 'Java，python,vue', '', 0, 1, '2025-12-07 09:36:23', NULL, 0.00, 1, 95, '2025-12-07 15:16:40', '2025-12-08 15:53:22');

-- ----------------------------
-- Table structure for industry
-- ----------------------------
DROP TABLE IF EXISTS `industry`;
CREATE TABLE `industry`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '行业名称',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '行业描述',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '行业信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of industry
-- ----------------------------
INSERT INTO `industry` VALUES (1, '计算机硬件', '这是计算机硬件');
INSERT INTO `industry` VALUES (2, '人工智能 / AI', '这是人工智能 / AI');
INSERT INTO `industry` VALUES (3, '大数据 / 云计算', '这是大数据 / 云计算');
INSERT INTO `industry` VALUES (4, '在线教育', '这是在线教育');
INSERT INTO `industry` VALUES (5, '游戏', '这是游戏');
INSERT INTO `industry` VALUES (6, '半导体 / 芯片', '这是半导体 / 芯片');
INSERT INTO `industry` VALUES (7, '电子 / 硬件开发', '这是电子 / 硬件开发');
INSERT INTO `industry` VALUES (8, '社交网络 / 媒体', '这是社交网络 / 媒体');
INSERT INTO `industry` VALUES (9, '生活服务（o2o）', '这是生活服务（o2o）');
INSERT INTO `industry` VALUES (10, '互联网 / 电子商务', '这是互联网 / 电子商务');
INSERT INTO `industry` VALUES (11, '计算机软件', '这是计算机软件');

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `project_id` int NULL DEFAULT NULL COMMENT '项目ID',
  `submission_id` int NULL DEFAULT NULL COMMENT '稿件ID',
  `sender_id` int NOT NULL COMMENT '发送者ID',
  `sender_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '发送者类型：ENTERPRISE/FREELANCER',
  `recipient_id` int NOT NULL COMMENT '接收者ID',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '消息内容',
  `is_read` tinyint(1) NULL DEFAULT 0 COMMENT '是否已读',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_submission_id`(`submission_id` ASC) USING BTREE,
  INDEX `idx_recipient_id`(`recipient_id` ASC) USING BTREE,
  INDEX `idx_is_read`(`is_read` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of message
-- ----------------------------
INSERT INTO `message` VALUES (1, 1, 5, 5, 'FREELANCER', 2, '111', 0, '2025-12-07 18:39:41');
INSERT INTO `message` VALUES (2, 1, 5, 2, 'ENTERPRISE', 5, '222', 0, '2025-12-07 18:39:54');
INSERT INTO `message` VALUES (3, 1, 5, 5, 'FREELANCER', 2, '🙂你好', 0, '2025-12-07 18:46:06');
INSERT INTO `message` VALUES (4, 1, 5, 5, 'FREELANCER', 2, '你觉得怎么样', 0, '2025-12-07 18:49:49');
INSERT INTO `message` VALUES (5, 1, 5, 5, 'FREELANCER', 2, '111111111111111111', 0, '2025-12-07 18:55:51');
INSERT INTO `message` VALUES (6, 1, 5, 2, 'ENTERPRISE', 5, '😃', 0, '2025-12-07 18:59:56');
INSERT INTO `message` VALUES (7, 1, 5, 5, 'FREELANCER', 2, '5320可以吗', 0, '2025-12-07 21:16:54');
INSERT INTO `message` VALUES (8, 1, 5, 2, 'ENTERPRISE', 5, '可以😁', 0, '2025-12-07 21:17:24');
INSERT INTO `message` VALUES (9, 8, 6, 2, 'ENTERPRISE', 5, '合作嘛', 0, '2025-12-07 22:32:48');
INSERT INTO `message` VALUES (10, 8, 6, 2, 'ENTERPRISE', 5, '低一点', 0, '2025-12-07 22:33:00');
INSERT INTO `message` VALUES (11, 8, 6, 5, 'FREELANCER', 2, '好的', 0, '2025-12-07 22:33:12');
INSERT INTO `message` VALUES (13, 8, NULL, 0, 'ENTERPRISE', 5, '您的项目《开发系统》成品验收失败（已超过截止时间），已扣除保证金并赔付给企业。', 1, '2025-12-08 15:35:45');

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '公告内容',
  `time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '发布时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统公告表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of notice
-- ----------------------------
INSERT INTO `notice` VALUES (4, '测试内容', '1111', '2025-12-07 16:11:10');

-- ----------------------------
-- Table structure for payment_record
-- ----------------------------
DROP TABLE IF EXISTS `payment_record`;
CREATE TABLE `payment_record`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `user_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户类型：ENTERPRISE/FREELANCER',
  `project_id` int NULL DEFAULT NULL COMMENT '关联项目ID',
  `submission_id` int NULL DEFAULT NULL COMMENT '关联稿件ID',
  `payment_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '支付类型：PUBLISH_DEPOSIT（发布保证金）/CONFIRM_PAYMENT（确认合作补款）/ACCEPT_DEPOSIT（接单保证金）/REFUND（退款）/COMPLETION_PAYMENT（完成付款）',
  `amount` decimal(10, 2) NOT NULL COMMENT '支付金额',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING（待支付）/SUCCESS（成功）/FAILED（失败）',
  `payment_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'MOCK' COMMENT '支付方式：MOCK（模拟支付）',
  `transaction_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '交易流水号',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '支付记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of payment_record
-- ----------------------------
INSERT INTO `payment_record` VALUES (1, 2, 'ENTERPRISE', 1, NULL, 'CONFIRM_PAYMENT', 2174.50, 'SUCCESS', 'MOCK', 'MOCK_cdd3e8448a0e4a12b0d5aa76dcd83abf', '确认合作补款（模拟支付）', '2025-12-07 22:00:52', '2025-12-07 22:00:52');
INSERT INTO `payment_record` VALUES (8, 2, 'ENTERPRISE', 8, NULL, 'PUBLISH_DEPOSIT', 1000.00, 'SUCCESS', 'MOCK', 'MOCK_6554fce90181437da5b58157bdf10382', '项目发布保证金（模拟支付）', '2025-12-07 22:23:54', '2025-12-07 22:23:54');
INSERT INTO `payment_record` VALUES (9, 2, 'ENTERPRISE', 8, NULL, 'CONFIRM_PAYMENT', 1400.00, 'SUCCESS', 'MOCK', 'MOCK_830c2a1563e74e00aa8459272c8b1544', '确认合作补款（模拟支付）', '2025-12-07 22:33:37', '2025-12-07 22:33:37');
INSERT INTO `payment_record` VALUES (10, 5, 'FREELANCER', 8, 6, 'ACCEPT_DEPOSIT', 120.00, 'SUCCESS', 'MOCK', 'MOCK_60cb605fadf246d8a3e895c0fde7f9cc', '接单保证金（模拟支付）', '2025-12-07 22:38:45', '2025-12-07 22:38:45');
INSERT INTO `payment_record` VALUES (11, 2, 'ENTERPRISE', 8, NULL, 'REFUND', 2400.00, 'SUCCESS', 'MOCK', 'MOCK_d7c7deec519e4de8872ee57d7d9f7d88', '项目未完成退款', '2025-12-08 15:35:45', '2025-12-08 15:35:45');
INSERT INTO `payment_record` VALUES (12, 5, 'FREELANCER', 8, NULL, 'COMPLETION_PAYMENT', 2280.00, 'SUCCESS', 'MOCK', 'MOCK_2fbe11f41f034dac96a5728fc8b943d6', '项目完成付款', '2025-12-08 15:53:23', '2025-12-08 15:53:23');

-- ----------------------------
-- Table structure for position
-- ----------------------------
DROP TABLE IF EXISTS `position`;
CREATE TABLE `position`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职位名称',
  `employ_id` int NULL DEFAULT NULL COMMENT '企业ID',
  `industry_id` int NULL DEFAULT NULL COMMENT '行业ID',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '求职类型',
  `experience` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工作经验',
  `salary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '薪资待遇',
  `education` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学历要求',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职位标签',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '职位描述',
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职位状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '职位信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of position
-- ----------------------------
INSERT INTO `position` VALUES (1, '前端软件开发工程师', 1, 9, '全职', '应届生', '10-20k', '硕士', '核心部门,福利待遇好,五险一金,节假日双休', '<p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\">岗位职责<br>1. 负责品牌大客在线点餐、品牌营销、会员、预订服务等B、C端的移动端、PC端业务产品开发，以及把控需求评估、方案设计及核心功能实现等重要环节，保障按时保质地交付；<br>2. 应用并共建诸如框架、组件、DevOps等大前端工程化产品，持续提升团队交付效率，保障产品工程质量；<br>3. 迎难而上，保持好奇和学习心态，促进团队共同成长。<br>4. 关注业界大前端动态，探索包括但不限于低代码、自动化等方向，持续迭代技术方案及技术规划。</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\"><br>岗位基本需求<br>1. 2年以上前端开发经验，主导或参与过复杂系统或项目的建设；<br>2. 有良好的技术储备，了解业界常见类库、框架、设计模式、解决方案，并在某一方向有深入的研究；<br>3. 熟练运用前端工程化技术，如Node.js、Webpack/Vite代码构建与优化、持续集成与部署，改善团队研发效率；<br>4. 具备一定的项目管理能力，能承担中大型项目，通过定义清晰的目标、合理的计划、有效的过程管理、控制风险最终达成项目目标；<br>5. 具备良好的团队协作沟通能力，有较强的逻辑思维能力，善于分析、归纳、解决问题，持续学习和总结，自我迭代；<br>6. 有较强的产品和业务理解，能协同产品及业务团队持续推动产品优化、达成业务目标。</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\"><br>具备以下者优先<br>1. 有ReactNative跨端开发经验者优先；<br>2. 热爱互联网和新技术，具有极强的快速学习能力，研究过优秀开源软件的源码并有心得者优先；<br>3. 在工程化、组件库、跨端等方向有深入研究及实践者优先；<br>4. 对研发体系稳定、质量、效率、性能等各方面有系统化认知，并有长远的规划能力者加分。</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\"><br>岗位亮点<br>1. 到店餐饮核心业务的前端团队，聚焦品牌大客业务领域的B、C端全链路，可以快速提升你的业务理解力和技术赋能业务能力；<br>2. 大团队中既有行业大会讲师、社区极客，也在持续不断地培养和引导校招新人，有完整的培养体系和广阔的成长空间。<br>3. 熟悉核心业务的发展过程，迭代认知，沉淀方法。</span></p>', '审核通过');
INSERT INTO `position` VALUES (2, '高级Java开发工程师', 1, 9, '全职', '1到3年', '10-20k', '本科', '个人成长,晋升通道,福利待遇好', '<p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\">部门介绍<br>企业平台研发部作为美团内部企业应用的技术研发团队，肩负着帮助美团运营得更好，经营得更好的使命。我们通过提供多样的工具、系统和平台，致力于打造数字化、智能化的财务、供应链、人力资源等信息化体系，帮助美团职能及业务团队完成精细化管理和高效运营。同时，为美团全体员工提供安全高效的协同办公平台，推动组织效能提升，实现美团组织运营的数字化升级。<br>我们期待优秀的你一起加入，共同建设数据化、智能化的企业研发平台。</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\"><br>岗位职责<br>1.负责美团公司事务平台廉正与阳光职场方向的系统设计与研发；<br>2.负责技术规划和架构设计，结合业务阶段，持续优化演进技术，提高系统能力复用性、扩展性；<br>3.以业务和技术价值为导向，能利用科学的方法进行指标定义、收集、分析，从而进行成果的衡量，对工作改进和提升提供指导；<br>4.指导初级工程师，促进团队共同成长和团队高效率运转；<br>5.跟进研究前沿技术和行业最佳实践，给团队带来创新突破。</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\"><br>岗位基本要求<br>1.三年及以上相关工作经验，具备良好的沟通技巧、较强自驱力、较强结果导向的心智；<br>2.精通Java及面向对象设计开发，熟悉JDK源码及JVM基本原理；<br>3.熟悉常见设计模式，精通Spring，MyBatis等主流开源框架；<br>4.熟悉MySQL应用开发，熟悉数据库原理和常用性能优化技术，以及NoSQL使用场景；<br>5.熟悉HTTPS协议、搜索引擎、缓存、RPC，消息等中间件，并且有相关实践经验；<br>6.重视系统建模方法论，对复杂业务分析、系统建模感兴趣，对系统设计有高标准；<br>7.熟悉DDD、微服务相关理论，能通过模块化、服务化、平台化的建设，降低系统间依赖，提升系统稳定性、可扩展性，提升开发效率。</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\"><br>具备以下条件优先<br>1.参与过大型分布式系统设计开发，对高可用、高性能、可扩展、可伸缩系统有深刻的理解；<br>2.具有敏捷开发经验，实践过持续集成和自动化；<br>3.有过复杂系统或领域驱动设计经验；<br>4.参与过优秀开源项目。</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\"><br>岗位亮点<br>1.良好的团队工作氛围，同事间乐于互助，共同成长；<br>2.团队有丰富的互联网技术储备和沉淀，能够在工作中快速提升核心竞争力；<br>3.面向高复杂度的公司级业务，需要跨业务线的沟通协调能力，助于从复杂的逻辑中抽象解决问题的方法，提高自身推进与解决问题能力。</span></p>', '审核通过');
INSERT INTO `position` VALUES (3, '高级C语言工程师', 5, 11, '全职', '3到5年', '20-50k', '硕士', '高薪,晋升通道,岗位竞争力', '<p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\">1、本科及以上学历，计算机、通信、自动化、电子信息相关专业毕业；<br>2、熟练掌握c/c++语言，熟悉Windows/Linux下常用调试方法和系统编程；<br>3、熟悉TCP/IP协议，熟悉socket编程；<br>4、熟悉常用代码管理工具SVN、CVS等，有良好的代码编写能力和习惯；<br>5、有Windows或Linux下网络通信产品开发经验优先考虑；<br>6、有较强的团队合作精神，主动性强，能够承受较大压力。</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\"><br>岗位职责：<br>1、负责量子通信产品系统软件的开发设计和维护；<br>2、负责开发过程中相关文档的编写和评审；<br>3、参与产品的调试与后期维护。</span></p>', '审核通过');
INSERT INTO `position` VALUES (4, '华为云测试工程师', 5, 11, '全职', '经验不限', '10-20k', '本科', '自动化测试,白盒测试,Shell', '<p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\">华为云数据库相关岗位，此岗位为OD岗位，入职后满足条件可转华为。</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\"><br>【岗位职责】<br>1. 负责华为云数据库的测试设计、测试验证与交付工作,保证产品高质量发布。<br>2. 负责华为云数据库的测试架构设计和开发,支撑自动化测试,提升测试质量和效率。<br>3. 支撑负责华为云数据库的性能/过载测试、可靠性/混沌测试、安全测试、客户化测试等。</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\"><br>【技能要求】<br>1. 熟悉测试基本的技术和方法,包括测试设计、测试执行、测试分析、测试工具、测试仿真、故障注入、自动化等。<br>2. 熟练掌握测试需求分析和设计方法,熟悉黑白盒测试流程,熟悉常见缺陷管理工具。<br>3. 熟悉自动化脚本的开发,熟练使用常用的自动化测试框架,具备独立的问题定位分析能力。<br>4. 有强烈的责任心和使命感,具备良好的沟通能力和团队合作意识。<br>5. 至少熟悉一种编程语言C、C++、java、Python、Go、shell等。</span></p>', '审核通过');
INSERT INTO `position` VALUES (5, '高级运营', 2, 10, '全职', '3到5年', '20-50k', '硕士', '五险一金,零食水果,福利待遇好,薪资高', '<p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">岗位职责： </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">1、社区团购-多多买菜业务，通过上门拜访，与商户（便利店、商铺等）谈判并达成合作； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">2、负责区域内客情维护，引导团长完成分享与推广任务，提高团长经营收入。 </span></p><p style=\"line-height: 2;\"><br></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">任职要求：</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">1、学历不限，工作区域可就近安排；</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">2、有销售经验，尤其有互联网、美团优选、淘菜菜，京喜拼拼、兴盛优选、十荟团、同程生活等社区团购经验者优先考虑； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">3、适应互联网的快工作节奏，极致追求结果达成。 目前岗位在招区域:陈村 北滘 伦教 大良 龙江 </span></p><p style=\"line-height: 2;\"><br></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">岗位要求: </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">1.自备电动车 ，在家附近上班，不包吃住 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">2.名下没有营业执照 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">3.善于沟通，乐于沟通，敢陌拜 </span></p><p style=\"line-height: 2;\"><br></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">工作内容：</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">维护区域内的团长，加团长微信，做好微信群运营，前期每天上门拜访20个，前期有师傅带 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">工作时间：</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">9点到6点，做六休一，外勤打卡 不用到公司报道 底薪3700+绩效12000，一个月平均8000-15000 如果满足以上条件，可以发简历和电话过来，进一步沟通</span></p>', '审核通过');
INSERT INTO `position` VALUES (6, 'Java软件开发工程师', 2, 10, '全职', '应届生', '10-20k', '硕士', 'Java,Springboot,Vue,福利待遇好,薪资高', '<p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">岗位职责： </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">1、负责仓储供应链基础功能的研发； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">2、学习理解能力强，有能力主导系统的分析，架构设计，核心功能开发； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">3、对系统稳定性和潜在问题高度敏感，主动发现潜在风险，并积极推动落地解决； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">4、较好的代码规范和文档规范，保证项目的进度和质量。 </span></p><p style=\"line-height: 2;\"><br></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">任职要求： </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">1、计算机软件或相关专业本科及以上学历优先，</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">5年以上分布式系统开发经验； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">2、Java基础扎实，理解并发，IO，对JVM原理和调优有一定的经验； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">3、熟悉web开发常用开发框架和工具，spring，spring boot，数据库，缓存，ES，flink等，有较强的编程能力和编程经验； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">4、熟悉分布式系统的设计和应用，有高并发系统的开发和调优经验，有长期线上系统稳定性治理和维护的经验； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">5、对技术有强烈的兴趣，学习能力强，具备较好的沟通能力，对线上系统能保持敬畏之心。 </span></p><p style=\"line-height: 2;\"><br></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">加分项： 有主导大型项目开发经验者优先</span></p>', '审核通过');
INSERT INTO `position` VALUES (7, '前端开发', 4, 11, '全职', '1到3年', '10-20k', '本科', '前端,css,JavaScript,Vue', '<p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">关于我们: </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">我们是美团外卖成都研发中心前端组，在这里我们通过在业务中的不断探索和实践，期望通过配置化的战略来提升中后台系统的前端研发效率，建立行业标准，成为行业标杆。</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">关于你: </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">期望你是一位对前端充满激情和富有经验的工程师，有永远向上的心态和积极努力的态度。 我们在成都等你，春天去龙泉山看桃花，夏天去青城山消暑，秋天去四姑娘山看雪山红叶，冬天去吃最正宗的火锅。 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">岗位职责: </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">●负责美团外卖CRM相关业务的前端技术方案设计、需求把控、功能开发，将业务需求拆解细化并实施; </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">●探索和实践针对中后台系统的前端体系化研发方案，提升前端研发效率; </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">●促进团队与产品及相关团队的密切合作。 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">岗位要求: </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">1)岗位基本要求: </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">●3年及以上互联网研发工作经验; </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">●具有扎实的计算机科学功底，扎实的编程基础和数据结构算法基础，较强的编程能力和问题解决能力; 对前端相关框架有深入研究，以及前端工程化经验，并在大中型项目中有过实际应用和调优经历; </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">●对前端的基础知识，CSS、 JavaScript、 性能优化、网络原理等有深入的理解; </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">●善于交流，有良好的团队合作精神和协调沟通能力，有与产品、后端、移动端等多方密切配合的经验和意识; </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">●全日制统招本科及以上学历。 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">2)具备以下者优先: </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">●热爱互联网和新技术，具有极强的快速学习能力，研究过优秀开源软件的源码并有心得者优先; </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">●有移动端或类似系统的研发经历; </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">●有“代码洁癖”、有极客精神。 欢迎有想法的同学私聊</span></p>', '审核通过');
INSERT INTO `position` VALUES (8, 'Java后端开发', 4, 11, '全职', '1到3年', '10-20k', '本科', '福利待遇好,晋升空间大', '<p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">岗位职责： </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">1、负责到家站外广告系统的相关设计和研发，包括但不限于DSP平台、广告投放引擎等的搭建和开发</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">2、协同业务方探索到家商家广告在外部场景下的投放，支撑海量投放业务快速落地 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">3、支持广告投放在工程落地上的实践和调优，沉淀业务优秀实践，保障所有服务稳定运行 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">岗位基本要求： </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">1、计算机相关专业，本科及以上学历 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">2、三年以上的开发经验，精通Java后台服务研发 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">3、对技术有追求，具备较强的代码编写和问题解决能力 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">4、善于交流，有良好的团队合作精神和协调沟通能力，有强烈的责任心和主人翁意识 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">具备以下优先： </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">1、有广告系统、推荐系统相关的工程开发经验者优先 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">2、有海量分布式系统架构设计、优化经验者优先 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">3、熟悉hadoop, flink, storm, HBase, hive, spark, storm等分布式大数据处理技术优先 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">岗位亮点：</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\"> 广告业务是互联网重要的变现方式之一 广告投放系统承载亿级用户、百亿级流量，有海量数据和高并发的技术挑战 广告业务结合了海量的系统架构和推荐算法（是AI落地产生价值的重要场景） 部门介绍： 美团的使命是“帮大家吃得更好，生活更好”。</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">作为一家生活服务电子商务平台，公司聚焦“Food +Platform”战略，以“吃”为核心，通过科技创新，和广大商户与各类合作伙伴一起，努力为消费者提供品质生活，推动生活服务业需求侧和供给侧数字化升级。 2018年9月20日，美团正式在港交所挂牌上市。美团将始终坚持以客户为中心，不断加大在科技研发方面的投入，更好承担社会责任，更多创造社会价值，与广大合作伙伴一起发展共赢。</span></p>', '审核通过');
INSERT INTO `position` VALUES (9, '抖音直播内容运营', 4, 11, '全职', '1到3年', '10-20k', '硕士', '抖音直播,千川投流,福利待遇好', '<p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\">岗位职责：<br>1、需要有抖音相关经验，其中直播投放相关经验者优先；<br>2、能够完成短视频创意、内容策划，独立撰稿及制作分镜脚本，优化与迭代渠道投放素材内容的表达，提升素材整体的爆款率和转化率；<br>3、负责短视频项目拍摄执行，跟进及反馈所负责项目制作进度，对产出结果负责；<br>4、熟悉媒体环境，研究行业热门的各类短视频作品，收集和分析行业视频创意(不限于种草、日常、直播预热等视频)会总结与方法论辅助提升团队短视频能力；<br>5、对项目有完善的策划能力，能独立完成短视频整体项目的方向把控和系列性内容策划。</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\"><br>岗位要求：<br>1、3年左右工作经验，本科及以上学历编导、传媒、广播电视、新闻学等相关专业优先，能自编自导自剪优先；<br>2、对新媒体内容运营有一定认知，有较强的创造性、发散性思维能力，良好的策划能力、文案能力、沟通能力；<br>3、具备用户运营以及产品运营思维，营销思维强，销售导向；<br>4、有高度的责任心和自驱力执行力强能主动思考推动工作的优化。</span></p>', '审核通过');
INSERT INTO `position` VALUES (10, '星火军团产品经理', 4, 11, '全职', '1到3年', '10-20k', '硕士', 'B端产品,商业化,晋升通道', '<p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\">工作职责<br>1、负责代码大模型产品的商业化工作，根据产品矩阵及市场情况，制定商业化策略<br> 2、通过产品数据分析及市场洞察，驱动产品进行迭代更新，负责用户触达、转化、留存等生命周期的提升 <br>3、跟踪行业/竞品最新动向和分析政策趋势变化，挖掘商业化的创新及改进的潜在机会<br> 4、通过一些列商业化方案举措达成组织战略目标</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\"><br>任职要求<br>1、本科及以上学历，有产品商业化实践经验为佳 <br>2、具备较好的目标感及逻辑思维，商业化嗅觉灵敏，对数据敏感，视角多元，深刻理解用户需求，通过数据分析和挖掘制定商业化策略，不断扩展产品市场规模<br> 3、具备高度的责任心和良好的抗压能力、协同推进能力，善于学习和独立思考 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 15px;\"><br>此岗位为科大讯飞集团统一招聘岗位，人员通过简历筛选、笔试、初试、复试、终审等环节后录用。</span></p>', '审核通过');
INSERT INTO `position` VALUES (11, '运营经理', 3, 8, '全职', '1到3年', '10-20k', '硕士', '待遇好,高速成长', '<p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">岗位诱惑：</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">高速成长，团队氛围好，待遇好，优秀员工平均工资都过万！ </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">岗位职责： </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">1，想象一下你会遇到五星级酒店的经理人，身价几千万的连锁大客户，别具风格的网红餐厅。也有路边十平米得小店，……我们需要你根据客户需求，制定个性化的营销方案，与商户谈判并达成合作。 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">2，开发及维护老客户，与各商户建立长期稳定的合作关系。 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">3，通过电话营销+上门拜访面销，执行公司销售政策，帮助客户做好效果，实现合作共赢。 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">4，与公司各部门有效配合，并快速有效的解决商户上线前后遇到的问题及突发事件，提高商家满意度。 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">岗位要求： </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">1，硕士学历 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">2，一年以上的销售经验，行业不限，其中有面对面销售，电商行业销售，互联网广告销售，广告从业经验者优先考虑，优秀的应届生可以择优录取 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">3，具备较强的人际沟通能力，及逻辑思维能力 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">4，认真负责，吃苦耐劳，对工作有激情，有上进心 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">5，热爱销售工作，有强烈的成功欲望和赚钱的企图心 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">薪酬福利： </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">1，美团铁军的履历，极高的行业认可度！ </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">2，互联网行业最优的销售及营销策划培训，你可以学到，餐饮从0到1的经典营销案例，网红爆款的如何打造…… </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">3，完善的培训机制，新人入职7天内即可进行培训，采取师徒制加主管直带双向管理培训，1个月内帮助你快速成长，签单！ </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">4，6-8k+薪酬绩效，规范透明，公司竞争的工作环境 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">5，完善的福利待遇，五险一金，分量足够的年终奖，各种补贴，体检，团建及合作精美的节日礼品 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">6，活动多，每月不低于2次团建，每个季度不定期的团建出游活动 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">7，双休，我们是一群没有后台没有背景靠自己打拼的有为青年，平均年龄25岁，沟通0障碍，团队氛围好，如果你想赚钱，如果想有更好的发展，请加入我们！ </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">公司介绍： 这是高速发展得互联网公司 你将和有战斗力的团队和有激情的伙伴一起工作 这是一份付出和回报对等的工作 这是自由发展得公司 覆盖全国一二三线超过150个城市的工作地点，可供选择 你还等什么，跟我沟通，发我简历 期待你的加入！</span></p>', '审核通过');
INSERT INTO `position` VALUES (12, '岗位运营', 3, 8, '全职', '应届生', '5-10k', '本科', '福利待遇好,晋升空间大', '<p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">岗位职责：</span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\"> 1、拓展及维护酒店商家，与商家建立长期稳定的合作关系； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">2、通过电话沟通、直面约谈，对酒店进行考察评估、洽谈合作，并根据商家需求制定合理的酒店运营方案； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">3、快速响应公司新产品业务的市场覆盖开拓； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">4、与商家保持紧密联系，处理各类活动、价格调整、到期续签等日常运营事务； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">5、及时处理来自商家及消费者的投诉、反馈、建议等，以提高消费者和商家的满意度。 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">岗位要求： </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">1、热爱自己的事业，敢于迎接挑战 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">2、优秀的谈判能力和沟通协调能力，能熟练运用办公软件（word 、excel、ppt）； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">3、执行力强，抗压能力强，有极强的责任心和服务意识； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">4、有较强的学习能力，善于发现问题和总结方法，有强烈的成就动机，能吃苦耐劳；； </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">5、有面销、电销经验，有OTA或酒店行业、互联网销售从业经验者优先 </span></p><p style=\"line-height: 2;\"><span style=\"color: rgb(34, 34, 34); background-color: rgb(255, 255, 255); font-size: 14px;\">欢迎希望更多发展、更大空间、更多成长的同学积极自荐和推荐～</span></p>', '审核通过');

-- ----------------------------
-- Table structure for project
-- ----------------------------
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `enterprise_id` int NULL DEFAULT NULL COMMENT '发布企业ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '项目标题',
  `description` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '项目描述',
  `skills_required` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '所需技能标签(逗号分隔)',
  `budget_min` decimal(10, 2) NULL DEFAULT NULL COMMENT '预算下限',
  `budget_max` decimal(10, 2) NULL DEFAULT NULL COMMENT '预算上限',
  `deadline` datetime NULL DEFAULT NULL COMMENT '截止时间',
  `delivery_deadline` datetime NULL DEFAULT NULL COMMENT '成品提交截止时间',
  `delivery_requirement` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '交付要求',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '状态：PUBLISHED/CLOSED/CONFIRMED',
  `confirmed_freelancer_id` int NULL DEFAULT NULL COMMENT '确定合作的自由职业者ID',
  `paid_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '已支付金额',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_enterprise_id`(`enterprise_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_deadline`(`deadline` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '外包项目表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of project
-- ----------------------------
INSERT INTO `project` VALUES (8, 1, '开发系统', '在线管理系统', 'java，前端', 1000.00, 3000.00, '2026-01-01 23:59:59', NULL, '11111', 'COMPLETED', 1, 2400.00, '2025-12-07 22:23:53', '2025-12-08 16:02:24');

-- ----------------------------
-- Table structure for project_order
-- ----------------------------
DROP TABLE IF EXISTS `project_order`;
CREATE TABLE `project_order`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '接单ID',
  `project_id` int NOT NULL COMMENT '项目ID',
  `freelancer_id` int NOT NULL COMMENT '自由职业者ID',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'ACCEPTED' COMMENT '状态：ACCEPTED（已接单）/COMPLETED（已完成）/CANCELLED（已取消）',
  `abandoned` tinyint(1) NULL DEFAULT 0 COMMENT '是否已放弃接单',
  `abandoned_at` timestamp NULL DEFAULT NULL COMMENT '放弃接单时间',
  `accepted_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '接单时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_project_freelancer`(`project_id` ASC, `freelancer_id` ASC) USING BTREE,
  INDEX `idx_freelancer_id`(`freelancer_id` ASC) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  CONSTRAINT `project_order_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `project_order_ibfk_2` FOREIGN KEY (`freelancer_id`) REFERENCES `freelancer` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目接单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_order
-- ----------------------------
INSERT INTO `project_order` VALUES (2, 8, 1, 'COMPLETED', 0, NULL, '2025-12-07 22:27:35', '2025-12-07 22:27:34', '2025-12-08 16:02:39');

-- ----------------------------
-- Table structure for project_payment
-- ----------------------------
DROP TABLE IF EXISTS `project_payment`;
CREATE TABLE `project_payment`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` int NOT NULL COMMENT '项目ID',
  `enterprise_id` int NOT NULL COMMENT '企业ID',
  `freelancer_id` int NULL DEFAULT NULL COMMENT '自由职业者ID',
  `deposit_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '已支付保证金（企业发布时支付）',
  `full_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '项目全额（确认合作时支付）',
  `freelancer_deposit` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '接单者保证金',
  `paid_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '已支付总金额',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DEPOSIT_PAID' COMMENT '支付状态：DEPOSIT_PAID（已付保证金）/FULL_PAID（已付全款）/REFUNDED（已退款）/COMPLETED（已完成付款）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_enterprise_id`(`enterprise_id` ASC) USING BTREE,
  INDEX `idx_freelancer_id`(`freelancer_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目支付记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of project_payment
-- ----------------------------
INSERT INTO `project_payment` VALUES (1, 1, 1, NULL, 3055.50, 5230.00, 0.00, 5230.00, 'FULL_PAID', '2025-12-07 22:00:52', '2025-12-07 22:00:52');
INSERT INTO `project_payment` VALUES (2, 8, 1, 1, 1000.00, 2400.00, 120.00, 2400.00, 'COMPLETED', '2025-12-07 22:23:54', '2025-12-08 15:53:23');

-- ----------------------------
-- Table structure for resume
-- ----------------------------
DROP TABLE IF EXISTS `resume`;
CREATE TABLE `resume`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '简历名称',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '姓名',
  `sex` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '性别',
  `salary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '期望薪资',
  `education` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学历',
  `experience` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工作年限',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电话',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `edu_exps` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '教育经历',
  `work_exps` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工作经历',
  `pro_exps` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '项目经验',
  `user_id` int NULL DEFAULT NULL COMMENT '用户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '简历信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of resume
-- ----------------------------
INSERT INTO `resume` VALUES (11, 'Java开发', '张三', '男', '3k以下', '本科', '1年以内', '18800009999', 'zhangsan@xm.com', '[{\"id\":\"1729648501885vfwuj8vz8m8\",\"school\":\"同济大学121\",\"speciality\":\"电子信息工程\",\"education\":\"本科\",\"start\":\"2024-10-16\",\"end\":\"2024-11-01\",\"course\":\"哈哈哈哈呵呵呵呵呵呵哈哈哈哈哈呵呵呵呵呵呵呵哈哈哈哈呵呵呵呵呵呵哈哈哈\"},{\"id\":\"1729648537662czu3ucws0oh\",\"school\":\"同济大学\",\"speciality\":\"计算机软件\",\"education\":\"硕士\",\"start\":\"2024-10-16\",\"end\":\"2024-11-07\",\"course\":\"哈哈哈哈呵呵呵呵呵呵哈哈哈哈哈呵呵呵呵呵呵呵哈哈哈哈呵呵呵呵呵呵哈哈哈\"}]', '[]', '[]', 1);

-- ----------------------------
-- Table structure for submission
-- ----------------------------
DROP TABLE IF EXISTS `submission`;
CREATE TABLE `submission`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '稿件ID',
  `project_id` int NOT NULL COMMENT '项目ID',
  `freelancer_id` int NOT NULL COMMENT '自由职业者ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '稿件标题',
  `description` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '完成思路/文字描述',
  `quote_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '报价（可选）',
  `quote_history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '报价修改历史（JSON格式）',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'SUBMITTED' COMMENT '状态：SUBMITTED/INTERESTED/REJECTED/CONFIRMED',
  `deposit_paid` tinyint(1) NULL DEFAULT 0 COMMENT '是否已支付接单保证金',
  `enterprise_confirmed` tinyint(1) NULL DEFAULT 0 COMMENT '企业是否已确认合作',
  `freelancer_confirmed` tinyint(1) NULL DEFAULT 0 COMMENT '接单者是否已确认合作',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_freelancer_id`(`freelancer_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '稿件表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of submission
-- ----------------------------
INSERT INTO `submission` VALUES (6, 8, 1, '开发系统', '初步开发效果', 2400.00, NULL, 'CONFIRMED', 1, 1, 1, '2025-12-07 22:32:21', '2025-12-07 22:38:44');

-- ----------------------------
-- Table structure for submission_attachment
-- ----------------------------
DROP TABLE IF EXISTS `submission_attachment`;
CREATE TABLE `submission_attachment`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `submission_id` int NOT NULL COMMENT '稿件ID',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件URL',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '原文件名',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小(bytes)',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件类型(ext)',
  `upload_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_submission_id`(`submission_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '稿件附件表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of submission_attachment
-- ----------------------------
INSERT INTO `submission_attachment` VALUES (3, 6, 'http://localhost:9090/api/attachments/download/3', '微信图片_20241021214959.jpg', 668656, '.jpg', '2025-12-07 22:32:21');

-- ----------------------------
-- Table structure for submit
-- ----------------------------
DROP TABLE IF EXISTS `submit`;
CREATE TABLE `submit`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employ_id` int NULL DEFAULT NULL COMMENT '企业ID',
  `position_id` int NULL DEFAULT NULL COMMENT '岗位ID',
  `user_id` int NULL DEFAULT NULL COMMENT '用户ID',
  `resume_id` int NULL DEFAULT NULL COMMENT '简历ID',
  `time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '投递时间',
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '简历状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '简历投递表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of submit
-- ----------------------------

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '账号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '姓名',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像',
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电话',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'zhangsan', '123456', '张三', 'http://localhost:9090/files/download/1726038081997-柴犬.jpeg', 'USER', '18899990000', 'zhangsan@xm.com');
INSERT INTO `user` VALUES (2, 'lisi', '123456', '李四', 'http://localhost:9090/files/download/1726038113795-柯基.jpeg', 'USER', '18877776666', 'lisi@xm.com');
INSERT INTO `user` VALUES (3, 'wangwu', '123456', '王五', 'http://localhost:9090/files/download/1726038136850-拉布拉多.jpeg', 'USER', '18855556666', 'wangwu@xm.com');
INSERT INTO `user` VALUES (4, 'zhaoliu', '123456', '赵六', 'http://localhost:9090/files/download/1726127830447-拉布拉多.jpeg', 'USER', '18899997777', 'zhaoliu@xm.com');
INSERT INTO `user` VALUES (5, 'user1', '123456', '杨过', 'http://localhost:9090/files/download/1765106085382-OIP.webp', 'USER', '19919871254', '111@qq.com');

-- ----------------------------
-- Table structure for withdrawal_record
-- ----------------------------
DROP TABLE IF EXISTS `withdrawal_record`;
CREATE TABLE `withdrawal_record`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `user_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户类型：ENTERPRISE/FREELANCER',
  `amount` decimal(10, 2) NOT NULL COMMENT '提现金额',
  `fee` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '手续费（3%）',
  `actual_amount` decimal(10, 2) NOT NULL COMMENT '实际到账金额',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING（待处理）/SUCCESS（成功）/FAILED（失败）',
  `bank_account` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '银行账户',
  `bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '银行名称',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '提现记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of withdrawal_record
-- ----------------------------
INSERT INTO `withdrawal_record` VALUES (1, 5, 'FREELANCER', 2400.00, 72.00, 2328.00, 'SUCCESS', '111', '11', '提现', '2025-12-08 16:03:46', '2025-12-08 16:03:46');

SET FOREIGN_KEY_CHECKS = 1;
