/*
  ==============================================================================

    ChordParser.cpp
    Created: 2025-11-01
    Author: AI Chord Generator

    Implementation of chord parsing logic.

  ==============================================================================
*/

#include "ChordParser.h"
#include "ChordDefinitions.h"
#include <regex>

// Filler words to ignore during parsing
static const std::vector<juce::String> FILLER_WORDS = {
    "the", "a", "an", "here's", "heres", "try", "this", "progression",
    "chord", "chords", "following", "these", "use", "play", "with",
    "in", "key", "of", "for", "and", "or", "is", "are", "be",
    "to", "from", "at", "on", "by", "as", "it", "that", "which"
};

std::vector<Chord> ChordParser::parseChordProgression(const juce::String& text)
{
    std::vector<Chord> chords;

    if (text.trim().isEmpty())
        return chords;

    // Tokenize by spaces, commas, newlines, and common separators
    juce::StringArray tokens;
    tokens.addTokens(text, " ,\n\r\t;|", "\"");

    for (const auto& token : tokens)
    {
        juce::String cleanToken = token.trim();

        // Skip empty tokens
        if (cleanToken.isEmpty())
            continue;

        // Skip filler words
        if (isFillerWord(cleanToken))
            continue;

        // Skip pure numbers and bullets
        if (cleanToken.containsOnly("0123456789.-*"))
            continue;

        // Remove trailing punctuation (but keep # and b for accidentals)
        while (cleanToken.isNotEmpty() &&
               (cleanToken.getLastCharacter() == '.' ||
                cleanToken.getLastCharacter() == ':' ||
                cleanToken.getLastCharacter() == '!' ||
                cleanToken.getLastCharacter() == '?'))
        {
            cleanToken = cleanToken.dropLastCharacters(1);
        }

        // Check if it matches a chord pattern
        if (isChordPattern(cleanToken))
        {
            Chord chord = parseChordName(cleanToken);
            if (chord.rootNote.isNotEmpty())
            {
                chords.push_back(chord);

                // Limit to 100 chords as per edge cases specification
                if (chords.size() >= 100)
                {
                    DBG("Chord progression limited to 100 chords");
                    break;
                }
            }
        }
    }

    return chords;
}

bool ChordParser::isChordPattern(const juce::String& token)
{
    // Regex pattern for chord matching
    // Root note: [A-G] followed by optional # or b
    // Quality: optional maj, min, m, dim, aug, sus
    // Extensions: optional numbers and symbols
    // Slash chord: optional /[A-G][#b]?

    std::string str = token.toStdString();

    // Comprehensive chord pattern
    std::regex chordPattern(
        "^[A-G][#b]?"                           // Root note (required)
        "("                                      // Quality group
            "maj|min|m|dim|aug|sus|add|dom"      // Quality keywords
        ")?"
        "("                                      // Extensions group
            "[0-9]+|"                            // Numbers (7, 9, 11, 13)
            "b[0-9]+|"                           // Flat extensions (b5, b9)
            "#[0-9]+"                            // Sharp extensions (#5, #9)
        ")*"
        "("                                      // Optional slash chord
            "/[A-G][#b]?"
        ")?$",
        std::regex::icase
    );

    return std::regex_match(str, chordPattern);
}

Chord ChordParser::parseChordName(const juce::String& token)
{
    // Extract root note
    auto [rootNote, remaining] = extractRootNote(token);

    if (rootNote.isEmpty() || !ChordDefinitions::isValidNoteName(rootNote))
        return Chord(); // Invalid chord

    // Check for slash chord (bass note)
    juce::String bassNote;
    int slashIndex = remaining.indexOf("/");
    if (slashIndex >= 0)
    {
        bassNote = remaining.substring(slashIndex + 1).trim();
        remaining = remaining.substring(0, slashIndex);
    }

    // Extract quality and extensions
    auto [quality, extensions] = extractQualityAndExtensions(remaining);

    // If no quality specified, assume major
    if (quality.isEmpty() && extensions.isEmpty())
        quality = "major";

    Chord chord(rootNote, quality, extensions, bassNote);
    chord.originalName = token; // Preserve original formatting

    return chord;
}

bool ChordParser::isFillerWord(const juce::String& token)
{
    juce::String lowerToken = token.toLowerCase();

    for (const auto& filler : FILLER_WORDS)
    {
        if (lowerToken == filler)
            return true;
    }

    return false;
}

std::pair<juce::String, juce::String> ChordParser::extractRootNote(const juce::String& chordStr)
{
    if (chordStr.isEmpty())
        return {"", ""};

    // Root note is first character (A-G)
    juce::String rootNote = chordStr.substring(0, 1).toUpperCase();

    juce::String remaining = chordStr.substring(1);

    // Check for accidental (# or b)
    if (remaining.isNotEmpty() &&
        (remaining[0] == '#' || remaining[0] == 'b'))
    {
        rootNote += remaining.substring(0, 1);
        remaining = remaining.substring(1);
    }

    return {rootNote, remaining};
}

std::pair<juce::String, juce::String> ChordParser::extractQualityAndExtensions(const juce::String& remaining)
{
    if (remaining.isEmpty())
        return {"major", ""};

    juce::String quality;
    juce::String extensions;

    juce::String str = remaining;

    // Check for quality keywords at the start
    if (str.startsWithIgnoreCase("maj7"))
    {
        quality = "maj7";
        str = str.substring(4);
    }
    else if (str.startsWithIgnoreCase("maj"))
    {
        quality = "major";
        str = str.substring(3);
    }
    else if (str.startsWithIgnoreCase("min7b5") || str.startsWithIgnoreCase("m7b5"))
    {
        quality = "min7b5";
        str = str.substring(str.startsWithIgnoreCase("min7b5") ? 6 : 4);
    }
    else if (str.startsWithIgnoreCase("min7") || str.startsWithIgnoreCase("m7"))
    {
        quality = "min7";
        str = str.substring(str.startsWithIgnoreCase("min7") ? 4 : 2);
    }
    else if (str.startsWithIgnoreCase("min9") || str.startsWithIgnoreCase("m9"))
    {
        quality = "min9";
        str = str.substring(str.startsWithIgnoreCase("min9") ? 4 : 2);
    }
    else if (str.startsWithIgnoreCase("min") || str.startsWithIgnoreCase("m"))
    {
        quality = "minor";
        str = str.substring(str.startsWithIgnoreCase("min") ? 3 : 1);
    }
    else if (str.startsWithIgnoreCase("dim7"))
    {
        quality = "dim7";
        str = str.substring(4);
    }
    else if (str.startsWithIgnoreCase("dim"))
    {
        quality = "dim";
        str = str.substring(3);
    }
    else if (str.startsWithIgnoreCase("aug"))
    {
        quality = "aug";
        str = str.substring(3);
    }
    else if (str.startsWithIgnoreCase("sus4"))
    {
        quality = "sus4";
        str = str.substring(4);
    }
    else if (str.startsWithIgnoreCase("sus2"))
    {
        quality = "sus2";
        str = str.substring(4);
    }
    else if (str.startsWithIgnoreCase("add9"))
    {
        quality = "add9";
        str = str.substring(4);
    }
    else if (str.startsWithIgnoreCase("add11"))
    {
        quality = "add11";
        str = str.substring(5);
    }
    else if (str.startsWithIgnoreCase("dom7"))
    {
        quality = "7";
        str = str.substring(4);
    }
    else if (str.startsWithIgnoreCase("dom9"))
    {
        quality = "9";
        str = str.substring(4);
    }
    else if (str.startsWith("7#5"))
    {
        quality = "7#5";
        str = str.substring(3);
    }
    else if (str.startsWith("7b9"))
    {
        quality = "7b9";
        str = str.substring(3);
    }
    else if (str.startsWith("7#9"))
    {
        quality = "7#9";
        str = str.substring(3);
    }
    else if (str.startsWith("7b5"))
    {
        quality = "7b5";
        str = str.substring(3);
    }
    else if (str.startsWith("13"))
    {
        quality = "13";
        str = str.substring(2);
    }
    else if (str.startsWith("11"))
    {
        quality = "11";
        str = str.substring(2);
    }
    else if (str.startsWith("9"))
    {
        quality = "9";
        str = str.substring(1);
    }
    else if (str.startsWith("7"))
    {
        quality = "7";
        str = str.substring(1);
    }
    else
    {
        // No quality keyword found, assume major
        quality = "major";
    }

    // Remaining string is extensions
    extensions = str;

    return {quality, extensions};
}
