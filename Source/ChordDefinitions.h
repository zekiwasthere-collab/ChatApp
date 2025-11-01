/*
  ==============================================================================

    ChordDefinitions.h
    Created: 2025-11-01
    Author: AI Chord Generator

    This file contains static chord interval mappings and music theory constants.
    All intervals are in semitones from the root note.

  ==============================================================================
*/

#pragma once

#include <JuceHeader.h>
#include <map>
#include <vector>

namespace ChordDefinitions
{
    // MIDI note number for middle C (C4)
    const int MIDDLE_C = 60;

    // Chromatic scale mapping: note name to semitone offset from C
    const std::map<juce::String, int> NOTE_OFFSETS = {
        {"C", 0},
        {"C#", 1}, {"Db", 1},
        {"D", 2},
        {"D#", 3}, {"Eb", 3},
        {"E", 4},
        {"F", 5},
        {"F#", 6}, {"Gb", 6},
        {"G", 7},
        {"G#", 8}, {"Ab", 8},
        {"A", 9},
        {"A#", 10}, {"Bb", 10},
        {"B", 11}
    };

    // Chord type to interval mappings (semitones from root)
    // Based on planning.md MIDI Generation Specification
    const std::map<juce::String, std::vector<int>> CHORD_INTERVALS = {
        // Basic triads
        {"major", {0, 4, 7}},
        {"minor", {0, 3, 7}},
        {"dim", {0, 3, 6}},
        {"diminished", {0, 3, 6}},
        {"aug", {0, 4, 8}},
        {"augmented", {0, 4, 8}},

        // Suspended chords
        {"sus2", {0, 2, 7}},
        {"sus4", {0, 5, 7}},

        // Seventh chords
        {"maj7", {0, 4, 7, 11}},
        {"min7", {0, 3, 7, 10}},
        {"m7", {0, 3, 7, 10}},
        {"7", {0, 4, 7, 10}},           // Dominant 7th
        {"dom7", {0, 4, 7, 10}},
        {"dim7", {0, 3, 6, 9}},
        {"min7b5", {0, 3, 6, 10}},      // Half-diminished
        {"m7b5", {0, 3, 6, 10}},

        // Ninth chords
        {"maj9", {0, 4, 7, 11, 14}},
        {"min9", {0, 3, 7, 10, 14}},
        {"m9", {0, 3, 7, 10, 14}},
        {"9", {0, 4, 7, 10, 14}},       // Dominant 9th
        {"dom9", {0, 4, 7, 10, 14}},

        // Eleventh chords
        {"maj11", {0, 4, 7, 11, 14, 17}},
        {"11", {0, 4, 7, 10, 14, 17}},

        // Thirteenth chords
        {"maj13", {0, 4, 7, 11, 14, 17, 21}},
        {"13", {0, 4, 7, 10, 14, 17, 21}},

        // Added tone chords
        {"add9", {0, 4, 7, 14}},
        {"add11", {0, 4, 7, 17}},

        // Altered chords
        {"7#5", {0, 4, 8, 10}},
        {"7b9", {0, 4, 7, 10, 13}},
        {"7#9", {0, 4, 7, 10, 15}},
        {"7b5", {0, 4, 6, 10}}
    };

    /**
     * Get intervals for a chord quality string.
     * Returns major triad as fallback if quality not found.
     */
    inline std::vector<int> getIntervalsForQuality(const juce::String& quality)
    {
        auto it = CHORD_INTERVALS.find(quality);
        if (it != CHORD_INTERVALS.end())
            return it->second;

        // Fallback to major triad
        DBG("Unknown chord quality: " + quality + ", using major triad");
        return {0, 4, 7};
    }

    /**
     * Convert note name (e.g., "C", "F#", "Bb") to MIDI note number in specified octave.
     * Returns -1 if note name is invalid.
     */
    inline int noteNameToMidiNumber(const juce::String& noteName, int octave)
    {
        auto it = NOTE_OFFSETS.find(noteName);
        if (it == NOTE_OFFSETS.end())
            return -1;

        int midiNote = (octave + 1) * 12 + it->second;

        // Clamp to valid MIDI range (0-127)
        return juce::jlimit(0, 127, midiNote);
    }

    /**
     * Check if a note name is valid.
     */
    inline bool isValidNoteName(const juce::String& noteName)
    {
        return NOTE_OFFSETS.find(noteName) != NOTE_OFFSETS.end();
    }
}
