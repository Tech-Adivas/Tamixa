import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import {
  getRecommendedStories,
  getRecentPlayback,
  type RecommendedStory,
  type PlaybackPosition,
} from "../lib/api";

export default function Dashboard() {
  useAuth();
  const [recommended, setRecommended] = useState<RecommendedStory[]>([]);
  const [recent, setRecent] = useState<PlaybackPosition[]>([]);

  useEffect(() => {
    getRecommendedStories(undefined, "ta", 6).then(setRecommended).catch(() => setRecommended([]));
  }, []);
  useEffect(() => {
    getRecentPlayback(5).then(setRecent).catch(() => setRecent([]));
  }, []);

  return (
    <div className="page prime-page">
      <header className="page-header-tamixa" style={{ marginTop: 0 }}>
        <h1>Home</h1>
        <p className="page-subtitle">Continue listening, recommendations, and badges.</p>
      </header>
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
                <div
                  className="poster-card-cover poster-card-placeholder--default-cover"
                  aria-hidden
                  style={{ backgroundImage: "url(/story-card-default.png)" }}
                />
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
                <div
                  className="poster-card-cover poster-card-placeholder--default-cover"
                  aria-hidden
                  style={{ backgroundImage: "url(/story-card-default.png)" }}
                />
                <p className="poster-card-title">{s.title}</p>
                <p className="poster-card-meta">{s.reason}</p>
              </Link>
            ))}
          </div>
        ) : (
          <p className="muted" style={{ padding: "0 0.125rem" }}>Add favorites for better recommendations.</p>
        )}
      </section>

      <section className="prime-row">
        <p className="muted" style={{ margin: 0, padding: "0 0.125rem" }}>Use the Stories page to browse the story library and generate new AI stories.</p>
      </section>
    </div>
  );
}
