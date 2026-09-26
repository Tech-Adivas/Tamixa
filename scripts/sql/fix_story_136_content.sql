-- Fix Story #136: Update content to match the civic survival interactive graph
-- The interactive graph is about a traffic stop scenario
-- But the content is about "The Day the Colors Ran Away" - completely wrong!

-- Backup current state
CREATE TABLE IF NOT EXISTS library_stories_before_content_fix_136 AS 
SELECT * FROM library_stories WHERE id = 136;

-- Update story with civic survival traffic stop content that matches the interactive graph
UPDATE library_stories 
SET 
  title = 'Traffic Stop: Know Your Rights',
  content = E'<speak>\n<storyteller_tone> [Calm tone] [Medium pacing] You\'re driving home after a long day. The evening traffic is heavy, and you\'re thinking about dinner. Suddenly, you see flashing lights in your rearview mirror. [Pause 500ms]\n\n[Serious tone] A uniformed officer waves you to pull over to the shoulder. Your heart starts beating faster. You have all your documents - registration certificate, insurance, driver\'s license, and pollution certificate. Everything is valid and up to date. [Pause 500ms]\n\n[Thoughtful tone] But you remember stories from your childhood. Stories about how refusing an officer always makes things worse. Stories about people who questioned authority and faced problems. [Pause 500ms]\n\nYour phone buzzes. It\'s a payment reminder. Your chest tightens. The officer approaches your window and asks for your vehicle keys. "For safety," they say. [Pause 700ms]\n\n[Gentle tone] This is a moment that tests what you know about your rights. What you do next matters. [Pause 500ms]\n\n<choice_point>\nYou have two options:\n\nOption 1: Hand over the keys immediately to avoid any trouble. After all, they\'re an officer, and you don\'t want to seem difficult or uncooperative.\n\nOption 2: Stay polite and calm. Ask which document they need. Offer your RC, insurance, license, and PUC. But don\'t hand over your keys - you know that\'s not required by law.\n</choice_point>\n\n[Pause 1s]\n\n[Wise tone] Remember: Knowing your rights isn\'t about being difficult. It\'s about protecting yourself while being respectful. Officers who follow proper procedure will ask for documents and explain any issues. They won\'t demand your keys without a valid legal reason. [Pause 500ms]\n\n[Calm tone] If you surrender control out of fear, it becomes much harder to address any issues later. Your keys mean you can\'t move your vehicle if needed. You lose agency in the situation. [Pause 500ms]\n\n[Reassuring tone] The right approach: Keep your hands visible. Have your documents ready. If something feels wrong, note the time, place, and vehicle number calmly. Don\'t argue on the roadside. Escalate through proper channels after you\'re safe. [Pause 500ms]\n\n[Gentle tone] Legal aid helplines exist for exactly these situations. They can guide you on the next steps if you face any improper conduct. [Pause 700ms]\n\n[Closing tone] This is about being informed, being prepared, and knowing that respect goes both ways. You can be polite and cooperative while also protecting your rights. [Pause 500ms]\n\nThat\'s civic survival - knowing how to navigate real-world situations with wisdom and integrity. [Pause 1s]\n</speak>',
  moral = 'Know your rights and exercise them respectfully. Being polite doesn''t mean surrendering your legal protections.',
  word_count = 450,
  theme = 'Learn · Simulator · Civic Survival',
  category = 'Learn · Simulator · Civic Survival',
  age = 12,
  updated_at = NOW()
WHERE id = 136;

-- Verify the update
SELECT 
  id, 
  title, 
  category,
  theme,
  word_count,
  interactive_graph IS NOT NULL as has_graph,
  LENGTH(interactive_graph) as graph_size,
  status
FROM library_stories 
WHERE id = 136;
