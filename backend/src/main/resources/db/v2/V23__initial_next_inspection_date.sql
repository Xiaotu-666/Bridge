ALTER TABLE tb_initial_inspection
    ADD COLUMN next_inspection_date DATE NULL COMMENT '下次定期检查日期' AFTER inspection_date;
