import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { logger } from "../lib/logger";
import {
  getRecommendedStories,
  getRecentPlayback,
  getListeningProgress,
  type RecommendedStory,
  type PlaybackPosition,
  type ListeningProgress,
} from "../lib/api";

function formatResumeMeta(positionSeconds: number): string {
  if (positionSeconds < 60) {
    return `${Math.max(0, Math.floor(positionSeconds))}s played · Continue`;
  }
  const m = Math.floor(positionSeconds / 60);
  return `${m} min in · Continue`;
}

function greetingName(user: { email: string; displayName?: string | null; nickname?: string | null } | null): string {
  if (!user) return "there";
  const d = user.displayName?.trim();
  if (d) return d;
  const n = user.nickname?.trim();
  if (n) return n;
  const local = user.email.split("@")[0]?.replace(/[._]+/g, " ").trim();
  if (local) {
    return local.slice(0, 1).toUpperCase() + local.slice(1);
  }
  return "there";
}

function completionPercent(rate: number): number {
  if (Number.isNaN(rate)) return 0;
  return rate <= 1 ? Math.round(rate * 100) : Math.round(rate);
}

export default function Dashboard() {
  const { user } = useAuth();
  const [recommended, setRecommended] = useState<RecommendedStory[]>([]);
  const [recent, setRecent] = useState<PlaybackPosition[]>([]);
  const [progress, setProgress] = useState<ListeningProgress | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([
      getRecommendedStories(undefined, "ta", 12).catch((err) => {
        logger.warn("dashboard", "Failed to load recommendations", {
          message: err instanceof Error ? err.message : String(err),
        });
        return [] as RecommendedStory[];
      }),
      getRecentPlayback(8).catch((err) => {
        logger.warn("dashboard", "Failed to load recent playback", {
          message: err instanceof Error ? err.message : String(err),
        });
        return [] as PlaybackPosition[];
      }),
      getListeningProgress(30).catch((err) => {
        logger.warn("dashboard", "Failed to load listening progress", {
          message: err instanceof Error ? err.message : String(err),
        });
        return null;
      }),
    ])
      .then(([rec, recents, prog]) => {
        if (!cancelled) {
          setRecommended(rec);
          setRecent(recents);
          setProgress(prog);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const name = greetingName(user);

  return (
    <div className="page prime-page dash-page">
      {loading ? (
        <div className="dash-loading" aria-busy="true" aria-live="polite">
          <div className="dash-hero dash-hero--skeleton">
            <div className="skeleton dash-skel-title" />
            <div className="skeleton dash-skel-line" />
            <div className="skeleton dash-skel-line dash-skel-line--short" />
          </div>
          <div className="dash-stat-grid">
            {[1, 2, 3].map((i) => (
              <div key={i} className="dash-stat-card dash-stat-card--skeleton">
                <div className="skeleton dash-skel-stat-label" />
                <div className="skeleton dash-skel-stat-value" />
              </div>
            ))}
          </div>
          <div className="skeleton dash-skel-section-title" />
          <div className="dash-poster-row dash-poster-row--skeleton">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="skeleton dash-skel-poster" />
            ))}
          </div>
        </div>
      ) : (
        <>
          <header className="dash-hero">
            <div className="dash-hero-text">
              <p className="dash-hero-eyebrow">Your story nook</p>
              <h1>Welcome back, {name}</h1>
              <p className="dash-hero-subtitle">
                Jump back into the tale you paused, see what Tamixa suggests next, or open the library for a brand-new
                adventure.
              </p>
            </div>
            <div className="dash-hero-actions">
              <Link to="/stories" className="btn btn-primary">
                Start listening
              </Link>
              <Link to="/stories" className="btn btn-outline btn-hero-secondary">
                Browse tales
              </Link>
            </div>
          </header>

          <section className="dash-stat-grid" aria-label="Your listening fun">
            <div className="dash-stat-card">
              <p className="dash-stat-label">Adventures begun</p>
              <p className="dash-stat-value">{progress != null ? progress.storiesStarted : "—"}</p>
              <p className="dash-stat-hint muted">Last 30 evenings</p>
            </div>
            <div className="dash-stat-card">
              <p className="dash-stat-label">Tales finished</p>
              <p className="dash-stat-value">{progress != null ? progress.storiesCompleted : "—"}</p>
              <p className="dash-stat-hint muted">All the way to the end</p>
            </div>
            <div className="dash-stat-card">
              <p className="dash-stat-label">Stick-with-it score</p>
              <p className="dash-stat-value">
                {progress != null ? `${completionPercent(progress.completionRate)}%` : "—"}
              </p>
              <p className="dash-stat-hint muted">Stories you complete</p>
            </div>
          </section>

          <nav className="dash-quick" aria-label="Shortcuts">
            <Link to="/stories" className="dash-quick-card">
              <span className="dash-quick-icon" aria-hidden>
                📚
              </span>
              <span className="dash-quick-title">Stories</span>
              <span className="dash-quick-desc muted">Library, favorites &amp; new tales</span>
            </Link>
            <Link to="/subscription" className="dash-quick-card">
              <span className="dash-quick-icon" aria-hidden>
                ✨
              </span>
              <span className="dash-quick-title">Tamixa Pass</span>
              <span className="dash-quick-desc muted">Plan &amp; listening perks</span>
            </Link>
            <Link to="/voice" className="dash-quick-card">
              <span className="dash-quick-icon" aria-hidden>
                🎙️
              </span>
              <span className="dash-quick-title">Voices</span>
              <span className="dash-quick-desc muted">Narration that feels like you</span>
            </Link>
            <Link to="/settings" className="dash-quick-card">
              <span className="dash-quick-icon" aria-hidden>
                🛡️
              </span>
              <span className="dash-quick-title">Family & privacy</span>
              <span className="dash-quick-desc muted">Account & safety</span>
            </Link>
          </nav>

          <section className="dash-section dash-section--stories" aria-labelledby="dash-continue-heading">
            <div className="dash-section-head">
              <h2 id="dash-continue-heading" className="dash-section-title dash-section-title--spotlight">
                Resume a story
              </h2>
              {recent.length > 0 ? (
                <Link to="/stories" className="dash-section-link">
                  See all
                </Link>
              ) : null}
            </div>
            {recent.length > 0 ? (
              <div className="dash-poster-row">
                {recent.map((p) => (
                  <Link
                    key={`${p.storyId}-${p.storySource}`}
                    to={`/stories?resume=${p.storyId}&source=${p.storySource}`}
                    className="poster-card dash-poster-card"
                  >
                    <div
                      className="poster-card-cover poster-card-placeholder--default-cover"
                      aria-hidden
                      style={{ backgroundImage: "url(/story-card-default.png)" }}
                    />
                    <p className="poster-card-title">Story #{p.storyId}</p>
                    <p className="poster-card-meta">{formatResumeMeta(p.positionSeconds)}</p>
                  </Link>
                ))}
              </div>
            ) : (
              <div className="dash-empty">
                <p className="dash-empty-title">No cliffhangers yet</p>
                <p className="dash-empty-desc muted">
                  Press play on any tale in the library—we&apos;ll bookmark your spot so the next chapter is one tap away.
                </p>
                <Link to="/stories" className="btn btn-primary">
                  Find a story
                </Link>
              </div>
            )}
          </section>

          <section className="dash-section dash-section--stories" aria-labelledby="dash-rec-heading">
            <div className="dash-section-head">
              <h2 id="dash-rec-heading" className="dash-section-title dash-section-title--spotlight">
                Tonight&apos;s picks
              </h2>
              <Link to="/stories" className="dash-section-link">
                See all
              </Link>
            </div>
            {recommended.length > 0 ? (
              <div className="dash-poster-row">
                {recommended.slice(0, 10).map((s) => (
                  <Link
                    key={`${s.storyId}-${s.storySource}`}
                    to="/stories"
                    state={{ playStoryId: s.storyId, playStorySource: s.storySource }}
                    className="poster-card dash-poster-card"
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
              <div className="dash-empty">
                <p className="dash-empty-title">We&apos;re lining up magic for you</p>
                <p className="dash-empty-desc muted">
                  Heart a few favorites in the library—Tamixa learns what your family loves and builds a fresher playlist.
                </p>
                <Link to="/stories" className="btn btn-outline">
                  Discover stories
                </Link>
              </div>
            )}
          </section>
        </>
      )}
    </div>
  );
}
