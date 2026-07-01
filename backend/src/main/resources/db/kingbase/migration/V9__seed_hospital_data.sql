-- V9__seed_hospital_data.sql
-- Seed realistic hospital data for a small clinic
-- All inserts use WHERE NOT EXISTS for idempotency (works on MySQL and H2)

-- ============================================================
-- DEPARTMENTS (add 4 new, keep existing internal-medicine)
-- ============================================================
INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'cardiology', '蹇冨唴绉?, '浜岀骇绉戝', '鍐犲績鐥呫€侀珮琛€鍘嬨€佸績寰嬪け甯搞€佸績鍔涜“绔瓑蹇冭绠＄郴缁熺柧鐥?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'cardiology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'neurology', '绁炵粡鍐呯', '浜岀骇绉戝', '澶寸棝銆佺湬鏅曘€佺櫕鐥€佸笗閲戞．缁煎悎寰併€佽剳琛€绠＄梾绛夌缁忕郴缁熺柧鐥?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'neurology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'orthopedics', '楠ㄧ', '涓€绾х瀹?, '楠ㄦ姌鍒涗激銆佸叧鑺傜柧鐥呫€佽剨鏌辩柧鐥呫€佽繍鍔ㄦ崯浼?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'orthopedics');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'dermatology', '鐨偆绉?, '涓€绾х瀹?, '杩囨晱鎬х毊鑲ょ梾銆侀摱灞戠梾銆佺棨鐤€佹箍鐤广€佺湡鑿屾劅鏌撶瓑鐨偆鐤剧梾', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'dermatology');

-- ============================================================
-- DOCTORS (keep existing doctor01, add 7 more)
-- Password: doctor123 (BCrypt hash matches existing DatabaseSeeder)
-- ============================================================
INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor02', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '鏉庡績鎬?, (SELECT id FROM department WHERE code = 'cardiology'),
       '鍓富浠诲尰甯?, '鍐犲績鐥呫€侀珮琛€鍘嬨€佸績寰嬪け甯?,
       '浠庝簨蹇冭绠″唴绉戜复搴婂伐浣?5骞达紝鎿呴暱鍐犲績鐥呬粙鍏ユ不鐤楀拰楂樿鍘嬬鐞嗐€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor02');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor03', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '鐜嬪缓鍗?, (SELECT id FROM department WHERE code = 'neurology'),
       '涓讳换鍖诲笀', '澶寸棝銆佺櫕鐥€佸笗閲戞．缁煎悎寰?,
       '绁炵粡鍐呯涓讳换鍖诲笀锛屽崥澹敓瀵煎笀锛屼粠浜嬬缁忕梾瀛︿复搴婂拰绉戠爺宸ヤ綔30骞达紝鍦ㄧ櫕鐥拰甯曢噾妫梾璇婄枟鏂归潰鏈変赴瀵岀粡楠屻€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor03');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor04', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '闄堟€濊繙', (SELECT id FROM department WHERE code = 'neurology'),
       '涓绘不鍖诲笀', '鑴戣绠＄梾銆佺湬鏅曘€佸け鐪?,
       '绁炵粡鍐呯涓绘不鍖诲笀锛屼笓娉ㄤ簬鑴戣绠＄梾鐨勬€ユ€ф湡娌荤枟鍜屼簩绾ч闃诧紝瀵圭湬鏅曞拰鐫＄湢闅滅璇婄枟鏈夋繁鍏ョ爺绌躲€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor04');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor05', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '璧靛垰', (SELECT id FROM department WHERE code = 'orthopedics'),
       '鍓富浠诲尰甯?, '楠ㄦ姌鍒涗激銆佸叧鑺傜疆鎹?,
       '楠ㄧ鍓富浠诲尰甯堬紝鎿呴暱鍥涜偄楠ㄦ姌寰垱娌荤枟鍜岄珛鑶濆叧鑺傜疆鎹㈡湳锛屽勾鎵嬫湳閲忚秴杩?00鍙般€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor05');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor06', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '鍒樼', (SELECT id FROM department WHERE code = 'orthopedics'),
       '涓绘不鍖诲笀', '杩愬姩鎹熶激銆佽剨鏌卞井鍒?,
       '楠ㄧ涓绘不鍖诲笀锛岃繍鍔ㄥ尰瀛︽柟鍚戯紝鎿呴暱鍏宠妭闀滄墜鏈拰鑴婃煴寰垱娌荤枟銆傛浘鎷呬换鐪佺骇杩愬姩闃熷尰鐤椾繚闅滃尰甯堛€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor06');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor07', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '瀛欎附鍗?, (SELECT id FROM department WHERE code = 'dermatology'),
       '涓讳换鍖诲笀', '杩囨晱鎬х毊鑲ょ梾銆侀摱灞戠梾銆佺棨鐤?,
       '鐨偆绉戜富浠诲尰甯堬紝浠庝簨鐨偆绉戜复搴婂伐浣?5骞达紝鍦ㄩ摱灞戠梾鐢熺墿鍒跺墏娌荤枟鍜岀枒闅剧毊鑲ょ梾璇婃柇鏂归潰缁忛獙涓板瘜銆?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor07');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor08', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '鍛ㄦ檽宄?, (SELECT id FROM department WHERE code = 'dermatology'),
       '涓绘不鍖诲笀', '婀跨柟銆佽崹楹荤柟銆佺湡鑿屾劅鏌?,
       '鐨偆绉戜富娌诲尰甯堬紝涓撴敞浜庤繃鏁忔€х毊鑲ょ梾鐨勭患鍚堟不鐤楀拰鐨偆鐪熻弻鐥呯殑瑙勮寖鍖栬瘖鐤椼€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor08');

-- ============================================================
-- PATIENTS (add 24 new, keep existing patient01)
-- Password: patient123 (BCrypt hash matches existing DatabaseSeeder)
-- ============================================================
INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient02', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '鐜嬪皬鏄?, '13800010002', 'MALE', '2018-03-15', 8, '鏃?, '鏃?, '110101201803150002', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient02');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient03', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '鍒樻€濈惇', '13800010003', 'FEMALE', '2001-07-22', 25, '闈掗湁绱?, '鏃?, '320501200107220003', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient03');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient04', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '闄堝缓鍥?, '13800010004', 'MALE', '1968-11-03', 58, '鏃?, '楂樿鍘?0骞达紝瑙勫緥鏈嶇敤闄嶅帇鑽?, '440103196811030004', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient04');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient05', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '寮犵鍏?, '13800010005', 'FEMALE', '1959-05-18', 67, '纾鸿兒绫?, '绯栧翱鐥?5骞淬€佸啝蹇冪梾锛屽彛鏈嶄簩鐢插弻鑳嶅拰闃挎墭浼愪粬姹€', '330102195905180005', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient05');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient06', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '鏉庢槑', '13800010006', 'MALE', '1991-02-14', 35, '鏃?, '鏃?, '510107199102140006', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient06');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient07', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '璧甸洩', '13800010007', 'FEMALE', '1998-09-30', 28, '澶村绫?, '杩囨晱鎬ч蓟鐐庯紙瀛ｈ妭鎬э級', '410103199809300007', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient07');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient08', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '瀛欐枃鍗?, '13800010008', 'MALE', '1984-06-20', 42, '鏃?, '楂樿鑴傦紝鍙ｆ湇闃挎墭浼愪粬姹€', '320102198406200008', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient08');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient09', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '鍛ㄥ┓濠?, '13800010009', 'FEMALE', '2007-12-01', 19, '鏃?, '鏃?, '500112200712010009', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient09');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient10', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '鍚村浗鏍?, '13800010010', 'MALE', '1971-04-08', 55, '闃垮徃鍖规灄', '楂樿鍘?骞淬€佺棝椋庣梾鍙?骞?, '440304197104080010', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient10');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient11', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '閮戝皬闆?, '13800010011', 'FEMALE', '2014-08-25', 12, '鏃?, '鍝枠锛堝効绔ユ湡鍙戜綔锛岄棿姝囦娇鐢ㄥ惛鍏ュ墏锛?, '330108201408250011', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient11');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient12', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '鍐附', '13800010012', 'FEMALE', '1981-01-12', 45, '鏃?, '鐢茬姸鑵哄姛鑳藉噺閫€鐥囷紝鍙ｆ湇宸︾敳鐘惰吅绱犻挔鐗?, '610103198101120012', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient12');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient13', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '瑜氬織寮?, '13800010013', 'MALE', '1964-10-17', 62, '鏃?, '2鍨嬬硸灏跨梾10骞淬€侀珮琛€鍘?5骞达紝鍙ｆ湇浜岀敳鍙岃儘鍜屾皑姘湴骞?, '120104196410170013', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient13');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient14', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '钂嬭姵', '13800010014', 'FEMALE', '1995-03-28', 31, '鏃?, '鏃?, '430103199503280014', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient14');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient15', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '娌堟旦', '13800010015', 'MALE', '1978-07-05', 48, '纰橀€犲奖鍓?, '鍐犲績鐥咃紝PCI鏀灦鏈悗2骞达紝鍙ｆ湇闃垮徃鍖规灄鍜屾隘鍚℃牸闆?, '310105197807050015', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient15');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient16', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '闊╅洩姊?, '13800010016', 'FEMALE', '1954-12-22', 72, '闈掗湁绱犮€佸ご瀛㈢被', '楠ㄨ川鐤忔澗锛堟浣撳帇缂╅鎶樺彶锛夈€侀珮琛€鍘?0骞?, '210102195412220016', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient16');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient17', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '鏉ㄦ尝', '13800010017', 'MALE', '2000-05-10', 26, '鏃?, '鏃?, '530102200005100017', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient17');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient18', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '鏈辩惓', '13800010018', 'FEMALE', '1988-11-08', 38, '鏃?, '鎱㈡€ц儍鐐庯紙鑳冮暅纭瘖锛夛紝鍋剁敤濂ョ編鎷夊攽', '370102198811080018', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient18');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient19', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '绉︽眽', '13800010019', 'MALE', '1956-02-28', 70, '鏃?, '鍓嶅垪鑵哄鐢熴€侀珮琛€鍘嬶紝鍙ｆ湇鍧︾储缃楄緵鍜屾皑姘湴骞?, '610103195602280019', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient19');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient20', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '璁歌', '13800010020', 'FEMALE', '2010-06-15', 16, '鏃?, '鏃?, '450103201006150020', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient20');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient21', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '浣曞媷', '13800010021', 'MALE', '1974-09-12', 52, '鏃?, '閰掔簿鎬ц倽鐥咃紝鑲濆姛鑳借交搴﹀紓甯革紝瀹氭湡澶嶆煡', '340103197409120021', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient21');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient22', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '鍚曡悕', '13800010022', 'FEMALE', '1966-04-03', 60, '纾鸿兒绫?, '绫婚婀垮叧鑺傜値15骞达紝鍙ｆ湇鐢叉皑铦跺懁锛屽伓鐢ㄥ鏉ユ様甯冩鐥?, '420103196604030022', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient22');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient23', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '鏂戒紵', '13800010023', 'MALE', '2004-01-20', 22, '鏃?, '鏃?, '350203200401200023', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient23');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient24', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '寮犺暰', '13800010024', 'FEMALE', '1993-08-08', 33, '鏃?, '濡婂24鍛紝瀹氭湡浜ф锛屾棤濡婂骞跺彂鐥?, '510105199308080024', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient24');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient25', '$2a$10$kZAveZJrzgB1t6AdFc4SseOIg4y6md61mGEcFlDpAvx0wNqQnL.3u',
       '璁告枃寮?, '13800010025', 'MALE', '1948-10-01', 78, '鏃?, '楂樿鍘?0骞淬€?鍨嬬硸灏跨梾20骞淬€佹參鎬ц偩鑴忕梾3鏈燂紝鍙ｆ湇澶氱鑽墿', '310101194810010025', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient25');

-- ============================================================
-- DRUGS (15 common drugs)
-- ============================================================
INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-001', '闃挎墭浼愪粬姹€閽欑墖', 'ATFTTGP', '20mg脳7鐗?, '鐗囧墏', '鐩?, '杈夌憺鍒惰嵂', 42.50,
       '鍙ｆ湇锛屾瘡娆?0mg锛屾瘡鏃?娆★紝鏅氶鏃舵湇鐢?,
       '娲诲姩鎬ц倽鐥呫€佷笉鏄庡師鍥犺浆姘ㄩ叾鎸佺画鍗囬珮銆佸濞犲強鍝轰钩鏈熷濂?,
       '娌荤枟鍓嶅強娌荤枟鏈熼棿鐩戞祴鑲濆姛鑳斤紝鍑虹幇鑲岀棝闇€鏌K',
       '楂樿儐鍥洪唶琛€鐥囥€佹贩鍚堝瀷楂樿剛琛€鐥囥€佸啝蹇冪梾',
       '涓庣幆瀛㈢礌銆佸厠鎷夐湁绱犮€佷紛鏇插悍鍞戝悎鐢ㄥ鍔犺倢鐥呴闄?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-001');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-002', '鑻：閰告皑姘湴骞崇墖', 'BHSALDPP', '5mg脳7鐗?, '鐗囧墏', '鐩?, '杈夌憺鍒惰嵂', 32.80,
       '鍙ｆ湇锛屾瘡娆?mg锛屾瘡鏃?娆★紝鍙鑷?0mg',
       '涓ラ噸浣庤鍘嬨€佷富鍔ㄨ剦鐡ｇ嫮绐?,
       '鑲濆姛鑳戒笉鍏ㄨ€呮厧鐢紝鑰佸勾浜轰粠灏忓墏閲忓紑濮?,
       '鍘熷彂鎬ч珮琛€鍘嬨€佺ǔ瀹氭€у績缁炵棝',
       '涓嶤YP3A4鎶戝埗鍓傚悎鐢ㄩ渶鐩戞祴',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-002');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-003', '闃垮徃鍖规灄鑲犳憾鐗?, 'ASPLCRP', '100mg脳30鐗?, '鐗囧墏', '鐩?, '鎷滆€冲尰鑽?, 15.60,
       '鍙ｆ湇锛屾瘡娆?00mg锛屾瘡鏃?娆★紝椁愬墠鏁寸墖鍚炴湇',
       '娲诲姩鎬ф秷鍖栨€ф簝鐤°€佸嚭琛€浣撹川銆佸闃垮徃鍖规灄杩囨晱',
       '闀挎湡浣跨敤娉ㄦ剰娑堝寲閬撳嚭琛€椋庨櫓锛屾墜鏈墠闇€鍋滆嵂',
       '蹇冭剳琛€绠＄柧鐥呴闃层€佺ǔ瀹氬瀷蹇冪粸鐥涖€佺己琛€鎬у崚涓?,
       '涓庢姉鍑濊嵂鍚堢敤澧炲姞鍑鸿椋庨櫓',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-003');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-004', '鐩愰吀姘熸鍒╁棯鑳跺泭', 'YSFGLQJN', '5mg脳20绮?, '鑳跺泭', '鐩?, '瑗垮畨鏉ㄦ．', 28.30,
       '鍙ｆ湇锛屾瘡娆?-10mg锛屾瘡鏅?娆?,
       '鎶戦儊鐥囥€佸笗閲戞．鐥呫€侀敟浣撳绯荤柧鐥?,
       '闀挎湡浣跨敤鍙兘鍑虹幇浣撻噸澧炲姞鍜屽棞鐫?,
       '鍋忓ご鐥涢闃层€佺湬鏅曪紙涓灑鎬ф垨鍛ㄥ洿鎬э級',
       '涓庨厭绮炬垨闀囬潤鑽悎鐢ㄥ姞閲嶅棞鐫?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-004');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-005', '鍗￠┈瑗垮钩鐗?, 'KMXPP', '100mg脳100鐗?, '鐗囧墏', '鐡?, '璇哄崕鍒惰嵂', 45.00,
       '鍙ｆ湇锛屽垵濮嬫瘡娆?00mg锛屾瘡鏃?娆★紝閫愭笎澧為噺',
       '鎴垮浼犲闃绘粸銆侀楂撴姂鍒躲€佸鍗￠┈瑗垮钩杩囨晱',
       '闇€鐩戞祴琛€甯歌鍜岃倽鍔熻兘锛岄伩鍏嶇獊鐒跺仠鑽?,
       '鐧棲锛堥儴鍒嗘€у彂浣溿€佸叏韬己鐩?闃垫寷鍙戜綔锛夈€佷笁鍙夌缁忕棝',
       '涓庡绉嶈嵂鐗╂湁鐩镐簰浣滅敤锛岄渶鏌ラ槄瀹屾暣璇存槑涔?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-005');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-006', '甯冩礇鑺紦閲婅兌鍥?, 'BLFHSJN', '300mg脳20绮?, '鑳跺泭', '鐩?, '涓編鍙插厠', 18.90,
       '鍙ｆ湇锛屾瘡娆?00mg锛屾瘡鏃?娆?,
       '娲诲姩鎬ф秷鍖栨€ф簝鐤°€佷弗閲嶅績琛般€佸NSAID杩囨晱',
       '鑲惧姛鑳戒笉鍏ㄨ€呮厧鐢紝涓嶆帹鑽愰暱鏈熷ぇ閲忎娇鐢?,
       '杞讳腑搴︾柤鐥涳紙澶寸棝銆佸叧鑺傜棝銆佺墮鐥涖€佺棝缁忥級銆佸彂鐑?,
       '涓庢姉鍑濊嵂銆佺敳姘ㄨ澏鍛ゅ悎鐢ㄩ渶璋ㄦ厧',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-006');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-007', '濉炴潵鏄斿竷鑳跺泭', 'SLXBJN', '200mg脳6绮?, '鑳跺泭', '鐩?, '杈夌憺鍒惰嵂', 38.50,
       '鍙ｆ湇锛屾瘡娆?00mg锛屾瘡鏃?-2娆?,
       '纾鸿兒绫昏嵂鐗╄繃鏁忋€佹椿鍔ㄦ€ф秷鍖栭亾婧冪枴銆佷弗閲嶅績琛?,
       '蹇冭绠＄柧鐥呮偅鑰呮厧鐢?,
       '楠ㄥ叧鑺傜値銆佺被椋庢箍鍏宠妭鐐庛€佸己鐩存€ц剨鏌辩値',
       '涓庡崕娉曟灄鍚堢敤澧炲姞鍑鸿椋庨櫓',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-007');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-008', '姘浄浠栧畾鐗?, 'LLTDP', '10mg脳6鐗?, '鐗囧墏', '鐩?, '鍏堢伒钁嗛泤', 22.30,
       '鍙ｆ湇锛屾瘡娆?0mg锛屾瘡鏃?娆?,
       '瀵规隘闆蜂粬瀹氭垨鍏惰緟鏂欒繃鏁?,
       '鑲濆姛鑳戒笉鍏ㄨ€呰捣濮嬪墏閲忓噺鍗?,
       '杩囨晱鎬ч蓟鐐庛€佹參鎬х壒鍙戞€ц崹楹荤柟',
       '涓庨叜搴峰攽銆佺孩闇夌礌鍚堢敤澧炲姞琛€鑽祿搴?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-008');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-009', '绯犻吀鑾背鏉句钩鑶?, 'KSMMSRG', '5g/鏀?, '涔宠啅', '鏀?, '榛樻矙涓?, 35.20,
       '澶栫敤锛屾瘡鏃?娆★紝娑備簬鎮ｅ',
       '鐨偆鎰熸煋锛堢粏鑿屻€佺湡鑿屻€佺梾姣掞級銆侀厭娓ｉ蓟銆佸彛鍛ㄧ毊鐐?,
       '涓嶅疁闀挎湡澶ч潰绉娇鐢紝闈㈤儴鍜岀毊鑲ょ毐瑜跺鎱庣敤',
       '婀跨柟銆佺壒搴旀€х毊鐐庛€佹帴瑙︽€х毊鐐庛€侀摱灞戠梾',
       '鏃犳槑鏄惧叏韬嵂鐗╃浉浜掍綔鐢?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-009');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-010', '濂ョ編鎷夊攽鑲犳憾鑳跺泭', 'AMLZCRJN', '20mg脳14绮?, '鑳跺泭', '鐩?, '闃挎柉鍒╁悍', 52.40,
       '鍙ｆ湇锛屾瘡娆?0mg锛屾瘡鏃?-2娆★紝鏅ㄨ捣绌鸿吂鏈嶇敤',
       '瀵瑰ゥ缇庢媺鍞戞垨鑻苟鍜攽绫昏繃鏁?,
       '闀挎湡浣跨敤闇€娉ㄦ剰缁寸敓绱燘12缂轰箯鍜岄鎶橀闄?,
       '鑳冮绠″弽娴佺梾銆佹秷鍖栨€ф簝鐤°€佹牴闄ゅ菇闂ㄨ灪鏉嗚弻锛堣仈鍚堟柟妗堬級',
       '涓庢隘鍚℃牸闆峰悎鐢ㄩ檷浣庡悗鑰呮姉琛€灏忔澘鏁堟灉',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-010');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-011', '鐩愰吀浜岀敳鍙岃儘鐗?, 'YSEJSGP', '500mg脳20鐗?, '鐗囧墏', '鐩?, '涓編鍙插厠', 12.80,
       '鍙ｆ湇锛岃捣濮嬫瘡娆?00mg锛屾瘡鏃?娆★紝椁愪腑鏈嶇敤锛屽彲澧炶嚦2000mg/鏃?,
       '涓ラ噸鑲惧姛鑳戒笉鍏?eGFR<30)銆佹€ユ€т唬璋㈡€ч吀涓瘨',
       '浣跨敤鍚閫犲奖鍓傚墠闇€鏆傚仠锛岃偩鍔熻兘鐩戞祴',
       '2鍨嬬硸灏跨梾锛堜竴绾跨敤鑽級',
       '涓庨€犲奖鍓傘€侀厭绮惧悎鐢ㄩ渶娉ㄦ剰',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-011');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-012', '澶村鍛嬭緵閰墖', 'TBFXZP', '250mg脳12鐗?, '鐗囧墏', '鐩?, '钁涘叞绱犲彶鍏?, 46.70,
       '鍙ｆ湇锛屾瘡娆?50mg锛屾瘡鏃?娆★紝椁愬悗鏈嶇敤',
       '澶村鑿岀礌绫昏繃鏁忋€侀潚闇夌礌涓ラ噸杩囨晱鑰?,
       '鑲惧姛鑳戒笉鍏ㄨ€呴渶璋冩暣鍓傞噺',
       '鍛煎惛閬撴劅鏌撱€佹硨灏块亾鎰熸煋銆佺毊鑲よ蒋缁勭粐鎰熸煋',
       '涓庝笝纾鸿垝鍚堢敤寤堕暱鍗婅“鏈?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-012');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-013', '闃胯帿瑗挎灄鑳跺泭', 'AMXLJN', '500mg脳24绮?, '鑳跺泭', '鐩?, '鑱旈偊鍒惰嵂', 18.20,
       '鍙ｆ湇锛屾瘡娆?00mg锛屾瘡8灏忔椂1娆?,
       '闈掗湁绱犺繃鏁忋€佷紶鏌撴€у崟鏍哥粏鑳炲澶氱棁',
       '鑲惧姛鑳戒笉鍏ㄨ€呭欢闀跨粰鑽棿闅?,
       '鏁忔劅鑿屽紩璧风殑鍛煎惛閬撱€佹硨灏块亾銆佽儐閬撴劅鏌?,
       '涓庡埆鍢岄唶鍚堢敤澧炲姞鐨柟椋庨櫓',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-013');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-014', '姘寲閽犳敞灏勬恫', 'LHNYSY', '250ml:2.25g/琚?, '娉ㄥ皠娑?, '琚?, '绉戜鸡鑽笟', 4.50,
       '闈欒剦婊存敞锛岀敤閲忛伒鍖诲槺',
       '楂橀挔琛€鐥囥€佹按閽犳酱鐣?,
       '蹇冭“銆侀珮琛€鍘嬨€佽偩鍔熻兘涓嶅叏鎮ｈ€呮厧鐢?,
       '鑴辨按銆佷綆閽犺鐥囥€佽嵂鐗╃█閲婃憾鍓?,
       '涓庡绉嶈嵂鐗╅厤浼嶄娇鐢?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-014');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-015', '钁¤悇绯栨敞灏勬恫', 'PTTZSY', '500ml:25g/琚?, '娉ㄥ皠娑?, '琚?, '绉戜鸡鑽笟', 5.20,
       '闈欒剦婊存敞锛岀敤閲忛伒鍖诲槺',
       '鏈籂姝ｇ殑绯栧翱鐥呴叜鐥囬吀涓瘨銆侀珮琛€绯栭珮娓楃姸鎬?,
       '绯栧翱鐥呮偅鑰呴渶鐩戞祴琛€绯?,
       '琛ュ厖鑳介噺鍜屼綋娑层€佷綆琛€绯栥€佽嵂鐗╃█閲婃憾鍓?,
       '涓庡绉嶈嵂鐗╅厤浼嶄娇鐢?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-015');

-- ============================================================
-- Ensure base department and seed doctor exist (created by Java DatabaseSeeder in production)
-- ============================================================
INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'internal-medicine', '鍐呯', '涓€绾х瀹?, '鍐呯甯歌鐤剧梾璇婃不', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'internal-medicine');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor01', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '寮犳槑杩?, (SELECT id FROM department WHERE code = 'internal-medicine'),
       '涓绘不鍖诲笀', '鍐呯甯歌鐥呫€佸鍙戠梾璇婃不',
       '浠庝簨鍐呯涓村簥宸ヤ綔10骞达紝鎿呴暱鍐呯甯歌鐥呭拰澶氬彂鐥呯殑璇婃柇涓庢不鐤椼€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor01');

-- ============================================================
-- SCHEDULES (7-day rolling from today for all 8 doctors)
-- Each doctor gets AM (30 slots) and PM (20 slots) for next 7 days
-- Uses PostgreSQL-compatible date arithmetic for rolling dates
-- Generates remaining_slots via random within bounds
-- ============================================================

-- Doctor 01 (寮犳槑杩? 鍐呯) 鈥?looks up doctor by username for cross-DB compatibility
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'internal-medicine'), (CURRENT_DATE + CAST(days.n AS INTEGER)), 'AM', 30, 15 + FLOOR(random() * 16), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor01') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = (CURRENT_DATE + CAST(days.n AS INTEGER)) AND period = 'AM');

INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'internal-medicine'), (CURRENT_DATE + CAST(days.n AS INTEGER)), 'PM', 20, 5 + FLOOR(random() * 16), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor01') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = (CURRENT_DATE + CAST(days.n AS INTEGER)) AND period = 'PM');

-- doctor02 (鏉庡績鎬? 蹇冨唴绉?
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'cardiology'), (CURRENT_DATE + CAST(days.n AS INTEGER)), period.p, period.slots, GREATEST(0, period.slots - FLOOR(random() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor02') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = (CURRENT_DATE + CAST(days.n AS INTEGER)) AND period = period.p);

-- doctor03 (鐜嬪缓鍗? 绁炵粡鍐呯)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'neurology'), (CURRENT_DATE + CAST(days.n AS INTEGER)), period.p, period.slots, GREATEST(0, period.slots - FLOOR(random() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor03') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = (CURRENT_DATE + CAST(days.n AS INTEGER)) AND period = period.p);

-- doctor04 (闄堟€濊繙, 绁炵粡鍐呯)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'neurology'), (CURRENT_DATE + CAST(days.n AS INTEGER)), period.p, period.slots, GREATEST(0, period.slots - FLOOR(random() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor04') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = (CURRENT_DATE + CAST(days.n AS INTEGER)) AND period = period.p);

-- doctor05 (璧靛垰, 楠ㄧ)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'orthopedics'), (CURRENT_DATE + CAST(days.n AS INTEGER)), period.p, period.slots, GREATEST(0, period.slots - FLOOR(random() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor05') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = (CURRENT_DATE + CAST(days.n AS INTEGER)) AND period = period.p);

-- doctor06 (鍒樼, 楠ㄧ)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'orthopedics'), (CURRENT_DATE + CAST(days.n AS INTEGER)), period.p, period.slots, GREATEST(0, period.slots - FLOOR(random() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor06') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = (CURRENT_DATE + CAST(days.n AS INTEGER)) AND period = period.p);

-- doctor07 (瀛欎附鍗? 鐨偆绉?
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'dermatology'), (CURRENT_DATE + CAST(days.n AS INTEGER)), period.p, period.slots, GREATEST(0, period.slots - FLOOR(random() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor07') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = (CURRENT_DATE + CAST(days.n AS INTEGER)) AND period = period.p);

-- doctor08 (鍛ㄦ檽宄? 鐨偆绉?
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'dermatology'), (CURRENT_DATE + CAST(days.n AS INTEGER)), period.p, period.slots, GREATEST(0, period.slots - FLOOR(random() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor08') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = (CURRENT_DATE + CAST(days.n AS INTEGER)) AND period = period.p);


