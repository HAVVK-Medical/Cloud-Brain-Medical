-- V17__expand_departments_doctors_and_triage.sql
-- Expand outpatient departments, doctors and schedules for broader triage coverage.
-- Idempotent inserts; safe to run on existing databases.

-- ============================================================
-- DEPARTMENTS
-- ============================================================
INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'respiratory-medicine', '鍛煎惛鍐呯', '浜岀骇绉戝', '鍜冲椊銆佸挸鐥般€佸枠鎲嬨€佹參闃昏偤銆佽偤鐐庛€佸摦鍠樼瓑鍛煎惛绯荤粺鐤剧梾', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'respiratory-medicine');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'gastroenterology', '娑堝寲鍐呯', '浜岀骇绉戝', '鑵圭棝銆佽吂娉汇€佽吂鑳€銆佽儍鐐庛€佹秷鍖栨€ф簝鐤°€佹秷鍖栦笉鑹瓑娑堝寲绯荤粺鐤剧梾', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'gastroenterology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'endocrinology', '鍐呭垎娉岀', '浜岀骇绉戝', '绯栧翱鐥呫€佺敳鐘惰吅鐤剧梾銆佽偉鑳栥€佷唬璋㈠紓甯搞€侀浠ｈ阿鐤剧梾绛?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'endocrinology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'otolaryngology', '鑰抽蓟鍠夌', '浜岀骇绉戝', '鑰抽福銆佸惉鍔涗笅闄嶃€侀蓟鐐庛€侀蓟绐︾値銆佸捊鍠夌棝銆佸０闊冲樁鍝戠瓑鑰抽蓟鍠夌柧鐥?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'otolaryngology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'ophthalmology', '鐪肩', '浜岀骇绉戝', '瑙嗗姏涓嬮檷銆佺溂绾€佺溂鐥涖€佸共鐪笺€佺粨鑶滅値銆佺櫧鍐呴殰銆侀潚鍏夌溂绛夌溂閮ㄧ柧鐥?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'ophthalmology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'urology', '娉屽翱澶栫', '浜岀骇绉戝', '灏块銆佸翱鎬ャ€佸翱鐥涖€佽灏裤€佺粨鐭炽€佸墠鍒楄吅鐤剧梾绛夋硨灏跨郴缁熼棶棰?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'urology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'gynecology', '濡囩', '浜岀骇绉戝', '鏈堢粡寮傚父銆佺泦鑵旂棝銆侀槾閬撳嚭琛€銆佺櫧甯﹀紓甯搞€佸绉戠値鐥囥€佸瓡浜х浉鍏冲挩璇㈢瓑', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'gynecology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'pediatrics', '鍎跨', '浜岀骇绉戝', '14宀佷互涓嬪効绔ュ彂鐑€佸挸鍡姐€佽吂娉汇€佺毊鐤广€佸杺鍏讳笌鐢熼暱鍙戣偛鐩稿叧闂', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'pediatrics');

-- ============================================================
-- DOCTORS
-- ============================================================
INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor09', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '闄堟旦鐒?, (SELECT id FROM department WHERE code = 'respiratory-medicine'),
       '涓讳换鍖诲笀', '鎱㈡€у挸鍡姐€佸摦鍠樸€佹參闃昏偤銆佽偤閮ㄦ劅鏌?,
       '浠庝簨鍛煎惛鍐呯涓村簥宸ヤ綔20骞达紝鎿呴暱鎱㈡€ф皵閬撶柧鐥呫€佽偤閮ㄦ劅鏌撳拰鍛煎惛鍗遍噸鐥囨棭鏈熻瘑鍒€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor09');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor10', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '鏋楅洩姊?, (SELECT id FROM department WHERE code = 'respiratory-medicine'),
       '鍓富浠诲尰甯?, '鏀皵绠″摦鍠樸€佽偤鐐庛€佽兏闂锋皵鐭?,
       '鎿呴暱鏀皵绠″摦鍠樸€佹參鎬у挸鍡戒笌鑲虹値鐨勮鑼冨寲璇婃不锛屽鍛煎惛鍥伴毦鐥囩姸鍒嗗眰鍒ゆ柇缁忛獙涓板瘜銆?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor10');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor11', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '鐜嬪織寮?, (SELECT id FROM department WHERE code = 'respiratory-medicine'),
       '涓绘不鍖诲笀', '鎱㈤樆鑲恒€佽偤缁撹妭銆佸懠鍚搁亾鎰熸煋',
       '涓撴敞鎱㈤樆鑲洪暱鏈熺鐞嗗拰甯歌鍛煎惛閬撴劅鏌撹瘖娌伙紝鐔熸倝闂ㄨ瘖闅忚鍜屽仴搴风鐞嗐€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor11');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor12', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '鍒樺€?, (SELECT id FROM department WHERE code = 'gastroenterology'),
       '涓讳换鍖诲笀', '鑳冪値銆佹秷鍖栨€ф簝鐤°€佽吂鐥涜吂鑳€',
       '鎿呴暱涓婃秷鍖栭亾鐤剧梾涓庡姛鑳芥€ц儍鑲犵梾鐨勮瘖娌伙紝瀵硅吂鐥涖€佸弽閰搞€佸棾姘旂瓑鐥囩姸鏈夎緝涓板瘜鐨勯壌鍒粡楠屻€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor12');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor13', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '璧垫槑杞?, (SELECT id FROM department WHERE code = 'gastroenterology'),
       '鍓富浠诲尰甯?, '鑵规郴銆佷究绉樸€佽偁鐐庛€佽倽鑳嗘秷鍖栭棶棰?,
       '鐔熸倝鑲犻亾鍔熻兘绱婁贡銆佺値鐥囨€ц偁鐥呭強鑲濊儐娑堝寲闂鐨勯棬璇婅瘎浼颁笌绠＄悊銆?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor13');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor14', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '鍛ㄥ娓?, (SELECT id FROM department WHERE code = 'gastroenterology'),
       '涓绘不鍖诲笀', '娑堝寲涓嶈壇銆佽吂娉汇€佸菇闂ㄨ灪鏉嗚弻鐩稿叧鐤剧梾',
       '鎿呴暱娑堝寲涓嶈壇銆佸弽娴佸拰骞介棬铻烘潌鑿屾劅鏌撶殑瑙勮寖鍖栨不鐤椼€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor14');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor15', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '鏉庢€濇兜', (SELECT id FROM department WHERE code = 'endocrinology'),
       '涓讳换鍖诲笀', '绯栧翱鐥呫€佺敳鐘惰吅鐤剧梾銆佷唬璋㈢患鍚堝緛',
       '浠庝簨鍐呭垎娉屼唬璋㈢柧鐥呰瘖鐤?0浣欏勾锛屾搮闀跨硸灏跨梾鎱㈢梾绠＄悊涓庣敳鐘惰吅鐤剧梾缁煎悎璇勪及銆?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor15');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor16', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '閮戝嚡', (SELECT id FROM department WHERE code = 'endocrinology'),
       '鍓富浠诲尰甯?, '绯栧翱鐥呭苟鍙戠棁銆佽偉鑳栥€侀浠ｈ阿寮傚父',
       '涓撴敞绯栧翱鐥呭苟鍙戠棁绠＄悊銆佽偉鑳栦笌浠ｈ阿寮傚父璇勪及锛岄噸瑙嗛暱鏈熼殢璁裤€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor16');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor17', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '瀹嬮洦妗?, (SELECT id FROM department WHERE code = 'endocrinology'),
       '涓绘不鍖诲笀', '鐢茬姸鑵恒€佽绯栥€佽鑴備唬璋㈤棶棰?,
       '鎿呴暱鐢茬姸鑵哄姛鑳藉紓甯搞€佽绯栨帶鍒跺拰浠ｈ阿鐩稿叧闂ㄨ瘖璇婃不銆?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor17');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor18', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '榛勫瓙澧?, (SELECT id FROM department WHERE code = 'otolaryngology'),
       '涓讳换鍖诲笀', '榧荤値銆侀蓟绐︾値銆佸捊鍠夌値銆佽€抽福',
       '鎿呴暱榧婚儴銆佸捊鍠夊強鑰抽儴甯歌鐥呯殑缁煎悎璇婃不锛岀啛鎮夐棬璇婇暅妫€璇勪及銆?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor18');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor19', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '钂嬮洦钖?, (SELECT id FROM department WHERE code = 'otolaryngology'),
       '鍓富浠诲尰甯?, '鍚姏涓嬮檷銆佸捊鍠夌棝銆佽繃鏁忔€ч蓟鐐?,
       '鎿呴暱鍎跨涓庢垚浜鸿€抽蓟鍠夊父瑙佺柧鐥呰瘖鐤楀強杩囨晱鎬ч蓟鐐庣鐞嗐€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor19');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor20', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '浣曚繆瀹?, (SELECT id FROM department WHERE code = 'ophthalmology'),
       '涓讳换鍖诲笀', '杩戣銆佺櫧鍐呴殰銆侀潚鍏夌溂銆佺粨鑶滅値',
       '浠庝簨鐪肩涓村簥宸ヤ綔澶氬勾锛屾搮闀垮父瑙佺溂鐥呯瓫鏌ャ€佸眻鍏夐棶棰樹笌鐪艰〃鐤剧梾璇婃不銆?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor20');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor21', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '寰愯嫢褰?, (SELECT id FROM department WHERE code = 'ophthalmology'),
       '鍓富浠诲尰甯?, '骞茬溂銆佺溂绾€佽鐤插姵銆佽鍔涗笅闄?,
       '鎿呴暱骞茬溂鐥囥€佽鐤插姵鍙婄溂琛ㄧ値鐥囩殑闂ㄨ瘖澶勭悊銆?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor21');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor22', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '瀛欏崥鏂?, (SELECT id FROM department WHERE code = 'urology'),
       '涓讳换鍖诲笀', '鍓嶅垪鑵哄鐢熴€佹硨灏跨郴缁撶煶銆佸翱棰戝翱鎬?,
       '鎿呴暱娉屽翱绯诲父瑙佺梾涓庣粨鐭崇柧鐥呯患鍚堟不鐤楋紝瀵硅灏垮拰鎺掑翱寮傚父鏈変赴瀵岄棬璇婄粡楠屻€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor22');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor23', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '鍞愪匠瀹?, (SELECT id FROM department WHERE code = 'gynecology'),
       '鍓富浠诲尰甯?, '鏈堢粡寮傚父銆佺泦鑵旂棝銆佸绉戠値鐥?,
       '鐔熸倝濡囩鐐庣棁銆佹湀缁忎笉璋冨拰鐩嗚厰鐤肩棝绛夐棬璇婅瘖娌汇€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor23');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor24', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '鍙舵竻闆?, (SELECT id FROM department WHERE code = 'gynecology'),
       '涓绘不鍖诲笀', '鐧藉甫寮傚父銆侀槾閬撶値銆佸瓡浜у挩璇?,
       '鎿呴暱濡囩甯歌鐥呬笌瀛曚骇鐩稿叧鍋ュ悍鍜ㄨ锛屾敞閲嶉殢璁夸笌鍋ュ悍鏁欒偛銆?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor24');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor25', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '楂樺瓙娑?, (SELECT id FROM department WHERE code = 'pediatrics'),
       '涓讳换鍖诲笀', '鍎跨鍙戠儹銆佸挸鍡姐€佽吂娉汇€佺敓闀垮彂鑲茶瘎浼?,
       '浠庝簨鍎跨闂ㄨ瘖涓庢€ヨ瘖宸ヤ綔澶氬勾锛屾搮闀垮父瑙佸効绔ョ柧鐥呭拰鐢熼暱鍙戣偛璇勪及銆?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor25');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor26', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '閭撳彲娆?, (SELECT id FROM department WHERE code = 'pediatrics'),
       '鍓富浠诲尰甯?, '鍎跨鐨柟銆佸枠鎭€佸杺鍏讳笌钀ュ吇闂',
       '鎿呴暱鍎跨鍛煎惛閬撶棁鐘躲€佺毊鐤瑰強钀ュ吇鍠傚吇闂鐨勭患鍚堣瘎浼般€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor26');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor27', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '涓ユ€濊繙', (SELECT id FROM department WHERE code = 'pediatrics'),
       '涓绘不鍖诲笀', '鍎跨甯歌鐥呫€佺柅鑻楀挩璇€佸彂鐑鐞?,
       '涓撴敞鍎跨甯歌鐤剧梾鍜岀柅鑻楀仴搴峰挩璇紝鍏虫敞鍙戠儹涓庢劅鏌撴€х柧鐥呯瓫鏌ャ€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor27');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor28', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '璁告櫒', (SELECT id FROM department WHERE code = 'internal-medicine'),
       '涓绘不鍖诲笀', '鍙戠儹銆佸挸鍡姐€佽吂鐥涖€佷箯鍔?,
       '鎿呴暱鍐呯甯歌鐥呫€佸鍙戠梾鐨勯棬璇婅瘎浼颁笌鍒濇澶勭悊锛岄噸瑙嗙棁鐘跺垎灞傚拰闅忚銆?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor28');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor29', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '鍛ㄤ害鍑?, (SELECT id FROM department WHERE code = 'cardiology'),
       '涓绘不鍖诲笀', '鑳哥棝銆佸績鎮搞€佽鍘嬪紓甯搞€佸績寰嬪け甯?,
       '涓撴敞蹇冨唴绉戦棬璇婂父瑙佺棁鐘惰瘎浼颁笌鎱㈢梾绠＄悊锛屾搮闀块珮琛€鍘嬪拰蹇冨緥澶卞父鐨勯暱鏈熼殢璁裤€?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor29');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor30', '$2a$10$lXb8fbPc6LnZvWg3LwTpdOEk2vmuvmutP7p2sWsh0s77vlBEv4qr2',
       '闊╁瓙澧?, (SELECT id FROM department WHERE code = 'urology'),
       '涓绘不鍖诲笀', '灏块銆佸翱鎬ャ€佸翱鐥涖€佽灏裤€佺粨鐭?,
       '鎿呴暱娉屽翱绯诲父瑙佺梾銆佺粨鐭冲拰鎺掑翱寮傚父鐨勯棬璇婅瘖娌伙紝閲嶈鍩虹鐥呭洜鎺掓煡銆?,
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor30');

-- ============================================================
-- SCHEDULES
-- ============================================================
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, dept.id, (CURRENT_DATE + CAST(days.n AS INTEGER)), period.p, period.slots,
       GREATEST(0, period.slots - FLOOR(random() * 20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
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
      AND s.work_date = (CURRENT_DATE + CAST(days.n AS INTEGER))
      AND s.period = period.p
);


