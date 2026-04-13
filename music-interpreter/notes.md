- esolangs.org/wiki/Velato
- https://velato.net/



Minimum Viable Product
- Interpreter für MusicXMl based language
- Expression stack based
- Interpreter given/edited
- Focus is on designing the language to "sound good"
- Playback while executing program
  - Advanced: vizualization in score/expression stack, etc.
- Turing Complete

Thoughts/Questions
- immediate mapping from MusicXML command units to OPCODES or inbetween layer?
  - e.g. abstract away expression stack, allow for more tradtitional programmin like structures
  - => i would tend torwards direct mapping
- How are jumps/loops handled
  - simply through instructions => most flexibitlity in programm, nesting
    - playback wouldn't necesarilit be equal to how the piece would normally be played
  - through repetitions, da capo, etc. => 1:1 correspondence of musical structure
- how to separate instructions
  - per measure?
  - fixed length for instructions => ignore measures?
- what do we consider?
  - pitch (class)
  - note length
  - measures
- how to make pieces sound good?
  - only consider notes/intervals relative to key signature and ignore sharps/flats => only 7 options (8 would be ideal because bit)
    - try to avoid natural tritone in major scale in commands

Example encoding:
"bits" - scale degree
0 - 1 C
1 - 2 D
2 - 3 E
3 - 4 F
4 - 5 G
5 - 6 A
6 - 7 H



Current progress 13.03.2026
- Interpreter
  - interprets our bytecode
  - negative opcodes mark measure ids and are ignored
  - a measure callback is called so the current measure can be tracked during execution
    - e.g. for playing back the measures
    - [done] check if jumps are correctly placed so that the jumps jump to the position before the measures
- Music compiler
  - Transforms music xml into bytecode of our interpreter language
  - Language definition
    - Use 7 scale degrees to denote 0 - 6
      - key signature marking are analized to determine the root
      - sharps and flats are ignored => more musical flexibility
    - Each measure is one instruction
    - 2 7-bytes - operation
      - empty bars/only one note => ignored
    - arbitrary amount of 7-bytes afterwards => argument, flexible interger size
      - if no argument is needed, rest is ignored
      - grace notes at the start of a number => negative number, otherwise ignored
    - only primary part and primary voice are considered
    - in the case of chords in a single voice, the to note (first highest line position, then true pitch after alterations if line is the same)
    - rests are ignored
    - double barlines (heavy-heavy or light-light) denote labels
      - all labels are indexed by ascending ids starting at 0
      - numerical argument for JMP denotes the label index
- Music decompiler
  - Transforms bytecode into music xml
  - all in c major
  - one measure per instruction
  - all eight notes
    - TODO: dynamically scale note values according to length of numbers
  - jumps are converted to barline labels correctly
  - fill up empty space with rests each bar

Future TODOs:
- check a lot of assumptions against music xml standard
- write a lot of test programs
- => use test results to find more musically interesting opcodes
  - maybe modulo assignment of opcodes?
  - other idea first note of bar denotes reference point not key signature?
- write a midi player and use it to play the measures
- support triplets, dotted and bound notes in MIDI player (divisions/durations)
- optional: support ScoreTimewise




