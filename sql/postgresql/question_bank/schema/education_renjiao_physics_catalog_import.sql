-- auto-generated from dzkbw.com renjiao high-school physics
DO $$
DECLARE
  v_subject_id bigint;
  v_version_id bigint;
  v_textbook_id bigint;
  v_chapter_id bigint;
BEGIN
  SELECT subject_id INTO v_subject_id FROM edu_subject WHERE subject_name = '物理' LIMIT 1;
  IF v_subject_id IS NULL THEN
    RAISE EXCEPTION 'subject physics not found';
  END IF;
  UPDATE edu_qb_question q
  SET textbook_id = NULL, chapter_id = NULL
  FROM edu_qb_textbook t, edu_qb_textbook_version v
  WHERE q.textbook_id = t.textbook_id
    AND t.version_id = v.version_id
    AND v.subject_id = v_subject_id;
  DELETE FROM edu_qb_chapter c
  USING edu_qb_textbook t, edu_qb_textbook_version v
  WHERE c.textbook_id = t.textbook_id
    AND t.version_id = v.version_id
    AND v.subject_id = v_subject_id;
  DELETE FROM edu_qb_textbook t
  USING edu_qb_textbook_version v
  WHERE t.version_id = v.version_id AND v.subject_id = v_subject_id;
  DELETE FROM edu_qb_textbook_version WHERE subject_id = v_subject_id;
  INSERT INTO edu_qb_textbook_version (subject_id, school_stage, version_name, order_num, status)
  VALUES (v_subject_id, '高中', '人教版(2019)', 1, '0')
  RETURNING version_id INTO v_version_id;
  INSERT INTO edu_qb_textbook (version_id, textbook_name, order_num, status)
  VALUES (v_version_id, '必修 第一册', 1, '0')
  RETURNING textbook_id INTO v_textbook_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '序言 物理学：研究物质及其运动规律的科学', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第一章 运动的描述', 2)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.质点 参考系', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.时间 位移', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.位置变化快慢的描述--速度', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.速度变化快慢的描述--加速度', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第二章 匀变速直线运动的研究', 3)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.实验：探究小车速度随时间变化的规律', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.匀变速直线运动的速度与时间的关系', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.匀变速直线运动的位移与时间的关系', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.自由落体运动', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第三章 相互作用--力', 4)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.重力与弹力', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.摩擦力', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.牛顿第三定律', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.力的合成和分解', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.共点力的平衡', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第四章 运动和力的关系', 5)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.牛顿第一定律', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.实验：探究加速度与力、质量的关系', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.牛顿第二定律', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.力学单位制', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.牛顿运动定律的应用', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '6.超重和失重', 6);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '课题研究', 6);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '学生实验', 7);
  INSERT INTO edu_qb_textbook (version_id, textbook_name, order_num, status)
  VALUES (v_version_id, '必修 第二册', 2, '0')
  RETURNING textbook_id INTO v_textbook_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第五章 抛体运动', 1)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.曲线运动', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.运动的合成与分解', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.实验：探究平抛运动的特点', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.抛体运动的规律', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第六章 圆周运动', 2)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.圆周运动', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.向心力', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.向心加速度', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.生活中的圆周运动', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第七章 万有引力与宇宙航行', 3)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.行星的运动', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.万有引力定律', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.万有引力理论的成就', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.宇宙航行', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.相对论时空观与牛顿力学的局限性', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第八章 机械能守恒定律', 4)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.功与功率', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.重力势能', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.动能和动能定理', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.机械能守恒定律', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.实验：验证机械能守恒定律', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '课题研究', 5);
  INSERT INTO edu_qb_textbook (version_id, textbook_name, order_num, status)
  VALUES (v_version_id, '必修 第三册', 3, '0')
  RETURNING textbook_id INTO v_textbook_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第九章 静电场及其应用', 1)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.电荷', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.库仑定律', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.电场 电场强度', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.静电的防止与利用', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第十章 静电场中的能量', 2)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.电势能和电势', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.电势差', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.电势差与电场强度的关系', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.电容器的电容', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.带电粒子在电场中的运动', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第十一章 电路及其应用', 3)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.电源和电流', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.导体的电阻', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.实验：导体电阻率的测量', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.串联电路和并联电路', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.实验：练习使用多用电表', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第十二章 电能 能量守恒定律', 4)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.电路中的能量转化', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.闭合电路的欧姆定律', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.实验：电池电动势和内阻的测量', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.能源与可持续发展', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第十三章 电磁感应与电磁波初步', 5)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.磁场 磁感线', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.磁感应强度 磁通量', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.电磁感应现象及应用', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.电磁波的发现及应用', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.能量量子化', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '课题研究', 6);
  INSERT INTO edu_qb_textbook (version_id, textbook_name, order_num, status)
  VALUES (v_version_id, '选择性必修 第一册', 4, '0')
  RETURNING textbook_id INTO v_textbook_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第一章 动量守恒定律', 1)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.动量', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.动量定理', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.动量守恒定律', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.实验：验证动量守恒定律', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.弹性碰撞和非弹性碰撞', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '6.反冲现象 火箭', 6);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第二章 机械振动', 2)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.简谐运动', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.简谐运动的描述', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.简谐运动的回复力和能量', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.单摆', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.实验：用单摆测量重力加速度', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '6.受迫振动 共振', 6);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第三章 机械波', 3)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.波的形成', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.波的描述', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.波的反射、折射和衍射', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.波的干涉', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.多普勒效应', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第四章 光', 4)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.光的折射', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.全反射', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.光的干涉', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.实验：用双缝干涉测量光的波长', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.光的衍射', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '6.光的偏振 激光', 6);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '课题研究', 5);
  INSERT INTO edu_qb_textbook (version_id, textbook_name, order_num, status)
  VALUES (v_version_id, '选择性必修 第二册', 5, '0')
  RETURNING textbook_id INTO v_textbook_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第一章 安培力与洛伦兹力', 1)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.磁场对通电导线的作用力', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.磁场对运动电荷的作用力', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.带电粒子在匀强磁场中的运动', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.质谱仪与回旋加速器', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第二章 电磁感应', 2)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.楞次定律', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.法拉第电磁感应定律', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.涡流、电磁阻尼和电磁驱动', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.互感和自感', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第三章 交变电流', 3)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.交变电流', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.交变电流的描述', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.变压器', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.电能的输送', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第四章 电磁振荡与电磁波', 4)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.电磁振荡', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.电磁场与电磁波', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.无线电波的发射和接收', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.电磁波谱', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第五章 传感器', 5)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.认识传感器', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.常见传感器的工作原理及应用', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.利用传感器制作简单的自动控制装置', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '课题研究', 6);
  INSERT INTO edu_qb_textbook (version_id, textbook_name, order_num, status)
  VALUES (v_version_id, '选择性必修 第三册', 6, '0')
  RETURNING textbook_id INTO v_textbook_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第一章 分子动理论', 1)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.分子动理论的基本内容', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.实验：用油膜法估测油酸分子的大小', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.分子运动速率分布规律', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.分子动能和分子势能', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第二章 气体、固体和液体', 2)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.温度和温标', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.气体的等温变化', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.气体的等压变化和等容变化', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.固体', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.液体', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第三章 热力学定律', 3)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.功、热和内能的改变', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.热力学第一定律', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.能量守恒定律', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.热力学第二定律', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第四章 原子结构和波粒二象性', 4)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.普朗克黑体辐射理论', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.光电效应', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.原子的核式结构模型', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.氢原子光谱和玻尔的原子模型', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.粒子的波动性和量子力学的建立', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第五章 原子核', 5)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.原子核的组成', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.放射性元素的衰变', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.核力与结合能', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.核裂变与核聚变', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.“基本”粒子', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '课题研究', 6);
END $$;
