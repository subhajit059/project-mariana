/**
 * Marketplace dynamic link scraper & Supabase updater
 * This Node.js script visits specified official sources of truth (forums, telegram endpoints, landing pages)
 * parses the HTML to extract the newest valid onion or system domain, and saves any updates in Supabase.
 * 
 * Dependencies required to run:
 * npm install @supabase/supabase-js axios cheerio dotenv
 */

require('dotenv').config();
const { createClient } = require('@supabase/supabase-js');
const axios = require('axios');
const cheerio = require('cheerio');

// Initialize Supabase Client using service role key (required to bypass RLS write restrictions)
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY;

if (!supabaseUrl || !supabaseServiceKey) {
  console.error("❌ Crucial configuration error: SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY environment variables are required.");
  process.exit(1);
}

const supabase = createClient(supabaseUrl, supabaseServiceKey);

/**
 * Main scraper engine
 */
async function runLinkScraper() {
  console.log(`🤖 Starting Link Scraper Process: [${new Date().toISOString()}]`);
  
  // 1. Fetch active marketplaces from database
  const { data: marketplaces, error } = await supabase
    .from('marketplaces')
    .select('*');
    
  if (error) {
    console.error("❌ Error fetching marketplaces from Supabase:", error.message);
    return;
  }
  
  console.log(`📡 Loaded ${marketplaces.length} marketplaces from database.`);

  for (const market of marketplaces) {
    console.log(`\n🔍 Checking updates for: ${market.name}...`);
    console.log(`🔗 Current DB URL: ${market.current_url}`);
    console.log(`📋 Source of Truth: ${market.source_of_truth_url}`);
    
    try {
      // 2. Fetch the HTML content from official Source of Truth
      // We simulate or make an actual HTTP GET with a standard User-Agent header
      const response = await axios.get(market.source_of_truth_url, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
          'Accept-Language': 'en-US,en;q=0.5'
        },
        timeout: 15000 // 15 seconds timeout
      });

      const html = response.data;
      const $ = cheerio.load(html);
      
      // 3. Extract the new domain using the configured CSS selector pattern
      let extractedUrl = null;
      const selector = market.selector_pattern || 'a.official-link';
      
      console.log(`🎯 Scraping using selector: "${selector}"`);
      
      if (selector.startsWith('regex:')) {
        // Regex extraction if selector is complex
        const regexStr = selector.replace('regex:', '');
        const regex = new RegExp(regexStr, 'i');
        const match = html.match(regex);
        if (match && match[1]) {
          extractedUrl = match[1].trim();
        }
      } else {
        // Cheerio selector extraction
        const element = $(selector).first();
        if (element.length > 0) {
          // Can be href or text
          extractedUrl = element.attr('href') || element.text();
          if (extractedUrl) {
            extractedUrl = extractedUrl.trim();
          }
        }
      }

      if (!extractedUrl) {
        console.warn(`⚠️ Warning: Selector "${selector}" yielded no matches for ${market.name}. Sticking with existing URL.`);
        continue;
      }

      // Ensure URL has appropriate protocol relative format or schema
      if (!extractedUrl.startsWith('http://') && !extractedUrl.startsWith('https://')) {
        // If it starts with local paths, concatenate. Otherwise assume http/https
        if (extractedUrl.startsWith('/')) {
          const origin = new URL(market.source_of_truth_url).origin;
          extractedUrl = `${origin}${extractedUrl}`;
        } else {
          extractedUrl = `http://${extractedUrl}`;
        }
      }

      console.log(`✅ Extracted Latest URL: ${extractedUrl}`);

      // 4. Compare with existing database URL
      if (extractedUrl !== market.current_url) {
        console.log(`🚨 ALERT: URL changed! Updating database for ${market.name}...`);
        
        // Measure server ping/latency to verify if indeed online
        let latency = 0;
        let testStatus = 'ONLINE';
        try {
          const start = Date.now();
          await axios.head(extractedUrl, { timeout: 8000 });
          latency = Date.now() - start;
        } catch (pingErr) {
          console.log(`⚠️ Network check failed for new url, setting status as VERIFYING. Error: ${pingErr.message}`);
          testStatus = 'VERIFYING';
        }

        // Start transaction updates in Supabase
        // A. Update marketplaces table
        const { error: updateError } = await supabase
          .from('marketplaces')
          .update({ 
            current_url: extractedUrl,
            status: testStatus,
            latency_ms: latency,
            last_updated: new Date().toISOString()
          })
          .eq('id', market.id);

        if (updateError) {
          console.error(`❌ DB Update Error for ${market.name}:`, updateError.message);
          continue;
        }

        // B. Log change inside domain_update_logs for security history
        const { error: logError } = await supabase
          .from('domain_update_logs')
          .insert({
            marketplace_id: market.id,
            old_url: market.current_url,
            new_url: extractedUrl,
            updated_by: 'automated-scraper'
          });

        if (logError) {
          console.error(`⚠️ Log Error: Failed to insert update history for ${market.name}:`, logError.message);
        }

        console.log(`🎉 Successfully updated ${market.name} link in Supabase!`);
      } else {
        console.log(`💤 Match found. DB link is up to date.`);
        
        // Optional: Update latency and status even if URL did not change
        try {
          const start = Date.now();
          await axios.head(market.current_url, { timeout: 8000 });
          const latency = Date.now() - start;
          await supabase
            .from('marketplaces')
            .update({ status: 'ONLINE', latency_ms: latency })
            .eq('id', market.id);
        } catch (e) {
          await supabase
            .from('marketplaces')
            .update({ status: 'OFFLINE', latency_ms: 0 })
            .eq('id', market.id);
        }
      }

    } catch (err) {
      console.error(`❌ Scraper failed to fetch/parse page for ${market.name}:`, err.message);
      // Mark as verifying if source of truth can't be reached
      await supabase
        .from('marketplaces')
        .update({ status: 'OFFLINE' })
        .eq('id', market.id);
    }
  }

  console.log(`\n🏁 Link Scraper Process Finished cleanly.`);
}

// Execute Scraper
runLinkScraper();
