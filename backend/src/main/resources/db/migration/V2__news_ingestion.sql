alter table news_articles
    add column if not exists source_code varchar(40),
    add column if not exists external_id varchar(300),
    add column if not exists region varchar(128),
    add column if not exists sector_hint varchar(128),
    add column if not exists fetched_at timestamptz not null default now(),
    add column if not exists status varchar(16) not null default 'RECEIVED',
    add column if not exists retry_count integer not null default 0,
    add column if not exists last_error text;

create unique index if not exists uk_news_source_external
    on news_articles (source_code, external_id)
    where external_id is not null and external_id <> '';

create unique index if not exists uk_news_url
    on news_articles (url)
    where url is not null and url <> '';

create index if not exists idx_news_source_published
    on news_articles (source_code, published_at desc);
