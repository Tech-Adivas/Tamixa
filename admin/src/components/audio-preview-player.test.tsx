import { render, screen } from "@testing-library/react";
import { AudioPreviewPlayer } from "./audio-preview-player";

describe("AudioPreviewPlayer", () => {
  it("renders without crashing", () => {
    const mockBlob = new Blob(["test audio"], { type: "audio/mp3" });
    
    render(
      <AudioPreviewPlayer
        audioSource={mockBlob}
        language="ta"
        languageLabel="Tamil"
      />
    );

    expect(screen.getByText("Tamil")).toBeInTheDocument();
    expect(screen.getByText("Language: ta")).toBeInTheDocument();
  });

  it("displays audio metadata when provided", () => {
    const mockBlob = new Blob(["test audio"], { type: "audio/mp3" });
    
    render(
      <AudioPreviewPlayer
        audioSource={mockBlob}
        language="en"
        languageLabel="English"
        duration={180}
        fileSize={2048000}
        generatedAt="2025-01-15 10:30 AM IST"
      />
    );

    expect(screen.getByText(/Duration:/)).toBeInTheDocument();
    expect(screen.getByText(/Size:/)).toBeInTheDocument();
    expect(screen.getByText(/Generated:/)).toBeInTheDocument();
  });

  it("displays loading state initially", () => {
    const mockBlob = new Blob(["test audio"], { type: "audio/mp3" });
    
    render(
      <AudioPreviewPlayer
        audioSource={mockBlob}
        language="hi"
        languageLabel="Hindi"
      />
    );

    expect(screen.getByText("Loading audio...")).toBeInTheDocument();
  });
});
