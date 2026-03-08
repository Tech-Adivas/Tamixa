import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import {
  getChildren,
  getAchievements,
  getRecommendedStories,
  getRecentPlayback,
  type Child,
  type Achievement,
  type RecommendedStory,
  type PlaybackPosition,
} from "../lib/api";

export default function Dashboard() {
  useAuth();
  const [children, setChildren] = useState<Child[]>([]);
  const [recommended, setRecommended] = useState<RecommendedStory[]>([]);
  const [recent, setRecent] = useState<PlaybackPosition[]>([]);
  const [childAchievements, setChildAchievements] = useState<Record<number, Achievement[]>>({});

  useEffect(() => {
    getChildren().then(setChildren).catch(() => setChildren([]));
  }, []);
  useEffect(() => {
    getRecommendedStories(children[0]?.id, "ta", 6).then(setRecommended).catch(() => setRecommended([]));
  }, [children]);
  useEffect(() => {
    getRecentPlayback(5).then(setRecent).catch(() => setRecent([]));
  }, []);
  useEffect(() => {
    Promise.all(children.map((c) => getAchievements(c.id))).then((results) => {
      const map: Record<number, Achievement[]> = {};
      children.forEach((c, i) => { map[c.id] = results[i] ?? []; });
      setChildAchievements(map);
    }).catch(() => {});
  }, [children]);

  const earnedCount = (achievements: Achievement[]) => achievements.filter((a) => a.earned).length;

  return (
    <div className="page prime-page">
      <section className="prime-row dashboard-first">
        <div className="prime-row-header">
          <h2 className="prime-row-title">Continue listening</h2>
          {recent.length > 0 && (
            <Link to="/stories" className="prime-row-link">See all</Link>
          )}
        </div>
        {recent.length > 0 ? (
          <div className="prime-row-scroll">
            {recent.map((p) => (
              <Link
                key={`${p.storyId}-${p.storySource}`}
                to={`/stories?resume=${p.storyId}&source=${p.storySource}`}
                className="poster-card"
              >
                <div className="poster-card-cover">
                  <div className="poster-card-placeholder" aria-hidden>▶</div>
                </div>
                <p className="poster-card-title">Story #{p.storyId}</p>
                <p className="poster-card-meta">{Math.floor(p.positionSeconds / 60)} min · Continue</p>
              </Link>
            ))}
          </div>
        ) : (
          <p className="muted" style={{ padding: "0 0.125rem" }}>Start a story to see continue options here.</p>
        )}
      </section>

      <section className="prime-row">
        <div className="prime-row-header">
          <h2 className="prime-row-title">Recommended for you</h2>
          <Link to="/stories" className="prime-row-link">See all</Link>
        </div>
        {recommended.length > 0 ? (
          <div className="prime-row-scroll">
            {recommended.slice(0, 10).map((s) => (
              <Link
                key={`${s.storyId}-${s.storySource}`}
                to="/stories"
                state={{ playStoryId: s.storyId, playStorySource: s.storySource }}
                className="poster-card"
              >
                <div className="poster-card-cover">
                  <div className="poster-card-placeholder" aria-hidden>📖</div>
                </div>
                <p className="poster-card-title">{s.title}</p>
                <p className="poster-card-meta">{s.reason}</p>
              </Link>
            ))}
          </div>
        ) : (
          <p className="muted" style={{ padding: "0 0.125rem" }}>Add favorites and child interests for better recommendations.</p>
        )}
      </section>

      {children.length > 0 && (
        <section className="prime-row">
          <div className="prime-row-header">
            <h2 className="prime-row-title">Badges & achievements</h2>
            <Link to="/children" className="prime-row-link">Manage profiles</Link>
          </div>
          <div style={{ padding: "0 0.125rem" }}>
            {children.map((c) => {
              const achievements = childAchievements[c.id] ?? [];
              const earned = earnedCount(achievements);
              return (
                <div key={c.id} className="dashboard-badge-block">
                  <strong>{c.name}</strong>: {earned} / {achievements.length} badges
                  <div className="dashboard-badges">
                    {achievements.filter((a) => a.earned).map((a) => (
                      <span key={a.type} title={a.description} className="dashboard-badge-emoji">{a.name.split(" ")[0]}</span>
                    ))}
                  </div>
                  <Link to="/children" className="btn btn-sm btn-outline">Edit profile</Link>
                </div>
              );
            })}
            <p className="muted" style={{ marginTop: "0.35rem", marginBottom: 0 }}>Complete stories with a child selected to earn badges.</p>
          </div>
        </section>
      )}

      <section className="prime-row">
        <p className="muted" style={{ margin: 0, padding: "0 0.125rem" }}>Use the Stories page to browse the curated library and generate new AI stories for your child.</p>
      </section>
    </div>
  );
}
