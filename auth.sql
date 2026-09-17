-- ============================================================
--  AUTH — giriş hesabları cədvəli (app_users)
--  Qeyd: API özü işə düşəndə bu cədvəli avtomatik yaradır və
--  boşdursa test/123 hesabını seed edir (AuthBootstrap).
--  Bu skript yalnız cədvəli əl ilə Supabase-də yaratmaq üçündür.
-- ============================================================

create table if not exists app_users (
  id            uuid primary key default gen_random_uuid(),
  username      text not null unique,
  password_hash text not null,          -- BCrypt hash (API tərəfindən yazılır)
  created_at    timestamptz not null default now()
);

-- Seed test/123-ü API özü edir. Parolu düz mətnlə DB-yə YAZMAYIN —
-- yeni istifadəçini API vasitəsilə yaradın: POST /api/auth/register.
