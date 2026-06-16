-- Android push device registry for FCM tokens
create table if not exists public.android_push_devices (
    user_id uuid not null references public.profiles(id) on delete cascade,
    token text not null,
    platform text not null default 'android',
    app_id text not null default 'com.howmylook.app',
    device_name text,
    last_seen_at timestamptz not null default now(),
    primary key (user_id, token)
);

alter table public.android_push_devices enable row level security;

drop policy if exists "Users manage own android push devices" on public.android_push_devices;

create policy "Users manage own android push devices"
on public.android_push_devices
for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);
