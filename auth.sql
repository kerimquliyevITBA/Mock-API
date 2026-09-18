-- ============================================================
--  AUTH & AUTHORIZATION — roles + app_users
--  Qeyd: API özü işə düşəndə bu cədvəlləri avtomatik yaradır,
--  rolları (ADMIN, USER) və boşdursa test/123 (ADMIN) seed edir
--  (AuthBootstrap). Bu skript əl ilə Supabase-də qurmaq üçündür.
-- ============================================================

-- ---- ROLES ----
create table if not exists roles (
  id   serial primary key,
  name text not null unique
);
insert into roles (name) values ('ADMIN') on conflict (name) do nothing;
insert into roles (name) values ('USER')  on conflict (name) do nothing;

-- ---- APP_USERS (giriş hesabları) ----
create table if not exists app_users (
  id            uuid primary key default gen_random_uuid(),
  username      text not null unique,
  password_hash text not null,          -- BCrypt hash (API yazır)
  role_id       int references roles(id),
  created_at    timestamptz not null default now()
);

-- köhnə cədvəldə role_id yoxdursa əlavə et
alter table app_users add column if not exists role_id int references roles(id);
update app_users set role_id = (select id from roles where name='USER') where role_id is null;

-- Qeyd: seed (test/123 = ADMIN) API tərəfindən edilir. Parolu düz mətnlə
-- DB-yə YAZMAYIN — yeni istifadəçini API ilə yaradın: POST /api/auth/register.
