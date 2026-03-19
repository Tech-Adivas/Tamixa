"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import Link from "next/link";
import { api, authStorage } from "@/lib/api";
import type { ParentSummary, PagedResponse, VoiceProfile, LibraryStorySummary, LibraryStoryStreamUrlResponse } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useActionResult } from "@/contexts/action-result-context";
import { FlaskConical, Upload, RefreshCw, Play, Pause, Square, Trash2, Volume2, Video, UserCircle, ImagePlus, Maximize2, Download, Loader2 } from "lucide-react";

const PAGE_SIZE = 100;

export default function VoiceTestPage() {
  const { showSuccess, showError } = useActionResult();
  const [parents, setParents] = useState<ParentSummary[]>([]);
  const [parentsLoading, setParentsLoading] = useState(true);
  const [selectedParentId, setSelectedParentId] = useState<number | null>(null);
  const [profiles, setProfiles] = useState<VoiceProfile[]>([]);
  const [profilesLoading, setProfilesLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [previewSampleProfileId, setPreviewSampleProfileId] = useState<number | null>(null);
  const [previewSampleLoading, setPreviewSampleLoading] = useState(false);
  const sampleAudioRef = useRef<{ element: HTMLAudioElement; objectUrl: string } | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  /** Per-profile consent file for Google voice cloning (key = profile id). */
  const [consentFileForProfileId, setConsentFileForProfileId] = useState<Record<number, File | null>>({});
  const [consentUploadingForId, setConsentUploadingForId] = useState<number | null>(null);
  const [runJobForId, setRunJobForId] = useState<number | null>(null);
  const [stories, setStories] = useState<LibraryStorySummary[]>([]);
  const [storiesLoading, setStoriesLoading] = useState(false);
  const [selectedStoryId, setSelectedStoryId] = useState<number | null>(null);
  const [previewLang, setPreviewLang] = useState("ta");
  const [selectedProfileId, setSelectedProfileId] = useState<number | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewPlaying, setPreviewPlaying] = useState(false);
  const [previewPaused, setPreviewPaused] = useState(false);
  const [deleteNarrationLoading, setDeleteNarrationLoading] = useState(false);
  const previewAudioRef = useRef<{ element: HTMLAudioElement; objectUrl: string } | null>(null);
  const [avatarVideoUrl, setAvatarVideoUrl] = useState<string | null>(null);
  /** Narration audio (Tamil/selected language) played in sync with avatar video so language is correct even if video has no track */
  const [avatarNarrationUrl, setAvatarNarrationUrl] = useState<string | null>(null);
  const [avatarVideoLoading, setAvatarVideoLoading] = useState(false);
  const [avatarVideoPending, setAvatarVideoPending] = useState(false);
  const [avatarVideoError, setAvatarVideoError] = useState<string | null>(null);
  const [avatarVideoSuccess, setAvatarVideoSuccess] = useState(false);
  const [avatarVideoProvider, setAvatarVideoProvider] = useState<string | null>(null);
  const avatarVideoRef = useRef<HTMLVideoElement | null>(null);
  const avatarNarrationAudioRef = useRef<HTMLAudioElement | null>(null);
  /** True when cloned-voice narration exists for current story+lang+voice (enables "Play with avatar" without doing Step 1 first) */
  const [narrationReadyForAvatar, setNarrationReadyForAvatar] = useState(false);
  const [narrationCheckLoading, setNarrationCheckLoading] = useState(false);
  /** When true, the avatar video has its own audio track (e.g. from Replicate); use it for perfect lip-sync instead of separate narration */
  const [videoHasNativeAudio, setVideoHasNativeAudio] = useState(false);
  const pollAttemptsRef = useRef(0);
  const POLL_INTERVAL_MS = 20000; // 20s between checks
  const POLL_MAX_ATTEMPTS = 30;    // up to ~10 min total (avatar generation can take 2–5+ min)
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null);
  const [avatarLoading, setAvatarLoading] = useState(false);
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [avatarDeleting, setAvatarDeleting] = useState(false);
  const [selectedAvatarFile, setSelectedAvatarFile] = useState<File | null>(null);
  const [avatarRegenerating, setAvatarRegenerating] = useState(false);

  const loadParents = useCallback(() => {
    setParentsLoading(true);
    api.admin
      .getParents(0, PAGE_SIZE)
      .then((res: PagedResponse<ParentSummary>) => {
        setParents(res.content.map((p) => ({ ...p, name: p.name ?? p.email.split("@")[0] })));
      })
      .catch((e) => showError("Load parents failed", e instanceof Error ? e.message : "Failed to load parents"))
      .finally(() => setParentsLoading(false));
  }, [showError]);

  const loadProfiles = useCallback(() => {
    if (selectedParentId == null) {
      setProfiles([]);
      return;
    }
    if (!authStorage.getToken()) {
      showError("Session expired", "Please sign in again. Your session may have expired.");
      setProfiles([]);
      return;
    }
    setProfilesLoading(true);
    api.admin
      .getVoiceProfilesForParent(selectedParentId)
      .then(setProfiles)
      .catch((e) => {
        const msg = e instanceof Error ? e.message : String(e);
        if (/unauthorized|log in|401/i.test(msg)) {
          showError("Session expired", "Please sign in again.");
        } else {
          showError("Load profiles failed", msg);
        }
        setProfiles([]);
      })
      .finally(() => setProfilesLoading(false));
  }, [selectedParentId, showError]);

  useEffect(() => {
    loadParents();
  }, [loadParents]);

  useEffect(() => {
    loadProfiles();
  }, [loadProfiles]);

  const loadAvatarUrl = useCallback(() => {
    if (selectedParentId == null) {
      setAvatarUrl(null);
      return;
    }
    setAvatarLoading(true);
    api.admin
      .getParentAvatarUrl(selectedParentId)
      .then((data) => setAvatarUrl(data?.avatarUrl ?? null))
      .catch(() => setAvatarUrl(null))
      .finally(() => setAvatarLoading(false));
  }, [selectedParentId]);

  useEffect(() => {
    loadAvatarUrl();
  }, [loadAvatarUrl]);

  useEffect(() => {
    if (profiles.length === 1 && selectedProfileId === null) setSelectedProfileId(profiles[0]!.id);
  }, [profiles, selectedProfileId]);

  useEffect(() => {
    setStoriesLoading(true);
    api.admin
      .getLibraryStories(0, 200)
      .then((res) => {
        // Show only approved stories (narration approved for delivery) in avatar/voice test
        const approved = (res.content ?? []).filter((s) => s.narrationApprovedAt != null);
        setStories(approved);
      })
      .catch(() => setStories([]))
      .finally(() => setStoriesLoading(false));
  }, []);

  const stopSamplePreview = useCallback(() => {
    const current = sampleAudioRef.current;
    if (current) {
      current.element.pause();
      current.element.currentTime = 0;
      URL.revokeObjectURL(current.objectUrl);
      sampleAudioRef.current = null;
    }
    setPreviewSampleProfileId(null);
  }, []);

  const handlePreviewClonedVoice = async (profileId: number) => {
    if (selectedParentId == null) return;
    if (previewSampleProfileId === profileId && sampleAudioRef.current) {
      stopSamplePreview();
      return;
    }
    stopSamplePreview();
    setPreviewSampleLoading(true);
    try {
      const blob = await api.admin.getVoiceReferenceAudioBlob(selectedParentId, profileId);
      const objectUrl = URL.createObjectURL(blob);
      const audio = new Audio(objectUrl);
      audio.addEventListener("ended", () => {
        URL.revokeObjectURL(objectUrl);
        sampleAudioRef.current = null;
        setPreviewSampleProfileId(null);
      });
      audio.addEventListener("error", () => {
        URL.revokeObjectURL(objectUrl);
        sampleAudioRef.current = null;
        setPreviewSampleProfileId(null);
        showError("Playback failed", "Could not play reference audio");
      });
      await audio.play();
      sampleAudioRef.current = { element: audio, objectUrl };
      setPreviewSampleProfileId(profileId);
      showSuccess("Playing uploaded sample", "This is the reference audio for this cloned voice.");
    } catch (e) {
      showError("Preview failed", e instanceof Error ? e.message : "Preview failed");
    } finally {
      setPreviewSampleLoading(false);
    }
  };

  const handleDeleteProfile = async (profileId: number) => {
    if (selectedParentId == null) return;
    if (!confirm(`Delete voice profile cloned:${profileId}? You can upload a new sample after.`)) return;
    setDeletingId(profileId);
    if (previewSampleProfileId === profileId) stopSamplePreview();
    try {
      await api.admin.deleteVoiceProfileForParent(selectedParentId, profileId);
      if (selectedProfileId === profileId) setSelectedProfileId(null);
      showSuccess("Voice deleted", "Upload a new audio file to create a replacement profile.");
      loadProfiles();
    } catch (e) {
      showError("Delete failed", e instanceof Error ? e.message : "Delete failed");
    } finally {
      setDeletingId(null);
    }
  };

  const handleUpload = async () => {
    if (selectedParentId == null || selectedFile == null) {
      showError("Validation", "Select a parent and a file.");
      return;
    }
    setUploading(true);
    try {
      await api.admin.uploadVoiceForParent(selectedParentId, selectedFile);
      showSuccess("Voice profile created", "Use cloned:{id} in the app for this parent.");
      setSelectedFile(null);
      loadProfiles();
    } catch (e) {
      showError("Upload failed", e instanceof Error ? e.message : "Upload failed");
    } finally {
      setUploading(false);
    }
  };

  const handleUploadConsent = async (profileId: number) => {
    if (selectedParentId == null) return;
    const file = consentFileForProfileId[profileId] ?? (document.getElementById(`consent-file-${profileId}`) as HTMLInputElement)?.files?.[0];
    if (!file) {
      showError("Validation", "Choose a consent audio file first (under “Consent (Google)” for this row).");
      return;
    }
    setConsentUploadingForId(profileId);
    try {
      const result = await api.admin.uploadConsentAndRunVoiceCloning(selectedParentId, profileId, file);
      showSuccess("Voice cloning job started (Google)", result.message);
      const input = document.getElementById(`consent-file-${profileId}`) as HTMLInputElement | null;
      if (input) input.value = "";
      setConsentFileForProfileId((prev) => ({ ...prev, [profileId]: null }));
    } catch (e) {
      showError("Consent upload failed", e instanceof Error ? e.message : "Upload failed");
    } finally {
      setConsentUploadingForId(null);
    }
  };

  const handleRunVoiceCloningJob = async (profileId: number) => {
    if (selectedParentId == null) return;
    setRunJobForId(profileId);
    try {
      const result = await api.admin.runVoiceCloningJobForProfile(selectedParentId, profileId);
      showSuccess("Voice cloning job started (ElevenLabs)", result.message);
    } catch (e) {
      showError("Run job failed", e instanceof Error ? e.message : "Failed");
    } finally {
      setRunJobForId(null);
    }
  };

  const stopPreview = useCallback(() => {
    const current = previewAudioRef.current;
    if (current) {
      current.element.pause();
      current.element.currentTime = 0;
      URL.revokeObjectURL(current.objectUrl);
      previewAudioRef.current = null;
    }
    setPreviewPlaying(false);
    setPreviewPaused(false);
  }, []);

  // When user changes story, parent, or voice — stop current playback so the next play is for the new selection
  useEffect(() => {
    stopPreview();
  }, [selectedStoryId, selectedProfileId, selectedParentId, stopPreview]);

  const handlePlayWithClonedVoice = async () => {
    if (selectedParentId == null || selectedStoryId == null || selectedProfileId == null) {
      showError("Validation", "Select a parent, a story, and a voice profile.");
      return;
    }
    const current = previewAudioRef.current;
    if (current && previewPaused) {
      current.element.play().then(() => {
        setPreviewPlaying(true);
        setPreviewPaused(false);
      }).catch(() => setPreviewPaused(true));
      return;
    }
    if (current) {
      stopPreview();
    }
    setPreviewLoading(true);
    try {
      const blob = await api.admin.getLibraryStoryPreviewAudioBlob(
        selectedStoryId,
        previewLang,
        `cloned:${selectedProfileId}`,
        selectedParentId
      );
      const objectUrl = URL.createObjectURL(blob);
      const audio = new Audio(objectUrl);
      audio.addEventListener("ended", () => {
        URL.revokeObjectURL(objectUrl);
        previewAudioRef.current = null;
        setPreviewPlaying(false);
        setPreviewPaused(false);
      });
      await audio.play();
      previewAudioRef.current = { element: audio, objectUrl };
      setPreviewPlaying(true);
      setPreviewPaused(false);
      setNarrationReadyForAvatar(true);
      showSuccess("Playing story with cloned voice", "First time may take a moment to generate.");
    } catch (e) {
      showError("Preview failed", e instanceof Error ? e.message : "Preview failed");
    } finally {
      setPreviewLoading(false);
    }
  };

  const handlePausePreview = () => {
    const current = previewAudioRef.current;
    if (current && previewPlaying) {
      current.element.pause();
      setPreviewPlaying(false);
      setPreviewPaused(true);
    }
  };

  const handleDownloadClonedVoice = async () => {
    if (selectedParentId == null || selectedStoryId == null || selectedProfileId == null) {
      showError("Validation", "Select a parent, a story, and a voice profile.");
      return;
    }
    try {
      const blob = await api.admin.getLibraryStoryPreviewAudioBlob(
        selectedStoryId,
        previewLang,
        `cloned:${selectedProfileId}`,
        selectedParentId
      );
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `cloned-voice-story-${selectedStoryId}-${previewLang}.mp3`;
      a.click();
      URL.revokeObjectURL(url);
      showSuccess("Download started", "Cloned voice audio saved.");
    } catch (e) {
      showError("Download failed", e instanceof Error ? e.message : "Download failed");
    }
  };

  const handleDeleteNarration = async () => {
    if (selectedParentId == null || selectedStoryId == null || selectedProfileId == null) {
      showError("Validation", "Select a parent, a story, and a voice profile.");
      return;
    }
    if (!confirm("Delete the generated cloned-voice narration for this story? The next time you press Play it will regenerate (may take a minute).")) return;
    setDeleteNarrationLoading(true);
    try {
      await api.admin.deleteLibraryStoryNarrationAudio(
        selectedStoryId,
        previewLang,
        `cloned:${selectedProfileId}`,
        selectedParentId
      );
      stopPreview();
      setNarrationReadyForAvatar(false);
      showSuccess("Narration deleted", "Next Play will regenerate the story in the cloned voice.");
    } catch (e) {
      showError("Delete failed", e instanceof Error ? e.message : "Delete failed");
    } finally {
      setDeleteNarrationLoading(false);
    }
  };

  const handleUploadAvatar = async () => {
    if (selectedParentId == null || selectedAvatarFile == null) {
      showError("Validation", "Select a parent and an image file (JPEG or PNG).");
      return;
    }
    setAvatarUploading(true);
    try {
      const data = await api.admin.uploadAvatarForParent(selectedParentId, selectedAvatarFile);
      setAvatarUrl(data.avatarUrl);
      setSelectedAvatarFile(null);
      showSuccess("Avatar uploaded", "This image will be used for talking-head video. Preview it below.");
    } catch (e) {
      showError("Upload failed", e instanceof Error ? e.message : "Upload failed");
    } finally {
      setAvatarUploading(false);
    }
  };

  const handleDeleteAvatar = async () => {
    if (selectedParentId == null) return;
    if (!confirm("Remove this avatar image? The parent can upload a new one later.")) return;
    setAvatarDeleting(true);
    try {
      await api.admin.deleteAvatarForParent(selectedParentId);
      setAvatarUrl(null);
      setAvatarVideoUrl(null);
      showSuccess("Avatar removed", "Upload a new image to use talking-head video again.");
    } catch (e) {
      showError("Delete failed", e instanceof Error ? e.message : "Delete failed");
    } finally {
      setAvatarDeleting(false);
    }
  };

  const handlePlayWithAvatar = async () => {
    if (selectedParentId == null || selectedStoryId == null || selectedProfileId == null) {
      showError("Validation", "Select a parent, a story, and a cloned voice profile.");
      return;
    }
    setAvatarVideoLoading(true);
    setAvatarVideoUrl(null);
    setAvatarVideoError(null);
    setAvatarVideoSuccess(false);
    try {
      const data: LibraryStoryStreamUrlResponse = await api.admin.getLibraryStoryStreamUrl(
        selectedStoryId,
        previewLang,
        `cloned:${selectedProfileId}`,
        selectedParentId
      );
      const status = data.avatarVideoStatus;
      const errorMsg = data.avatarVideoError;
      const provider = data.avatarVideoProvider ?? null;
      setAvatarVideoProvider(provider);
      if (data?.avatarVideoUrl) {
        setAvatarVideoUrl(data.avatarVideoUrl);
        setAvatarNarrationUrl(typeof data?.streamUrl === "string" ? data.streamUrl : null);
        setAvatarVideoPending(false);
        setAvatarVideoError(null);
        setAvatarVideoSuccess(true);
        showSuccess("Avatar video ready", "Press play on the video below to watch the full lipsync.");
      } else if (status === "FAILED" && errorMsg) {
        setAvatarVideoPending(false);
        setAvatarVideoError(errorMsg);
        showError("Avatar video failed", errorMsg);
      } else {
        setAvatarVideoPending(true);
      }
    } catch (e) {
      setAvatarVideoPending(false);
      showError("Avatar video failed", e instanceof Error ? e.message : "Failed to load avatar video");
    } finally {
      setAvatarVideoLoading(false);
    }
  };

  const handleRegenerateAvatar = async () => {
    if (selectedParentId == null || selectedStoryId == null || selectedProfileId == null) {
      showError("Validation", "Select a parent, a story, and a cloned voice profile.");
      return;
    }
    setAvatarRegenerating(true);
    setAvatarVideoError(null);
    try {
      await api.admin.deleteLibraryStoryAvatarVideo(
        selectedStoryId,
        previewLang,
        `cloned:${selectedProfileId}`,
        selectedParentId
      );
      setAvatarVideoUrl(null);
      setAvatarVideoSuccess(false);
      setAvatarNarrationUrl(null);
      showSuccess("Avatar video removed", "Generating a new one…");
      setAvatarVideoLoading(true);
      const data: LibraryStoryStreamUrlResponse = await api.admin.getLibraryStoryStreamUrl(
        selectedStoryId,
        previewLang,
        `cloned:${selectedProfileId}`,
        selectedParentId
      );
      const provider = data.avatarVideoProvider ?? null;
      setAvatarVideoProvider(provider);
      setAvatarVideoPending(true);
    } catch (e) {
      showError("Regenerate failed", e instanceof Error ? e.message : "Failed to regenerate avatar video");
    } finally {
      setAvatarRegenerating(false);
      setAvatarVideoLoading(false);
    }
  };

  useEffect(() => {
    setAvatarVideoPending(false);
    setAvatarVideoUrl(null);
    setAvatarNarrationUrl(null);
    setAvatarVideoError(null);
    setAvatarVideoSuccess(false);
    setAvatarVideoProvider(null);
    setNarrationReadyForAvatar(false);
    setAvatarRegenerating(false);
    pollAttemptsRef.current = 0;
  }, [selectedParentId, selectedStoryId, selectedProfileId, previewLang]);

  // When story + language + voice are set, check if cloned-voice narration exists and if avatar video already exists (so we show hint + video without clicking "Play with avatar")
  useEffect(() => {
    if (selectedParentId == null || selectedStoryId == null || selectedProfileId == null) {
      setNarrationReadyForAvatar(false);
      return;
    }
    let cancelled = false;
    setNarrationCheckLoading(true);
    api.admin
      .getLibraryStoryStreamUrl(selectedStoryId, previewLang, `cloned:${selectedProfileId}`, selectedParentId)
      .then((data) => {
        if (cancelled) return;
        if (data?.streamUrl) setNarrationReadyForAvatar(true);
        else setNarrationReadyForAvatar(false);
        if (data?.avatarVideoUrl) {
          setAvatarVideoUrl(data.avatarVideoUrl);
          setAvatarNarrationUrl(typeof data.streamUrl === "string" ? data.streamUrl : null);
          setAvatarVideoSuccess(true);
          setAvatarVideoError(null);
          setAvatarVideoProvider(data.avatarVideoProvider ?? null);
        }
      })
      .catch(() => {
        if (!cancelled) setNarrationReadyForAvatar(false);
      })
      .finally(() => {
        if (!cancelled) setNarrationCheckLoading(false);
      });
    return () => { cancelled = true; };
  }, [selectedParentId, selectedStoryId, selectedProfileId, previewLang]);

  // Root cause fix: Replicate SadTalker output has the driven audio embedded in the video with correct lip-sync.
  // When the video has a native audio track, use it (unmuted) so lips and audio stay in sync. Only use separate
  // narration when the video has no audio track (fallback).
  useEffect(() => {
    const video = avatarVideoRef.current;
    const audio = avatarNarrationAudioRef.current;
    if (!video || !audio || !avatarNarrationUrl || videoHasNativeAudio) return;
    const keepVideoMuted = () => {
      if (video && !video.muted) video.muted = true;
    };
    const syncToVideo = () => {
      const t = video.currentTime;
      if (Math.abs(audio.currentTime - t) > 0.03) audio.currentTime = t;
    };
    const onPlay = () => {
      keepVideoMuted();
      audio.currentTime = video.currentTime;
      audio.play().catch(() => {});
    };
    const onPlaying = () => {
      keepVideoMuted();
      audio.currentTime = video.currentTime;
      if (!audio.paused) return;
      audio.play().catch(() => {});
    };
    const onPause = () => audio.pause();
    const onSeeked = () => { audio.currentTime = video.currentTime; };
    video.addEventListener("volumechange", keepVideoMuted);
    video.addEventListener("play", onPlay);
    video.addEventListener("playing", onPlaying);
    video.addEventListener("pause", onPause);
    video.addEventListener("timeupdate", syncToVideo);
    video.addEventListener("seeked", onSeeked);
    keepVideoMuted();
    return () => {
      video.removeEventListener("volumechange", keepVideoMuted);
      video.removeEventListener("play", onPlay);
      video.removeEventListener("playing", onPlaying);
      video.removeEventListener("pause", onPause);
      video.removeEventListener("timeupdate", syncToVideo);
      video.removeEventListener("seeked", onSeeked);
    };
  }, [avatarVideoUrl, avatarNarrationUrl, videoHasNativeAudio]);

  // When avatar video URL changes, assume no native audio until we detect it
  useEffect(() => {
    if (avatarVideoUrl) setVideoHasNativeAudio(false);
  }, [avatarVideoUrl]);

  useEffect(() => {
    if (!avatarVideoPending || selectedParentId == null || selectedStoryId == null || selectedProfileId == null) return;
    pollAttemptsRef.current = 0;
    const poll = async () => {
      pollAttemptsRef.current += 1;
      if (pollAttemptsRef.current > POLL_MAX_ATTEMPTS) {
        setAvatarVideoPending(false);
        setAvatarVideoError("Generation is taking longer than usual (we waited about 10 minutes). You can try again below; the job may still complete in the background.");
        return;
      }
      try {
        const data: LibraryStoryStreamUrlResponse = await api.admin.getLibraryStoryStreamUrl(
          selectedStoryId,
          previewLang,
          `cloned:${selectedProfileId}`,
          selectedParentId
        );
        const status = data.avatarVideoStatus;
        const errorMsg = data.avatarVideoError;
        const provider = data.avatarVideoProvider ?? null;
        setAvatarVideoProvider(provider);
        if (data?.avatarVideoUrl) {
          setAvatarVideoUrl(data.avatarVideoUrl);
          setAvatarNarrationUrl(typeof data?.streamUrl === "string" ? data.streamUrl : null);
          setAvatarVideoPending(false);
          setAvatarVideoError(null);
          setAvatarVideoSuccess(true);
          showSuccess("Avatar video ready", "Press play on the video below to watch the full lipsync.");
        } else if (status === "FAILED" && errorMsg) {
          setAvatarVideoPending(false);
          setAvatarVideoError(errorMsg);
          showError("Avatar video failed", errorMsg);
        }
      } catch {
        // ignore; next poll will retry
      }
    };
    const id = setInterval(poll, POLL_INTERVAL_MS);
    return () => clearInterval(id);
  }, [avatarVideoPending, selectedParentId, selectedStoryId, selectedProfileId, previewLang, showSuccess]);

  useEffect(() => {
    return () => {
      const current = previewAudioRef.current;
      if (current) {
        current.element.pause();
        URL.revokeObjectURL(current.objectUrl);
      }
      const sample = sampleAudioRef.current;
      if (sample) {
        sample.element.pause();
        URL.revokeObjectURL(sample.objectUrl);
      }
    };
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight flex items-center gap-2">
          <FlaskConical className="h-6 w-6" />
          Voice & Avatar Studio
        </h1>
        <p className="text-muted-foreground mt-1">
          Test voice cloning and avatar (talking-head) narration for any parent. Upload voice samples and avatar images, then preview stories. In the app, avatar video is for premium users only.{" "}
          <Link href="/login" className="text-primary underline">Sign in again</Link> if you see &quot;Unauthorized&quot;.
        </p>
      </div>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-base flex items-center gap-2">
            <UserCircle className="h-4 w-4" />
            Select parent
          </CardTitle>
          <p className="text-sm text-muted-foreground">
            All actions below apply to the selected parent.
          </p>
        </CardHeader>
        <CardContent>
          <Select
            value={selectedParentId?.toString() ?? ""}
            onValueChange={(v) => setSelectedParentId(v ? Number(v) : null)}
            disabled={parentsLoading}
          >
            <SelectTrigger className="max-w-md">
              <SelectValue placeholder={parentsLoading ? "Loading parents…" : "Choose a parent"} />
            </SelectTrigger>
            <SelectContent>
              {parents.map((p) => (
                <SelectItem key={p.id} value={p.id.toString()}>
                  {p.email} (ID: {p.id})
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </CardContent>
      </Card>

      {selectedParentId != null && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Volume2 className="h-5 w-5" />
                Voice
              </CardTitle>
              <p className="text-sm text-muted-foreground">
                Upload a voice sample for cloning. This parent can use <code className="rounded bg-muted px-1">cloned:&lt;id&gt;</code> in the app for story narration.
              </p>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Audio file (MP3/WAV, max 10MB)</label>
                <input
                  type="file"
                  accept="audio/*,.mp3,.wav,.m4a"
                  className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm file:border-0 file:bg-transparent file:text-sm file:font-medium"
                  onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)}
                />
              </div>
              <div className="flex flex-wrap gap-2">
                <Button
                  onClick={handleUpload}
                  disabled={uploading || selectedFile == null}
                >
                  <Upload className="mr-2 h-4 w-4" />
                  {uploading ? "Uploading…" : "Upload voice"}
                </Button>
                <Button
                  variant="outline"
                  onClick={loadProfiles}
                  disabled={profilesLoading}
                >
                  <RefreshCw className={`mr-2 h-4 w-4 ${profilesLoading ? "animate-spin" : ""}`} />
                  Refresh list
                </Button>
              </div>
              {profilesLoading ? (
                <p className="text-sm text-muted-foreground">Loading profiles…</p>
              ) : profiles.length === 0 ? (
                <p className="text-sm text-muted-foreground">No voice profiles yet. Upload a sample above.</p>
              ) : (
                <>
                  <p className="text-xs text-muted-foreground">
                    Default: ElevenLabs — upload reference above, then &quot;Run job (ElevenLabs)&quot; (no consent). For Google: upload consent and &quot;Run job (Google)&quot;. Backend: <code className="rounded bg-muted px-1">VOICE_CLONING_PROVIDER=elevenlabs</code> + <code className="rounded bg-muted px-1">ELEVENLABS_API_KEY</code> (or <code className="rounded bg-muted px-1">google</code> + <code className="rounded bg-muted px-1">GOOGLE_CLOUD_TTS_API_KEY</code>).
                  </p>
                  <details className="rounded-md border border-muted bg-muted/30 px-3 py-2 text-xs text-muted-foreground">
                    <summary className="cursor-pointer font-medium text-foreground">How to get the consent audio file (Google only)</summary>
                    <p className="mt-2">
                      The <strong>consent audio</strong> is a short recording of the <strong>same person</strong> whose voice is in the reference, reading the consent sentence aloud. Record in the same quiet environment as the reference (e.g. phone voice memo or computer mic), then export/save as MP3 or WAV and upload under &quot;Consent (Google)&quot;.
                    </p>
                    <p className="mt-1 font-medium">Script to read (English, up to ~10 seconds):</p>
                    <blockquote className="mt-1 rounded bg-muted/50 px-2 py-1 font-mono text-[11px]">
                      I am the owner of this voice and I consent to Google using this voice to create a synthetic voice model.
                    </blockquote>
                    <p className="mt-1">
                      For other languages (e.g. Tamil, Hindi), see{" "}
                      <a href="https://cloud.google.com/text-to-speech/docs/chirp3-instant-custom-voice" target="_blank" rel="noopener noreferrer" className="text-primary underline">
                        Google Cloud: Chirp 3 Instant Custom Voice
                      </a>{" "}
                      (consent statement table).
                    </p>
                  </details>
                  <div className="rounded-md border">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>ID</TableHead>
                          <TableHead>App key</TableHead>
                          <TableHead className="w-[140px]">Actions</TableHead>
                          <TableHead>Voice cloning job</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {profiles.map((p) => (
                          <TableRow key={p.id}>
                            <TableCell>{p.id}</TableCell>
                            <TableCell><code className="text-xs">cloned:{p.id}</code></TableCell>
                            <TableCell>
                              <div className="flex gap-1">
                                <Button
                                  type="button"
                                  variant="outline"
                                  size="sm"
                                  disabled={previewSampleLoading || deletingId === p.id}
                                  onClick={() => handlePreviewClonedVoice(p.id)}
                                >
                                  {previewSampleProfileId === p.id ? "Stop" : "Preview"}
                                </Button>
                                <Button
                                  type="button"
                                  variant="outline"
                                  size="sm"
                                  className="text-destructive hover:bg-destructive/10"
                                  disabled={deletingId === p.id || previewSampleProfileId === p.id}
                                  onClick={() => handleDeleteProfile(p.id)}
                                >
                                  {deletingId === p.id ? "…" : "Delete"}
                                </Button>
                              </div>
                            </TableCell>
                            <TableCell>
                              <div className="flex flex-wrap items-center gap-2">
                                <Button
                                  type="button"
                                  variant="secondary"
                                  size="sm"
                                  disabled={runJobForId === p.id}
                                  onClick={() => handleRunVoiceCloningJob(p.id)}
                                >
                                  {runJobForId === p.id ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                  ) : (
                                    "Run job (ElevenLabs)"
                                  )}
                                </Button>
                                <label className="text-xs text-muted-foreground shrink-0">Consent (Google):</label>
                                <input
                                  id={`consent-file-${p.id}`}
                                  type="file"
                                  accept="audio/*,.mp3,.wav,.m4a"
                                  className="max-w-[120px] text-xs file:mr-1 file:rounded file:border-0 file:bg-primary file:px-2 file:py-1 file:text-xs file:text-primary-foreground"
                                  onChange={(e) => {
                                    const f = e.target.files?.[0] ?? null;
                                    setConsentFileForProfileId((prev) => ({ ...prev, [p.id]: f }));
                                  }}
                                />
                                <Button
                                  type="button"
                                  variant="outline"
                                  size="sm"
                                  disabled={consentUploadingForId === p.id || !consentFileForProfileId[p.id]}
                                  title={!consentFileForProfileId[p.id] ? "Select a consent audio file first" : undefined}
                                  onClick={() => handleUploadConsent(p.id)}
                                >
                                  {consentUploadingForId === p.id ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                  ) : (
                                    "Run job (Google)"
                                  )}
                                </Button>
                              </div>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                </>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <ImagePlus className="h-5 w-5" />
                Avatar
              </CardTitle>
              <p className="text-sm text-muted-foreground">
                Upload a face photo (JPEG/PNG, max 5MB). It is used for talking-head video so the story is &quot;told&quot; by this avatar.
              </p>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex flex-col sm:flex-row gap-4 items-start">
                <div className="rounded-lg border bg-muted/30 flex items-center justify-center w-40 h-40 shrink-0 overflow-hidden">
                  {avatarLoading ? (
                    <span className="text-sm text-muted-foreground">Loading…</span>
                  ) : avatarUrl ? (
                    <img src={avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
                  ) : (
                    <span className="text-sm text-muted-foreground text-center px-2">No avatar</span>
                  )}
                </div>
                <div className="flex-1 space-y-3 min-w-0">
                  <div className="space-y-1">
                    <label className="text-sm font-medium">New image (JPEG or PNG)</label>
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/jpg"
                      className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm file:border-0 file:bg-transparent file:text-sm file:font-medium"
                      onChange={(e) => setSelectedAvatarFile(e.target.files?.[0] ?? null)}
                    />
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Button
                      onClick={handleUploadAvatar}
                      disabled={avatarUploading || selectedAvatarFile == null}
                    >
                      <Upload className="mr-2 h-4 w-4" />
                      {avatarUploading ? "Uploading…" : "Upload avatar"}
                    </Button>
                    <Button
                      variant="outline"
                      className="text-destructive hover:bg-destructive/10"
                      onClick={handleDeleteAvatar}
                      disabled={avatarDeleting || avatarLoading || !avatarUrl}
                    >
                      {avatarDeleting ? "Removing…" : "Remove avatar"}
                    </Button>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {selectedParentId != null && profiles.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Preview story: audio → video</CardTitle>
            <p className="text-sm text-muted-foreground">
              Pick a story and voice, then follow the two steps: first hear the narration (cloned voice), then generate and watch the talking-head video (avatar). Both use the same story, language, and voice.
            </p>
          </CardHeader>
          <CardContent className="space-y-8">
            {/* Shared: story, language, cloned voice (used by both steps) */}
            <div className="rounded-lg border bg-muted/20 p-4 space-y-4">
              <p className="text-sm font-medium">Story & voice (used for Step 1 and Step 2)</p>
              <div className="grid gap-4 sm:grid-cols-3">
                <div className="space-y-2">
                  <label className="text-sm text-muted-foreground">Story</label>
                  <Select
                    value={selectedStoryId?.toString() ?? ""}
                    onValueChange={(v) => setSelectedStoryId(v ? Number(v) : null)}
                    disabled={storiesLoading}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={storiesLoading ? "Loading…" : "Select story"} />
                    </SelectTrigger>
                    <SelectContent>
                      {stories.map((s) => (
                        <SelectItem key={s.id} value={s.id.toString()}>
                          {s.title || `Story ${s.id}`}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <label className="text-sm text-muted-foreground">Language</label>
                  <Select value={previewLang} onValueChange={setPreviewLang}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ta">Tamil (ta)</SelectItem>
                      <SelectItem value="en">English (en)</SelectItem>
                      <SelectItem value="hi">Hindi (hi)</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <label className="text-sm text-muted-foreground">Cloned voice</label>
                  <Select
                    value={selectedProfileId?.toString() ?? ""}
                    onValueChange={(v) => setSelectedProfileId(v ? Number(v) : null)}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select voice" />
                    </SelectTrigger>
                    <SelectContent>
                      {profiles.map((p) => (
                        <SelectItem key={p.id} value={p.id.toString()}>
                          cloned:{p.id}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </div>

            {/* Step 1: Listen — audio only (cloned voice) */}
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary">1</span>
                <div>
                  <h3 className="font-medium flex items-center gap-2">
                    <Volume2 className="h-4 w-4" />
                    Listen to the story (audio)
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    Generate and play the story in the selected language with the parent&apos;s cloned voice. First playback may take about a minute.
                  </p>
                </div>
              </div>
              <div className="flex flex-wrap items-center gap-2 pl-9">
                <Button
                  onClick={handlePlayWithClonedVoice}
                  disabled={
                    previewLoading ||
                    selectedStoryId == null ||
                    selectedProfileId == null ||
                    (previewPlaying && !previewPaused)
                  }
                >
                  <Play className={`mr-2 h-4 w-4 ${previewLoading ? "animate-pulse" : ""}`} />
                  {previewLoading ? "Generating…" : previewPaused ? "Resume" : "Play"}
                </Button>
                <Button variant="outline" onClick={handlePausePreview} disabled={!previewPlaying || previewLoading}>
                  <Pause className="mr-2 h-4 w-4" />
                  Pause
                </Button>
                <Button variant="outline" onClick={stopPreview} disabled={!previewPlaying && !previewPaused}>
                  <Square className="mr-2 h-4 w-4" />
                  Stop
                </Button>
                <Button
                  variant="outline"
                  onClick={handleDownloadClonedVoice}
                  disabled={
                    selectedStoryId == null ||
                    selectedProfileId == null ||
                    selectedParentId == null
                  }
                  title="Download the story narration in this cloned voice as MP3"
                >
                  <Download className="mr-2 h-4 w-4" />
                  Download
                </Button>
                <Button
                  variant="outline"
                  className="text-destructive hover:text-destructive hover:bg-destructive/10"
                  onClick={handleDeleteNarration}
                  disabled={
                    deleteNarrationLoading ||
                    selectedStoryId == null ||
                    selectedProfileId == null ||
                    selectedParentId == null ||
                    !narrationReadyForAvatar
                  }
                  title={
                    !narrationReadyForAvatar
                      ? "No stored narration for this story and voice — play first to generate, then you can delete"
                      : "Remove stored narration so next Play regenerates it"
                  }
                >
                  {deleteNarrationLoading ? (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  ) : (
                    <Trash2 className="mr-2 h-4 w-4" />
                  )}
                  Delete narration
                </Button>
              </div>
            </div>

            {/* Step 2: Watch — avatar video (same story + voice + avatar) */}
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary">2</span>
                <div>
                  <h3 className="font-medium flex items-center gap-2">
                    <Video className="h-4 w-4" />
                    Watch the story (talking-head video)
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    Generate a video where the parent&apos;s avatar &quot;tells&quot; the same story. Uses the same story, language, and cloned voice as Step 1, plus the parent&apos;s uploaded avatar. Takes 1–3 minutes to generate.
                  </p>
                </div>
              </div>
              <div className="pl-9 space-y-4">
                {avatarVideoProvider && (
                  <p className="text-sm text-muted-foreground">
                    Lip-sync API: <strong>{avatarVideoProvider === "heygen" ? "HeyGen (primary)" : avatarVideoProvider?.includes("fallback") ? avatarVideoProvider : avatarVideoProvider === "sadtalker" ? "Replicate (SadTalker)" : avatarVideoProvider === "gooey" ? "Gooey.AI" : avatarVideoProvider === "d-id" ? "D-ID" : avatarVideoProvider}</strong>. {avatarVideoProvider?.includes("fallback") && " Primary is HeyGen; set AVATAR_VIDEO_PROVIDER=heygen and HEYGEN_API_KEY to use it."} API key is sent only from backend.
                  </p>
                )}
                {!avatarVideoProvider && (avatarVideoLoading || avatarVideoPending) && (
                  <p className="text-sm text-amber-600 dark:text-amber-400">
                    No lip-sync provider. Primary is HeyGen: set <code className="rounded bg-muted px-1">AVATAR_VIDEO_ENABLED=true</code>, <code className="rounded bg-muted px-1">AVATAR_VIDEO_PROVIDER=heygen</code>, <code className="rounded bg-muted px-1">HEYGEN_API_KEY</code> in backend <code className="rounded bg-muted px-1">.env</code>. Fallbacks: sadtalker, d-id, gooey (when HeyGen not configured). Restart after changing.
                  </p>
                )}
                <div className="flex flex-wrap items-center gap-2">
                <Button
                onClick={handlePlayWithAvatar}
                disabled={
                  avatarVideoLoading ||
                  avatarVideoPending ||
                  selectedStoryId == null ||
                  selectedProfileId == null ||
                  !narrationReadyForAvatar ||
                  !!avatarVideoUrl
                }
              >
                {avatarVideoLoading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Requesting…
                  </>
                ) : avatarVideoPending ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Generating…
                  </>
                ) : (
                  <>
                    <Video className="mr-2 h-4 w-4" />
                    Play with avatar
                  </>
                )}
                </Button>
                </div>
                    {avatarVideoSuccess && avatarVideoUrl && (
                  <div className="flex items-start gap-3 rounded-lg border border-green-200 bg-green-50 dark:border-green-800 dark:bg-green-950/40 p-4">
                    <p className="text-sm font-medium text-green-800 dark:text-green-200">Avatar video is ready</p>
                    <p className="text-sm text-green-700 dark:text-green-300">Press play on the video below to watch the full lipsync.</p>
                  </div>
                )}
                {avatarVideoError && (
                  <div className="flex flex-col gap-3 rounded-lg border border-destructive/50 bg-destructive/10 p-4">
                    <div>
                      <p className="text-sm font-medium text-destructive">Avatar video failed</p>
                      <p className="text-sm text-muted-foreground whitespace-pre-wrap mt-1">{avatarVideoError}</p>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      You can try again; the previous attempt will be cleared and a new generation will start.
                    </p>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={handleRegenerateAvatar}
                      disabled={avatarRegenerating || avatarVideoLoading || avatarVideoPending}
                    >
                      {avatarRegenerating || avatarVideoLoading ? (
                        <>
                          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          Starting…
                        </>
                      ) : (
                        <>
                          <RefreshCw className="mr-2 h-4 w-4" />
                          Try again
                        </>
                      )}
                    </Button>
                  </div>
                )}
                {avatarVideoPending && !avatarVideoUrl && (
                  <div className="flex items-start gap-3 rounded-lg border border-amber-200 bg-amber-50 dark:border-amber-800 dark:bg-amber-950/40 p-4">
                    <Loader2 className="h-5 w-5 shrink-0 animate-spin text-amber-600 dark:text-amber-400 mt-0.5" />
                    <div className="text-sm">
                      <p className="font-medium text-amber-800 dark:text-amber-200">Generating talking-head video</p>
                      <p className="text-amber-700 dark:text-amber-300 mt-1">
                        Usually takes 1–3 minutes. The video will appear here when ready.
                      </p>
                    </div>
                  </div>
                )}
                {!avatarVideoUrl && !avatarVideoLoading && !avatarVideoPending && !avatarVideoError && (
                  <p className="text-sm text-muted-foreground">
                    {narrationReadyForAvatar
                      ? <>Click <strong>Play with avatar</strong> to generate. The parent must have an avatar image uploaded above.</>
                      : "Listen to the story first (Step 1) to generate the cloned-voice narration, or wait a moment if we’re still checking. Then you can generate the avatar video."}
                    {!narrationReadyForAvatar && narrationCheckLoading && <span className="text-muted-foreground"> (checking…)</span>}
                  </p>
                )}
                {avatarVideoUrl && (
                  <div className="space-y-2">
                    {narrationReadyForAvatar && (
                      <div className="rounded-lg border border-blue-200 bg-blue-50 dark:border-blue-800 dark:bg-blue-950/40 px-3 py-2">
                        <p className="text-sm text-blue-800 dark:text-blue-200">
                          This story is already narrated with the selected cloned voice and has an avatar video. Play it below or use <strong>Regenerate avatar</strong> to create a new one.
                        </p>
                      </div>
                    )}
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-sm text-muted-foreground">
                        Avatar video for this story and voice. Play below.
                      </p>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={handleRegenerateAvatar}
                        disabled={avatarRegenerating || avatarVideoLoading || avatarVideoPending}
                      >
                        {avatarRegenerating ? (
                          <>
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                            Regenerating…
                          </>
                        ) : (
                          <>
                            <RefreshCw className="mr-2 h-4 w-4" />
                            Regenerate avatar
                          </>
                        )}
                      </Button>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      Audio: {previewLang === "ta" ? "Tamil" : previewLang}.
                      {videoHasNativeAudio
                        ? " Using the video’s built-in audio (best lip-sync)."
                        : " Narration only; video is muted so one voice plays."}
                    </p>
                {avatarNarrationUrl && !videoHasNativeAudio && (
                  <audio ref={avatarNarrationAudioRef} src={avatarNarrationUrl} preload="auto" className="hidden" />
                )}
                <div className="rounded-lg border bg-muted/30 overflow-hidden relative group">
                  <video
                    ref={avatarVideoRef}
                    src={avatarVideoUrl}
                    controls
                    muted={!!avatarNarrationUrl && !videoHasNativeAudio}
                    className={`w-full max-w-4xl aspect-video bg-black ${avatarNarrationUrl && !videoHasNativeAudio ? "avatar-video-narration-only" : ""}`}
                    playsInline
                    preload="auto"
                    onLoadedMetadata={(e) => {
                      const v = e.currentTarget as HTMLVideoElement & { audioTracks?: { length: number } };
                      const hasAudio = typeof v.audioTracks !== "undefined" && v.audioTracks.length > 0;
                      setVideoHasNativeAudio(!!hasAudio);
                    }}
                  >
                    Your browser does not support the video tag.
                  </video>
                  <div className="absolute top-2 right-2 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      className="bg-black/60 hover:bg-black/80"
                      onClick={() => avatarVideoRef.current?.requestFullscreen?.()}
                    >
                      <Maximize2 className="h-4 w-4 mr-1" />
                      Fullscreen
                    </Button>
                    <a
                      href={avatarVideoUrl}
                      download="avatar-lipsync.mp4"
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center justify-center rounded-md text-sm font-medium bg-black/60 hover:bg-black/80 text-white h-8 px-3"
                    >
                      <Download className="h-4 w-4 mr-1" />
                      Download
                    </a>
                  </div>
                  </div>
                </div>
                )}
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
