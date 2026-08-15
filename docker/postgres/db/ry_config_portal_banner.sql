-- Portal home banner settings (apply on existing DB). Admin: ϵͳ���� -> ��������
INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 9, '�Ż���ҳ-Bannerģʽ', 'portal.home.banner.mode', 'none', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, 'none=Ĭ�Ͻ���, image=����ͼ, video=������Ƶ'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.mode');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 10, '�Ż���ҳ-BannerͼƬ', 'portal.home.banner.imageUrl', '', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, 'ͼƬURL���� /profile/upload/... ������ http ��ַ'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.imageUrl');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 11, '�Ż���ҳ-Banner��Ƶ', 'portal.home.banner.videoUrl', '', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, 'mp4/webm ��Ƶ��ַ'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.videoUrl');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 12, '�Ż���ҳ-Banner��Ƶ����', 'portal.home.banner.videoPoster', '', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, '��Ƶ����ǰ����ͼ'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.videoPoster');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 13, '�Ż���ҳ-Banner����', 'portal.home.banner.overlay', '0.42', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, '0-1��Խ�����ֶԱ�Խǿ������ 0.3-0.55'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.overlay');

-- Portal top header background (Logo / search / nav area)
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT E'\u95e8\u6237\u9876\u680f-\u80cc\u666f\u6a21\u5f0f', 'portal.header.banner.mode', 'none', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'none=\u9ed8\u8ba4, image=\u80cc\u666f\u56fe'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.header.banner.mode');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT E'\u95e8\u6237\u9876\u680f-\u80cc\u666f\u56fe\u7247', 'portal.header.banner.imageUrl', '', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'\u9876\u680f\u80cc\u666f\u56fe URL'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.header.banner.imageUrl');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT E'\u95e8\u6237\u9876\u680f-\u906e\u7f69', 'portal.header.banner.overlay', '0.4', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'0-1\u6697\u8272\u906e\u7f69'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.header.banner.overlay');
