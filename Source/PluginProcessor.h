/*
  ==============================================================================

    PluginProcessor.h
    Created: 2025-11-01
    Author: AI Chord Generator

    Main audio processor class for the VST3 plugin.
    Manages chord progression state and MIDI output generation.

  ==============================================================================
*/

#pragma once

#include <JuceHeader.h>
#include "ChordParser.h"
#include "MidiGenerator.h"
#include <vector>

/**
 * Main audio processor for AI Chord Generator plugin.
 * Generates MIDI events from parsed chord progressions.
 */
class ChordGeneratorProcessor : public juce::AudioProcessor
{
public:
    //==============================================================================
    ChordGeneratorProcessor();
    ~ChordGeneratorProcessor() override;

    //==============================================================================
    // AudioProcessor interface implementation
    void prepareToPlay (double sampleRate, int samplesPerBlock) override;
    void releaseResources() override;

    bool isBusesLayoutSupported (const BusesLayout& layouts) const override;

    void processBlock (juce::AudioBuffer<float>&, juce::MidiBuffer&) override;

    //==============================================================================
    juce::AudioProcessorEditor* createEditor() override;
    bool hasEditor() const override;

    //==============================================================================
    const juce::String getName() const override;

    bool acceptsMidi() const override;
    bool producesMidi() const override;
    bool isMidiEffect() const override;
    double getTailLengthSeconds() const override;

    //==============================================================================
    int getNumPrograms() override;
    int getCurrentProgram() override;
    void setCurrentProgram (int index) override;
    const juce::String getProgramName (int index) override;
    void changeProgramName (int index, const juce::String& newName) override;

    //==============================================================================
    void getStateInformation (juce::MemoryBlock& destData) override;
    void setStateInformation (const void* data, int sizeInBytes) override;

    //==============================================================================
    // Plugin-specific interface

    /**
     * Update the chord progression to be generated.
     * Called by editor when user pastes new text.
     */
    void updateChordProgression(const std::vector<Chord>& chords);

    /**
     * Trigger MIDI generation for current chord progression.
     * Called by editor when user clicks "Generate MIDI" button.
     */
    void triggerMidiGeneration();

    /**
     * Get current chord progression.
     */
    const std::vector<Chord>& getChordProgression() const { return chordProgression; }

    /**
     * Check if MIDI generation is currently active.
     */
    bool isGenerating() const { return generationActive; }

    //==============================================================================
    // Parameter access methods

    int getOctave() const { return octave; }
    void setOctave(int value) { octave = juce::jlimit(0, 8, value); }

    double getDuration() const { return duration; }
    void setDuration(double value) { duration = juce::jlimit(0.25, 16.0, value); }

    int getVelocity() const { return velocity; }
    void setVelocity(int value) { velocity = juce::jlimit(1, 127, value); }

    PlayStyle getPlayStyle() const { return playStyle; }
    void setPlayStyle(PlayStyle style) { playStyle = style; }

    ArpSpeed getArpSpeed() const { return arpSpeed; }
    void setArpSpeed(ArpSpeed speed) { arpSpeed = speed; }

private:
    //==============================================================================
    // Plugin parameters
    int octave = 4;                     // Base octave (0-8)
    double duration = 1.0;              // Chord duration in bars
    int velocity = 100;                 // MIDI velocity (1-127)
    PlayStyle playStyle = PlayStyle::BlockChord;
    ArpSpeed arpSpeed = ArpSpeed::Sixteenth;

    // Chord progression state
    std::vector<Chord> chordProgression;

    // Generation state
    bool generationActive = false;
    int currentChordIndex = 0;
    int samplesUntilNextChord = 0;
    double currentSampleRate = 44100.0;
    int currentSamplesPerBlock = 512;

    // Buffered MIDI messages waiting to be output
    std::vector<juce::MidiMessage> pendingMidiMessages;
    int pendingMessageStartSample = 0;

    //==============================================================================
    JUCE_DECLARE_NON_COPYABLE_WITH_LEAK_DETECTOR (ChordGeneratorProcessor)
};
