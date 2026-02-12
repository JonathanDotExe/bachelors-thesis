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



First language:
 - Use 7 scale degrees to denote 0 - 6
 - Each measure is one instruction
 - 2 7ths - operation
 - 8 7ths - value => 7^8 = 5764801
 - 




