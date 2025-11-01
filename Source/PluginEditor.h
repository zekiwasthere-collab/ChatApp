/*
  ==============================================================================

    PluginEditor.h
    Created: 2025-11-01
    Author: AI Chord Generator

    GUI editor interface for the plugin.
    Displays text input, chord display, settings, and generate button.

  ==============================================================================
*/

#pragma once

#include <JuceHeader.h>
#include "PluginProcessor.h"

/**
 * GUI editor for AI Chord Generator plugin.
 */
class ChordGeneratorEditor : public juce::AudioProcessorEditor,
                              public juce::TextEditor::Listener,
                              public juce::Slider::Listener,
                              public juce::ComboBox::Listener,
                              public juce::Button::Listener,
                              public juce::Timer
{
public:
    ChordGeneratorEditor (ChordGeneratorProcessor&);
    ~ChordGeneratorEditor() override;

    //==============================================================================
    void paint (juce::Graphics&) override;
    void resized() override;

    //==============================================================================
    // Listener callbacks
    void textEditorTextChanged (juce::TextEditor&) override;
    void sliderValueChanged (juce::Slider* slider) override;
    void comboBoxChanged (juce::ComboBox* comboBox) override;
    void buttonClicked (juce::Button* button) override;

    //==============================================================================
    // Timer callback for debounced parsing
    void timerCallback() override;

private:
    // Reference to processor
    ChordGeneratorProcessor& audioProcessor;

    //==============================================================================
    // UI Components

    // Header
    juce::Label headerLabel;

    // Text input area
    juce::Label inputLabel;
    juce::TextEditor textInput;

    // Chord display
    juce::Label chordDisplayLabel;
    juce::Label chordDisplay;

    // Settings controls
    juce::Label octaveLabel;
    juce::Slider octaveSlider;

    juce::Label durationLabel;
    juce::Slider durationSlider;

    juce::Label velocityLabel;
    juce::Slider velocitySlider;

    juce::Label styleLabel;
    juce::ComboBox styleComboBox;

    juce::Label arpSpeedLabel;
    juce::ComboBox arpSpeedComboBox;

    // Generate button
    juce::TextButton generateButton;

    //==============================================================================
    // Internal state
    std::vector<Chord> currentChords;
    bool parseScheduled = false;
    juce::String pendingText;

    //==============================================================================
    // Helper methods

    /**
     * Parse text and update chord display.
     */
    void parseAndUpdateChords();

    /**
     * Update chord display label with parsed chords.
     */
    void updateChordDisplay(const std::vector<Chord>& chords);

    /**
     * Update generate button enabled state based on chord availability.
     */
    void updateGenerateButtonState();

    /**
     * Update arp speed control enabled state based on style selection.
     */
    void updateArpSpeedControlState();

    /**
     * Initialize UI component appearance and properties.
     */
    void setupUIComponents();

    JUCE_DECLARE_NON_COPYABLE_WITH_LEAK_DETECTOR (ChordGeneratorEditor)
};
