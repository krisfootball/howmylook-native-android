-- User reports for posts and profiles
create table if not exists public.content_reports (
    id uuid primary key default gen_random_uuid(),
    reporter_id uuid not null references public.profiles(id) on delete cascade,
    target_type text not null check (target_type in ('post', 'profile')),
    target_id uuid not null,
    reason text,
    created_at timestamptz not null default now()
);

create index if not exists content_reports_target_idx
    on public.content_reports (target_type, target_id);

create index if not exists posts_expiry_purge_idx
    on public.posts (expires_at)
    where keep_forever = false and is_active = true;

alter table public.content_reports enable row level security;

drop policy if exists "Users can submit reports" on public.content_reports;
create policy "Users can submit reports"
on public.content_reports
for insert
with check (auth.uid() = reporter_id);

drop policy if exists "Users can read own reports" on public.content_reports;
create policy "Users can read own reports"
on public.content_reports
for select
using (auth.uid() = reporter_id);
