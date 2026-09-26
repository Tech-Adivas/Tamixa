-- Update Story Titles and Categories with Professional, Unique Names
-- Based on Tamixa Naming Strategy

-- ============================================
-- PART 1: Update Category Names
-- ============================================

-- Digital Safety → Suraksha Chakra (सुरक्षा चक्र)
UPDATE library_stories 
SET category = 'Suraksha Chakra · Digital Safety',
    theme = 'Suraksha Chakra · Digital Safety'
WHERE category LIKE '%Digital Safety%' OR theme LIKE '%Digital Safety%';

-- Money/Finance → Dhan Vidya (धन विद्या)
UPDATE library_stories 
SET category = 'Dhan Vidya · Money Wisdom',
    theme = 'Dhan Vidya · Money Wisdom'
WHERE category LIKE '%Money%' OR theme LIKE '%Money%';

-- Leadership → Neta Guna (नेता गुण)
UPDATE library_stories 
SET category = 'Neta Guna · Leadership',
    theme = 'Neta Guna · Leadership'
WHERE category LIKE '%Leadership%' OR theme LIKE '%Leadership%';

-- Business → Vyapar Yukti (व्यापार युक्ति)
UPDATE library_stories 
SET category = 'Vyapar Yukti · Business',
    theme = 'Vyapar Yukti · Business'
WHERE category LIKE '%Business%' OR theme LIKE '%Business%';

-- Ethics → Dharma Path (धर्म पथ)
UPDATE library_stories 
SET category = 'Dharma Path · Ethics',
    theme = 'Dharma Path · Ethics'
WHERE category LIKE '%Ethics%' OR theme LIKE '%Ethics%';

-- ============================================
-- PART 2: Update Story Titles - Digital Safety
-- ============================================

UPDATE library_stories 
SET title = 'The Midnight Call: Digital Arrest Scam'
WHERE title LIKE '%Phantom OTP%';

UPDATE library_stories 
SET title = 'The Voice That Wasn''t Real'
WHERE title LIKE '%Deepfake Call%';

UPDATE library_stories 
SET title = 'The Viral Lie'
WHERE title LIKE '%WhatsApp Forward%';

-- ============================================
-- PART 3: Update Story Titles - Money/Finance
-- ============================================

UPDATE library_stories 
SET title = 'The Hidden Price of "Free"'
WHERE title LIKE '%Zero-Interest Illusion%';

UPDATE library_stories 
SET title = 'Plastic Power: Your First Card'
WHERE title LIKE '%First Credit Card%';

UPDATE library_stories 
SET title = 'Flash Sale Frenzy'
WHERE title LIKE '%Discount Trap%';

UPDATE library_stories 
SET title = 'Where Did My Money Go?'
WHERE title LIKE '%SIP Leak Hunt%';

UPDATE library_stories 
SET title = 'The ₹50,000 Hospital Bill Shock'
WHERE title LIKE '%Hospital Nightmare%Room Rent%';

UPDATE library_stories 
SET title = 'Insurance Said No: Now What?'
WHERE title LIKE '%Surgery Denied%Proposal Truth%';

UPDATE library_stories 
SET title = 'What Your Policy Doesn''t Cover'
WHERE title LIKE '%Health Insurance Fine Print%';

UPDATE library_stories 
SET title = 'The Loan Your Spouse Doesn''t Know'
WHERE title LIKE '%Secret Loan%Spouse%Relative%';

UPDATE library_stories 
SET title = 'Dream Wedding vs. Bank Balance'
WHERE title LIKE '%Wedding Budget%';

UPDATE library_stories 
SET title = 'Term vs Money-Back: The 20-Year Choice'
WHERE title LIKE '%Term vs Money-Back%Twenty Years%';

UPDATE library_stories 
SET title = 'The Vanishing Balance: Fees & Auto-Sweep'
WHERE title LIKE '%Vanishing Minimum%Fees%Auto-Sweep%';

-- ============================================
-- PART 4: Update Story Titles - Leadership
-- ============================================

UPDATE library_stories 
SET title = 'The Slacker Teammate'
WHERE title LIKE '%Group Project Crisis%';

UPDATE library_stories 
SET title = 'Speak Up or Stay Silent?'
WHERE title LIKE '%Society Meeting%';

UPDATE library_stories 
SET title = 'When the Customer Crosses the Line'
WHERE title LIKE '%Customer Is Wrong%';

UPDATE library_stories 
SET title = 'The Bully Next Door'
WHERE title LIKE '%Neighborhood Bully%';

-- ============================================
-- PART 5: Update Story Titles - Business
-- ============================================

UPDATE library_stories 
SET title = 'Pickle Business: Jars or Taste?'
WHERE title LIKE '%Kirana Start-up%';

UPDATE library_stories 
SET title = 'Family Loan: Yes or No?'
WHERE title LIKE '%Relative''s Request%';

-- ============================================
-- PART 6: Update Story Titles - Ethics
-- ============================================

UPDATE library_stories 
SET title = 'AI Wrote My Essay'
WHERE title LIKE '%Plagiarism Shortcut%';

UPDATE library_stories 
SET title = 'Cheat Sheet Temptation'
WHERE title LIKE '%Exam Pressure%';

-- ============================================
-- PART 7: Update Story Titles - Legal/Civic
-- ============================================

UPDATE library_stories 
SET title = 'Pulled Over: Know Your Rights'
WHERE title LIKE '%Traffic Stop%Keys%Papers%';

UPDATE library_stories 
SET title = 'Fired or Resigned? The Paper Trap'
WHERE title LIKE '%Pink Slip%Voluntary Resignation%';

UPDATE library_stories 
SET title = 'The Tax Notice: Nil GST Return Trap'
WHERE title LIKE '%Tax Notice%Nil GST Return%';

-- ============================================
-- PART 8: Update Story Titles - Career/Work
-- ============================================

UPDATE library_stories 
SET title = 'AI Will Take My Job: Myth or Reality?'
WHERE title LIKE '%AI Career Fear%';

UPDATE library_stories 
SET title = 'The Accent Question: Interview Dilemma'
WHERE title LIKE '%Interview Accent%';

-- ============================================
-- PART 9: Update Story Titles - Consumer Rights
-- ============================================

UPDATE library_stories 
SET title = 'The Lost Receipt: Return Policy Battle'
WHERE title LIKE '%Lost Receipt%';

-- ============================================
-- PART 10: Update Story Titles - Elder Safety
-- ============================================

UPDATE library_stories 
SET title = 'Grandpa''s UPI: Teaching Digital Payments'
WHERE title LIKE '%Grandparent''s Wallet%UPI%';

-- ============================================
-- PART 11: Update Story Titles - Emergency Preparedness
-- ============================================

UPDATE library_stories 
SET title = 'Survival 101: Three Life-Saving Drills'
WHERE title LIKE '%Survival 101%Three Drills%';

-- ============================================
-- VERIFICATION QUERIES
-- ============================================

-- Check updated categories
SELECT DISTINCT category, COUNT(*) as story_count
FROM library_stories
WHERE interactive_graph IS NOT NULL
GROUP BY category
ORDER BY category;

-- Check updated titles (sample)
SELECT id, title, category, status
FROM library_stories
WHERE interactive_graph IS NOT NULL
ORDER BY category, title
LIMIT 20;

-- Find stories that still have old naming patterns
SELECT id, title, category
FROM library_stories
WHERE (
    title LIKE '%[LifeSim]%' 
    OR title LIKE '%[Survival]%'
    OR title LIKE '%[Civic]%'
    OR category LIKE '%Learn · Simulator%'
)
AND interactive_graph IS NOT NULL;

-- Count stories by new category names
SELECT 
    CASE 
        WHEN category LIKE '%Suraksha Chakra%' THEN 'Suraksha Chakra (Digital Safety)'
        WHEN category LIKE '%Dhan Vidya%' THEN 'Dhan Vidya (Money Wisdom)'
        WHEN category LIKE '%Neta Guna%' THEN 'Neta Guna (Leadership)'
        WHEN category LIKE '%Vyapar Yukti%' THEN 'Vyapar Yukti (Business)'
        WHEN category LIKE '%Dharma Path%' THEN 'Dharma Path (Ethics)'
        ELSE 'Other'
    END as category_group,
    COUNT(*) as count
FROM library_stories
WHERE interactive_graph IS NOT NULL
GROUP BY category_group
ORDER BY count DESC;
