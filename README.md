# AI Chord Generator - FL Studio VST3 Plugin

A VST3 plugin for FL Studio (and other DAWs) that generates MIDI chord progressions from text descriptions created by AI services like ChatGPT, Claude, or any other text-based AI assistant.

## Overview

The AI Chord Generator plugin allows you to:
- Paste chord progressions from AI-generated text responses
- Automatically parse chord names from natural language
- Generate MIDI output with customizable settings
- Create block chords or arpeggios
- Insert MIDI directly into your DAW

**Workflow:**
1. Ask an AI (ChatGPT, Claude, etc.) for a chord progression
2. Copy the AI's response
3. Paste into the plugin
4. Plugin parses and displays the chords
5. Adjust settings (octave, duration, velocity, style)
6. Click "Generate MIDI" to create the progression in your DAW

## Requirements

### Software Requirements

- **JUCE Framework**: Version 7.x or later ([download from juce.com](https://juce.com))
- **Projucer**: Included with JUCE framework
- **DAW**: FL Studio 20+ (or any VST3-compatible DAW)

### Platform-Specific Build Tools

**Windows:**
- Windows 10 or later
- Visual Studio 2019 or later (Community Edition is fine)
- Visual Studio C++ Desktop Development workload

**macOS:**
- macOS 10.13 or later
- Xcode 11 or later
- Command Line Tools for Xcode

**Linux:**
- GCC 7+ or Clang 6+
- Development libraries: `libasound2-dev`, `libcurl4-openssl-dev`, `libfreetype6-dev`, `libx11-dev`, `libxcomposite-dev`, `libxcursor-dev`, `libxext-dev`, `libxinerama-dev`, `libxrandr-dev`, `libxrender-dev`, `mesa-common-dev`

### Runtime Requirements

- C++17 compatible system
- VST3 plugin support in your DAW

## Building the Plugin

### Step 1: Download JUCE

1. Visit [juce.com](https://juce.com) and download JUCE 7.x (latest stable)
2. Extract JUCE to a location like `C:\JUCE` (Windows), `~/JUCE` (macOS/Linux)
3. Note the path to the JUCE modules directory (e.g., `C:\JUCE\modules`)

### Step 2: Configure the Project

1. Open Projucer (located in `JUCE/extras/Projucer/Builds/`)
2. Open `AIChordGenerator.jucer` from this repository
3. Update the JUCE module paths:
   - Click on each exporter (Visual Studio, Xcode, or Linux Makefile)
   - In "Module Paths", update the path to point to your JUCE modules directory
4. Configure plugin metadata (optional):
   - Company Name
   - Plugin Manufacturer Code (4 characters, e.g., "AcGn")
   - Plugin Code (4 characters, e.g., "AiCg")
5. Save the project in Projucer

### Step 3: Build the Plugin

#### Windows (Visual Studio)

1. In Projucer, click "Save and Open in IDE" and select Visual Studio
2. Visual Studio will open the generated solution
3. Select **Release** configuration and **x64** platform
4. Build → Build Solution (or press F7)
5. The VST3 file will be in: `Builds/VisualStudio2022/x64/Release/VST3/AI Chord Generator.vst3`

#### macOS (Xcode)

1. In Projucer, click "Save and Open in IDE" and select Xcode
2. Xcode will open the generated project
3. Select **Release** scheme
4. Product → Build (or press Cmd+B)
5. The VST3 file will be in: `Builds/MacOSX/build/Release/AI Chord Generator.vst3`

#### Linux (Makefile)

1. In Projucer, save the project to generate Linux Makefile
2. Open terminal and navigate to `Builds/LinuxMakefile/`
3. Run: `make CONFIG=Release`
4. The VST3 file will be in: `Builds/LinuxMakefile/build/AI Chord Generator.vst3`

### Step 4: Install the Plugin

Copy the built `.vst3` file to your system's VST3 plugin directory:

**Windows:**
```
C:\Program Files\Common Files\VST3\
```

**macOS:**
```
/Library/Audio/Plug-Ins/VST3/
or
~/Library/Audio/Plug-Ins/VST3/
```

**Linux:**
```
~/.vst3/
or
/usr/lib/vst3/
```

### Step 5: Verify Installation in FL Studio

1. Launch FL Studio
2. Go to **Options → Manage Plugins**
3. Click **Find plugins** to rescan
4. Look for "AI Chord Generator" in the plugin list
5. It should appear in the **Generators** category

## Usage

### Basic Workflow

1. **Create a new channel** in FL Studio
2. **Load the plugin** on that channel (Effects → AI Chord Generator)
3. **Ask an AI** for a chord progression:
   - Example prompt: "Give me a jazz progression in Dm with some tension"
4. **Copy the AI's response** (e.g., "Try Dm9, G13, Cmaj7, Fmaj7")
5. **Paste into the plugin's text area**
6. The plugin will automatically parse and display: `Dm9 - G13 - Cmaj7 - Fmaj7`
7. **Adjust settings**:
   - **Octave**: Base octave for the chords (0-8, default: 4)
   - **Duration**: Length of each chord in bars (0.25-16, default: 1)
   - **Velocity**: MIDI velocity for note intensity (1-127, default: 100)
   - **Style**: Block Chord (all notes at once) or Arpeggio (notes in sequence)
   - **Arp Speed**: Speed of arpeggiation (1/32, 1/16, 1/8, 1/4) - only active when Style is Arpeggio
8. **Arm the channel for recording** in FL Studio
9. **Click "Generate MIDI"**
10. MIDI notes appear in the piano roll
11. **Edit or adjust** the generated MIDI as needed

### Tips

- **Natural Language**: The parser is forgiving - you can paste full AI responses, and it will extract just the chord names
- **Multiple Formats**: Works with comma-separated (C, Dm, G7) or space-separated (C Dm G7) chords
- **Any AI Service**: Use ChatGPT, Claude, Gemini, local LLMs, or even type chords manually
- **Experimentation**: Try different styles and durations to find what sounds best for your track

## Supported Chord Types

The plugin recognizes a comprehensive set of chord types:

### Basic Triads
- **Major**: C, D, E, etc.
- **Minor**: Cm, Dmin, Em (both "m" and "min" notation supported)
- **Diminished**: Cdim, Ddim
- **Augmented**: Caug, Eaug

### Suspended Chords
- **Sus2**: Csus2, Dsus2
- **Sus4**: Csus4, Dsus4

### Seventh Chords
- **Major 7th**: Cmaj7, Dmaj7
- **Minor 7th**: Cmin7, Cm7
- **Dominant 7th**: C7, D7
- **Diminished 7th**: Cdim7
- **Half-diminished 7th**: Cmin7b5, Cm7b5

### Extended Chords
- **9th chords**: Cmaj9, Cmin9, C9
- **11th chords**: Cmaj11, C11
- **13th chords**: Cmaj13, C13

### Added Tone Chords
- **Add9**: Cadd9
- **Add11**: Cadd11

### Altered Chords
- **7#5**: C7#5 (dominant with raised 5th)
- **7b9**: C7b9 (dominant with flat 9th)
- **7#9**: C7#9 (dominant with sharp 9th)
- **7b5**: C7b5 (dominant with flat 5th)

### Slash Chords (Bass Notes)
- **Format**: C/E, Dm7/G, etc.
- The note after "/" becomes the bass note (played one octave below)

### Accidentals
- **Sharps**: C#, F#, G#
- **Flats**: Db, Eb, Bb

### Root Notes
A, A#/Bb, B, C, C#/Db, D, D#/Eb, E, F, F#/Gb, G, G#/Ab

## Troubleshooting

### Plugin doesn't appear in FL Studio
- Ensure you copied the .vst3 file to the correct directory
- Rescan plugins: Options → Manage Plugins → Find plugins
- Check the plugin is enabled in the plugin manager
- Restart FL Studio

### Build errors
- **JUCE not found**: Update module paths in Projucer to point to your JUCE installation
- **Compiler errors**: Ensure you have C++17 support enabled
- **Missing dependencies (Linux)**: Install required dev libraries listed in Requirements

### No MIDI output generated
- Ensure the channel is armed for recording in FL Studio
- Check that you clicked "Generate MIDI" button
- Verify chords were parsed (should appear in "Detected Chords" area)
- Try increasing velocity if notes are too quiet

### Chords not parsing correctly
- Ensure chord names are separated by spaces or commas
- Check spelling of chord names against supported types
- The plugin ignores filler words, so paste full AI responses
- Complex or unusual chord voicings may not be recognized

### Arp Speed greyed out
- This is normal when "Block Chord" style is selected
- Switch to "Arpeggio" style to enable Arp Speed control

## Technical Details

- **Plugin Format**: VST3
- **Framework**: JUCE 7.x
- **Language**: C++17
- **Audio Processing**: None (MIDI-only plugin)
- **MIDI Channel**: Channel 1
- **Thread Safety**: UI thread for parsing, audio thread for MIDI generation

## Project Structure

```
ChatApp/
├── Source/
│   ├── ChordDefinitions.h      # Chord interval mappings
│   ├── ChordParser.h/cpp       # Text parsing logic
│   ├── MidiGenerator.h/cpp     # MIDI event generation
│   ├── PluginProcessor.h/cpp   # Main audio processor
│   └── PluginEditor.h/cpp      # GUI interface
├── AIChordGenerator.jucer      # JUCE project configuration
├── README.md                   # This file
└── .gitignore                  # Git ignore rules
```

## License

MIT License

Copyright (c) 2025 AI Chord Generator

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

### Development Setup

1. Follow the building instructions above
2. Make changes to source files in `Source/`
3. Test thoroughly in FL Studio
4. Ensure code compiles on at least one platform
5. Submit pull request with clear description

## Support

For issues, questions, or feature requests, please open an issue on the GitHub repository.

## Acknowledgments

- Built with [JUCE Framework](https://juce.com)
- Designed for use with AI services like ChatGPT, Claude, and others
- Inspired by the need to rapidly prototype musical ideas from AI suggestions

---

**Version**: 1.0.0
**Last Updated**: 2025-11-01
