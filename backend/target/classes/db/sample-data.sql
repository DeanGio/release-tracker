-- db/sample-data.sql
INSERT INTO deployment (service_name, version, environment, status, start_time, end_time) VALUES
  -- PROD, within 30 days
  ('billing',   '1.4.0',       'PROD',    'SUCCEEDED', now() - interval '20 days', now() - interval '20 days' + interval '12 minutes'),
  ('billing',   '1.4.1',       'PROD',    'FAILED',    now() - interval '18 days', now() - interval '18 days' + interval '5 minutes'),
  ('billing',   '1.4.2',       'PROD',    'SUCCEEDED', now() - interval '15 days', now() - interval '15 days' + interval '9 minutes'),
  ('auth',      '2.0.0',       'PROD',    'SUCCEEDED', now() - interval '10 days', now() - interval '10 days' + interval '20 minutes'),
  ('auth',      '2.0.1',       'PROD',    'FAILED',    now() - interval '6 days',  now() - interval '6 days' + interval '3 minutes'),
  ('auth',      '2.0.2',       'PROD',    'SUCCEEDED', now() - interval '3 days',  now() - interval '3 days' + interval '14 minutes'),
  ('search',    '3.1.0',       'PROD',    'SUCCEEDED', now() - interval '1 day',   now() - interval '1 day' + interval '7 minutes'),
  ('search',    '3.1.1',       'PROD',    'IN_PROGRESS', now() - interval '2 hours', NULL),

  -- Non-PROD, within 30 days
  ('billing',   '1.5.0',       'STAGING', 'SUCCEEDED', now() - interval '4 days',  now() - interval '4 days' + interval '11 minutes'),
  ('auth',      '2.1.0-rc1',   'STAGING', 'FAILED',    now() - interval '2 days',  now() - interval '2 days' + interval '4 minutes'),
  ('search',    '3.2.0-beta',  'DEV',     'SUCCEEDED', now() - interval '5 days',  now() - interval '5 days' + interval '6 minutes'),
  ('reporting', '0.9.1',       'DEV',     'IN_PROGRESS', now() - interval '1 hour', NULL),

  -- PROD, outside the 30-day window
  ('reporting', '0.9.0',       'PROD',    'SUCCEEDED', now() - interval '45 days', now() - interval '45 days' + interval '8 minutes');
