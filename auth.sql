-- ============================================================
--  AUTH & AUTHORIZATION — roles, permissions, role_permissions, app_users
--  Qeyd: API özü işə düşəndə bu cədvəlləri avtomatik yaradır, default
--  icazələri seed edir və boşdursa test/123 (ADMIN) əlavə edir
--  (AuthBootstrap). Bu skript əl ilə Supabase-də qurmaq üçündür.
-- ============================================================

-- ---- ROLES ----
create table if not exists roles (
  id   serial primary key,
  name text not null unique
);
insert into roles (name) values ('ADMIN') on conflict (name) do nothing;
insert into roles (name) values ('USER')  on conflict (name) do nothing;

-- ---- PERMISSIONS ----
create table if not exists permissions (
  id   serial primary key,
  code text not null unique
);
insert into permissions (code) values
  ('USER_MANAGE'),        -- app_users yaratmaq/silmək/siyahı
  ('ROLE_MANAGE'),        -- rolun icazələrini dəyişmək
  ('RESERVATION_READ'),   -- rooms/reservations/availability/... GET
  ('RESERVATION_WRITE')   -- reservations POST/PUT/DELETE
on conflict (code) do nothing;

-- ---- ROLE_PERMISSIONS ----
create table if not exists role_permissions (
  role_id       int not null references roles(id) on delete cascade,
  permission_id int not null references permissions(id) on delete cascade,
  primary key (role_id, permission_id)
);

-- Default seed (yalnız cari halda yoxdursa)
insert into role_permissions(role_id, permission_id)
select r.id, p.id from roles r cross join permissions p
where r.name='ADMIN'
on conflict do nothing;

insert into role_permissions(role_id, permission_id)
select r.id, p.id from roles r cross join permissions p
where r.name='USER' and p.code in ('RESERVATION_READ','RESERVATION_WRITE')
on conflict do nothing;

-- ---- APP_USERS (giriş hesabları) ----
create table if not exists app_users (
  id            uuid primary key default gen_random_uuid(),
  username      text not null unique,
  password_hash text not null,          -- BCrypt hash (API yazır)
  role_id       int references roles(id),
  created_at    timestamptz not null default now()
);
alter table app_users add column if not exists role_id int references roles(id);
update app_users set role_id = (select id from roles where name='USER') where role_id is null;

-- Qeyd: seed (test/123 = ADMIN) API tərəfindən edilir. Parolu düz mətnlə
-- DB-yə YAZMAYIN — yeni istifadəçini API ilə yaradın: POST /api/auth/register.
