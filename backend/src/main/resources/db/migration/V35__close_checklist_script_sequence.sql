ALTER TABLE close_checklist_run_steps
    DROP CONSTRAINT chk_close_checklist_step_type;

ALTER TABLE close_checklist_run_steps
    ADD CONSTRAINT chk_close_checklist_step_type
        CHECK (step_type IN ('FO_PNL_REPORT', 'BO_OPERATIONS_REPORT', 'BO_LIFECYCLE_REPORT', 'PORTFOLIO_PRICING', 'EXPOSURE', 'CVA', 'EOD', 'SCRIPT_TEMPLATE')),
    ADD COLUMN template_id UUID REFERENCES execute_script_templates (id),
    ADD COLUMN script_mode VARCHAR(20),
    ADD CONSTRAINT chk_close_checklist_script_mode CHECK (script_mode IS NULL OR script_mode IN ('DRY_RUN', 'REAL_RUN'));
