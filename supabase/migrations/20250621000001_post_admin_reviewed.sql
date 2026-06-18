-- Posts publish as approved immediately; admin_reviewed tracks the admin queue.

alter table public.posts
    add column if not exists admin_reviewed boolean not null default false;

-- Existing posts should not flood the admin queue.
update public.posts
set admin_reviewed = true
where admin_reviewed = false;

drop index if exists posts_pending_moderation_idx;

create index if not exists posts_admin_review_queue_idx
    on public.posts (created_at)
    where admin_reviewed = false and is_active = true and moderation_status = 'approved';

drop policy if exists "Admins can read pending posts" on public.posts;
drop policy if exists "Admins can read review queue posts" on public.posts;

create policy "Admins can read review queue posts"
on public.posts
for select
using (
    public.is_admin_user()
    and admin_reviewed = false
    and is_active = true
    and moderation_status = 'approved'
);
