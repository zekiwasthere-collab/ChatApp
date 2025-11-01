/*
  ==============================================================================

    PluginProcessor.cpp
    Created: 2025-11-01
    Author: AI Chord Generator

    Implementation of main audio processor.

  ==============================================================================
*/

#include "PluginProcessor.h"
#include "PluginEditor.h"

//==============================================================================
ChordGeneratorProcessor::ChordGeneratorProcessor()
     : AudioProcessor (BusesProperties()
                       .withOutput ("Output", juce::AudioChannelSet::stereo(), true)
                       )
{
    // Plugin is a MIDI generator, no audio processing needed
}

ChordGeneratorProcessor::~ChordGeneratorProcessor()
{
}

//==============================================================================
const juce::String ChordGeneratorProcessor::getName() const
{
    return JucePlugin_Name;
}

bool ChordGeneratorProcessor::acceptsMidi() const
{
    return false;
}

bool ChordGeneratorProcessor::producesMidi() const
{
    return true;
}

bool ChordGeneratorProcessor::isMidiEffect() const
{
    return false;
}

double ChordGeneratorProcessor::getTailLengthSeconds() const
{
    return 0.0;
}

int ChordGeneratorProcessor::getNumPrograms()
{
    return 1;
}

int ChordGeneratorProcessor::getCurrentProgram()
{
    return 0;
}

void ChordGeneratorProcessor::setCurrentProgram (int index)
{
    juce::ignoreUnused(index);
}

const juce::String ChordGeneratorProcessor::getProgramName (int index)
{
    juce::ignoreUnused(index);
    return {};
}

void ChordGeneratorProcessor::changeProgramName (int index, const juce::String& newName)
{
    juce::ignoreUnused(index, newName);
}

//==============================================================================
void ChordGeneratorProcessor::prepareToPlay (double sampleRate, int samplesPerBlock)
{
    currentSampleRate = sampleRate;
    currentSamplesPerBlock = samplesPerBlock;
}

void ChordGeneratorProcessor::releaseResources()
{
    // Nothing to release
}

bool ChordGeneratorProcessor::isBusesLayoutSupported (const BusesLayout& layouts) const
{
    // This plugin doesn't process audio, but needs at least one output bus for some hosts
    return layouts.getMainOutputChannelSet() == juce::AudioChannelSet::stereo()
        || layouts.getMainOutputChannelSet() == juce::AudioChannelSet::mono();
}

void ChordGeneratorProcessor::processBlock (juce::AudioBuffer<float>& buffer, juce::MidiBuffer& midiMessages)
{
    juce::ScopedNoDenormals noDenormals;

    // Clear audio buffer (this is a MIDI-only plugin)
    buffer.clear();

    // If generation is not active, nothing to do
    if (!generationActive)
        return;

    // Check if we have chords to generate
    if (chordProgression.empty() || currentChordIndex >= static_cast<int>(chordProgression.size()))
    {
        // Finished generating all chords
        generationActive = false;
        currentChordIndex = 0;
        pendingMidiMessages.clear();
        return;
    }

    int bufferSize = buffer.getNumSamples();
    int samplePosition = 0;

    // Calculate samples per beat (quarter note)
    // Assume 120 BPM if no tempo info available
    double bpm = 120.0;

    // Try to get tempo from host
    juce::AudioPlayHead* playHead = getPlayHead();
    if (playHead != nullptr)
    {
        juce::AudioPlayHead::CurrentPositionInfo posInfo;
        if (playHead->getCurrentPosition(posInfo))
        {
            if (posInfo.bpm > 0)
                bpm = posInfo.bpm;
        }
    }

    int samplesPerBeat = static_cast<int>((60.0 / bpm) * currentSampleRate);

    // Process pending MIDI messages first
    if (!pendingMidiMessages.empty())
    {
        for (const auto& msg : pendingMidiMessages)
        {
            int sampleOffset = static_cast<int>(msg.getTimeStamp()) - pendingMessageStartSample;
            if (sampleOffset >= 0 && sampleOffset < bufferSize)
            {
                midiMessages.addEvent(msg, sampleOffset);
            }
        }
        pendingMidiMessages.clear();
    }

    // Generate MIDI for current chord if just starting
    if (samplesUntilNextChord <= 0)
    {
        // Get current chord
        const Chord& currentChord = chordProgression[currentChordIndex];

        // Convert duration from bars to beats (1 bar = 4 beats in 4/4 time)
        double durationInBeats = duration * 4.0;

        // Calculate duration in samples
        int durationInSamples = static_cast<int>(durationInBeats * samplesPerBeat);
        samplesUntilNextChord = durationInSamples;

        // Generate MIDI messages for this chord
        std::vector<juce::MidiMessage> chordMessages = MidiGenerator::generateMidiForChord(
            currentChord,
            octave,
            durationInBeats,
            velocity,
            playStyle,
            arpSpeed,
            currentSampleRate,
            samplesPerBeat,
            samplePosition
        );

        // Add messages to MIDI buffer
        for (const auto& msg : chordMessages)
        {
            int sampleOffset = static_cast<int>(msg.getTimeStamp());

            if (sampleOffset < bufferSize)
            {
                // Message fits in current buffer
                midiMessages.addEvent(msg, sampleOffset);
            }
            else
            {
                // Message needs to be sent in future buffer
                pendingMidiMessages.push_back(msg);
                pendingMessageStartSample = 0; // Will be adjusted in next process block
            }
        }

        // Move to next chord
        currentChordIndex++;
    }

    // Decrement samples until next chord
    samplesUntilNextChord -= bufferSize;

    // Check if we've finished all chords
    if (currentChordIndex >= static_cast<int>(chordProgression.size()) && samplesUntilNextChord <= 0)
    {
        generationActive = false;
        currentChordIndex = 0;
    }
}

//==============================================================================
bool ChordGeneratorProcessor::hasEditor() const
{
    return true;
}

juce::AudioProcessorEditor* ChordGeneratorProcessor::createEditor()
{
    return new ChordGeneratorEditor (*this);
}

//==============================================================================
void ChordGeneratorProcessor::getStateInformation (juce::MemoryBlock& destData)
{
    // Save plugin parameters (not chord progression or text input - those are ephemeral)
    juce::XmlElement xml("AIChordGeneratorState");

    xml.setAttribute("octave", octave);
    xml.setAttribute("duration", duration);
    xml.setAttribute("velocity", velocity);
    xml.setAttribute("playStyle", static_cast<int>(playStyle));
    xml.setAttribute("arpSpeed", static_cast<int>(arpSpeed));

    copyXmlToBinary(xml, destData);
}

void ChordGeneratorProcessor::setStateInformation (const void* data, int sizeInBytes)
{
    // Restore plugin parameters
    std::unique_ptr<juce::XmlElement> xml(getXmlFromBinary(data, sizeInBytes));

    if (xml != nullptr && xml->hasTagName("AIChordGeneratorState"))
    {
        octave = xml->getIntAttribute("octave", 4);
        duration = xml->getDoubleAttribute("duration", 1.0);
        velocity = xml->getIntAttribute("velocity", 100);
        playStyle = static_cast<PlayStyle>(xml->getIntAttribute("playStyle", 0));
        arpSpeed = static_cast<ArpSpeed>(xml->getIntAttribute("arpSpeed", 1));
    }
}

//==============================================================================
// Plugin-specific methods

void ChordGeneratorProcessor::updateChordProgression(const std::vector<Chord>& chords)
{
    chordProgression = chords;

    // Reset generation state
    generationActive = false;
    currentChordIndex = 0;
    samplesUntilNextChord = 0;
    pendingMidiMessages.clear();
}

void ChordGeneratorProcessor::triggerMidiGeneration()
{
    if (chordProgression.empty())
    {
        DBG("Cannot generate MIDI: no chords in progression");
        return;
    }

    if (generationActive)
    {
        DBG("Generation already in progress, ignoring trigger");
        return;
    }

    // Start generation
    generationActive = true;
    currentChordIndex = 0;
    samplesUntilNextChord = 0;
    pendingMidiMessages.clear();

    DBG("Starting MIDI generation for " + juce::String(chordProgression.size()) + " chords");
}

//==============================================================================
// This creates new instances of the plugin
juce::AudioProcessor* JUCE_CALLTYPE createPluginFilter()
{
    return new ChordGeneratorProcessor();
}
