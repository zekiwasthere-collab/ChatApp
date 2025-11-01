/*
  ==============================================================================

    MidiGenerator.cpp
    Created: 2025-11-01
    Author: AI Chord Generator

    Implementation of MIDI generation logic.

  ==============================================================================
*/

#include "MidiGenerator.h"
#include "ChordDefinitions.h"

std::vector<juce::MidiMessage> MidiGenerator::generateMidiForChord(
    const Chord& chord,
    int octave,
    double durationInBeats,
    int velocity,
    PlayStyle style,
    ArpSpeed arpSpeed,
    double sampleRate,
    int samplesPerBeat,
    int startSample)
{
    std::vector<juce::MidiMessage> messages;

    // Validate inputs
    if (durationInBeats <= 0.0)
        durationInBeats = 0.25; // Minimum duration

    if (velocity <= 0)
        velocity = 1; // Minimum velocity

    velocity = juce::jlimit(1, 127, velocity);

    // Get root note MIDI number
    int rootMidiNote = rootNoteToMidiNumber(chord.rootNote, octave);
    if (rootMidiNote < 0)
    {
        DBG("Invalid root note: " + chord.rootNote);
        return messages; // Empty vector
    }

    // Get intervals for this chord
    std::vector<int> intervals = getIntervalsForChord(chord);

    // Convert intervals to MIDI note numbers
    std::vector<int> midiNotes;

    // Add bass note if slash chord (one octave below root)
    if (chord.bassNote.isNotEmpty())
    {
        int bassMidiNote = rootNoteToMidiNumber(chord.bassNote, octave - 1);
        if (bassMidiNote >= 0)
            midiNotes.push_back(bassMidiNote);
    }

    // Add chord notes
    for (int interval : intervals)
    {
        int midiNote = rootMidiNote + interval;
        // Clamp to valid MIDI range
        midiNote = juce::jlimit(0, 127, midiNote);
        midiNotes.push_back(midiNote);
    }

    // Calculate duration in samples
    int durationInSamples = static_cast<int>(durationInBeats * samplesPerBeat);

    // Generate MIDI based on style
    if (style == PlayStyle::BlockChord)
    {
        messages = generateBlockChord(midiNotes, velocity, startSample, durationInSamples);
    }
    else // Arpeggio
    {
        int samplesPerNote = getSamplesPerArpNote(arpSpeed, samplesPerBeat);
        messages = generateArpeggio(midiNotes, velocity, startSample, durationInSamples, samplesPerNote);
    }

    return messages;
}

std::vector<int> MidiGenerator::getIntervalsForChord(const Chord& chord)
{
    return ChordDefinitions::getIntervalsForQuality(chord.quality);
}

int MidiGenerator::rootNoteToMidiNumber(const juce::String& rootNote, int octave)
{
    return ChordDefinitions::noteNameToMidiNumber(rootNote, octave);
}

int MidiGenerator::getSamplesPerArpNote(ArpSpeed arpSpeed, int samplesPerBeat)
{
    switch (arpSpeed)
    {
        case ArpSpeed::ThirtySecond:
            return samplesPerBeat / 8;  // 1/32 note = 1/8 of a quarter note

        case ArpSpeed::Sixteenth:
            return samplesPerBeat / 4;  // 1/16 note = 1/4 of a quarter note

        case ArpSpeed::Eighth:
            return samplesPerBeat / 2;  // 1/8 note = 1/2 of a quarter note

        case ArpSpeed::Quarter:
            return samplesPerBeat;      // 1/4 note = quarter note

        default:
            return samplesPerBeat / 4;  // Default to 16th note
    }
}

std::vector<juce::MidiMessage> MidiGenerator::generateBlockChord(
    const std::vector<int>& midiNotes,
    int velocity,
    int startSample,
    int durationInSamples)
{
    std::vector<juce::MidiMessage> messages;

    if (midiNotes.empty())
        return messages;

    // Add note-on messages for all notes at start time
    for (int midiNote : midiNotes)
    {
        juce::MidiMessage noteOn = juce::MidiMessage::noteOn(1, midiNote, (juce::uint8)velocity);
        noteOn.setTimeStamp(startSample);
        messages.push_back(noteOn);
    }

    // Add note-off messages for all notes at end time
    int endSample = startSample + durationInSamples;
    for (int midiNote : midiNotes)
    {
        juce::MidiMessage noteOff = juce::MidiMessage::noteOff(1, midiNote, (juce::uint8)0);
        noteOff.setTimeStamp(endSample);
        messages.push_back(noteOff);
    }

    return messages;
}

std::vector<juce::MidiMessage> MidiGenerator::generateArpeggio(
    const std::vector<int>& midiNotes,
    int velocity,
    int startSample,
    int totalDurationInSamples,
    int samplesPerNote)
{
    std::vector<juce::MidiMessage> messages;

    if (midiNotes.empty() || samplesPerNote <= 0)
        return messages;

    int currentSample = startSample;
    int endSample = startSample + totalDurationInSamples;

    // Loop through arpeggio pattern until duration is filled
    while (currentSample < endSample)
    {
        for (size_t i = 0; i < midiNotes.size(); ++i)
        {
            int midiNote = midiNotes[i];

            // Note on at current position
            juce::MidiMessage noteOn = juce::MidiMessage::noteOn(1, midiNote, (juce::uint8)velocity);
            noteOn.setTimeStamp(currentSample);
            messages.push_back(noteOn);

            // Note off after note duration (staccato feel)
            int noteOffSample = currentSample + samplesPerNote;
            // Don't extend notes beyond total duration
            if (noteOffSample > endSample)
                noteOffSample = endSample;

            juce::MidiMessage noteOff = juce::MidiMessage::noteOff(1, midiNote, (juce::uint8)0);
            noteOff.setTimeStamp(noteOffSample);
            messages.push_back(noteOff);

            // Advance to next note position
            currentSample += samplesPerNote;

            // Stop if we've exceeded the duration
            if (currentSample >= endSample)
                break;
        }
    }

    return messages;
}
