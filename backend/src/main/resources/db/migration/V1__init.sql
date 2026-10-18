-- V1__init.sql : schema ban dau cua he thong quan ly va tu dong lap lich ca nhan
-- Quy uoc: moi moc thoi gian la TIMESTAMPTZ (luu UTC); khoa chinh BIGINT tu tang.

-- ============ users ============
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    timezone      VARCHAR(50)  NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);
-- email khong phan biet hoa thuong
CREATE UNIQUE INDEX uq_users_email ON users (LOWER(email));

-- ============ user_preferences (1-1 voi users) ============
CREATE TABLE user_preferences (
    user_id           BIGINT      PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    work_start        TIME        NOT NULL DEFAULT '08:00',
    work_end          TIME        NOT NULL DEFAULT '22:00',
    work_days         VARCHAR(30) NOT NULL DEFAULT 'MON,TUE,WED,THU,FRI,SAT',
    buffer_minutes    INT         NOT NULL DEFAULT 10,
    max_block_minutes INT         NOT NULL DEFAULT 120,
    CONSTRAINT chk_pref_hours  CHECK (work_end > work_start),
    CONSTRAINT chk_pref_buffer CHECK (buffer_minutes >= 0),
    CONSTRAINT chk_pref_block  CHECK (max_block_minutes >= 30)
);

-- ============ categories ============
CREATE TABLE categories (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name             VARCHAR(100) NOT NULL,
    color            VARCHAR(7)   NOT NULL DEFAULT '#3B82F6',
    priority_weight  SMALLINT     NOT NULL DEFAULT 50,   -- thanh phan U cua Priority Score (0-100)
    preferred_start  TIME,                               -- khung gio uu tien (tuy chon)
    preferred_end    TIME,
    CONSTRAINT uq_categories_user_name UNIQUE (user_id, name),
    CONSTRAINT chk_cat_weight CHECK (priority_weight BETWEEN 0 AND 100),
    CONSTRAINT chk_cat_hours  CHECK (
        (preferred_start IS NULL AND preferred_end IS NULL)
        OR (preferred_start IS NOT NULL AND preferred_end IS NOT NULL AND preferred_end > preferred_start))
);

-- ============ tasks ============
CREATE TABLE tasks (
    id                BIGSERIAL    PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id       BIGINT       REFERENCES categories (id) ON DELETE SET NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    estimated_minutes INT          NOT NULL,
    deadline          TIMESTAMPTZ,
    priority          VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    status            VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    locked            BOOLEAN      NOT NULL DEFAULT FALSE,
    at_risk           BOOLEAN      NOT NULL DEFAULT FALSE,  -- co nguy co tre han (do Scheduling Engine gan)
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_tasks_minutes  CHECK (estimated_minutes > 0),
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT chk_tasks_status   CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'))
);
CREATE INDEX idx_tasks_user_status   ON tasks (user_id, status);
CREATE INDEX idx_tasks_user_deadline ON tasks (user_id, deadline);

-- ============ task_dependencies ============
CREATE TABLE task_dependencies (
    task_id            BIGINT NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    depends_on_task_id BIGINT NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, depends_on_task_id),
    CONSTRAINT chk_dep_not_self CHECK (task_id <> depends_on_task_id)
);
CREATE INDEX idx_task_dep_reverse ON task_dependencies (depends_on_task_id);

-- ============ schedule_blocks ============
CREATE TABLE schedule_blocks (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    task_id    BIGINT       REFERENCES tasks (id) ON DELETE CASCADE,   -- NULL neu la su kien co dinh
    title      VARCHAR(200),
    start_at   TIMESTAMPTZ  NOT NULL,
    end_at     TIMESTAMPTZ  NOT NULL,
    locked     BOOLEAN      NOT NULL DEFAULT FALSE,
    source     VARCHAR(10)  NOT NULL DEFAULT 'MANUAL',
    CONSTRAINT chk_block_time   CHECK (end_at > start_at),
    CONSTRAINT chk_block_source CHECK (source IN ('AUTO', 'MANUAL')),
    CONSTRAINT chk_block_title  CHECK (task_id IS NOT NULL OR title IS NOT NULL)
);
CREATE INDEX idx_blocks_user_start ON schedule_blocks (user_id, start_at);
CREATE INDEX idx_blocks_task       ON schedule_blocks (task_id);

-- ============ task_logs ============
CREATE TABLE task_logs (
    id         BIGSERIAL   PRIMARY KEY,
    task_id    BIGINT      NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at   TIMESTAMPTZ,
    CONSTRAINT chk_log_time CHECK (ended_at IS NULL OR ended_at >= started_at)
);
CREATE INDEX idx_task_logs_task ON task_logs (task_id);

-- ============ notifications ============
CREATE TABLE notifications (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    task_id    BIGINT       REFERENCES tasks (id) ON DELETE SET NULL,
    type       VARCHAR(30)  NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read, created_at DESC);

-- ============ refresh_tokens ============
CREATE TABLE refresh_tokens (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX uq_refresh_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);
