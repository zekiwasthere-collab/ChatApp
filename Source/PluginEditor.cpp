/*
  ==============================================================================

    PluginEditor.cpp
    Created: 2025-11-01
    Author: AI Chord Generator

    Implementation of GUI editor.

  ==============================================================================
*/

#include "PluginProcessor.h"
#include "PluginEditor.h"

//==============================================================================
ChordGeneratorEditor::ChordGeneratorEditor (ChordGeneratorProcessor& p)
    : AudioProcessorEditor (&p), audioProcessor (p)
{
    setupUIComponents();

    // Set initial size
    setSize (400, 550);
    setResizable(true, true);
    setResizeLimits(350, 500, 600, 800);
}

ChordGeneratorEditor::~ChordGeneratorEditor()
{
}

//==============================================================================
void ChordGeneratorEditor::paint (juce::Graphics& g)
{
    // Background color
    g.fillAll (juce::Colour(0xfff5f5f5));
}

void ChordGeneratorEditor::resized()
{
    auto area = getLocalBounds().reduced(10);

    // Header
    headerLabel.setBounds(area.removeFromTop(30));
    area.removeFromTop(5);

    // Text input section
    inputLabel.setBounds(area.removeFromTop(20));
    area.removeFromTop(3);
    textInput.setBounds(area.removeFromTop(80));
    area.removeFromTop(10);

    // Chord display section
    chordDisplayLabel.setBounds(area.removeFromTop(20));
    area.removeFromTop(3);
    chordDisplay.setBounds(area.removeFromTop(60));
    area.removeFromTop(15);

    // Settings section
    int rowHeight = 50;
    int labelWidth = 80;
    int controlWidth = area.getWidth() - labelWidth - 10;

    // Row 1: Octave
    auto row = area.removeFromTop(rowHeight);
    octaveLabel.setBounds(row.removeFromLeft(labelWidth));
    octaveSlider.setBounds(row.reduced(5));

    // Row 2: Duration
    row = area.removeFromTop(rowHeight);
    durationLabel.setBounds(row.removeFromLeft(labelWidth));
    durationSlider.setBounds(row.reduced(5));

    // Row 3: Velocity
    row = area.removeFromTop(rowHeight);
    velocityLabel.setBounds(row.removeFromLeft(labelWidth));
    velocitySlider.setBounds(row.reduced(5));

    // Row 4: Style
    row = area.removeFromTop(rowHeight);
    styleLabel.setBounds(row.removeFromLeft(labelWidth));
    styleComboBox.setBounds(row.reduced(5));

    // Row 5: Arp Speed
    row = area.removeFromTop(rowHeight);
    arpSpeedLabel.setBounds(row.removeFromLeft(labelWidth));
    arpSpeedComboBox.setBounds(row.reduced(5));

    area.removeFromTop(10);

    // Generate button
    generateButton.setBounds(area.removeFromTop(40));
}

//==============================================================================
void ChordGeneratorEditor::setupUIComponents()
{
    // Header
    headerLabel.setText("AI Chord Generator", juce::dontSendNotification);
    headerLabel.setFont(juce::Font(20.0f, juce::Font::bold));
    headerLabel.setJustificationType(juce::Justification::centred);
    addAndMakeVisible(headerLabel);

    // Input label
    inputLabel.setText("Paste AI Response:", juce::dontSendNotification);
    inputLabel.setFont(juce::Font(14.0f));
    addAndMakeVisible(inputLabel);

    // Text input
    textInput.setMultiLine(true);
    textInput.setReturnKeyStartsNewLine(true);
    textInput.setScrollbarsShown(true);
    textInput.setCaretVisible(true);
    textInput.setPopupMenuEnabled(true);
    textInput.setTextToShowWhenEmpty("Paste chord names here (e.g., Cmaj7 Dm7 G7)",
                                      juce::Colours::grey);
    textInput.addListener(this);
    addAndMakeVisible(textInput);

    // Chord display label
    chordDisplayLabel.setText("Detected Chords:", juce::dontSendNotification);
    chordDisplayLabel.setFont(juce::Font(14.0f));
    addAndMakeVisible(chordDisplayLabel);

    // Chord display
    chordDisplay.setText("", juce::dontSendNotification);
    chordDisplay.setFont(juce::Font(juce::Font::getDefaultMonospacedFontName(), 14.0f, juce::Font::plain));
    chordDisplay.setColour(juce::Label::backgroundColourId, juce::Colours::white);
    chordDisplay.setColour(juce::Label::outlineColourId, juce::Colours::grey);
    chordDisplay.setJustificationType(juce::Justification::topLeft);
    chordDisplay.setBorderSize(juce::BorderSize<int>(5));
    addAndMakeVisible(chordDisplay);

    // Octave control
    octaveLabel.setText("Octave:", juce::dontSendNotification);
    octaveLabel.setJustificationType(juce::Justification::centredLeft);
    addAndMakeVisible(octaveLabel);

    octaveSlider.setRange(0, 8, 1);
    octaveSlider.setValue(audioProcessor.getOctave(), juce::dontSendNotification);
    octaveSlider.setSliderStyle(juce::Slider::LinearHorizontal);
    octaveSlider.setTextBoxStyle(juce::Slider::TextBoxRight, false, 50, 20);
    octaveSlider.addListener(this);
    addAndMakeVisible(octaveSlider);

    // Duration control
    durationLabel.setText("Duration:", juce::dontSendNotification);
    durationLabel.setJustificationType(juce::Justification::centredLeft);
    addAndMakeVisible(durationLabel);

    durationSlider.setRange(0.25, 16.0, 0.25);
    durationSlider.setValue(audioProcessor.getDuration(), juce::dontSendNotification);
    durationSlider.setSliderStyle(juce::Slider::LinearHorizontal);
    durationSlider.setTextBoxStyle(juce::Slider::TextBoxRight, false, 60, 20);
    durationSlider.setTextValueSuffix(" bars");
    durationSlider.addListener(this);
    addAndMakeVisible(durationSlider);

    // Velocity control
    velocityLabel.setText("Velocity:", juce::dontSendNotification);
    velocityLabel.setJustificationType(juce::Justification::centredLeft);
    addAndMakeVisible(velocityLabel);

    velocitySlider.setRange(1, 127, 1);
    velocitySlider.setValue(audioProcessor.getVelocity(), juce::dontSendNotification);
    velocitySlider.setSliderStyle(juce::Slider::LinearHorizontal);
    velocitySlider.setTextBoxStyle(juce::Slider::TextBoxRight, false, 50, 20);
    velocitySlider.addListener(this);
    addAndMakeVisible(velocitySlider);

    // Style control
    styleLabel.setText("Style:", juce::dontSendNotification);
    styleLabel.setJustificationType(juce::Justification::centredLeft);
    addAndMakeVisible(styleLabel);

    styleComboBox.addItem("Block Chord", 1);
    styleComboBox.addItem("Arpeggio", 2);
    styleComboBox.setSelectedId(audioProcessor.getPlayStyle() == PlayStyle::BlockChord ? 1 : 2,
                                 juce::dontSendNotification);
    styleComboBox.addListener(this);
    addAndMakeVisible(styleComboBox);

    // Arp Speed control
    arpSpeedLabel.setText("Arp Speed:", juce::dontSendNotification);
    arpSpeedLabel.setJustificationType(juce::Justification::centredLeft);
    addAndMakeVisible(arpSpeedLabel);

    arpSpeedComboBox.addItem("1/32", 1);
    arpSpeedComboBox.addItem("1/16", 2);
    arpSpeedComboBox.addItem("1/8", 3);
    arpSpeedComboBox.addItem("1/4", 4);

    int arpSpeedId = 2; // Default to 1/16
    switch (audioProcessor.getArpSpeed())
    {
        case ArpSpeed::ThirtySecond: arpSpeedId = 1; break;
        case ArpSpeed::Sixteenth:    arpSpeedId = 2; break;
        case ArpSpeed::Eighth:       arpSpeedId = 3; break;
        case ArpSpeed::Quarter:      arpSpeedId = 4; break;
    }
    arpSpeedComboBox.setSelectedId(arpSpeedId, juce::dontSendNotification);
    arpSpeedComboBox.addListener(this);
    addAndMakeVisible(arpSpeedComboBox);

    // Update arp speed state based on style
    updateArpSpeedControlState();

    // Generate button
    generateButton.setButtonText("Generate MIDI");
    generateButton.addListener(this);
    generateButton.setEnabled(false); // Disabled until chords are parsed
    addAndMakeVisible(generateButton);
}

//==============================================================================
// Listener implementations

void ChordGeneratorEditor::textEditorTextChanged (juce::TextEditor&)
{
    // Schedule parse with debounce
    pendingText = textInput.getText();
    parseScheduled = true;
    startTimer(300); // 300ms debounce
}

void ChordGeneratorEditor::sliderValueChanged (juce::Slider* slider)
{
    if (slider == &octaveSlider)
    {
        audioProcessor.setOctave(static_cast<int>(slider->getValue()));
    }
    else if (slider == &durationSlider)
    {
        audioProcessor.setDuration(slider->getValue());
    }
    else if (slider == &velocitySlider)
    {
        audioProcessor.setVelocity(static_cast<int>(slider->getValue()));
    }
}

void ChordGeneratorEditor::comboBoxChanged (juce::ComboBox* comboBox)
{
    if (comboBox == &styleComboBox)
    {
        int selectedId = styleComboBox.getSelectedId();
        audioProcessor.setPlayStyle(selectedId == 1 ? PlayStyle::BlockChord : PlayStyle::Arpeggio);
        updateArpSpeedControlState();
    }
    else if (comboBox == &arpSpeedComboBox)
    {
        int selectedId = arpSpeedComboBox.getSelectedId();
        ArpSpeed speed = ArpSpeed::Sixteenth; // Default

        switch (selectedId)
        {
            case 1: speed = ArpSpeed::ThirtySecond; break;
            case 2: speed = ArpSpeed::Sixteenth; break;
            case 3: speed = ArpSpeed::Eighth; break;
            case 4: speed = ArpSpeed::Quarter; break;
        }

        audioProcessor.setArpSpeed(speed);
    }
}

void ChordGeneratorEditor::buttonClicked (juce::Button* button)
{
    if (button == &generateButton)
    {
        // Trigger MIDI generation
        audioProcessor.triggerMidiGeneration();

        // Update button text temporarily
        generateButton.setButtonText("Generating...");
        generateButton.setEnabled(false);

        // Re-enable after a short delay
        juce::Timer::callAfterDelay(1000, [this]()
        {
            generateButton.setButtonText("Generate MIDI");
            updateGenerateButtonState();
        });
    }
}

void ChordGeneratorEditor::timerCallback()
{
    stopTimer();

    if (parseScheduled)
    {
        parseScheduled = false;
        parseAndUpdateChords();
    }
}

//==============================================================================
// Helper methods

void ChordGeneratorEditor::parseAndUpdateChords()
{
    // Parse chord progression from text
    currentChords = ChordParser::parseChordProgression(pendingText);

    // Update processor with new chords
    audioProcessor.updateChordProgression(currentChords);

    // Update display
    updateChordDisplay(currentChords);

    // Update button state
    updateGenerateButtonState();
}

void ChordGeneratorEditor::updateChordDisplay(const std::vector<Chord>& chords)
{
    if (chords.empty())
    {
        chordDisplay.setText("No chords detected. Paste chord names (e.g., Cmaj7 Dm7 G7)",
                            juce::dontSendNotification);
        chordDisplay.setColour(juce::Label::textColourId, juce::Colour(0xffd32f2f)); // Red for error
    }
    else
    {
        // Build display string
        juce::String displayText;
        for (size_t i = 0; i < chords.size(); ++i)
        {
            displayText += chords[i].getDisplayName();
            if (i < chords.size() - 1)
                displayText += " - ";
        }

        chordDisplay.setText(displayText, juce::dontSendNotification);
        chordDisplay.setColour(juce::Label::textColourId, juce::Colour(0xff333333)); // Normal color
    }
}

void ChordGeneratorEditor::updateGenerateButtonState()
{
    // Enable button only if we have chords and not currently generating
    bool hasChords = !currentChords.empty();
    bool isGenerating = audioProcessor.isGenerating();

    generateButton.setEnabled(hasChords && !isGenerating);
}

void ChordGeneratorEditor::updateArpSpeedControlState()
{
    // Enable arp speed only when style is Arpeggio
    bool isArpeggio = audioProcessor.getPlayStyle() == PlayStyle::Arpeggio;

    arpSpeedComboBox.setEnabled(isArpeggio);
    arpSpeedLabel.setEnabled(isArpeggio);

    // Visual feedback: gray out when disabled
    float alpha = isArpeggio ? 1.0f : 0.5f;
    arpSpeedComboBox.setAlpha(alpha);
    arpSpeedLabel.setAlpha(alpha);
}
