-- SQL Script to set up database schema on Supabase (PostgreSQL)
-- This table stores secondary marketplaces, their dynamic URLs, checking information, and statuses

-- 1. Create status enumeration
CREATE TYPE market_status AS ENUM ('ONLINE', 'OFFLINE', 'VERIFYING', 'REDIRECTING');

-- 2. Create marketplaces table
CREATE TABLE public.marketplaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    current_url VARCHAR(255) NOT NULL,
    source_of_truth_url VARCHAR(255) NOT NULL, -- e.g. official forum tracking link or TG message
    selector_pattern VARCHAR(100) DEFAULT 'a.official-link', -- CSS selector or pattern for the scraper
    status market_status DEFAULT 'ONLINE',
    latency_ms INTEGER DEFAULT 0,
    description TEXT,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 3. Create historical logs table to keep track of domain updates
CREATE TABLE public.domain_update_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marketplace_id UUID REFERENCES public.marketplaces(id) ON DELETE CASCADE,
    old_url VARCHAR(255) NOT NULL,
    new_url VARCHAR(255) NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'automated-scraper',
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 4. Enable Row Level Security (RLS)
ALTER TABLE public.marketplaces ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.domain_update_logs ENABLE ROW LEVEL SECURITY;

-- 5. Create RLS Policies
-- Public can read marketplaces (so frontend can fetch them safely)
CREATE POLICY "Allow public read access to marketplaces" 
ON public.marketplaces 
FOR SELECT 
USING (true);

-- Only authenticated scraper api/actions can write, update, or delete marketplaces
CREATE POLICY "Allow authenticated service role full access to marketplaces" 
ON public.marketplaces 
FOR ALL 
TO service_role 
USING (true) 
WITH CHECK (true);

-- Same policy for update logs
CREATE POLICY "Allow public read access to update logs" 
ON public.domain_update_logs 
FOR SELECT 
USING (true);

CREATE POLICY "Allow authenticated service role full access to update logs" 
ON public.domain_update_logs 
FOR ALL 
TO service_role 
USING (true) 
WITH CHECK (true);

-- 6. Indexes for optimized queries
CREATE INDEX idx_marketplaces_status ON public.marketplaces(status);
CREATE INDEX idx_domain_update_logs_market_id ON public.domain_update_logs(marketplace_id);

-- 7. Add automatic updated_at trigger function
CREATE OR REPLACE FUNCTION update_last_updated_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = timezone('utc'::text, now());
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_update_market_timestamp
    BEFORE UPDATE ON public.marketplaces
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();

-- 8. Seed initial robust marketplace data
INSERT INTO public.marketplaces (name, current_url, source_of_truth_url, selector_pattern, status, description)
VALUES 
('Archetyp Market', 'http://archetyp.onion', 'https://archetyp.info', 'span.onion-address', 'ONLINE', 'The premiere decentralized marketplace focused strictly on pharmacy, privacy, and streamlined user experience.'),
('ASAP Market', 'http://asap46fgf.onion', 'https://asapdnm.co', 'code.mirror-link', 'ONLINE', 'A highly secure, multi-signature enabled escrow marketplace prioritizing custom secure communications.'),
('AlphaBay Reborn', 'http://alphabaywy.onion', 'https://alphabay.is', 'div.mirror-address', 'ONLINE', 'A legacy-styled secure escrow market rebuilt with full-spectrum decentralized hosting nodes.'),
('Abacus Market', 'http://abacus37df7.onion', 'https://abacusmarket.link', 'span.link-active', 'ONLINE', 'An advanced multi-category platform featuring automated coin mixers and enhanced vendor rating layers.'),
('Nemesis Market', 'http://nemesis3f8.onion', 'https://nemesismarket.onion.ly', 'div.live-mirror', 'OFFLINE', 'A hybrid community platform that operates both as a security forum and a specialized decentralized store.');
