-- V14__add_test_registrations_and_records.sql
-- Add test registrations, triage records, medical records, and prescriptions
-- for the test accounts documented in docs/娴嬭瘯璐﹀彿.md
-- All operations are idempotent (WHERE NOT EXISTS guards)

-- ============================================================
-- Helper: pick today's AM schedule for a given doctor
-- ============================================================

-- ============================================================
-- Scenario A: 闄堝缓鍥?(patient04) 楂樿鍘?鈫?doctor02 鏉庡績鎬?蹇冨唴绉?
-- Status: COMPLETED 鈥?full flow with record + prescription
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, '澶存檿澶寸棝涓夊ぉ锛屼即琛€鍘嬪崌楂橈紝鏀剁缉鍘嬫渶楂?65mmHg',
       '蹇冨唴绉?,
       '[{"name":"鏉庡績鎬?,"title":"鍓富浠诲尰甯?}]',
       '{"department":"蹇冨唴绉?,"reason":"鎮ｈ€呴珮琛€鍘嬬梾鍙?0骞达紝杩戞湡琛€鍘嬫帶鍒朵笉浣充即澶存檿澶寸棝锛屽缓璁績鍐呯杩涗竴姝ヨ瘎浼?}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient04'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%澶存檿澶寸棝%' AND tr.created_at > CURRENT_TIMESTAMP);

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, slot_released, version, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient04'),
  (SELECT id FROM doctor WHERE username = 'doctor02'),
  (SELECT id FROM department WHERE code = 'cardiology'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND s.work_date = CURRENT_DATE AND s.period = 'AM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = (SELECT id FROM patient WHERE username = 'patient04') AND tr.chief_complaint LIKE '%澶存檿澶寸棝%' ORDER BY tr.created_at DESC LIMIT 1),
  CURRENT_TIMESTAMP, 'COMPLETED',
  '蹇冨唴绉?, '鏉庡績鎬?鍓富浠诲尰甯?, 'NORMAL',
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = (SELECT id FROM patient WHERE username = 'patient04') AND doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND 1=1);

INSERT INTO medical_record (patient_id, doctor_id, registration_id, chief_complaint, present_illness, past_history, physical_exam, preliminary_diagnosis, treatment_plan, conversation_text, ai_generated, version, created_at, updated_at)
SELECT
  p.id, d.id, r.id,
  '澶存檿澶寸棝浼磋鍘嬪崌楂?澶?,
  '鎮ｈ€?澶╁墠鏃犳槑鏄捐鍥犲嚭鐜板ご鏅曘€佸ご鐥涳紝浠ラ閮ㄤ负涓伙紝浼撮潰鑹叉疆绾€傝嚜娴嬭鍘?65/95mmHg銆傛棤鎭跺績鍛曞悙锛屾棤瑙嗙墿妯＄硦锛屾棤鑲綋楹绘湪銆傝繎鏈熷伐浣滃帇鍔涜緝澶э紝鐫＄湢涓嶈冻銆?,
  '楂樿鍘嬬梾鍙?0骞达紝瑙勫緥鍙ｆ湇闄嶅帇鑽紙鍏蜂綋涓嶈锛夈€傚惁璁ょ硸灏跨梾銆佸啝蹇冪梾鍙层€傚惛鐑?0骞达紝姣忔棩10鏀€?,
  'T 36.5鈩?P 78娆?鍒?R 18娆?鍒?BP 162/92mmHg銆傚績鑲哄惉璇婃湭闂诲強鏄庢樉寮傚父銆?,
  '鍘熷彂鎬ч珮琛€鍘嬶紙鎺у埗涓嶄匠锛?,
  '1. 璋冩暣闄嶅帇鏂规锛氳嫰纾洪吀姘ㄦ隘鍦板钩5mg qd + 闃挎墭浼愪粬姹€20mg qn\n2. 浣庣洂浣庤剛楗锛岄檺閰掓垝鐑焅n3. 姣忓懆鑷祴琛€鍘嬪苟璁板綍\n4. 2鍛ㄥ悗澶嶈瘖',
  '鍖荤敓锛氭偍濂斤紝璇烽棶鍝噷涓嶈垝鏈嶏紵\n鎮ｈ€咃細鏈€杩戜笁澶╁ご鏅曞ご鐥涳紝閲忎簡琛€鍘嬫湁鐐归珮\n鍖荤敓锛氫互鍓嶆湁楂樿鍘嬪悧锛焅n鎮ｈ€咃細鏈夛紝宸茬粡10骞翠簡锛屼竴鐩村悆鑽痋n鍖荤敓锛氭渶杩戞湁娌℃湁鐗瑰埆鍔崇疮鎴栨儏缁尝鍔紵\n鎮ｈ€咃細宸ヤ綔鍘嬪姏澶э紝鐫＄湢涓嶅お濂?,
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

-- Prescription items for patient04: 姘ㄦ隘鍦板钩 + 闃挎墭浼愪粬姹€
INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT
  pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
  5, 'qd', '30澶?, 30, '鍙ｆ湇锛屾瘡娆?mg锛?鐗囷級锛屾瘡鏃?娆★紝鏅ㄨ捣鏈嶇敤锛屾敞鎰忕洃娴嬭鍘?,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient04')
  AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-002'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);

INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT
  pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
  20, 'qn', '30澶?, 30, '鍙ｆ湇锛屾瘡娆?0mg锛?鐗囷級锛屾瘡鏃?娆★紝鏅氶鏃舵湇鐢紝娉ㄦ剰鐩戞祴鑲濆姛鑳?,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient04')
  AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-001'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);


-- ============================================================
-- Scenario B: 寮犵鍏?(patient05) 绯栧翱鐥?鍐犲績鐥?鈫?doctor02 蹇冨唴绉?
-- Status: COMPLETED 鈥?multi-condition + multi-drug prescription
-- (uses PM schedule to avoid conflict with Scenario A)
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, '鑳搁椃涓嶉€備竴鍛紝娲诲姩鍚庡姞閲嶏紝鏈夊啝蹇冪梾鍜岀硸灏跨梾鍙?,
       '蹇冨唴绉?,
       '[{"name":"鏉庡績鎬?,"title":"鍓富浠诲尰甯?}]',
       '{"department":"蹇冨唴绉?,"reason":"鎮ｈ€呭啝蹇冪梾鍚堝苟绯栧翱鐥咃紝杩戞湡鑳搁椃鍔犻噸闇€蹇冨唴绉戣瘎浼帮紝鎺掗櫎鎬ユ€у啝鑴夌患鍚堝緛"}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient05'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%鑳搁椃%');

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, slot_released, version, created_at, updated_at)
SELECT
  p.id, d.id,
  (SELECT id FROM department WHERE code = 'cardiology'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = d.id AND s.work_date = CURRENT_DATE AND s.period = 'PM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%鑳搁椃%' ORDER BY tr.created_at DESC LIMIT 1),
  CURRENT_TIMESTAMP, 'COMPLETED',
  '蹇冨唴绉?, '鏉庡績鎬?鍓富浠诲尰甯?, 'NORMAL',
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
CROSS JOIN doctor d
WHERE p.username = 'patient05' AND d.username = 'doctor02'
  AND NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = p.id AND doctor_id = d.id AND 1=1);

INSERT INTO medical_record (patient_id, doctor_id, registration_id, chief_complaint, present_illness, past_history, physical_exam, preliminary_diagnosis, treatment_plan, conversation_text, ai_generated, version, created_at, updated_at)
SELECT
  p.id, d.id, r.id,
  '鑳搁椃涓嶉€?鍛紝娲诲姩鍚庡姞閲?,
  '鎮ｈ€?鍛ㄥ墠鏃犳槑鏄捐鍥犲嚭鐜拌兏闂凤紝浣嶄簬鑳搁鍚庯紝鍛堝帇杩劅锛屾椿鍔ㄥ悗鍔犻噸锛屼紤鎭悗鍙紦瑙ｏ紝姣忔鎸佺画绾?-10鍒嗛挓銆傛棤鏀惧皠鎬х柤鐥涳紝鏃犲ぇ姹楁穻婕撱€備即姘旂煭銆佷箯鍔涖€傝繎鏈熻绯栨帶鍒舵瑺浣筹紝绌鸿吂琛€绯?.5mmol/L銆?,
  '2鍨嬬硸灏跨梾15骞达紝鍙ｆ湇浜岀敳鍙岃儘銆傚啝蹇冪梾鍙?骞淬€傞珮琛€鍘?0骞达紝鍙ｆ湇姘ㄦ隘鍦板钩銆傞潚闇夌礌杩囨晱鍙层€?,
  'T 36.2鈩?P 82娆?鍒?R 20娆?鍒?BP 145/88mmHg銆傚績鑲哄惉璇婏細蹇冪巼82娆?鍒嗭紝寰嬮綈锛屾湭闂诲強鏉傞煶銆傚弻鑲哄懠鍚搁煶娓呫€?,
  '鍐犵姸鍔ㄨ剦绮ユ牱纭寲鎬у績鑴忕梾锛堢ǔ瀹氬瀷蹇冪粸鐥涳級\n2鍨嬬硸灏跨梾',
  '1. 闃垮徃鍖规灄100mg qd 鎶楄灏忔澘\n2. 闃挎墭浼愪粬姹€20mg qn 璋冭剛\n3. 浜岀敳鍙岃儘500mg bid 闄嶇硸\n4. 浣庣洂浣庤剛绯栧翱鐥呴ギ椋焅n5. 鐩戞祴琛€绯栧拰琛€鍘嬶紝涓嶉€傞殢璇?,
  '鍖荤敓锛氬紶闃垮Ж锛屾渶杩戞€庝箞涓嶈垝鏈嶏紵\n鎮ｈ€咃細鑳稿彛闂烽椃鐨勶紝璧拌矾蹇偣灏卞枠\n鍖荤敓锛氭湁澶氫箙浜嗭紵\n鎮ｈ€咃細宸笉澶氫竴涓槦鏈熶簡\n鍖荤敓锛氫互鍓嶆湁蹇冭剰鐥呮垨绯栧翱鐥呭悧锛焅n鎮ｈ€咃細閮芥湁锛屽啝蹇冪梾浜斿勾浜嗭紝绯栧翱鐥呭崄浜斿勾浜?,
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

-- Items: 闃垮徃鍖规灄 + 闃挎墭浼愪粬姹€ + 浜岀敳鍙岃儘
INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
       100, 'qd', '30澶?, 30, '鍙ｆ湇锛屾瘡娆?00mg锛?鐗囷級锛屾瘡鏃?娆★紝椁愬墠鏁寸墖鍚炴湇锛屾敞鎰忔秷鍖栭亾涓嶈壇鍙嶅簲',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient05') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-003'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);

INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
       20, 'qn', '30澶?, 30, '鍙ｆ湇锛屾瘡娆?0mg锛?鐗囷級锛屾瘡鏃?娆★紝鏅氶鏃舵湇鐢?,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient05') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-001'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);

INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
       500, 'bid', '60澶?, 60, '鍙ｆ湇锛屾瘡娆?00mg锛?鐗囷級锛屾瘡鏃?娆★紝椁愪腑鏈嶇敤锛屾敞鎰忕洃娴嬭偩鍔熻兘',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient05') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-011'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);


-- ============================================================
-- Scenario C: 鍒樻€濈惇 (patient03) 鐨偆杩囨晱 鈫?doctor07 瀛欎附鍗?鐨偆绉?
-- Status: COMPLETED 鈥?allergy-safe prescription (no penicillin!)
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, '鎵嬭噦鍜岄閮ㄧ孩鐤逛即鐦欑棐3澶?,
       '鐨偆绉?,
       '[{"name":"瀛欎附鍗?,"title":"涓讳换鍖诲笀"}]',
       '{"department":"鐨偆绉?,"reason":"鐨偆绾㈢柟鐦欑棐锛岃€冭檻杩囨晱鎬х毊鐐庢垨鑽ㄩ夯鐤癸紝寤鸿鐨偆绉戣瘖娌?}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient03'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%绾㈢柟%');

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, slot_released, version, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient03'),
  (SELECT id FROM doctor WHERE username = 'doctor07'),
  (SELECT id FROM department WHERE code = 'dermatology'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor07') AND s.work_date = CURRENT_DATE AND s.period = 'AM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = (SELECT id FROM patient WHERE username = 'patient03') AND tr.chief_complaint LIKE '%绾㈢柟%' ORDER BY tr.created_at DESC LIMIT 1),
  CURRENT_TIMESTAMP, 'COMPLETED',
  '鐨偆绉?, '瀛欎附鍗?涓讳换鍖诲笀', 'NORMAL',
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = (SELECT id FROM patient WHERE username = 'patient03') AND doctor_id = (SELECT id FROM doctor WHERE username = 'doctor07') AND 1=1);

INSERT INTO medical_record (patient_id, doctor_id, registration_id, chief_complaint, present_illness, past_history, physical_exam, preliminary_diagnosis, treatment_plan, conversation_text, ai_generated, version, created_at, updated_at)
SELECT
  p.id, d.id, r.id,
  '鎵嬭噦鍙婇閮ㄧ孩鏂戜即鐦欑棐3澶?,
  '鎮ｈ€?澶╁墠浣跨敤鏂版崲鐨勬矏娴撮湶鍚庡嚭鐜板弻鍓嶈噦浼镐晶鍙婇閮ㄧ孩鑹叉枒涓樼柟锛屼即鏄庢樉鐦欑棐锛屽闂村姞閲嶃€傛棤鍙戠儹锛屾棤鍛煎惛鍥伴毦锛屾棤鍙ｈ厰榛忚啘鎹熷銆傝嚜琛屾秱鎶圭毊鐐庡钩锛堝鏂归唻閰稿湴濉炵背鏉句钩鑶忥級1娆★紝鏁堟灉涓嶄匠銆?,
  '闈掗湁绱犺繃鏁忓彶锛堟棦寰€鐨瘯闃虫€э級銆傚鑺傛€ц繃鏁忔€ч蓟鐐庡彶5骞淬€?,
  '鍙屽墠鑷備几渚у強棰堥儴鍒嗗竷绾㈣壊鏂戜笜鐤癸紝閮ㄥ垎铻嶅悎鎴愮墖锛岃竟鐣屾竻鏅帮紝琛ㄩ潰鏃犳笚鍑恒€傞潰閮ㄦ棤鐨柟銆傚捊閮ㄦ棤鍏呰銆傚弻鑲哄懠鍚搁煶娓呫€?,
  '杩囨晱鎬ф帴瑙︽€х毊鐐庯紙娌愭荡闇茶繃鏁忓彲鑳芥€уぇ锛?,
  '1. 閬垮厤鎺ヨЕ鍙枒杩囨晱鍘燂紙鍋滅敤鏂版矏娴撮湶锛塡n2. 姘浄浠栧畾10mg qd 鍙ｆ湇鎶楄繃鏁廫n3. 绯犻吀鑾背鏉句钩鑶?澶栫敤 qd锛堥伩寮€闈㈤儴锛塡n4. 閬垮厤鎼旀姄锛屼繚鎸佸眬閮ㄦ竻娲乗n5. 濡傚嚭鐜板懠鍚稿洶闅剧珛鍗冲氨鍖?,
  '鍖荤敓锛氱毊鑲ゆ€庝箞浜嗭紵\n鎮ｈ€咃細鎵嬭噦鍜岃剸瀛愯捣浜嗙孩鐤癸紝鐗瑰埆鐥抃n鍖荤敓锛氬嚑澶╀簡锛熸湁娌℃湁鐢ㄨ繃浠€涔堟柊鐨勪笢瑗匡紵\n鎮ｈ€咃細涓夊ぉ浜嗭紝鍓嶄袱澶╂崲浜嗕釜鏂扮殑娌愭荡闇瞈n鍖荤敓锛氫互鍓嶆湁浠€涔堣繃鏁忓悧锛焅n鎮ｈ€咃細闈掗湁绱犺繃鏁忥紝涓嶈兘鎵撻潚闇夌礌',
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
       10, 'qd', '7澶?, 7, '鍙ｆ湇锛屾瘡娆?0mg锛?鐗囷級锛屾瘡鏃?娆?,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient03') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-008'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);

INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
       NULL, 'qd', '7澶?, 1, '澶栫敤锛屾瘡鏃?娆★紝钖勬秱浜庢偅澶?,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient03') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-009'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);


-- ============================================================
-- Scenario D: 鏉ㄦ尝 (patient17) 杩愬姩鎹熶激 鈫?doctor05 璧靛垰 楠ㄧ
-- Status: IN_PROGRESS 鈥?consultation started but not finished
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, '鎵撶鐞冩椂鎵激鍙宠啙鍏宠妭锛岃偪鑳€鐤肩棝',
       '楠ㄧ',
       '[{"name":"璧靛垰","title":"鍓富浠诲尰甯?}]',
       '{"department":"楠ㄧ","reason":"杩愬姩鐩稿叧鑶濆叧鑺傛壄浼わ紝闇€楠ㄧ璇勪及鏄惁鏈夐煣甯︽崯浼?}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient17'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%鑶濆叧鑺?');

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, consultation_start_time, slot_released, version, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient17'),
  (SELECT id FROM doctor WHERE username = 'doctor05'),
  (SELECT id FROM department WHERE code = 'orthopedics'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor05') AND s.work_date = CURRENT_DATE AND s.period = 'AM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = (SELECT id FROM patient WHERE username = 'patient17') AND tr.chief_complaint LIKE '%鑶濆叧鑺?' ORDER BY tr.created_at DESC LIMIT 1),
  CURRENT_TIMESTAMP, 'IN_PROGRESS',
  '楠ㄧ', '璧靛垰 鍓富浠诲尰甯?, 'NORMAL',
  CURRENT_TIMESTAMP,
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = (SELECT id FROM patient WHERE username = 'patient17') AND doctor_id = (SELECT id FROM doctor WHERE username = 'doctor05') AND 1=1);


-- ============================================================
-- Scenario E: 瑜氬織寮?(patient13) 绯栧翱鐥?楂樿鍘?鈫?doctor02 蹇冨唴绉?
-- Status: COMPLETED 鈥?chronic disease management
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, '杩戞湡琛€绯栨帶鍒舵瑺浣筹紝绌鸿吂琛€绯?-9mmol/L锛屼即涔忓姏',
       '蹇冨唴绉?,
       '[{"name":"鏉庡績鎬?,"title":"鍓富浠诲尰甯?}]',
       '{"department":"蹇冨唴绉?,"reason":"鎮ｈ€呯硸灏跨梾鍚堝苟楂樿鍘嬶紝杩戞湡琛€绯栨帶鍒朵笉浣筹紝闇€缁煎悎绠＄悊"}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient13'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%琛€绯?');

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, slot_released, version, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient13'),
  (SELECT id FROM doctor WHERE username = 'doctor02'),
  (SELECT id FROM department WHERE code = 'cardiology'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND s.work_date = CURRENT_DATE + 1 AND s.period = 'AM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = (SELECT id FROM patient WHERE username = 'patient13') AND tr.chief_complaint LIKE '%琛€绯?' ORDER BY tr.created_at DESC LIMIT 1),
  CURRENT_TIMESTAMP, 'COMPLETED',
  '蹇冨唴绉?, '鏉庡績鎬?鍓富浠诲尰甯?, 'NORMAL',
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = (SELECT id FROM patient WHERE username = 'patient13') AND doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND 1=1);

INSERT INTO medical_record (patient_id, doctor_id, registration_id, chief_complaint, present_illness, past_history, physical_exam, preliminary_diagnosis, treatment_plan, conversation_text, ai_generated, version, created_at, updated_at)
SELECT
  p.id, d.id, r.id,
  '琛€绯栨帶鍒舵瑺浣?鍛紝浼翠箯鍔涖€佸彛骞?,
  '鎮ｈ€?鍛ㄥ墠鑷祴绌鸿吂琛€绯?-9mmol/L锛堟棦寰€鎺у埗鍦?-7mmol/L锛夛紝椁愬悗2灏忔椂琛€绯?2mmol/L銆備即涔忓姏銆佸彛骞层€佸楗€傛棤澶氶銆佹秷鐦︺€傝鍘?45/90mmHg銆傛湭瑙勫緥鐩戞祴琛€绯栥€?,
  '2鍨嬬硸灏跨梾10骞达紝鍙ｆ湇浜岀敳鍙岃儘500mg bid銆傞珮琛€鍘?5骞达紝鍙ｆ湇姘ㄦ隘鍦板钩5mg qd銆傞珮琛€鑴傚彶锛屽彛鏈嶉樋鎵樹紣浠栨眬銆?,
  'T 36.6鈩?P 76娆?鍒?R 18娆?鍒?BP 148/92mmHg銆侭MI 28.5kg/m虏銆傚績鑲哄惉璇婏紙-锛夈€傚弻涓嬭偄鏃犳按鑲裤€?,
  '2鍨嬬硸灏跨梾锛堣绯栨帶鍒朵笉浣筹級\n鍘熷彂鎬ч珮琛€鍘媆n楂樿剛琛€鐥?,
  '1. 浜岀敳鍙岃儘璋冩暣鑷?000mg bid\n2. 姘ㄦ隘鍦板钩5mg qd 缁х画\n3. 闃挎墭浼愪粬姹€20mg qn 缁х画\n4. 绯栧翱鐥呴ギ椋?杩愬姩鎸囧\n5. 寤鸿璐拱琛€绯栦华锛屾瘡鍛ㄨ嚦灏戞祴3娆＄┖鑵?3娆￠鍚庤绯朶n6. 1鏈堝悗澶嶈瘖',
  '鎮ｈ€咃細鍖荤敓锛屾渶杩戣绯栦笉澶ソ\n鍖荤敓锛氬叿浣撳灏戯紵\n鎮ｈ€咃細绌鸿吂8鐐瑰锛屼互鍓嶉兘鏄?鐐瑰\n鍖荤敓锛氭渶杩戦ギ椋熷拰杩愬姩鏈夋病鏈夊彉鍖栵紵\n鎮ｈ€咃細鏈€杩戝ぉ鐑病鎬庝箞杩愬姩锛屾按鏋滃悆寰楀浜嗕簺',
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
       1000, 'bid', '60澶?, 120, '鍙ｆ湇锛屾瘡娆?000mg锛?鐗囷級锛屾瘡鏃?娆★紝椁愪腑鏈嶇敤锛屾敞鎰忕洃娴嬭偩鍔熻兘鍜屾秷鍖栭亾鍙嶅簲',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient13') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-011'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);


-- ============================================================
-- Scenario F: 娌堟旦 (patient15) PCI鏈悗 鈫?doctor02 蹇冨唴绉?
-- Status: COMPLETED 鈥?post-PCI follow-up
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, 'PCI鏈悗3涓湀澶嶆煡锛屾棤鏄庢樉涓嶉€?,
       '蹇冨唴绉?,
       '[{"name":"鏉庡績鎬?,"title":"鍓富浠诲尰甯?}]',
       '{"department":"蹇冨唴绉?,"reason":"PCI鏈悗瀹氭湡闅忚澶嶆煡"}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient15'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%PCI鏈悗%');

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, slot_released, version, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient15'),
  (SELECT id FROM doctor WHERE username = 'doctor02'),
  (SELECT id FROM department WHERE code = 'cardiology'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND s.work_date = CURRENT_DATE + 1 AND s.period = 'PM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = (SELECT id FROM patient WHERE username = 'patient15') AND tr.chief_complaint LIKE '%PCI鏈悗%' ORDER BY tr.created_at DESC LIMIT 1),
  CURRENT_TIMESTAMP, 'COMPLETED',
  '蹇冨唴绉?, '鏉庡績鎬?鍓富浠诲尰甯?, 'NORMAL',
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = (SELECT id FROM patient WHERE username = 'patient15') AND doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND 1=1);

INSERT INTO medical_record (patient_id, doctor_id, registration_id, chief_complaint, present_illness, past_history, physical_exam, preliminary_diagnosis, treatment_plan, conversation_text, ai_generated, version, created_at, updated_at)
SELECT
  p.id, d.id, r.id,
  'PCI鏈悗3涓湀澶嶆煡锛屾棤鐗规畩涓嶉€?,
  '鎮ｈ€?涓湀鍓嶅洜鎬ユ€у啝鑴夌患鍚堝緛琛孭CI鏈紝浜庡墠闄嶆敮妞嶅叆鑽墿娲楄劚鏀灦1鏋氥€傛湳鍚庤寰嬫湇鑽紝鏃犺兏鐥涜兏闂凤紝鏃犳皵鐭€傛棩甯告椿鍔ㄨ€愬姏鑹ソ銆傛棤鐗欓緢鍑鸿銆侀粦渚跨瓑鍑鸿琛ㄧ幇銆?,
  '鍐犲績鐥咃紙PCI鏈悗锛夈€侀珮琛€鍘嬨€佺閫犲奖鍓傝繃鏁忓彶銆?,
  'T 36.3鈩?P 72娆?鍒?R 16娆?鍒?BP 132/80mmHg銆傚績鑲哄惉璇婃棤寮傚父銆傚績鐢靛浘锛氱鎬у績寰嬶紝鏈ST-T鏀瑰彉銆?,
  '鍐犵姸鍔ㄨ剦绮ユ牱纭寲鎬у績鑴忕梾\nPCI鏈悗3涓湀\n蹇冨姛鑳絀绾э紙NYHA锛?,
  '1. 缁ф湇闃垮徃鍖规灄100mg qd + 闃挎墭浼愪粬姹€20mg qn\n2. 閬垮厤鍋滆嵂锛屽弻鎶楄嚦灏?2涓湀\n3. 浣庣洂浣庤剛楗锛屾帶鍒朵綋閲峔n4. 瑙勫緥涓瓑寮哄害杩愬姩锛堟瘡鍛ㄢ墺150鍒嗛挓锛塡n5. 3涓湀鍚庡鏌ヨ鑴傘€佽倽鍔熴€佸績鐢靛浘',
  '鍖荤敓锛氭湳鍚庢仮澶嶅緱鎬庝箞鏍凤紵\n鎮ｈ€咃細鎸哄ソ鐨勶紝娌′粈涔堜笉鑸掓湇\n鍖荤敓锛氳嵂涓€鐩村湪鍚冨悧锛焅n鎮ｈ€咃細閮芥寜鏃跺悆鐨刓n鍖荤敓锛氭湁娌℃湁鐗欓緢鍑鸿鎴栬€呭ぇ渚垮彂榛戯紵\n鎮ｈ€咃細娌℃湁锛岄兘姝ｅ父',
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
       100, 'qd', '90澶?, 90, '鍙ｆ湇锛屾瘡娆?00mg锛?鐗囷級锛屾瘡鏃?娆★紝椁愬墠鏁寸墖鍚炴湇锛屽弻鎶楁不鐤楄嚦灏?2涓湀',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient15') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-003'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);

INSERT INTO prescription_item (prescription_id, drug_id, drug_name, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, dosage, frequency, duration, quantity, usage_instruction, created_at, updated_at)
SELECT pr.id, d.id, d.name, d.specification, d.dosage_form, d.package_unit, d.manufacturer, d.unit_price, d.default_usage,
       20, 'qn', '90澶?, 90, '鍙ｆ湇锛屾瘡娆?0mg锛?鐗囷級锛屾瘡鏃?娆★紝鏅氶鏃舵湇鐢紝娉ㄦ剰鐩戞祴鑲濆姛鑳?,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM prescription pr, drug d
WHERE pr.patient_id = (SELECT id FROM patient WHERE username = 'patient15') AND pr.status = 'SUBMITTED'
  AND d.code = 'DRUG-001'
  AND NOT EXISTS (SELECT 1 FROM prescription_item pi WHERE pi.prescription_id = pr.id AND pi.drug_id = d.id);


-- ============================================================
-- Scenario G: 鐜嬪皬鏄?(patient02) 鍎跨 鈫?doctor03 鐜嬪缓鍗?绁炲唴
-- Status: WAITING 鈥?registered but not yet consulted
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, '瀛╁瓙缁忓父澶寸棝锛屾湁鏃朵即鎭跺績锛屾寔缁害2鍛?,
       '绁炵粡鍐呯',
       '[{"name":"鐜嬪缓鍗?,"title":"涓讳换鍖诲笀"}]',
       '{"department":"绁炵粡鍐呯","reason":"鍎跨鍙嶅澶寸棝闇€鎺掗櫎鍋忓ご鐥涖€佽鍔涢棶棰樼瓑锛屽缓璁缁忓唴绉戣瘎浼?}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient02'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%澶寸棝%');

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, slot_released, version, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient02'),
  (SELECT id FROM doctor WHERE username = 'doctor03'),
  (SELECT id FROM department WHERE code = 'neurology'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor03') AND s.work_date = CURRENT_DATE AND s.period = 'AM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = (SELECT id FROM patient WHERE username = 'patient02') AND tr.chief_complaint LIKE '%澶寸棝%' ORDER BY tr.created_at DESC LIMIT 1),
  CURRENT_TIMESTAMP, 'WAITING',
  '绁炵粡鍐呯', '鐜嬪缓鍗?涓讳换鍖诲笀', 'NORMAL',
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = (SELECT id FROM patient WHERE username = 'patient02') AND doctor_id = (SELECT id FROM doctor WHERE username = 'doctor03') AND 1=1);


-- ============================================================
-- Scenario H: 寮犺暰 (patient24) 瀛曞 鈫?doctor02 蹇冨唴绉?
-- Status: WAITING 鈥?pregnancy safety validation
-- ============================================================

INSERT INTO triage_record (patient_id, chief_complaint, recommended_dept, recommended_doctors, ai_response_raw, call_status, recommendation_source, created_at, updated_at)
SELECT p.id, '瀛?4鍛紝杩戞湡蹇冩偢銆佹椿鍔ㄥ悗姘旂煭',
       '蹇冨唴绉?,
       '[{"name":"鏉庡績鎬?,"title":"鍓富浠诲尰甯?}]',
       '{"department":"蹇冨唴绉?,"reason":"濡婂鏈熷績鎮告皵鐭渶璇勪及蹇冨姛鑳斤紝鎺掗櫎鍥翠骇鏈熷績鑲岀梾"}',
       'COMPLETED', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patient p
WHERE p.username = 'patient24'
  AND NOT EXISTS (SELECT 1 FROM triage_record tr WHERE tr.patient_id = p.id AND tr.chief_complaint LIKE '%瀛?蹇冩偢%');

INSERT INTO registration (patient_id, doctor_id, department_id, schedule_id, triage_record_id, registration_time, status, department_snapshot, doctor_snapshot, visit_level_snapshot, slot_released, version, created_at, updated_at)
SELECT
  (SELECT id FROM patient WHERE username = 'patient24'),
  (SELECT id FROM doctor WHERE username = 'doctor02'),
  (SELECT id FROM department WHERE code = 'cardiology'),
  (SELECT s.id FROM schedule s WHERE s.doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND s.work_date = CURRENT_DATE + 2 AND s.period = 'AM' LIMIT 1),
  (SELECT tr.id FROM triage_record tr WHERE tr.patient_id = (SELECT id FROM patient WHERE username = 'patient24') AND tr.chief_complaint LIKE '%瀛?蹇冩偢%' ORDER BY tr.created_at DESC LIMIT 1),
  CURRENT_TIMESTAMP, 'WAITING',
  '蹇冨唴绉?, '鏉庡績鎬?鍓富浠诲尰甯?, 'NORMAL',
  FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE patient_id = (SELECT id FROM patient WHERE username = 'patient24') AND doctor_id = (SELECT id FROM doctor WHERE username = 'doctor02') AND 1=1);

