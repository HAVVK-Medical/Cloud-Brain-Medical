-- V14__add_test_registrations_and_records.sql
-- Add test registrations, triage records, medical records, and prescriptions
-- for the test accounts documented in docs/测试账号.md
-- All operations are idempotent (WHERE NOT EXISTS guards)

-- ============================================================
-- Helper: pick today's AM schedule for a given doctor
-- ============================================================

-- ============================================================
-- Scenario A: 陈建国 (patient04) 高血压 → doctor02 李心怡 心内科
-- Status: COMPLETED — full flow with record + prescription
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, '头晕头痛三天，伴血压升高，收缩压最高165mmHg',
       '心内科',
       '[{"name":"李心怡","title":"副主任医师"}]',
       '{"department":"心内科","reason":"患者高血压病史10年，近期血压控制不佳伴头晕头痛，建议心内科进一步评估"}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient04'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%头晕头痛%' AND tr.created_at > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 HOUR));

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, slot_released, version, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient04'),
  (SELECT id FROM doctor WHERE username = 'doctor02'),
  (SELECT id FROM department WHERE code = 'cardiology'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND s.work_date = CURRENT_DATE AND s.period = 'AM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = (SELECT id FROM patient WHERE username = 'patient04') AND tr.chief_complaint LIKE '%头晕头痛%' ORDER BY tr.created_at DESC LIMIT 1),
  DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 HOUR), 'COMPLETED',
  '心内科', '李心怡 副主任医师', 'NORMAL',
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = (SELECT id FROM patient WHERE username = 'patient04') AND doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND registration_time > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY));

INSERT INTO medical_record (patient_id, doctor_id, registration_id, chief_complaint, present_illness, past_history, physical_exam, preliminary_diagnosis, treatment_plan, conversation_text, ai_generated, version, created_at, updated_at)
SELECT
  p.id, d.id, r.id,
  '头晕头痛伴血压升高3天',
  '患者3天前无明显诱因出现头晕、头痛，以额部为主，伴面色潮红。自测血压165/95mmHg。无恶心呕吐，无视物模糊，无肢体麻木。近期工作压力较大，睡眠不足。',
  '高血压病史10年，规律口服降压药（具体不详）。否认糖尿病、冠心病史。吸烟20年，每日10支。',
  'T 36.5℃ P 78次/分 R 18次/分 BP 162/92mmHg。心肺听诊未闻及明显异常。',
  '原发性高血压（控制不佳）',
  '1. 调整降压方案：苯磺酸氨氯地平5mg qd + 阿托伐他汀20mg qn\n2. 低盐低脂饮食，限酒戒烟\n3. 每周自测血压并记录\n4. 2周后复诊',
  '医生：您好，请问哪里不舒服？\n患者：最近三天头晕头痛，量了血压有点高\n医生：以前有高血压吗？\n患者：有，已经10年了，一直吃药\n医生：最近有没有特别劳累或情绪波动？\n患者：工作压力大，睡眠不太好',
  TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p, doctor d, registration r
WHERE p.username = 'patient04' AND d.username = 'doctor02'
  AND r.patient_id = p.id AND r.doctor_id = d.id AND r.status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM medical_record mr WHERE mr.registration_id = r.id);

INSERT INTO prescription (patient_id, doctor_id, registration_id, review_id, risk_level, status, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient04'),
  (SELECT id FROM doctor WHERE username = 'doctor02'),
  r.id, NULL, 'LOW', 'SUBMITTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM registration r
WHERE r.patient_id = (SELECT id FROM patient WHERE username = 'patient04')
  AND r.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02')
  AND r.status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM prescription pr WHERE pr.registration_id = r.id);

-- Prescription items for patient04: 氨氯地平 + 阿托伐他汀
INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT
  pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
  5, 'qd', '30天', 30, '口服，每次5mg（1片），每日1次，晨起服用，注意监测血压',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient04')
  AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-002'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);

INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT
  pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
  20, 'qn', '30天', 30, '口服，每次20mg（1片），每日1次，晚餐时服用，注意监测肝功能',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient04')
  AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-001'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);


-- ============================================================
-- Scenario B: 张秀兰 (patient05) 糖尿病+冠心病 → doctor02 心内科
-- Status: COMPLETED — multi-condition + multi-drug prescription
-- (uses PM schedule to avoid conflict with Scenario A)
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, '胸闷不适一周，活动后加重，有冠心病和糖尿病史',
       '心内科',
       '[{"name":"李心怡","title":"副主任医师"}]',
       '{"department":"心内科","reason":"患者冠心病合并糖尿病，近期胸闷加重需心内科评估，排除急性冠脉综合征"}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient05'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%胸闷%');

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, slot_released, version, created_at, updated_at)
SELECT
  p.id, d.id,
  (SELECT id FROM department WHERE code = 'cardiology'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = d.id AND s.work_date = CURRENT_DATE AND s.period = 'PM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%胸闷%' ORDER BY tr.created_at DESC LIMIT 1),
  DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 HOUR), 'COMPLETED',
  '心内科', '李心怡 副主任医师', 'NORMAL',
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
CROSS JOIN doctor d
WHERE p.username = 'patient05' AND d.username = 'doctor02'
  AND NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = p.id AND doctor_id = d.id AND registration_time > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY));

INSERT INTO medical_record (patient_id, doctor_id, registration_id, chief_complaint, present_illness, past_history, physical_exam, preliminary_diagnosis, treatment_plan, conversation_text, ai_generated, version, created_at, updated_at)
SELECT
  p.id, d.id, r.id,
  '胸闷不适1周，活动后加重',
  '患者1周前无明显诱因出现胸闷，位于胸骨后，呈压迫感，活动后加重，休息后可缓解，每次持续约5-10分钟。无放射性疼痛，无大汗淋漓。伴气短、乏力。近期血糖控制欠佳，空腹血糖8.5mmol/L。',
  '2型糖尿病15年，口服二甲双胍。冠心病史5年。高血压10年，口服氨氯地平。青霉素过敏史。',
  'T 36.2℃ P 82次/分 R 20次/分 BP 145/88mmHg。心肺听诊：心率82次/分，律齐，未闻及杂音。双肺呼吸音清。',
  '冠状动脉粥样硬化性心脏病（稳定型心绞痛）\n2型糖尿病',
  '1. 阿司匹林100mg qd 抗血小板\n2. 阿托伐他汀20mg qn 调脂\n3. 二甲双胍500mg bid 降糖\n4. 低盐低脂糖尿病饮食\n5. 监测血糖和血压，不适随诊',
  '医生：张阿姨，最近怎么不舒服？\n患者：胸口闷闷的，走路快点就喘\n医生：有多久了？\n患者：差不多一个星期了\n医生：以前有心脏病或糖尿病吗？\n患者：都有，冠心病五年了，糖尿病十五年了',
  TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p, doctor d, registration r
WHERE p.username = 'patient05' AND d.username = 'doctor02'
  AND r.patient_id = p.id AND r.doctor_id = d.id AND r.status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM medical_record mr WHERE mr.registration_id = r.id);

INSERT INTO prescription (patient_id, doctor_id, registration_id, review_id, risk_level, status, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient05'),
  (SELECT id FROM doctor WHERE username = 'doctor02'),
  r.id, NULL, 'MEDIUM', 'SUBMITTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM registration r
WHERE r.patient_id = (SELECT id FROM patient WHERE username = 'patient05')
  AND r.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02')
  AND r.status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM prescription pr WHERE pr.registration_id = r.id);

-- Items: 阿司匹林 + 阿托伐他汀 + 二甲双胍
INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
       100, 'qd', '30天', 30, '口服，每次100mg（1片），每日1次，餐前整片吞服，注意消化道不良反应',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient05') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-003'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);

INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
       20, 'qn', '30天', 30, '口服，每次20mg（1片），每日1次，晚餐时服用',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient05') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-001'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);

INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
       500, 'bid', '60天', 60, '口服，每次500mg（1片），每日2次，餐中服用，注意监测肾功能',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient05') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-011'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);


-- ============================================================
-- Scenario C: 刘思琪 (patient03) 皮肤过敏 → doctor07 孙丽华 皮肤科
-- Status: COMPLETED — allergy-safe prescription (no penicillin!)
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, '手臂和颈部红疹伴瘙痒3天',
       '皮肤科',
       '[{"name":"孙丽华","title":"主任医师"}]',
       '{"department":"皮肤科","reason":"皮肤红疹瘙痒，考虑过敏性皮炎或荨麻疹，建议皮肤科诊治"}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient03'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%红疹%');

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, slot_released, version, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient03'),
  (SELECT id FROM doctor WHERE username = 'doctor07'),
  (SELECT id FROM department WHERE code = 'dermatology'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor07') AND s.work_date = CURRENT_DATE AND s.period = 'AM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = (SELECT id FROM patient WHERE username = 'patient03') AND tr.chief_complaint LIKE '%红疹%' ORDER BY tr.created_at DESC LIMIT 1),
  DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 4 HOUR), 'COMPLETED',
  '皮肤科', '孙丽华 主任医师', 'NORMAL',
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = (SELECT id FROM patient WHERE username = 'patient03') AND doctor_id = (SELECT id FROM doctor WHERE username = 'doctor07') AND registration_time > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY));

INSERT INTO medical_record (patient_id, doctor_id, registration_id, chief_complaint, present_illness, past_history, physical_exam, preliminary_diagnosis, treatment_plan, conversation_text, ai_generated, version, created_at, updated_at)
SELECT
  p.id, d.id, r.id,
  '手臂及颈部红斑伴瘙痒3天',
  '患者3天前使用新换的沐浴露后出现双前臂伸侧及颈部红色斑丘疹，伴明显瘙痒，夜间加重。无发热，无呼吸困难，无口腔黏膜损害。自行涂抹皮炎平（复方醋酸地塞米松乳膏）1次，效果不佳。',
  '青霉素过敏史（既往皮试阳性）。季节性过敏性鼻炎史5年。',
  '双前臂伸侧及颈部分布红色斑丘疹，部分融合成片，边界清晰，表面无渗出。面部无皮疹。咽部无充血。双肺呼吸音清。',
  '过敏性接触性皮炎（沐浴露过敏可能性大）',
  '1. 避免接触可疑过敏原（停用新沐浴露）\n2. 氯雷他定10mg qd 口服抗过敏\n3. 糠酸莫米松乳膏 外用 qd（避开面部）\n4. 避免搔抓，保持局部清洁\n5. 如出现呼吸困难立即就医',
  '医生：皮肤怎么了？\n患者：手臂和脖子起了红疹，特别痒\n医生：几天了？有没有用过什么新的东西？\n患者：三天了，前两天换了个新的沐浴露\n医生：以前有什么过敏吗？\n患者：青霉素过敏，不能打青霉素',
  TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p, doctor d, registration r
WHERE p.username = 'patient03' AND d.username = 'doctor07'
  AND r.patient_id = p.id AND r.doctor_id = d.id AND r.status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM medical_record mr WHERE mr.registration_id = r.id);

INSERT INTO prescription (patient_id, doctor_id, registration_id, review_id, risk_level, status, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient03'),
  (SELECT id FROM doctor WHERE username = 'doctor07'),
  r.id, NULL, 'LOW', 'SUBMITTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM registration r
WHERE r.patient_id = (SELECT id FROM patient WHERE username = 'patient03')
  AND r.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor07')
  AND r.status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM prescription pr WHERE pr.registration_id = r.id);

INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
       10, 'qd', '7天', 7, '口服，每次10mg（1片），每日1次',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient03') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-008'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);

INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
       NULL, 'qd', '7天', 1, '外用，每日1次，薄涂于患处',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient03') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-009'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);


-- ============================================================
-- Scenario D: 杨波 (patient17) 运动损伤 → doctor05 赵刚 骨科
-- Status: IN_PROGRESS — consultation started but not finished
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, '打篮球时扭伤右膝关节，肿胀疼痛',
       '骨科',
       '[{"name":"赵刚","title":"副主任医师"}]',
       '{"department":"骨科","reason":"运动相关膝关节扭伤，需骨科评估是否有韧带损伤"}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient17'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%膝关节%');

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, consultation_start_time, slot_released, version, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient17'),
  (SELECT id FROM doctor WHERE username = 'doctor05'),
  (SELECT id FROM department WHERE code = 'orthopedics'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor05') AND s.work_date = CURRENT_DATE AND s.period = 'AM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = (SELECT id FROM patient WHERE username = 'patient17') AND tr.chief_complaint LIKE '%膝关节%' ORDER BY tr.created_at DESC LIMIT 1),
  DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 HOUR), 'IN_PROGRESS',
  '骨科', '赵刚 副主任医师', 'NORMAL',
  CURRENT_TIMESTAMP,
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = (SELECT id FROM patient WHERE username = 'patient17') AND doctor_id = (SELECT id FROM doctor WHERE username = 'doctor05') AND registration_time > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY));


-- ============================================================
-- Scenario E: 褚志强 (patient13) 糖尿病+高血压 → doctor02 心内科
-- Status: COMPLETED — chronic disease management
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, '近期血糖控制欠佳，空腹血糖8-9mmol/L，伴乏力',
       '心内科',
       '[{"name":"李心怡","title":"副主任医师"}]',
       '{"department":"心内科","reason":"患者糖尿病合并高血压，近期血糖控制不佳，需综合管理"}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient13'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%血糖%');

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, slot_released, version, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient13'),
  (SELECT id FROM doctor WHERE username = 'doctor02'),
  (SELECT id FROM department WHERE code = 'cardiology'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND s.work_date = DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY) AND s.period = 'AM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = (SELECT id FROM patient WHERE username = 'patient13') AND tr.chief_complaint LIKE '%血糖%' ORDER BY tr.created_at DESC LIMIT 1),
  DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 8 HOUR), 'COMPLETED',
  '心内科', '李心怡 副主任医师', 'NORMAL',
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = (SELECT id FROM patient WHERE username = 'patient13') AND doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND registration_time > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY));

INSERT INTO medical_record (patient_id, doctor_id, registration_id, chief_complaint, present_illness, past_history, physical_exam, preliminary_diagnosis, treatment_plan, conversation_text, ai_generated, version, created_at, updated_at)
SELECT
  p.id, d.id, r.id,
  '血糖控制欠佳2周，伴乏力、口干',
  '患者2周前自测空腹血糖8-9mmol/L（既往控制在6-7mmol/L），餐后2小时血糖12mmol/L。伴乏力、口干、多饮。无多食、消瘦。血压145/90mmHg。未规律监测血糖。',
  '2型糖尿病10年，口服二甲双胍500mg bid。高血压15年，口服氨氯地平5mg qd。高血脂史，口服阿托伐他汀。',
  'T 36.6℃ P 76次/分 R 18次/分 BP 148/92mmHg。BMI 28.5kg/m²。心肺听诊（-）。双下肢无水肿。',
  '2型糖尿病（血糖控制不佳）\n原发性高血压\n高脂血症',
  '1. 二甲双胍调整至1000mg bid\n2. 氨氯地平5mg qd 继续\n3. 阿托伐他汀20mg qn 继续\n4. 糖尿病饮食+运动指导\n5. 建议购买血糖仪，每周至少测3次空腹+3次餐后血糖\n6. 1月后复诊',
  '患者：医生，最近血糖不太好\n医生：具体多少？\n患者：空腹8点多，以前都是6点多\n医生：最近饮食和运动有没有变化？\n患者：最近天热没怎么运动，水果吃得多了些',
  TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p, doctor d, registration r
WHERE p.username = 'patient13' AND d.username = 'doctor02'
  AND r.patient_id = p.id AND r.doctor_id = d.id AND r.status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM medical_record mr WHERE mr.registration_id = r.id);

INSERT INTO prescription (patient_id, doctor_id, registration_id, review_id, risk_level, status, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient13'),
  (SELECT id FROM doctor WHERE username = 'doctor02'),
  r.id, NULL, 'MEDIUM', 'SUBMITTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM registration r
WHERE r.patient_id = (SELECT id FROM patient WHERE username = 'patient13')
  AND r.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02')
  AND r.status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM prescription pr WHERE pr.registration_id = r.id);

INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
       1000, 'bid', '60天', 120, '口服，每次1000mg（2片），每日2次，餐中服用，注意监测肾功能和消化道反应',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient13') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-011'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);


-- ============================================================
-- Scenario F: 沈浩 (patient15) PCI术后 → doctor02 心内科
-- Status: COMPLETED — post-PCI follow-up
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, 'PCI术后3个月复查，无明显不适',
       '心内科',
       '[{"name":"李心怡","title":"副主任医师"}]',
       '{"department":"心内科","reason":"PCI术后定期随访复查"}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient15'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%PCI术后%');

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, slot_released, version, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient15'),
  (SELECT id FROM doctor WHERE username = 'doctor02'),
  (SELECT id FROM department WHERE code = 'cardiology'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND s.work_date = DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY) AND s.period = 'PM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = (SELECT id FROM patient WHERE username = 'patient15') AND tr.chief_complaint LIKE '%PCI术后%' ORDER BY tr.created_at DESC LIMIT 1),
  DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 10 HOUR), 'COMPLETED',
  '心内科', '李心怡 副主任医师', 'NORMAL',
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = (SELECT id FROM patient WHERE username = 'patient15') AND doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND registration_time > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY));

INSERT INTO medical_record (patient_id, doctor_id, registration_id, chief_complaint, present_illness, past_history, physical_exam, preliminary_diagnosis, treatment_plan, conversation_text, ai_generated, version, created_at, updated_at)
SELECT
  p.id, d.id, r.id,
  'PCI术后3个月复查，无特殊不适',
  '患者3个月前因急性冠脉综合征行PCI术，于前降支植入药物洗脱支架1枚。术后规律服药，无胸痛胸闷，无气短。日常活动耐力良好。无牙龈出血、黑便等出血表现。',
  '冠心病（PCI术后）、高血压、碘造影剂过敏史。',
  'T 36.3℃ P 72次/分 R 16次/分 BP 132/80mmHg。心肺听诊无异常。心电图：窦性心律，未见ST-T改变。',
  '冠状动脉粥样硬化性心脏病\nPCI术后3个月\n心功能I级（NYHA）',
  '1. 继服阿司匹林100mg qd + 阿托伐他汀20mg qn\n2. 避免停药，双抗至少12个月\n3. 低盐低脂饮食，控制体重\n4. 规律中等强度运动（每周≥150分钟）\n5. 3个月后复查血脂、肝功、心电图',
  '医生：术后恢复得怎么样？\n患者：挺好的，没什么不舒服\n医生：药一直在吃吗？\n患者：都按时吃的\n医生：有没有牙龈出血或者大便发黑？\n患者：没有，都正常',
  TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p, doctor d, registration r
WHERE p.username = 'patient15' AND d.username = 'doctor02'
  AND r.patient_id = p.id AND r.doctor_id = d.id AND r.status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM medical_record mr WHERE mr.registration_id = r.id);

INSERT INTO prescription (patient_id, doctor_id, registration_id, review_id, risk_level, status, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient15'),
  (SELECT id FROM doctor WHERE username = 'doctor02'),
  r.id, NULL, 'MEDIUM', 'SUBMITTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM registration r
WHERE r.patient_id = (SELECT id FROM patient WHERE username = 'patient15')
  AND r.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02')
  AND r.status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM prescription pr WHERE pr.registration_id = r.id);

INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
       100, 'qd', '90天', 90, '口服，每次100mg（1片），每日1次，餐前整片吞服，双抗治疗至少12个月',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient15') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-003'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);

INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
       20, 'qn', '90天', 90, '口服，每次20mg（1片），每日1次，晚餐时服用，注意监测肝功能',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient15') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-001'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);


-- ============================================================
-- Scenario G: 王小明 (patient02) 儿童 → doctor03 王建华 神内
-- Status: WAITING — registered but not yet consulted
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, '孩子经常头痛，有时伴恶心，持续约2周',
       '神经内科',
       '[{"name":"王建华","title":"主任医师"}]',
       '{"department":"神经内科","reason":"儿童反复头痛需排除偏头痛、视力问题等，建议神经内科评估"}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient02'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%头痛%');

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, slot_released, version, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient02'),
  (SELECT id FROM doctor WHERE username = 'doctor03'),
  (SELECT id FROM department WHERE code = 'neurology'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor03') AND s.work_date = CURRENT_DATE AND s.period = 'AM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = (SELECT id FROM patient WHERE username = 'patient02') AND tr.chief_complaint LIKE '%头痛%' ORDER BY tr.created_at DESC LIMIT 1),
  CURRENT_TIMESTAMP, 'WAITING',
  '神经内科', '王建华 主任医师', 'NORMAL',
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = (SELECT id FROM patient WHERE username = 'patient02') AND doctor_id = (SELECT id FROM doctor WHERE username = 'doctor03') AND registration_time > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY));


-- ============================================================
-- Scenario H: 张蕾 (patient24) 孕妇 → doctor02 心内科
-- Status: WAITING — pregnancy safety validation
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, '孕24周，近期心悸、活动后气短',
       '心内科',
       '[{"name":"李心怡","title":"副主任医师"}]',
       '{"department":"心内科","reason":"妊娠期心悸气短需评估心功能，排除围产期心肌病"}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient24'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%孕%心悸%');

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, slot_released, version, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient24'),
  (SELECT id FROM doctor WHERE username = 'doctor02'),
  (SELECT id FROM department WHERE code = 'cardiology'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND s.work_date = DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY) AND s.period = 'AM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = (SELECT id FROM patient WHERE username = 'patient24') AND tr.chief_complaint LIKE '%孕%心悸%' ORDER BY tr.created_at DESC LIMIT 1),
  CURRENT_TIMESTAMP, 'WAITING',
  '心内科', '李心怡 副主任医师', 'NORMAL',
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = (SELECT id FROM patient WHERE username = 'patient24') AND doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND registration_time > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY));
