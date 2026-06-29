-- V17__expand_departments_doctors_and_triage.sql
-- Expand outpatient departments, doctors and schedules for broader triage coverage.
-- Idempotent inserts; safe to run on existing databases.

-- ============================================================
-- DEPARTMENTS
-- ============================================================
INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'respiratory-medicine', '呼吸内科', '二级科室', '咳嗽、咳痰、喘憋、慢阻肺、肺炎、哮喘等呼吸系统疾病', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'respiratory-medicine');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'gastroenterology', '消化内科', '二级科室', '腹痛、腹泻、腹胀、胃炎、消化性溃疡、消化不良等消化系统疾病', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'gastroenterology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'endocrinology', '内分泌科', '二级科室', '糖尿病、甲状腺疾病、肥胖、代谢异常、骨代谢疾病等', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'endocrinology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'otolaryngology', '耳鼻喉科', '二级科室', '耳鸣、听力下降、鼻炎、鼻窦炎、咽喉痛、声音嘶哑等耳鼻喉疾病', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'otolaryngology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'ophthalmology', '眼科', '二级科室', '视力下降、眼红、眼痛、干眼、结膜炎、白内障、青光眼等眼部疾病', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'ophthalmology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'urology', '泌尿外科', '二级科室', '尿频、尿急、尿痛、血尿、结石、前列腺疾病等泌尿系统问题', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'urology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'gynecology', '妇科', '二级科室', '月经异常、盆腔痛、阴道出血、白带异常、妇科炎症、孕产相关咨询等', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'gynecology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'pediatrics', '儿科', '二级科室', '14岁以下儿童发热、咳嗽、腹泻、皮疹、喂养与生长发育相关问题', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'pediatrics');

-- ============================================================
-- DOCTORS
-- ============================================================
INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor09', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '陈浩然', (SELECT id FROM department WHERE code = 'respiratory-medicine'),
       '主任医师', '慢性咳嗽、哮喘、慢阻肺、肺部感染',
       '从事呼吸内科临床工作20年，擅长慢性气道疾病、肺部感染和呼吸危重症早期识别。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor09');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor10', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '林雪梅', (SELECT id FROM department WHERE code = 'respiratory-medicine'),
       '副主任医师', '支气管哮喘、肺炎、胸闷气短',
       '擅长支气管哮喘、慢性咳嗽与肺炎的规范化诊治，对呼吸困难症状分层判断经验丰富。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor10');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor11', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '王志强', (SELECT id FROM department WHERE code = 'respiratory-medicine'),
       '主治医师', '慢阻肺、肺结节、呼吸道感染',
       '专注慢阻肺长期管理和常见呼吸道感染诊治，熟悉门诊随访和健康管理。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor11');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor12', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '刘倩', (SELECT id FROM department WHERE code = 'gastroenterology'),
       '主任医师', '胃炎、消化性溃疡、腹痛腹胀',
       '擅长上消化道疾病与功能性胃肠病的诊治，对腹痛、反酸、嗳气等症状有较丰富的鉴别经验。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor12');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor13', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '赵明轩', (SELECT id FROM department WHERE code = 'gastroenterology'),
       '副主任医师', '腹泻、便秘、肠炎、肝胆消化问题',
       '熟悉肠道功能紊乱、炎症性肠病及肝胆消化问题的门诊评估与管理。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor13');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor14', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '周婉清', (SELECT id FROM department WHERE code = 'gastroenterology'),
       '主治医师', '消化不良、腹泻、幽门螺杆菌相关疾病',
       '擅长消化不良、反流和幽门螺杆菌感染的规范化治疗。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor14');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor15', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '李思涵', (SELECT id FROM department WHERE code = 'endocrinology'),
       '主任医师', '糖尿病、甲状腺疾病、代谢综合征',
       '从事内分泌代谢疾病诊疗20余年，擅长糖尿病慢病管理与甲状腺疾病综合评估。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor15');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor16', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '郑凯', (SELECT id FROM department WHERE code = 'endocrinology'),
       '副主任医师', '糖尿病并发症、肥胖、骨代谢异常',
       '专注糖尿病并发症管理、肥胖与代谢异常评估，重视长期随访。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor16');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor17', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '宋雨桐', (SELECT id FROM department WHERE code = 'endocrinology'),
       '主治医师', '甲状腺、血糖、血脂代谢问题',
       '擅长甲状腺功能异常、血糖控制和代谢相关门诊诊治。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor17');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor18', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '黄子墨', (SELECT id FROM department WHERE code = 'otolaryngology'),
       '主任医师', '鼻炎、鼻窦炎、咽喉炎、耳鸣',
       '擅长鼻部、咽喉及耳部常见病的综合诊治，熟悉门诊镜检评估。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor18');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor19', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '蒋雨薇', (SELECT id FROM department WHERE code = 'otolaryngology'),
       '副主任医师', '听力下降、咽喉痛、过敏性鼻炎',
       '擅长儿童与成人耳鼻喉常见疾病诊疗及过敏性鼻炎管理。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor19');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor20', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '何俊宇', (SELECT id FROM department WHERE code = 'ophthalmology'),
       '主任医师', '近视、白内障、青光眼、结膜炎',
       '从事眼科临床工作多年，擅长常见眼病筛查、屈光问题与眼表疾病诊治。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor20');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor21', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '徐若彤', (SELECT id FROM department WHERE code = 'ophthalmology'),
       '副主任医师', '干眼、眼红、视疲劳、视力下降',
       '擅长干眼症、视疲劳及眼表炎症的门诊处理。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor21');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor22', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '孙博文', (SELECT id FROM department WHERE code = 'urology'),
       '主任医师', '前列腺增生、泌尿系结石、尿频尿急',
       '擅长泌尿系常见病与结石疾病综合治疗，对血尿和排尿异常有丰富门诊经验。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor22');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor23', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '唐佳宁', (SELECT id FROM department WHERE code = 'gynecology'),
       '副主任医师', '月经异常、盆腔痛、妇科炎症',
       '熟悉妇科炎症、月经不调和盆腔疼痛等门诊诊治。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor23');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor24', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '叶清雅', (SELECT id FROM department WHERE code = 'gynecology'),
       '主治医师', '白带异常、阴道炎、孕产咨询',
       '擅长妇科常见病与孕产相关健康咨询，注重随访与健康教育。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor24');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor25', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '高子涵', (SELECT id FROM department WHERE code = 'pediatrics'),
       '主任医师', '儿童发热、咳嗽、腹泻、生长发育评估',
       '从事儿科门诊与急诊工作多年，擅长常见儿童疾病和生长发育评估。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor25');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor26', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '邓可欣', (SELECT id FROM department WHERE code = 'pediatrics'),
       '副主任医师', '儿童皮疹、喘息、喂养与营养问题',
       '擅长儿童呼吸道症状、皮疹及营养喂养问题的综合评估。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor26');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor27', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '严思远', (SELECT id FROM department WHERE code = 'pediatrics'),
       '主治医师', '儿童常见病、疫苗咨询、发热管理',
       '专注儿童常见疾病和疫苗健康咨询，关注发热与感染性疾病筛查。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor27');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor28', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '许晨', (SELECT id FROM department WHERE code = 'internal-medicine'),
       '主治医师', '发热、咳嗽、腹痛、乏力',
       '擅长内科常见病、多发病的门诊评估与初步处理，重视症状分层和随访。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor28');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor29', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '周亦凡', (SELECT id FROM department WHERE code = 'cardiology'),
       '主治医师', '胸痛、心悸、血压异常、心律失常',
       '专注心内科门诊常见症状评估与慢病管理，擅长高血压和心律失常的长期随访。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor29');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor30', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '韩子墨', (SELECT id FROM department WHERE code = 'urology'),
       '主治医师', '尿频、尿急、尿痛、血尿、结石',
       '擅长泌尿系常见病、结石和排尿异常的门诊诊治，重视基础病因排查。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor30');

-- ============================================================
-- SCHEDULES
-- ============================================================
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, dept.id, CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE), period.p, period.slots,
       GREATEST(0, period.slots - FLOOR(RAND() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'doctor09' AS username, 'respiratory-medicine' AS dept_code
    UNION ALL SELECT 'doctor10', 'respiratory-medicine'
    UNION ALL SELECT 'doctor11', 'respiratory-medicine'
    UNION ALL SELECT 'doctor12', 'gastroenterology'
    UNION ALL SELECT 'doctor13', 'gastroenterology'
    UNION ALL SELECT 'doctor14', 'gastroenterology'
    UNION ALL SELECT 'doctor15', 'endocrinology'
    UNION ALL SELECT 'doctor16', 'endocrinology'
    UNION ALL SELECT 'doctor17', 'endocrinology'
    UNION ALL SELECT 'doctor18', 'otolaryngology'
    UNION ALL SELECT 'doctor19', 'otolaryngology'
    UNION ALL SELECT 'doctor20', 'ophthalmology'
    UNION ALL SELECT 'doctor21', 'ophthalmology'
    UNION ALL SELECT 'doctor22', 'urology'
    UNION ALL SELECT 'doctor23', 'gynecology'
    UNION ALL SELECT 'doctor24', 'gynecology'
    UNION ALL SELECT 'doctor25', 'pediatrics'
    UNION ALL SELECT 'doctor26', 'pediatrics'
    UNION ALL SELECT 'doctor27', 'pediatrics'
    UNION ALL SELECT 'doctor28', 'internal-medicine'
    UNION ALL SELECT 'doctor29', 'cardiology'
    UNION ALL SELECT 'doctor30', 'urology'
) seed
JOIN doctor d ON d.username = seed.username
JOIN department dept ON dept.code = seed.dept_code
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (
    SELECT 1 FROM schedule s
    WHERE s.doctor_id = d.id
      AND s.work_date = CAST(TIMESTAMPADD(DAY, days.n, CURRENT_DATE) AS DATE)
      AND s.period = period.p
);

