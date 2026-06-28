-- V9__seed_hospital_data.sql
-- Seed realistic hospital data for a small clinic
-- All inserts use WHERE NOT EXISTS for idempotency (works on MySQL and H2)

-- ============================================================
-- DEPARTMENTS (add 4 new, keep existing internal-medicine)
-- ============================================================
INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'cardiology', '心内科', '二级科室', '冠心病、高血压、心律失常、心力衰竭等心血管系统疾病', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'cardiology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'neurology', '神经内科', '二级科室', '头痛、眩晕、癫痫、帕金森综合征、脑血管病等神经系统疾病', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'neurology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'orthopedics', '骨科', '一级科室', '骨折创伤、关节疾病、脊柱疾病、运动损伤', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'orthopedics');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'dermatology', '皮肤科', '一级科室', '过敏性皮肤病、银屑病、痤疮、湿疹、真菌感染等皮肤疾病', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'dermatology');

-- ============================================================
-- DOCTORS (keep existing doctor01, add 7 more)
-- Password: doctor123 (BCrypt hash matches existing DatabaseSeeder)
-- ============================================================
INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor02', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '李心怡', (SELECT id FROM department WHERE code = 'cardiology'),
       '副主任医师', '冠心病、高血压、心律失常',
       '从事心血管内科临床工作15年，擅长冠心病介入治疗和高血压管理。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor02');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor03', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '王建华', (SELECT id FROM department WHERE code = 'neurology'),
       '主任医师', '头痛、癫痫、帕金森综合征',
       '神经内科主任医师，博士生导师，从事神经病学临床和科研工作30年，在癫痫和帕金森病诊疗方面有丰富经验。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor03');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor04', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '陈思远', (SELECT id FROM department WHERE code = 'neurology'),
       '主治医师', '脑血管病、眩晕、失眠',
       '神经内科主治医师，专注于脑血管病的急性期治疗和二级预防，对眩晕和睡眠障碍诊疗有深入研究。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor04');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor05', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '赵刚', (SELECT id FROM department WHERE code = 'orthopedics'),
       '副主任医师', '骨折创伤、关节置换',
       '骨科副主任医师，擅长四肢骨折微创治疗和髋膝关节置换术，年手术量超过500台。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor05');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor06', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '刘磊', (SELECT id FROM department WHERE code = 'orthopedics'),
       '主治医师', '运动损伤、脊柱微创',
       '骨科主治医师，运动医学方向，擅长关节镜手术和脊柱微创治疗。曾担任省级运动队医疗保障医师。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor06');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor07', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '孙丽华', (SELECT id FROM department WHERE code = 'dermatology'),
       '主任医师', '过敏性皮肤病、银屑病、痤疮',
       '皮肤科主任医师，从事皮肤科临床工作25年，在银屑病生物制剂治疗和疑难皮肤病诊断方面经验丰富。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor07');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor08', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '周晓峰', (SELECT id FROM department WHERE code = 'dermatology'),
       '主治医师', '湿疹、荨麻疹、真菌感染',
       '皮肤科主治医师，专注于过敏性皮肤病的综合治疗和皮肤真菌病的规范化诊疗。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor08');

-- ============================================================
-- PATIENTS (add 24 new, keep existing patient01)
-- Password: patient123 (BCrypt hash matches existing DatabaseSeeder)
-- ============================================================
INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient02', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '王小明', '13800010002', 'MALE', '2018-03-15', 8, '无', '无', '110101201803150002', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient02');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient03', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '刘思琪', '13800010003', 'FEMALE', '2001-07-22', 25, '青霉素', '无', '320501200107220003', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient03');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient04', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '陈建国', '13800010004', 'MALE', '1968-11-03', 58, '无', '高血压10年，规律服用降压药', '440103196811030004', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient04');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient05', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '张秀兰', '13800010005', 'FEMALE', '1959-05-18', 67, '磺胺类', '糖尿病15年、冠心病，口服二甲双胍和阿托伐他汀', '330102195905180005', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient05');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient06', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '李明', '13800010006', 'MALE', '1991-02-14', 35, '无', '无', '510107199102140006', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient06');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient07', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '赵雪', '13800010007', 'FEMALE', '1998-09-30', 28, '头孢类', '过敏性鼻炎（季节性）', '410103199809300007', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient07');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient08', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '孙文博', '13800010008', 'MALE', '1984-06-20', 42, '无', '高血脂，口服阿托伐他汀', '320102198406200008', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient08');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient09', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '周婷婷', '13800010009', 'FEMALE', '2007-12-01', 19, '无', '无', '500112200712010009', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient09');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient10', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '吴国栋', '13800010010', 'MALE', '1971-04-08', 55, '阿司匹林', '高血压8年、痛风病史3年', '440304197104080010', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient10');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient11', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '郑小雨', '13800010011', 'FEMALE', '2014-08-25', 12, '无', '哮喘（儿童期发作，间歇使用吸入剂）', '330108201408250011', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient11');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient12', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '冯丽', '13800010012', 'FEMALE', '1981-01-12', 45, '无', '甲状腺功能减退症，口服左甲状腺素钠片', '610103198101120012', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient12');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient13', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '褚志强', '13800010013', 'MALE', '1964-10-17', 62, '无', '2型糖尿病10年、高血压15年，口服二甲双胍和氨氯地平', '120104196410170013', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient13');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient14', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '蒋芳', '13800010014', 'FEMALE', '1995-03-28', 31, '无', '无', '430103199503280014', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient14');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient15', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '沈浩', '13800010015', 'MALE', '1978-07-05', 48, '碘造影剂', '冠心病，PCI支架术后2年，口服阿司匹林和氯吡格雷', '310105197807050015', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient15');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient16', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '韩雪梅', '13800010016', 'FEMALE', '1954-12-22', 72, '青霉素、头孢类', '骨质疏松（椎体压缩骨折史）、高血压20年', '210102195412220016', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient16');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient17', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '杨波', '13800010017', 'MALE', '2000-05-10', 26, '无', '无', '530102200005100017', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient17');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient18', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '朱琳', '13800010018', 'FEMALE', '1988-11-08', 38, '无', '慢性胃炎（胃镜确诊），偶用奥美拉唑', '370102198811080018', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient18');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient19', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '秦汉', '13800010019', 'MALE', '1956-02-28', 70, '无', '前列腺增生、高血压，口服坦索罗辛和氨氯地平', '610103195602280019', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient19');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient20', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '许诺', '13800010020', 'FEMALE', '2010-06-15', 16, '无', '无', '450103201006150020', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient20');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient21', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '何勇', '13800010021', 'MALE', '1974-09-12', 52, '无', '酒精性肝病，肝功能轻度异常，定期复查', '340103197409120021', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient21');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient22', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '吕萍', '13800010022', 'FEMALE', '1966-04-03', 60, '磺胺类', '类风湿关节炎15年，口服甲氨蝶呤，偶用塞来昔布止痛', '420103196604030022', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient22');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient23', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '施伟', '13800010023', 'MALE', '2004-01-20', 22, '无', '无', '350203200401200023', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient23');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient24', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '张蕾', '13800010024', 'FEMALE', '1993-08-08', 33, '无', '妊娠24周，定期产检，无妊娠并发症', '510105199308080024', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient24');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient25', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '许文强', '13800010025', 'MALE', '1948-10-01', 78, '无', '高血压30年、2型糖尿病20年、慢性肾脏病3期，口服多种药物', '310101194810010025', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient25');

-- ============================================================
-- DRUGS (15 common drugs)
-- ============================================================
INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-001', '阿托伐他汀钙片', 'ATFTTGP', '20mg×7片', '片剂', '盒', '辉瑞制药', 42.50,
       '口服，每次20mg，每日1次，晚餐时服用',
       '活动性肝病、不明原因转氨酶持续升高、妊娠及哺乳期妇女',
       '治疗前及治疗期间监测肝功能，出现肌痛需查CK',
       '高胆固醇血症、混合型高脂血症、冠心病',
       '与环孢素、克拉霉素、伊曲康唑合用增加肌病风险',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-001');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-002', '苯磺酸氨氯地平片', 'BHSALDPP', '5mg×7片', '片剂', '盒', '辉瑞制药', 32.80,
       '口服，每次5mg，每日1次，可增至10mg',
       '严重低血压、主动脉瓣狭窄',
       '肝功能不全者慎用，老年人从小剂量开始',
       '原发性高血压、稳定性心绞痛',
       '与CYP3A4抑制剂合用需监测',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-002');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-003', '阿司匹林肠溶片', 'ASPLCRP', '100mg×30片', '片剂', '盒', '拜耳医药', 15.60,
       '口服，每次100mg，每日1次，餐前整片吞服',
       '活动性消化性溃疡、出血体质、对阿司匹林过敏',
       '长期使用注意消化道出血风险，手术前需停药',
       '心脑血管疾病预防、稳定型心绞痛、缺血性卒中',
       '与抗凝药合用增加出血风险',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-003');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-004', '盐酸氟桂利嗪胶囊', 'YSFGLQJN', '5mg×20粒', '胶囊', '盒', '西安杨森', 28.30,
       '口服，每次5-10mg，每晚1次',
       '抑郁症、帕金森病、锥体外系疾病',
       '长期使用可能出现体重增加和嗜睡',
       '偏头痛预防、眩晕（中枢性或周围性）',
       '与酒精或镇静药合用加重嗜睡',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-004');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-005', '卡马西平片', 'KMXPP', '100mg×100片', '片剂', '瓶', '诺华制药', 45.00,
       '口服，初始每次100mg，每日2次，逐渐增量',
       '房室传导阻滞、骨髓抑制、对卡马西平过敏',
       '需监测血常规和肝功能，避免突然停药',
       '癫痫（部分性发作、全身强直-阵挛发作）、三叉神经痛',
       '与多种药物有相互作用，需查阅完整说明书',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-005');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-006', '布洛芬缓释胶囊', 'BLFHSJN', '300mg×20粒', '胶囊', '盒', '中美史克', 18.90,
       '口服，每次300mg，每日2次',
       '活动性消化性溃疡、严重心衰、对NSAID过敏',
       '肾功能不全者慎用，不推荐长期大量使用',
       '轻中度疼痛（头痛、关节痛、牙痛、痛经）、发热',
       '与抗凝药、甲氨蝶呤合用需谨慎',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-006');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-007', '塞来昔布胶囊', 'SLXBJN', '200mg×6粒', '胶囊', '盒', '辉瑞制药', 38.50,
       '口服，每次200mg，每日1-2次',
       '磺胺类药物过敏、活动性消化道溃疡、严重心衰',
       '心血管疾病患者慎用',
       '骨关节炎、类风湿关节炎、强直性脊柱炎',
       '与华法林合用增加出血风险',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-007');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-008', '氯雷他定片', 'LLTDP', '10mg×6片', '片剂', '盒', '先灵葆雅', 22.30,
       '口服，每次10mg，每日1次',
       '对氯雷他定或其辅料过敏',
       '肝功能不全者起始剂量减半',
       '过敏性鼻炎、慢性特发性荨麻疹',
       '与酮康唑、红霉素合用增加血药浓度',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-008');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-009', '糠酸莫米松乳膏', 'KSMMSRG', '5g/支', '乳膏', '支', '默沙东', 35.20,
       '外用，每日1次，涂于患处',
       '皮肤感染（细菌、真菌、病毒）、酒渣鼻、口周皮炎',
       '不宜长期大面积使用，面部和皮肤皱褶处慎用',
       '湿疹、特应性皮炎、接触性皮炎、银屑病',
       '无明显全身药物相互作用',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-009');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-010', '奥美拉唑肠溶胶囊', 'AMLZCRJN', '20mg×14粒', '胶囊', '盒', '阿斯利康', 52.40,
       '口服，每次20mg，每日1-2次，晨起空腹服用',
       '对奥美拉唑或苯并咪唑类过敏',
       '长期使用需注意维生素B12缺乏和骨折风险',
       '胃食管反流病、消化性溃疡、根除幽门螺杆菌（联合方案）',
       '与氯吡格雷合用降低后者抗血小板效果',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-010');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-011', '盐酸二甲双胍片', 'YSEJSGP', '500mg×20片', '片剂', '盒', '中美史克', 12.80,
       '口服，起始每次500mg，每日2次，餐中服用，可增至2000mg/日',
       '严重肾功能不全(eGFR<30)、急性代谢性酸中毒',
       '使用含碘造影剂前需暂停，肾功能监测',
       '2型糖尿病（一线用药）',
       '与造影剂、酒精合用需注意',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-011');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-012', '头孢呋辛酯片', 'TBFXZP', '250mg×12片', '片剂', '盒', '葛兰素史克', 46.70,
       '口服，每次250mg，每日2次，餐后服用',
       '头孢菌素类过敏、青霉素严重过敏者',
       '肾功能不全者需调整剂量',
       '呼吸道感染、泌尿道感染、皮肤软组织感染',
       '与丙磺舒合用延长半衰期',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-012');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-013', '阿莫西林胶囊', 'AMXLJN', '500mg×24粒', '胶囊', '盒', '联邦制药', 18.20,
       '口服，每次500mg，每8小时1次',
       '青霉素过敏、传染性单核细胞增多症',
       '肾功能不全者延长给药间隔',
       '敏感菌引起的呼吸道、泌尿道、胆道感染',
       '与别嘌醇合用增加皮疹风险',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-013');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-014', '氯化钠注射液', 'LHNYSY', '250ml:2.25g/袋', '注射液', '袋', '科伦药业', 4.50,
       '静脉滴注，用量遵医嘱',
       '高钠血症、水钠潴留',
       '心衰、高血压、肾功能不全患者慎用',
       '脱水、低钠血症、药物稀释溶剂',
       '与多种药物配伍使用',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-014');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-015', '葡萄糖注射液', 'PTTZSY', '500ml:25g/袋', '注射液', '袋', '科伦药业', 5.20,
       '静脉滴注，用量遵医嘱',
       '未纠正的糖尿病酮症酸中毒、高血糖高渗状态',
       '糖尿病患者需监测血糖',
       '补充能量和体液、低血糖、药物稀释溶剂',
       '与多种药物配伍使用',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-015');

-- ============================================================
-- Ensure base department and seed doctor exist (created by Java DatabaseSeeder in production)
-- ============================================================
INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'internal-medicine', '内科', '一级科室', '内科常见疾病诊治', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'internal-medicine');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor01', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '张明远', (SELECT id FROM department WHERE code = 'internal-medicine'),
       '主治医师', '内科常见病、多发病诊治',
       '从事内科临床工作10年，擅长内科常见病和多发病的诊断与治疗。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor01');

-- ============================================================
-- SCHEDULES (7-day rolling from today for all 8 doctors)
-- Each doctor gets AM (30 slots) and PM (20 slots) for next 7 days
-- Uses TIMESTAMPADD for H2/MySQL-compatible rolling dates
-- Generates remaining_slots via random within bounds
-- ============================================================

-- Doctor 01 (张明远, 内科) — looks up doctor by username for cross-DB compatibility
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'internal-medicine'), CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE), 'AM', 30, 15 + FLOOR(RAND() * 16), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor01') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE) AND period = 'AM');

INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'internal-medicine'), CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE), 'PM', 20, 5 + FLOOR(RAND() * 16), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor01') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE) AND period = 'PM');

-- doctor02 (李心怡, 心内科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'cardiology'), CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE), period.p, period.slots, GREATEST(0, period.slots - FLOOR(RAND() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor02') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE) AND period = period.p);

-- doctor03 (王建华, 神经内科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'neurology'), CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE), period.p, period.slots, GREATEST(0, period.slots - FLOOR(RAND() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor03') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE) AND period = period.p);

-- doctor04 (陈思远, 神经内科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'neurology'), CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE), period.p, period.slots, GREATEST(0, period.slots - FLOOR(RAND() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor04') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE) AND period = period.p);

-- doctor05 (赵刚, 骨科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'orthopedics'), CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE), period.p, period.slots, GREATEST(0, period.slots - FLOOR(RAND() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor05') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE) AND period = period.p);

-- doctor06 (刘磊, 骨科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'orthopedics'), CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE), period.p, period.slots, GREATEST(0, period.slots - FLOOR(RAND() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor06') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE) AND period = period.p);

-- doctor07 (孙丽华, 皮肤科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'dermatology'), CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE), period.p, period.slots, GREATEST(0, period.slots - FLOOR(RAND() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor07') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE) AND period = period.p);

-- doctor08 (周晓峰, 皮肤科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'dermatology'), CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE), period.p, period.slots, GREATEST(0, period.slots - FLOOR(RAND() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor08') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE) AND period = period.p);

