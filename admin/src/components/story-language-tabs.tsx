"use client";

import * as React from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Globe, CheckCircle2 } from "lucide-react";
import { cn } from "@/lib/utils";
import type { LibraryTranslationTabFields } from "@/lib/library-story-admin-constants";
import { LIBRARY_TAB_LANGUAGES } from "@/lib/library-story-admin-constants";

export interface StoryLanguageTabsProps {
  /** Current source/master language code */
  sourceLanguage: string;
  /** Translation entries for non-master languages */
  translationEntries: Record<string, LibraryTranslationTabFields>;
  /** Callback when translation content changes */
  onTranslationChange: (language: string, field: keyof LibraryTranslationTabFields, value: string) => void;
  /** Optional: show which languages have been translated */
  showCompletionStatus?: boolean;
  /** Optional: disable editing */
  disabled?: boolean;
  /** Optional: className for the container */
  className?: string;
}

/**
 * Language tabs component for editing story content across multiple languages.
 * Displays tabs for all 6 supported languages (Tamil, English, Hindi, Telugu, Kannada, Malayalam).
 * The source language is marked as "Master" and other languages show translation fields.
 */
export function StoryLanguageTabs({
  sourceLanguage,
  translationEntries,
  onTranslationChange,
  showCompletionStatus = true,
  disabled = false,
  className,
}: StoryLanguageTabsProps) {
  const sourceLangCode = sourceLanguage.toLowerCase();
  
  // Filter out the source language from tabs (it's edited in the main form)
  const translationLanguages = LIBRARY_TAB_LANGUAGES.filter(
    (lang) => lang.code !== sourceLangCode
  );

  // Check if a language has content
  const hasContent = (langCode: string): boolean => {
    const entry = translationEntries[langCode];
    return !!(entry?.content?.trim() || entry?.title?.trim() || entry?.moral?.trim());
  };

  return (
    <Card className={cn("w-full", className)}>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Globe className="h-5 w-5" />
          Language Translations
        </CardTitle>
        <p className="text-sm text-muted-foreground">
          Edit story content for each language. The master language ({sourceLangCode.toUpperCase()}) is edited in the main form above.
        </p>
      </CardHeader>
      <CardContent>
        <Tabs defaultValue={translationLanguages[0]?.code ?? "en"} className="w-full">
          <TabsList className="grid w-full grid-cols-5 lg:grid-cols-5">
            {translationLanguages.map((lang) => (
              <TabsTrigger
                key={lang.code}
                value={lang.code}
                className="relative"
                disabled={disabled}
              >
                <span>{lang.label}</span>
                {showCompletionStatus && hasContent(lang.code) && (
                  <CheckCircle2 className="ml-1 h-3 w-3 text-emerald-600" />
                )}
              </TabsTrigger>
            ))}
          </TabsList>

          {translationLanguages.map((lang) => {
            const entry = translationEntries[lang.code] || {
              content: "",
              title: "",
              moral: "",
              postStoryMission: "",
              postStoryResourceUrl: "",
            };

            return (
              <TabsContent key={lang.code} value={lang.code} className="space-y-4 mt-4">
                <div className="space-y-4">
                  {/* Title */}
                  <div className="space-y-2">
                    <Label htmlFor={`title-${lang.code}`}>
                      Title ({lang.label})
                    </Label>
                    <Input
                      id={`title-${lang.code}`}
                      value={entry.title}
                      onChange={(e) => onTranslationChange(lang.code, "title", e.target.value)}
                      placeholder={`Story title in ${lang.label}...`}
                      disabled={disabled}
                      maxLength={255}
                    />
                  </div>

                  {/* Content */}
                  <div className="space-y-2">
                    <Label htmlFor={`content-${lang.code}`}>
                      Story Content ({lang.label})
                    </Label>
                    <textarea
                      id={`content-${lang.code}`}
                      value={entry.content}
                      onChange={(e) => onTranslationChange(lang.code, "content", e.target.value)}
                      placeholder={`Story content in ${lang.label}...`}
                      disabled={disabled}
                      className="flex min-h-[200px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                      rows={10}
                    />
                    <p className="text-xs text-muted-foreground">
                      {entry.content.length} characters
                    </p>
                  </div>

                  {/* Moral */}
                  <div className="space-y-2">
                    <Label htmlFor={`moral-${lang.code}`}>
                      Moral ({lang.label})
                    </Label>
                    <Input
                      id={`moral-${lang.code}`}
                      value={entry.moral}
                      onChange={(e) => onTranslationChange(lang.code, "moral", e.target.value)}
                      placeholder={`Moral of the story in ${lang.label}...`}
                      disabled={disabled}
                    />
                  </div>

                  {/* Post Story Mission */}
                  <div className="space-y-2">
                    <Label htmlFor={`mission-${lang.code}`}>
                      Post-Story Mission ({lang.label})
                      <span className="ml-2 text-xs text-muted-foreground">Optional</span>
                    </Label>
                    <textarea
                      id={`mission-${lang.code}`}
                      value={entry.postStoryMission}
                      onChange={(e) => onTranslationChange(lang.code, "postStoryMission", e.target.value)}
                      placeholder={`Post-story mission text in ${lang.label}...`}
                      disabled={disabled}
                      className="flex min-h-[100px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                      rows={4}
                      maxLength={8000}
                    />
                  </div>

                  {/* Post Story Resource URL */}
                  <div className="space-y-2">
                    <Label htmlFor={`resource-${lang.code}`}>
                      Post-Story Resource URL ({lang.label})
                      <span className="ml-2 text-xs text-muted-foreground">Optional</span>
                    </Label>
                    <Input
                      id={`resource-${lang.code}`}
                      value={entry.postStoryResourceUrl}
                      onChange={(e) => onTranslationChange(lang.code, "postStoryResourceUrl", e.target.value)}
                      placeholder={`Resource URL in ${lang.label}...`}
                      disabled={disabled}
                      maxLength={512}
                      type="url"
                    />
                  </div>

                  {/* Status indicator */}
                  {showCompletionStatus && (
                    <div className="pt-2">
                      {hasContent(lang.code) ? (
                        <Badge variant="outline" className="border-emerald-500/60 bg-emerald-500/15 text-emerald-700">
                          <CheckCircle2 className="mr-1 h-3 w-3" />
                          Content provided
                        </Badge>
                      ) : (
                        <Badge variant="outline" className="border-amber-500/60 bg-amber-500/15 text-amber-700">
                          No content yet
                        </Badge>
                      )}
                    </div>
                  )}
                </div>
              </TabsContent>
            );
          })}
        </Tabs>
      </CardContent>
    </Card>
  );
}
