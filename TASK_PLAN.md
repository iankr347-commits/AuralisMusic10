# Task Plan: Minimum Player Screen with Synchronized Lyrics and Waveform Progress Bar

## Requirements
1. **Minimum player screen** - A compact player view
2. **1 line of synchronized lyrics** - Show current lyric line in sync with playback
3. **Song title** - Display current song title
4. **Artist name** - Display artist name
5. **Waveform progress bar at the very bottom** - Progress bar at the very bottom of the screen

## Implementation Steps

- [x] Analyze existing player code structure (MiniPlayer, BottomSheetPlayer, InlineLyricsView)
- [x] Check PreferenceKeys.kt for constants
- [x] Add new preference keys for minimal player mode
- [x] Add imports for new preference keys in Player.kt
- [x] Create a new MinimalPlayer composable that shows:
  - [x] Song title
  - [x] Artist name
  - [x] 1 line of synchronized lyrics (currentLyricLine)
  - [x] Waveform progress bar at the very bottom
- [x] Add configuration/preference for minimal player mode
- [x] Integrate minimal player mode into existing player flow
- [x] Ensure the waveform progress bar works with different slider styles (DEFAULT, SQUIGGLY, SLIM)
- [ ] Test with both local and casting playback

## Files Modified
1. `AURALIS_MUSIC_MAIN/app/src/main/kotlin/com/auralis/music/constants/PreferenceKeys.kt` - Added new preference keys (MinimalPlayerEnabledKey, MinimalPlayerHeight)
2. `AURALIS_MUSIC_MAIN/app/src/main/kotlin/com/auralis/music/ui/player/Player.kt` - Added MinimalPlayer composable with:
   - Song title display
   - Artist name display
   - 1 line of synchronized lyrics
   - Waveform progress bar (supports DEFAULT, SQUIGGLY, SLIM styles) at the very bottom
   - Time indicators
   - Tap to navigate to full player
   - Cast support
   - Theme-aware colors