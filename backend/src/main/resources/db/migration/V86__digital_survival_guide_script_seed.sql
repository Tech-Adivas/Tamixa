-- Digital Survival Guide: Episode 1 (6 locales, interactive_graph) + pilot outlines Ep2–Ep15.
-- DRAFT rows; story_owner = seed:digital-survival-guide-v1. Publish from admin after audio/CDN.
-- Source doc: docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG] Ep01 — The Midnight Blackout (en)',
  '# The Midnight Blackout (Episode 1)

Co-listening simulator: senior + family; electricity bill urgency scam; two decision nodes (call vs verify vs collaborate; then ₹10 UPI link).

**Doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md — full multilingual VO in §5.

**Patterns:** urgency, unknown sender, small-amount lure, screen-share / fake payment page.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  50,
  0.25,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  '{"startSegmentId":"ep01_hook","overlayStyle":"CARDS","segments":{"ep01_hook":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/en/hook_to_node1.mp3","choices":[{"id":"call_sms_number","label":"Call the number in the SMS immediately","nextSegmentId":"ep01_mid_panic_call","skillDeltas":{"DIGITAL_WISDOM":-2,"balance":-1}},{"id":"open_official_app","label":"Open the official electricity app or website to check","nextSegmentId":"ep01_mid_verify_app","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"ask_family_review_sms","label":"Ask Priya to read the SMS carefully together","nextSegmentId":"ep01_mid_collaborate","skillDeltas":{"DIGITAL_WISDOM":1,"balance":1}}]},"ep01_mid_panic_call":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/en/branch_call_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"Pay ₹10 on the link — small amount, stay safe","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"Stop — never enter UPI PIN on a link from SMS","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"Report to the real helpline and block the number","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_mid_verify_app":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/en/branch_app_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"Pay ₹10 on the link — small amount, stay safe","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"Stop — never enter UPI PIN on a link from SMS","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"Report to the real helpline and block the number","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_mid_collaborate":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/en/branch_collab_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"Pay ₹10 on the link — small amount, stay safe","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"Stop — never enter UPI PIN on a link from SMS","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"Report to the real helpline and block the number","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_outcome_pin_trap":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/en/outcome_what_if_pin.mp3","choices":[]},"ep01_outcome_safe":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/en/outcome_safe_family.mp3","choices":[]},"ep01_outcome_report":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/en/outcome_reported.mp3","choices":[]}}}',
  'The Screen-Share Challenge: With a parent or grandparent, open one unknown “click link” SMS; name urgency, sender, and grammar; delete it together.',
  '["Who sent this message — a short code or a random mobile?","Would a real utility ask for your UPI PIN on an SMS link?"]'::jsonb,
  'EduStory simulator pilot; full scripts in repo docs §5.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG] Ep01 — The Midnight Blackout (hi)',
  '# अर्धरात्रि बिजली गुल (एपिसोड 1)

सह-सुनने वाला सिम्युलेटर: बिजली बिल की झूठी आपातकालीन SMS; दो निर्णय बिंदु।

**पूरा स्क्रिप्ट:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §5.2

**पैटर्न:** जल्दबाज़ी, अज्ञात भेजने वाला, छोटी रकम का जाल, फर्जी लिंक।
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'hi',
  10,
  'Family',
  50,
  0.25,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  '{"startSegmentId":"ep01_hook","overlayStyle":"CARDS","segments":{"ep01_hook":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/hi/hook_to_node1.mp3","choices":[{"id":"call_sms_number","label":"SMS वाले नंबर पर तुरंत कॉल करूँ","nextSegmentId":"ep01_mid_panic_call","skillDeltas":{"DIGITAL_WISDOM":-2,"balance":-1}},{"id":"open_official_app","label":"आधिकारिक बिजली ऐप या वेबसाइट खोलकर चेक करूँ","nextSegmentId":"ep01_mid_verify_app","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"ask_family_review_sms","label":"प्रिया से SMS साथ मिलकर ध्यान से पढ़वाऊँ","nextSegmentId":"ep01_mid_collaborate","skillDeltas":{"DIGITAL_WISDOM":1,"balance":1}}]},"ep01_mid_panic_call":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/hi/branch_call_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"दस रुपये छोटी रकम है, भर देता हूँ","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"रुकूँगा — SMS के लिंक पर UPI PIN कभी नहीं","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"असली हेल्पलाइन पर रिपोर्ट करूँगा और नंबर ब्लॉक करूँगा","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_mid_verify_app":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/hi/branch_app_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"दस रुपये छोटी रकम है, भर देता हूँ","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"रुकूँगा — SMS के लिंक पर UPI PIN कभी नहीं","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"असली हेल्पलाइन पर रिपोर्ट करूँगा और नंबर ब्लॉक करूँगा","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_mid_collaborate":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/hi/branch_collab_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"दस रुपये छोटी रकम है, भर देता हूँ","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"रुकूँगा — SMS के लिंक पर UPI PIN कभी नहीं","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"असली हेल्पलाइन पर रिपोर्ट करूँगा और नंबर ब्लॉक करूँगा","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_outcome_pin_trap":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/hi/outcome_what_if_pin.mp3","choices":[]},"ep01_outcome_safe":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/hi/outcome_safe_family.mp3","choices":[]},"ep01_outcome_report":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/hi/outcome_reported.mp3","choices":[]}}}',
  'स्क्रीन-शेयर चैलेंज: माता-पिता या दादा-दादी के साथ एक अनजान ‘लिंक’ SMS खोलें; जल्दबाज़ी, भेजने वाला, भाषा बताएँ; साथ में डिलीट करें।',
  '["Who sent this message — a short code or a random mobile?","Would a real utility ask for your UPI PIN on an SMS link?"]'::jsonb,
  'EduStory simulator pilot; full scripts in repo docs §5.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG] Ep01 — The Midnight Blackout (ta)',
  '# அரையிரவு மின் துண்டிப்பு (அத்தியாயம் 1)

குடும்பத்துடன் கேட்கும் சிமுலேட்டர்: மின் பில் மோசடி SMS; இரண்டு தேர்வு முனைகள்.

**முழு வசனம்:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §5.3

**பேட்டர்ன்கள்:** அவசரம், அந்நிய அனுப்புநர், சிறு தொகை, போலி இணைப்பு.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'ta',
  10,
  'Family',
  50,
  0.25,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  '{"startSegmentId":"ep01_hook","overlayStyle":"CARDS","segments":{"ep01_hook":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/ta/hook_to_node1.mp3","choices":[{"id":"call_sms_number","label":"SMS-ல இருக்க நம்பருக்கு உடனே கால்","nextSegmentId":"ep01_mid_panic_call","skillDeltas":{"DIGITAL_WISDOM":-2,"balance":-1}},{"id":"open_official_app","label":"TANGEDCO அதிகாரப்பூர்வ ஆப் அல்லது வலைத்தளம் திறந்து சரிபார்ப்பு","nextSegmentId":"ep01_mid_verify_app","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"ask_family_review_sms","label":"பிரியாவை கூப்பிட்டு SMS ஒண்ணா படித்து பார்க்க வைக்கிறேன்","nextSegmentId":"ep01_mid_collaborate","skillDeltas":{"DIGITAL_WISDOM":1,"balance":1}}]},"ep01_mid_panic_call":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/ta/branch_call_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"பத்து ரூபாய் சின்னதுதான், கட்டிடுறேன்","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"நிறுத்து — SMS லிங்குல UPI PIN போட மாட்டேன்","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"உண்மையான ஹெல்ப்லைனுக்கு புகார்; எண்ணை ப்ளாக்","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_mid_verify_app":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/ta/branch_app_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"பத்து ரூபாய் சின்னதுதான், கட்டிடுறேன்","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"நிறுத்து — SMS லிங்குல UPI PIN போட மாட்டேன்","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"உண்மையான ஹெல்ப்லைனுக்கு புகார்; எண்ணை ப்ளாக்","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_mid_collaborate":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/ta/branch_collab_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"பத்து ரூபாய் சின்னதுதான், கட்டிடுறேன்","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"நிறுத்து — SMS லிங்குல UPI PIN போட மாட்டேன்","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"உண்மையான ஹெல்ப்லைனுக்கு புகார்; எண்ணை ப்ளாக்","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_outcome_pin_trap":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/ta/outcome_what_if_pin.mp3","choices":[]},"ep01_outcome_safe":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/ta/outcome_safe_family.mp3","choices":[]},"ep01_outcome_report":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/ta/outcome_reported.mp3","choices":[]}}}',
  'ஸ்கிரீன்-ஷேர் சவால்: பாட்டி/தாத்தாவுடன் ‘லிங்க் கிளிக்’ மெசேஜ் ஒன்றைத் திறந்து அவசரம், அனுப்புநர், மொழி சொல்லி ஒண்ணா டிலீட் பண்ணுங்க.',
  '["Who sent this message — a short code or a random mobile?","Would a real utility ask for your UPI PIN on an SMS link?"]'::jsonb,
  'EduStory simulator pilot; full scripts in repo docs §5.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG] Ep01 — The Midnight Blackout (te)',
  '# మధ్యరాత్రి కరెంట్ కట్ (ఎపిసోడ్ 1)

కుటుంబంతో కలిసి వినే సిమ్యులేటర్; విద్యుత్ బిల్ మోసం SMS; రెండు నిర్ణయాలు.

**పూర్తి స్క్రిప్ట్:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §5.4
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'te',
  10,
  'Family',
  50,
  0.25,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  '{"startSegmentId":"ep01_hook","overlayStyle":"CARDS","segments":{"ep01_hook":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/te/hook_to_node1.mp3","choices":[{"id":"call_sms_number","label":"SMS లోని నంబర్‌కు వెంటనే కాల్","nextSegmentId":"ep01_mid_panic_call","skillDeltas":{"DIGITAL_WISDOM":-2,"balance":-1}},{"id":"open_official_app","label":"డిస్కాం అధికారిక యాప్ లేదా వెబ్‌సైట్ తెరిచి చెక్","nextSegmentId":"ep01_mid_verify_app","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"ask_family_review_sms","label":"ప్రియాతో కలిసి SMS జాగ్రత్తగా చదవించు","nextSegmentId":"ep01_mid_collaborate","skillDeltas":{"DIGITAL_WISDOM":1,"balance":1}}]},"ep01_mid_panic_call":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/te/branch_call_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"పది రూపాయలు చిన్నదే, కట్టేస్తా","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"ఆగు — SMS లింక్‌లో UPI PIN ఇవ్వను","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"నిజమైన హెల్ప్‌లైన్‌కు రిపోర్ట్, నంబర్ బ్లాక్","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_mid_verify_app":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/te/branch_app_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"పది రూపాయలు చిన్నదే, కట్టేస్తా","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"ఆగు — SMS లింక్‌లో UPI PIN ఇవ్వను","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"నిజమైన హెల్ప్‌లైన్‌కు రిపోర్ట్, నంబర్ బ్లాక్","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_mid_collaborate":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/te/branch_collab_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"పది రూపాయలు చిన్నదే, కట్టేస్తా","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"ఆగు — SMS లింక్‌లో UPI PIN ఇవ్వను","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"నిజమైన హెల్ప్‌లైన్‌కు రిపోర్ట్, నంబర్ బ్లాక్","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_outcome_pin_trap":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/te/outcome_what_if_pin.mp3","choices":[]},"ep01_outcome_safe":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/te/outcome_safe_family.mp3","choices":[]},"ep01_outcome_report":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/te/outcome_reported.mp3","choices":[]}}}',
  'స్క్రీన్-షేర్ సవాల్: తాతయ్య/నానమ్మతో ‘లింక్ క్లిక్’ మెసేజ్ తెరవండి; తొందర, పంపినవారు, భాష చెప్పి కలిసి డిలీట్ చేయండి.',
  '["Who sent this message — a short code or a random mobile?","Would a real utility ask for your UPI PIN on an SMS link?"]'::jsonb,
  'EduStory simulator pilot; full scripts in repo docs §5.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG] Ep01 — The Midnight Blackout (kn)',
  '# ಮಧ್ಯರಾತ್ರಿ ವಿದ್ಯುತ್ ಕಡಿತ (ಎಪಿಸೋಡ್ 1)

ಕುಟುಂಬದೊಂದಿಗೆ ಕೇಳುವ ಸಿಮ್ಯುಲೇಟರ್; ವಿದ್ಯುತ್ ಬಿಲ್ ವಂಚನೆ; ಎರಡು ಆಯ್ಕೆ ನೋಡ್‌ಗಳು.

**ಪೂರ್ಣ ಲಿಪಿ:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §5.5
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'kn',
  10,
  'Family',
  50,
  0.25,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  '{"startSegmentId":"ep01_hook","overlayStyle":"CARDS","segments":{"ep01_hook":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/kn/hook_to_node1.mp3","choices":[{"id":"call_sms_number","label":"SMS ನಲ್ಲಿನ ನಂಬರ್‌ಗೆ ತಕ್ಷಣ ಕರೆ","nextSegmentId":"ep01_mid_panic_call","skillDeltas":{"DIGITAL_WISDOM":-2,"balance":-1}},{"id":"open_official_app","label":"BESCOM / ಅಧಿಕೃತ ವಿದ್ಯುತ್ ಯಾಪ್ ಅಥವಾ ವೆಬ್‌ಸೈಟ್ ತೆರೆದು ಪರಿಶೀಲಿಸು","nextSegmentId":"ep01_mid_verify_app","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"ask_family_review_sms","label":"ಪ್ರಿಯಾಳ ಜೊತೆ SMS ಒಟ್ಟಿಗೆ ಓದಿ ನೋಡು","nextSegmentId":"ep01_mid_collaborate","skillDeltas":{"DIGITAL_WISDOM":1,"balance":1}}]},"ep01_mid_panic_call":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/kn/branch_call_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"ಹತ್ತು ರೂಪಾಯಿ ಚಿಕ್ಕದು, ಪಾವತಿಸುತ್ತೇನೆ","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"ನಿಲ್ಲು — SMS ಲಿಂಕ್‌ನಲ್ಲಿ UPI PIN ಇಡುವುದಿಲ್ಲ","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"ನಿಜವಾದ ಹೆಲ್ಪ್‌ಲೈನ್‌ಗೆ ದೂರು, ನಂಬರ್ ಬ್ಲಾಕ್","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_mid_verify_app":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/kn/branch_app_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"ಹತ್ತು ರೂಪಾಯಿ ಚಿಕ್ಕದು, ಪಾವತಿಸುತ್ತೇನೆ","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"ನಿಲ್ಲು — SMS ಲಿಂಕ್‌ನಲ್ಲಿ UPI PIN ಇಡುವುದಿಲ್ಲ","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"ನಿಜವಾದ ಹೆಲ್ಪ್‌ಲೈನ್‌ಗೆ ದೂರು, ನಂಬರ್ ಬ್ಲಾಕ್","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_mid_collaborate":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/kn/branch_collab_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"ಹತ್ತು ರೂಪಾಯಿ ಚಿಕ್ಕದು, ಪಾವತಿಸುತ್ತೇನೆ","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"ನಿಲ್ಲು — SMS ಲಿಂಕ್‌ನಲ್ಲಿ UPI PIN ಇಡುವುದಿಲ್ಲ","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"ನಿಜವಾದ ಹೆಲ್ಪ್‌ಲೈನ್‌ಗೆ ದೂರು, ನಂಬರ್ ಬ್ಲಾಕ್","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_outcome_pin_trap":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/kn/outcome_what_if_pin.mp3","choices":[]},"ep01_outcome_safe":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/kn/outcome_safe_family.mp3","choices":[]},"ep01_outcome_report":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/kn/outcome_reported.mp3","choices":[]}}}',
  'ಸ್ಕ್ರೀನ್-ಶೇರ್ ಸವಾಲ್: ಅಜ್ಜಿ/ಅಜ್ಜನೊಂದಿಗೆ ‘ಲಿಂಕ್ ಕ್ಲಿಕ್’ ಮೆಸೇಜ್ ತೆರೆಯಿರಿ; ತ್ವರೆ, ಕಳುಹಿಸಿದವರು, ಭಾಷೆ ಹೇಳಿ ಒಟ್ಟಿಗೆ ಡಿಲೀಟ್ ಮಾಡಿ.',
  '["Who sent this message — a short code or a random mobile?","Would a real utility ask for your UPI PIN on an SMS link?"]'::jsonb,
  'EduStory simulator pilot; full scripts in repo docs §5.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG] Ep01 — The Midnight Blackout (ml)',
  '# അർദ്ധരാത്രി വൈദ്യുതി മുറിക്കൽ (എപ്പിസോഡ് 1)

കുടുംബത്തോടൊപ്പം കേൾക്കുന്ന സിമുലേറ്റർ; KSEB തട്ടിപ്പ് SMS; രണ്ട് തീരുമാന നോഡുകൾ.

**പൂർണ്ണ സ്ക്രിപ്റ്റ്:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §5.6
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'ml',
  10,
  'Family',
  50,
  0.25,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  '{"startSegmentId":"ep01_hook","overlayStyle":"CARDS","segments":{"ep01_hook":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/ml/hook_to_node1.mp3","choices":[{"id":"call_sms_number","label":"SMS-ലെ നമ്പറിലേക്ക് ഉടൻ വിളിക്കും","nextSegmentId":"ep01_mid_panic_call","skillDeltas":{"DIGITAL_WISDOM":-2,"balance":-1}},{"id":"open_official_app","label":"KSEB ഔദ്യോഗിക ആപ്പ് അല്ലെങ്കിൽ വെബ്സൈറ്റ് തുറന്ന് പരിശോധിക്കും","nextSegmentId":"ep01_mid_verify_app","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"ask_family_review_sms","label":"പ്രിയയോടൊപ്പം SMS ഒന്നിച്ച് ശ്രദ്ധിച്ച് വായിക്കും","nextSegmentId":"ep01_mid_collaborate","skillDeltas":{"DIGITAL_WISDOM":1,"balance":1}}]},"ep01_mid_panic_call":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/ml/branch_call_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"പത്തു രൂപ ചെറിയതാണ്, അടയ്ക്കാം","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"നിർത്തുക — SMS ലിങ്കിൽ UPI PIN ഇടില്ല","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"യഥാർത്ഥ ഹെൽപ്‌ലൈനിൽ റിപ്പോർട്ട്, നമ്പർ ബ്ലോക്ക്","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_mid_verify_app":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/ml/branch_app_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"പത്തു രൂപ ചെറിയതാണ്, അടയ്ക്കാം","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"നിർത്തുക — SMS ലിങ്കിൽ UPI PIN ഇടില്ല","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"യഥാർത്ഥ ഹെൽപ്‌ലൈനിൽ റിപ്പോർട്ട്, നമ്പർ ബ്ലോക്ക്","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_mid_collaborate":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/ml/branch_collab_plus_fee_trap.mp3","choices":[{"id":"pay_ten_fee","label":"പത്തു രൂപ ചെറിയതാണ്, അടയ്ക്കാം","nextSegmentId":"ep01_outcome_pin_trap","skillDeltas":{"DIGITAL_WISDOM":-3}},{"id":"refuse_upi_on_link","label":"നിർത്തുക — SMS ലിങ്കിൽ UPI PIN ഇടില്ല","nextSegmentId":"ep01_outcome_safe","skillDeltas":{"DIGITAL_WISDOM":2,"balance":1}},{"id":"report_and_block","label":"യഥാർത്ഥ ഹെൽപ്‌ലൈനിൽ റിപ്പോർട്ട്, നമ്പർ ബ്ലോക്ക്","nextSegmentId":"ep01_outcome_report","skillDeltas":{"DIGITAL_WISDOM":3,"balance":1}}]},"ep01_outcome_pin_trap":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/ml/outcome_what_if_pin.mp3","choices":[]},"ep01_outcome_safe":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/ml/outcome_safe_family.mp3","choices":[]},"ep01_outcome_report":{"audioUrl":"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/ml/outcome_reported.mp3","choices":[]}}}',
  'സ്‌ക്രീൻ-ഷെയർ ചലഞ്ച്: മുത്തശ്ശി/മുത്തച്ഛനൊപ്പം ‘ലിങ്ക് ക്ലിക്ക്’ മെസേജ് തുറന്ന് അത്യാവശ്യം, അയച്ചവർ, ഭാഷ പറഞ്ഞ് ഒന്നിച്ച് ഡിലീറ്റ് ചെയ്യുക.',
  '["Who sent this message — a short code or a random mobile?","Would a real utility ask for your UPI PIN on an SMS link?"]'::jsonb,
  'EduStory simulator pilot; full scripts in repo docs §5.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG Pilot] Ep02 — The KYC Countdown',
  '# The Digital Survival Guide — Episode 2 (Pilot outline)

**Hook:** SMS: "HDFC/SBI — KYC incomplete, account blocked in 2 hours. Link to update."

**Node 1:** A) Tap link now · B) Open bank app only · C) Call number printed on debit card back.

**Fallout:** A → fake form + OTP harvest; B/C → no pending KYC in real app.

**Node 2:** "Pay ₹2 gateway charge to unfreeze." A) Pay · B) Never pay via SMS link · C) Call official 1800 from card / statement.

**Mission:** Find the one official customer-care number for your bank (statement/card) and save it as a contact.

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 2.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  110,
  0.55,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  NULL,
  NULL,
  '["What is one red flag in an urgent payment message?"]'::jsonb,
  'English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode 2.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG Pilot] Ep03 — The ₹49 Courier',
  '# Episode 3 — The ₹49 Courier

**Hook:** "BlueDart — your parcel on hold; ₹49 customs digitization fee."

**Node 1:** A) Pay ₹49 · B) Check tracking on brand site with AWB · C) Ask family if anyone ordered.

**Converge:** Second SMS: "Download tracking APK."

**Node 2:** A) Install APK · B) Never install APK from SMS · C) Call courier from website number only.

**Mission:** Compare sender ID on a real courier SMS vs a random 10-digit sender.

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 3.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  85,
  0.42,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  NULL,
  NULL,
  '["What is one red flag in an urgent payment message?"]'::jsonb,
  'English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode 3.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG Pilot] Ep04 — The School Panic Call',
  '# Episode 4 — The School Panic Call

**Hook:** Voice call: "I am from your child''s school; accident; send money for ambulance UPI."

**Node 1:** A) Send immediately · B) Hang up, call school landline you already have · C) Call spouse first, conference.

**Node 2:** "Don''t tell teachers — secrecy fee ₹100." A) Pay · B) Secrecy = red flag · C) Conference with class teacher on known number.

**Mission:** Agree a family codeword for real emergencies (no shame in verifying).

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 4.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  88,
  0.44,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  NULL,
  NULL,
  '["What is one red flag in an urgent payment message?"]'::jsonb,
  'English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode 4.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG Pilot] Ep05 — The Job Registration Fee',
  '# Episode 5 — The Job Registration Fee

**Hook:** WhatsApp: "TCS/Infosys walk-in confirmed; pay ₹499 registration."

**Node 1:** A) Pay for "sure seat" · B) Check careers.* official site only · C) Ask elder who works in IT.

**Node 2:** "Upgrade to fast-track ₹99." A) Pay · B) Real employers don''t charge registration · C) Report number.

**Mission:** List three signs a job offer is fake (money upfront, personal Gmail, urgency).

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 5.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  77,
  0.39,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  NULL,
  NULL,
  '["What is one red flag in an urgent payment message?"]'::jsonb,
  'English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode 5.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG Pilot] Ep06 — The Lottery You Never Entered',
  '# Episode 6 — The Lottery You Never Entered

**Hook:** "Congratulations — KBC / Google winner ₹5 lakh; pay ₹1 verification."

**Node 1:** A) Pay ₹1 · B) Delete — you never entered · C) Search scam pattern with family.

**Node 2:** "Add admin on Telegram for prize OTP." A) Add · B) OTP = account key · C) Block and mark spam.

**Mission:** One dinner question: "What would you never do for ''free'' money?"

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 6.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  81,
  0.41,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  NULL,
  NULL,
  '["What is one red flag in an urgent payment message?"]'::jsonb,
  'English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode 6.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG Pilot] Ep07 — The Subsidy Portal',
  '# Episode 7 — The Subsidy Portal

**Hook:** SMS: "PM Ujjwala / subsidy — register on this portal before midnight."

**Node 1:** A) Register on link · B) Check mygov / official channel news · C) Ask panchayat / gas agency next visit.

**Node 2:** "Upload Aadhaar front-back on this form." A) Upload · B) Never upload Aadhaar to random sites · C) Use only official UIDAI flows if ever needed.

**Mission:** Point out one fake .com domain vs a known .gov.in / .nic.in pattern (age-appropriate).

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 7.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  92,
  0.46,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  NULL,
  NULL,
  '["What is one red flag in an urgent payment message?"]'::jsonb,
  'English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode 7.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG Pilot] Ep08 — Insurance Lapse Tonight',
  '# Episode 8 — Insurance Lapse Tonight

**Hook:** Call: "Your LIC policy lapsed; pay ₹500 now or lose ₹2 lakh benefit."

**Node 1:** A) Pay on link · B) Open policy document / agent number you chose yourself · C) Visit branch next working day.

**Node 2:** "Share screen so I can fill the form." A) Share · B) No insurer asks Anydesk · C) Hang up, call policy helpline.

**Mission:** Locate your real policy number and helpline on paper; put in one drawer.

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 8.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  90,
  0.45,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  NULL,
  NULL,
  '["What is one red flag in an urgent payment message?"]'::jsonb,
  'English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode 8.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG Pilot] Ep09 — PAN–Aadhaar Last Day',
  '# Episode 9 — PAN–Aadhaar Last Day

**Hook:** Urgent SMS with "income tax" logo image — link to link PAN "or fine ₹10,000."

**Node 1:** A) Panic-link · B) Open incometax.gov.in from typed URL · C) Ask CA / literate family member.

**Node 2:** "Pay ₹100 processing on UPI." A) Pay · B) Government portals don''t collect random ₹100 on SMS · C) Verify deadline on official site only.

**Mission:** Practice typing one government URL together (no tap from message).

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 9.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  86,
  0.43,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  NULL,
  NULL,
  '["What is one red flag in an urgent payment message?"]'::jsonb,
  'English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode 9.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG Pilot] Ep10 — The Accidental UPI Transfer',
  '# Episode 10 — The Accidental UPI Transfer

**Hook:** "I sent ₹5000 to your number by mistake; please return to this UPI ID."

**Node 1:** A) Return immediately · B) Check if money actually credited in app · C) Ignore — common script.

**Node 2:** "Send ₹1 test first." A) Send · B) Test payments still train you to trust scammer · C) If real credit, use bank dispute channel, not stranger ID.

**Mission:** Read aloud: "UPI received vs promised."

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 10.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  86,
  0.43,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  NULL,
  NULL,
  '["What is one red flag in an urgent payment message?"]'::jsonb,
  'English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode 10.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG Pilot] Ep11 — The Refund Executive',
  '# Episode 11 — The Refund Executive

**Hook:** "Amazon/Flipkart refund failed; download TeamViewer for verification."

**Node 1:** A) Download · B) Open app orders only — no refund pending · C) Ask teen to verify order ID.

**Node 2:** "Type refund amount in this remote box." A) Type · B) Never type amounts/PIN for a ''refund'' · C) Use in-app help chat only.

**Mission:** Show one real in-app "help" path on a parent phone.

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 11.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  80,
  0.4,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  NULL,
  NULL,
  '["What is one red flag in an urgent payment message?"]'::jsonb,
  'English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode 11.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG Pilot] Ep12 — The Rishta Verification',
  '# Episode 12 — The Rishta Verification

**Hook:** Matrimonial site DM: "Pay ₹999 for police verification badge."

**Node 1:** A) Pay for trust · B) Check site settings / verified rules on official FAQ · C) Discuss with family offline first.

**Node 2:** "Video KYC on this APK." A) Install · B) APK outside Play Store = high risk · C) Meet through arranged family channel only.

**Mission:** List two green flags (known introducer, slow pace) vs red flags (money before meeting).

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 12.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  88,
  0.44,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  NULL,
  NULL,
  '["What is one red flag in an urgent payment message?"]'::jsonb,
  'English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode 12.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG Pilot] Ep13 — The Telegram Tipster',
  '# Episode 13 — The Telegram Tipster

**Hook:** "Join VIP group — bank Nifty sure shot; pay ₹50 admin fee."

**Node 1:** A) Pay to join · B) Remember: no one sells sure returns · C) Talk to parent about investing only via SEBI-registered advisors.

**Node 2:** "Deposit ₹500 in this UPI for ''margin''." A) Deposit · B) Stop — pyramid pattern · C) Exit group, report.

**Mission:** One line: "If they need your money to teach you money, walk away."

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 13.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  87,
  0.43,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  NULL,
  NULL,
  '["What is one red flag in an urgent payment message?"]'::jsonb,
  'English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode 13.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG Pilot] Ep14 — Society Maintenance Portal',
  '# Episode 14 — Society Maintenance Portal

**Hook:** WhatsApp from "Secretary" unknown number: "Pay maintenance on new portal link."

**Node 1:** A) Pay on link · B) Check society notice board / known secretary number · C) Ask neighbor on known phone.

**Node 2:** "Late fee ₹20 if not paid in 10 minutes." A) Pay · B) Real societies rarely minute-level threaten · C) Confirm in elevator group with known admins.

**Mission:** Save two trusted society contacts (not just "Secretary" label).

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 14.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  87,
  0.43,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  NULL,
  NULL,
  '["What is one red flag in an urgent payment message?"]'::jsonb,
  'English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode 14.'
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[DSG Pilot] Ep15 — The Hospital Bed Deposit',
  '# Episode 15 — The Hospital Bed Deposit (sensitive)

**Tone:** Calm VO; no graphic detail.

**Hook:** Call claiming hospital needs instant deposit for a named relative.

**Node 1:** A) Transfer now · B) Call relative on saved number · C) Call hospital main line from Google Maps / known bill.

**Node 2:** "Don''t tell family — they''ll worry." A) Obey secrecy · B) Secrecy + urgency = scam signature · C) Conference call with family.

**Mission:** Agree: "Any real hospital bill can wait one verified phone call."

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 15.
',
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  10,
  'Family',
  93,
  0.47,
  'Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.',
  'DRAFT',
  'seed:digital-survival-guide-v1',
  NULL,
  NULL,
  '["What is one red flag in an urgent payment message?"]'::jsonb,
  'English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode 15.'
);
