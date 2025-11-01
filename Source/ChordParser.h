/*
  ==============================================================================

    ChordParser.h
    Created: 2025-11-01
    Author: AI Chord Generator

    Parses chord names from natural language text input.
    Supports various chord types, slash chords, and filters filler words.

  ==============================================================================
*/

#pragma once

#include <JuceHeader.h>
#include <vector>

/**
 * Structure representing a parsed chord.
 */
struct Chord
{
    juce::String rootNote;      // Root note: "C", "D#", "Bb", etc.
    juce::String quality;       // Chord quality: "major", "minor", "maj7", etc.
    juce::String extensions;    // Extensions/alterations: "7", "9", "b9", etc.
    juce::String bassNote;      // Bass note for slash chords (empty if not slash chord)
    juce::String originalName;  // Full chord name as parsed (e.g., "Cmaj7/E")

    Chord() = default;

    Chord(const juce::String& root, const juce::String& qual,
          const juce::String& ext = "", const juce::String& bass = "")
        : rootNote(root), quality(qual), extensions(ext), bassNote(bass)
    {
        // Build original name
        originalName = rootNote + quality + extensions;
        if (bassNote.isNotEmpty())
            originalName += "/" + bassNote;
    }

    // For display purposes
    juce::String getDisplayName() const
    {
        return originalName.isNotEmpty() ? originalName : (rootNote + quality + extensions);
    }
};

/**
 * Static utility class for parsing chord progressions from text.
 */
class ChordParser
{
public:
    /**
     * Parse a chord progression from natural language text.
     * Extracts chord names and filters out filler words.
     *
     * @param text Input text potentially containing chord names
     * @return Vector of parsed Chord objects
     */
    static std::vector<Chord> parseChordProgression(const juce::String& text);

private:
    /**
     * Check if a token matches a chord pattern.
     */
    static bool isChordPattern(const juce::String& token);

    /**
     * Parse a single chord name token into a Chord object.
     */
    static Chord parseChordName(const juce::String& token);

    /**
     * Check if a token is a filler word to be ignored.
     */
    static bool isFillerWord(const juce::String& token);

    /**
     * Extract root note from chord string.
     * Returns root note and remaining string.
     */
    static std::pair<juce::String, juce::String> extractRootNote(const juce::String& chordStr);

    /**
     * Extract quality and extensions from chord string (after root note removed).
     */
    static std::pair<juce::String, juce::String> extractQualityAndExtensions(const juce::String& remaining);
};
