-- V87–V90 stored Digital Survival Guide Ep02–Ep15 interactive_graph with a stray leading "$": the generator wrote
-- $tag$$<json>$tag$ expecting Flyway to collapse "$$", but the dollar-quoted body keeps it. Jackson can't parse
-- "$..." so those episodes played as plain stories without choices. Strip that one character.
UPDATE library_stories
SET interactive_graph = substr(interactive_graph, 2),
    updated_at = NOW()
WHERE interactive_graph LIKE '$%'
  AND substr(interactive_graph, 2) LIKE '{%';
