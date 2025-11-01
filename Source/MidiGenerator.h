/*
  ==============================================================================

    MidiGenerator.h
    Created: 2025-11-01
    Author: AI Chord Generator

    Generates MIDI note events from parsed chord objects.
    Supports block chords and arpeggios with configurable settings.

  ==============================================================================
*/

#pragma once

#include <JuceHeader.h>
#include "ChordParser.h"
#include <vector>

/**
 * Play style for chord generation.
 */
enum class PlayStyle
{
    BlockChord,     // All notes play simultaneously
    Arpeggio        // Notes play in ascending sequence
};

/**
 * Arpeggio speed (note duration between arpeggio notes).
 */
enum class ArpSpeed
{
    ThirtySecond,   // 1/32 note
    Sixteenth,      // 1/16 note
    Eighth,         // 1/8 note
    Quarter         // 1/4 note
};

/**
 * Static utility class for generating MIDI events from chords.
 */
class MidiGenerator
{
public:
    /**
     * Generate MIDI messages for a single chord.
     *
     * @param chord Chord object to generate MIDI for
     * @param octave Base octave (0-8)
     * @param durationInBeats Chord duration in beats (quarter notes)
     * @param velocity MIDI velocity (1-127)
     * @param style Play style (block chord or arpeggio)
     * @param arpSpeed Arpeggio speed (only used if style is Arpeggio)
     * @param sampleRate Audio sample rate
     * @param samplesPerBeat Samples per beat (quarter note)
     * @param startSample Sample offset to start generating from
     * @return Vector of MIDI messages with sample-accurate timing
     */
    static std::vector<juce::MidiMessage> generateMidiForChord(
        const Chord& chord,
        int octave,
        double durationInBeats,
        int velocity,
        PlayStyle style,
        ArpSpeed arpSpeed,
        double sampleRate,
        int samplesPerBeat,
        int startSample
    );

    /**
     * Get interval pattern (semitones from root) for a chord.
     * Uses ChordDefinitions mappings.
     *
     * @param chord Chord object
     * @return Vector of intervals in semitones
     */
    static std::vector<int> getIntervalsForChord(const Chord& chord);

    /**
     * Convert root note name and octave to MIDI note number.
     *
     * @param rootNote Note name (e.g., "C", "F#", "Bb")
     * @param octave Octave number (0-8)
     * @return MIDI note number (0-127), or -1 if invalid
     */
    static int rootNoteToMidiNumber(const juce::String& rootNote, int octave);

    /**
     * Get samples per note for arpeggio speed.
     *
     * @param arpSpeed Arpeggio speed enum
     * @param samplesPerBeat Samples per quarter note
     * @return Number of samples for each arpeggio note
     */
    static int getSamplesPerArpNote(ArpSpeed arpSpeed, int samplesPerBeat);

private:
    /**
     * Generate block chord MIDI events (all notes at once).
     */
    static std::vector<juce::MidiMessage> generateBlockChord(
        const std::vector<int>& midiNotes,
        int velocity,
        int startSample,
        int durationInSamples
    );

    /**
     * Generate arpeggio MIDI events (notes in sequence).
     */
    static std::vector<juce::MidiMessage> generateArpeggio(
        const std::vector<int>& midiNotes,
        int velocity,
        int startSample,
        int totalDurationInSamples,
        int samplesPerNote
    );
};
