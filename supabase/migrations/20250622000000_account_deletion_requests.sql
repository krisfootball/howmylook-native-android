create table if not exists public.account_deletion_requests (
    user_id uuid primary key references auth.users (id) on delete cascade,
    token_hash text not null,
    expires_at timestamptz not null,
    created_at timestamptz not null default now()
);

create index if not exists account_deletion_requests_expires_at_idx
    on public.account_deletion_requests (expires_at);

alter table public.account_deletion_requests enable row level security;
