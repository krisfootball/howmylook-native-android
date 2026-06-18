-- Admin moderation: flag on profiles + RLS for reviewing pending posts.

alter table public.profiles
    add column if not exists is_admin boolean not null default false;

create or replace function public.is_admin_user()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select coalesce(
        (select is_admin from public.profiles where id = auth.uid()),
        false
    );
$$;

create index if not exists posts_pending_moderation_idx
    on public.posts (created_at)
    where moderation_status = 'pending' and is_active = true;

drop policy if exists "Admins can read pending posts" on public.posts;
create policy "Admins can read pending posts"
on public.posts
for select
using (
    public.is_admin_user()
    and moderation_status = 'pending'
    and is_active = true
);

drop policy if exists "Admins can moderate posts" on public.posts;
create policy "Admins can moderate posts"
on public.posts
for update
using (public.is_admin_user())
with check (public.is_admin_user());

drop policy if exists "Admins can delete posts" on public.posts;
create policy "Admins can delete posts"
on public.posts
for delete
using (public.is_admin_user());

drop policy if exists "Admins can delete post images" on public.post_images;
create policy "Admins can delete post images"
on public.post_images
for delete
using (public.is_admin_user());

drop policy if exists "Admins can delete votes on moderated posts" on public.votes;
create policy "Admins can delete votes on moderated posts"
on public.votes
for delete
using (public.is_admin_user());

-- Grant admin to your account (replace username with yours, then run this migration):
-- update public.profiles set is_admin = true where username = 'yourusername';
