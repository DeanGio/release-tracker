CREATE TABLE deployment (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name TEXT        NOT NULL,
    version      TEXT        NOT NULL,
    environment  TEXT        NOT NULL,
    status       TEXT        NOT NULL,
    start_time   TIMESTAMPTZ NOT NULL,
    end_time     TIMESTAMPTZ,

    CONSTRAINT deployment_status_check 
        CHECK (status IN ('IN_PROGRESS', 'SUCCEEDED', 'FAILED')),

    CONSTRAINT deployment_environment_check 
        CHECK (environment IN ('PROD', 'STAGING', 'DEV')),

    CONSTRAINT deployment_version_check 
        CHECK (btrim(version) <> ''),

    CONSTRAINT deployment_end_time_check 
        CHECK (
            (status = 'IN_PROGRESS' AND end_time IS NULL) 
            OR (status IN ('SUCCEEDED', 'FAILED') AND end_time IS NOT NULL)
        )
);
