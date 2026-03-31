-- Seed default Tamixa project + story narration workflow for AI control plane (see docs/AI_GOVERNANCE_E2E.md).

INSERT INTO ai_projects (name, code, status)
SELECT 'Tamixa', 'tamixa', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM ai_projects WHERE code = 'tamixa');

INSERT INTO ai_workflows (project_id, workflow_key, name, description, category, status, version, definition_json, requires_human_approval)
SELECT p.id,
       'story.create_with_narration',
       'Story with narration',
       'Generated story with narration pipeline',
       'story',
       'ACTIVE',
       1,
       '{"steps":[{"key":"narration.generate","type":"llm"},{"key":"narration.safety","type":"policy"}]}'::jsonb,
       false
FROM ai_projects p
WHERE p.code = 'tamixa'
  AND NOT EXISTS (SELECT 1 FROM ai_workflows w WHERE w.workflow_key = 'story.create_with_narration');

INSERT INTO ai_workflow_steps (workflow_id, step_key, step_type, step_order, config_json, is_required)
SELECT w.id, 'narration.generate', 'llm', 1, '{}'::jsonb, true
FROM ai_workflows w
JOIN ai_projects p ON w.project_id = p.id
WHERE p.code = 'tamixa' AND w.workflow_key = 'story.create_with_narration'
  AND NOT EXISTS (
    SELECT 1 FROM ai_workflow_steps s WHERE s.workflow_id = w.id AND s.step_key = 'narration.generate'
);

INSERT INTO ai_workflow_steps (workflow_id, step_key, step_type, step_order, config_json, is_required)
SELECT w.id, 'narration.safety', 'policy', 2, '{}'::jsonb, true
FROM ai_workflows w
JOIN ai_projects p ON w.project_id = p.id
WHERE p.code = 'tamixa' AND w.workflow_key = 'story.create_with_narration'
  AND NOT EXISTS (
    SELECT 1 FROM ai_workflow_steps s WHERE s.workflow_id = w.id AND s.step_key = 'narration.safety'
);
